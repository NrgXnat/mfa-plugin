//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>

package com.radiologics.mfa.filter;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
import com.radiologics.mfa.manager.MFAStrategyManager;
import com.radiologics.mfa.preference.MFAPreferences;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Holds a session that has authenticated with a password, but has not yet passed multi-factor authentication, on the
 * MFA pages until it does.
 *
 * <p>Every path this filter compares is the path within the application: the context path is stripped off, so the
 * plugin works the same on a ROOT deployment and on one under a context path such as {@code /xnat}. Redirects add the
 * context path back. Paths are compared for equality, never by suffix, because Turbine reads trailing path segments
 * as parameters and a longer path is therefore a request for something other than the page it starts with.</p>
 *
 * <p>This filter runs immediately before XNAT's {@code XnatExpiredPasswordFilter}, which sends a user who must change
 * their password to the password page. Without coordination the two filters redirect each other's pages in a loop, and
 * neither the password change nor the MFA check can finish. See {@link #isForcedPasswordChange} and
 * {@link #deferExpiryCheck}.</p>
 */
@Component
@PropertySource("classpath:/config/mfa/xnat-mfa.properties")
@Slf4j
public class MultifactorAuthenticationFilter extends OncePerRequestFilter {
    private final AliasTokenService                aliasTokenService;
    private final MultifactorAuthenticationService multifactorAuthenticationService;
    private final MFAPreferences                   mfaPreferences;
    private final String                           errorPath;
    private final String                           changePasswordPath;
    private final String                           changePasswordAction;
    private final Set<String>                      passwordChangePaths;

    public MultifactorAuthenticationFilter(final MultifactorAuthenticationService multifactorAuthenticationService,
                                           final MFAPreferences mfaPreferences,
                                           final AliasTokenService aliasTokenService,
                                           @Value("${mfa.errorPath}") final String errorPath,
                                           @Value("${mfa.changePasswordPath}") final String changePasswordPath,
                                           @Value("${mfa.changePasswordAction}") final String changePasswordAction,
                                           @Value("${mfa.loginPath}") final String loginPath,
                                           @Value("${mfa.loginAction}") final String loginAction,
                                           @Value("${mfa.logoutAction}") final String logoutAction) {
        this.multifactorAuthenticationService = multifactorAuthenticationService;
        this.aliasTokenService                = aliasTokenService;
        this.mfaPreferences                   = mfaPreferences;
        this.errorPath                        = errorPath;
        this.changePasswordPath               = changePasswordPath;
        this.changePasswordAction             = changePasswordAction;

        // The same set XnatExpiredPasswordFilter lets through while a password change is pending. Both filters have to
        // agree on it, or they redirect each other's pages forever.
        this.passwordChangePaths = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                changePasswordPath, changePasswordAction, loginPath, loginAction, logoutAction)));
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        final HttpSession httpSession = request.getSession(false);
        if (null == httpSession) {
            filterChain.doFilter(request, response);
            return;
        }

        final UserI user = currentUser();
        if (null == user) {
            filterChain.doFilter(request, response);
            return;
        }

        final String path = getPathWithinApplication(request);

        if (isAliasToken(request)) {
            // Requests using an alias token should bypass MFA.
            // Set the token as verified here even though it isn't. This way they won't get blocked if they
            // request a jsessionid using an alias token and then try to pass the cookie in subsequent requests.
            httpSession.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
            filterChain.doFilter(request, response);
            return;
        }

        final boolean mfaRequired      = BooleanUtils.toBoolean(multifactorAuthenticationService.isMFARequired(user.getUsername()));
        final boolean mfaTokenVerified = BooleanUtils.toBoolean((Boolean) httpSession.getAttribute(MFAConstants.MFA_TOKEN_VERIFIED));

        log.debug("MultifactorAuthenticationFilter.doFilterInternal(): path: {}, mfaRequired: {}, mfaTokenVerified: {}, userLogin: {}",
                  path, mfaRequired, mfaTokenVerified, user.getLogin());

        // Let them in if MFA doesn't apply or the token has been verified.
        if (!mfaRequired || mfaTokenVerified) {
            restoreExpiryCheck(httpSession);
            filterChain.doFilter(request, response);
            return;
        }

        if (user.isGuest()) {
            // A guest has no MFA to complete, so the MFA pages would only render an empty form for them.
            if (isMfaPage(path)) {
                redirect(request, response, mfaPreferences.getMfaRedirectPath());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // From here the user has authenticated with a password but has not passed MFA.

        MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
        if (null == mfe) {
            mfe = multifactorAuthenticationService.createMfe(user.getUsername());
        }

        if (mfe.isMfaExempted()) {
            restoreExpiryCheck(httpSession);
            filterChain.doFilter(request, response);
            return;
        }

        // XNAT's expired-password filter runs next and would send every request below, including the MFA pages and
        // their scripts, to the password page. Hold its check until MFA is done, then restore it so XNAT can ask for
        // the new password on the first request after that.
        deferExpiryCheck(httpSession);

        // The MFA pages are ordinary XNAT pages, so they need XNAT's scripts and styles. These are public: a user who
        // has not logged in at all can already request them.
        if (isStaticAsset(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // A password reset link or a reactivation link logs the user in with a forced password change. The link proves
        // the user controls the account's email, and XNAT ends the session as soon as the password changes, so let the
        // password change finish and ask for MFA at the next login. Until then this session can reach nothing else.
        if (isForcedPasswordChange(httpSession)) {
            if (passwordChangePaths.contains(path)) {
                filterChain.doFilter(request, response);
            } else {
                redirect(request, response, changePasswordPath);
            }
            return;
        }

        // The endpoints the verification page itself calls.
        if (isUriAllowed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        final MFAStrategyI mfaStrategy;
        try {
            restorePreferredMfa(httpSession, mfe);
            mfaStrategy = multifactorAuthenticationService.getPreferredMFAStrategy(user.getUsername());
        } catch (MFAStrategyNotFoundException e) {
            log.error("Failed to retrieve mfaStrategy for user: {}.", user.getUsername(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Multi-factor authentication is not configured correctly. Please contact your site administrator.");
            return;
        }

        if (isErrorPage(httpSession, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String registrationPath = mfaStrategy.getRegistrationTemplatePath();
        if (mfaStrategy.needsRegistration() && !mfe.isMfaRegistered() && StringUtils.isNotBlank(registrationPath)) {
            if (StringUtils.equals(path, registrationPath)) {
                filterChain.doFilter(request, response);
            } else {
                redirect(request, response, registrationPath);
            }
            return;
        }

        // Send one code per session. A page makes several requests at once, and each code replaces the one before it,
        // so sending a code per request leaves the user with a mailbox of codes of which only the last one works.
        synchronized (WebUtils.getSessionMutex(httpSession)) {
            if (!BooleanUtils.toBooleanDefaultIfNull((Boolean) httpSession.getAttribute(MFAConstants.MFA_TOKEN_SENT), false)) {
                httpSession.setAttribute(MFAConstants.MFA_TOKEN_SENT, true);
                try {
                    multifactorAuthenticationService.sendCode(user);
                } catch (Exception e) {
                    log.error("Failed to send a multi-factor authentication code to user: {}.", user.getUsername(), e);
                    httpSession.setAttribute(MFAConstants.MFA_TOKEN_SENT, false);
                    httpSession.setAttribute(MFAConstants.MFA_ERROR, true);
                    redirect(request, response, mfaStrategy.getErrorTemplatePath());
                    return;
                }
            }
        }

        final String verificationPath = mfaStrategy.getVerificationTemplatePath();
        if (StringUtils.equals(path, verificationPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Redirecting to the verification page for the strategy: {}", verificationPath);
        redirect(request, response, verificationPath);
    }

    /**
     * Returns the user this request is authenticated as, or null if there isn't one. Overridable for testing.
     *
     * @return The user this request is authenticated as.
     */
    protected UserI currentUser() {
        return XDAT.getUserDetails();
    }

    /**
     * Returns the request's path within the application, with the context path and any session ID removed.
     *
     * <p>This is built from the servlet path and path info rather than from the request URI, because the container has
     * already decoded and normalized those: a request for {@code /app/../xapi/users} arrives here as
     * {@code /xapi/users}, and so can't be passed off as a request for an allowed page.</p>
     *
     * @param request The request to read.
     *
     * @return The request's path within the application.
     */
    private static String getPathWithinApplication(final HttpServletRequest request) {
        final String servletPath = StringUtils.defaultString(request.getServletPath());
        final String pathInfo    = StringUtils.defaultString(request.getPathInfo());
        final String path        = servletPath + pathInfo;
        return StringUtils.defaultIfBlank(path, "/");
    }

    /**
     * Sends a redirect to a path within the application, adding the context path.
     *
     * @param request  The request being redirected.
     * @param response The response to redirect.
     * @param path     The path within the application to redirect to.
     */
    private static void redirect(final HttpServletRequest request, final HttpServletResponse response, final String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * Indicates whether the path is one of the MFA plugin's own pages, for any configured method.
     *
     * @param path The path within the application.
     *
     * @return Whether the path is one of the plugin's pages.
     */
    private boolean isMfaPage(final String path) {
        if (StringUtils.equals(path, errorPath)) {
            return true;
        }
        final List<MFAStrategyI> strategies = MFAStrategyManager.GetAvailableStrategies();
        if (null == strategies) {
            return false;
        }
        return strategies.stream().anyMatch(strategy -> StringUtils.equals(path, strategy.getVerificationTemplatePath())
                                                        || StringUtils.equals(path, strategy.getRegistrationTemplatePath()));
    }

    /**
     * Indicates whether this session reached XNAT through a link that requires the password to be changed, which is
     * how both the "forgot password" email and the account reactivation email log a user in.
     *
     * @param session The session to check.
     *
     * @return Whether a password change is required before anything else.
     */
    private static boolean isForcedPasswordChange(final HttpSession session) {
        return BooleanUtils.toBooleanDefaultIfNull((Boolean) session.getAttribute(MFAConstants.XNAT_FORCE_PASSWORD_CHANGE), false);
    }

    /**
     * Records that this session's password has been checked and isn't expired, so that XNAT's expired-password filter
     * lets the MFA pages through. The real check is restored by {@link #restoreExpiryCheck} once MFA is done.
     *
     * @param session The session to defer the check on.
     */
    private static void deferExpiryCheck(final HttpSession session) {
        if (BooleanUtils.isFalse((Boolean) session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED))) {
            return;
        }
        session.setAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED, Boolean.FALSE);
        session.setAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK, Boolean.TRUE);
    }

    /**
     * Undoes {@link #deferExpiryCheck}, so that XNAT checks the password's age again on this request and sends the user
     * to the password page if it really has expired.
     *
     * @param session The session to restore the check on.
     */
    private static void restoreExpiryCheck(final HttpSession session) {
        if (null == session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK)) {
            return;
        }
        session.removeAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK);
        session.removeAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED);
    }

    /**
     * Puts back the method the user had before they switched to the email backup, once that switch is over.
     *
     * @param session The session to read the backup state from.
     * @param mfe     The user's MFA record.
     */
    private void restorePreferredMfa(final HttpSession session, final MultifactorEntity mfe) {
        if (null != session.getAttribute(MFAConstants.MFA_EMAIL_BACKUP_SESSION)) {
            return;
        }
        final String tempPreferredMfaBackup = mfe.getTempPreferredMfaBackup();
        if (null != tempPreferredMfaBackup) {
            mfe.setPreferredMfas(tempPreferredMfaBackup);
            mfe.setTempPreferredMfaBackup(null);
            multifactorAuthenticationService.update(mfe);
        }
    }

    /**
     * Indicates whether this is a request for the error page after a failure this session actually hit.
     *
     * @param session The session to check.
     * @param path    The path within the application.
     *
     * @return Whether the error page should be shown.
     */
    private boolean isErrorPage(final HttpSession session, final String path) {
        return BooleanUtils.toBooleanDefaultIfNull((Boolean) session.getAttribute(MFAConstants.MFA_ERROR), false)
               && StringUtils.equals(path, errorPath);
    }

    private boolean isAliasToken(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (StringUtils.startsWith(header, "Basic ")) {
            //noinspection deprecation
            final String[] atoms = new String(Base64.decode(header.substring(6).getBytes(UTF_8)), UTF_8).split(":");
            if (AliasToken.isAliasFormat(atoms[0])) {
                final AliasToken alias = aliasTokenService.locateToken(atoms[0]);
                // We don't care if the token is actually valid at this time.
                return alias != null;
            }
        }
        return false;
    }

    /**
     * Indicates whether the path is a static asset, which XNAT serves to anyone, logged in or not.
     *
     * @param path The path within the application.
     *
     * @return Whether the path is a static asset.
     */
    private static boolean isStaticAsset(final String path) {
        return StringUtils.startsWithAny(path, "/scripts/", "/style/", "/images/")
               || StringUtils.equals(path, "/favicon.ico");
    }

    /**
     * Indicates whether the path is one of the plugin's own endpoints, which the MFA pages call to show the form, send
     * a code and verify it.
     *
     * @param path The path within the application.
     *
     * @return Whether the endpoint can be called before MFA is complete.
     */
    private static boolean isUriAllowed(final String path) {
        return ALLOWED_PATHS.contains(path);
    }

    private static final Set<String> ALLOWED_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/xapi/mfa",
            "/xapi/mfa/verify",
            "/xapi/mfa/status",
            "/xapi/mfa/emailbackup",
            "/xapi/mfa/preference",
            "/xapi/mfa/switch_to_email",
            "/xapi/mfa/send_code")));
}
