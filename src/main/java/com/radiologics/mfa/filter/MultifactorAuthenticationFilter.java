//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.filter;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
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
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Component
public class MultifactorAuthenticationFilter extends OncePerRequestFilter {

    private final AliasTokenService aliasTokenService;
    private final MultifactorAuthenticationService multifactorAuthenticationService;
    private final MFAPreferences mfaPreferences;

    public MultifactorAuthenticationFilter(final MultifactorAuthenticationService multifactorAuthenticationService,
                                           final MFAPreferences mfaPreferences,
                                           final AliasTokenService aliasTokenService) {
        this.multifactorAuthenticationService = multifactorAuthenticationService;
        this.aliasTokenService = aliasTokenService;
        this.mfaPreferences = mfaPreferences;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final HttpSession httpSession = request.getSession(false);
        if (null == httpSession) {
            filterChain.doFilter(request, response);
            return;
        }

        final UserI user = XDAT.getUserDetails();
        final Object mfaTokenVerified = httpSession.getAttribute(MFAConstants.MFA_TOKEN_VERIFIED);
        final Boolean mfaTokenSent = (Boolean) httpSession.getAttribute(MFAConstants.MFA_TOKEN_SENT);
        final String requestUri = request.getRequestURI();
        final String shortUri = requestUri.contains("?") ? requestUri.substring(0, requestUri.indexOf("?")) : requestUri;
        final boolean mfaRequired = multifactorAuthenticationService.isMFARequired(user.getUsername());
        final Object emailBackup = httpSession.getAttribute("EmailBackup");

        log.debug("MultifactorAuthenticationFilter.doFilterInternal(): shortUri: {}, mfaRequired: {}, mfaTokenVerified: {}, userLogin: {}",
                shortUri, mfaRequired, mfaTokenVerified, user.getLogin());

        if (isAliasToken(request)) {
            // Requests using an alias token should bypass MFA.
            // Set the token as verified here even though it isn't. This way they wont get blocked if they
            // request a jsessionid using an alias token and then try to pass the cookie in subsequent requests.
            httpSession.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
            filterChain.doFilter(request, response);
            return;
        }

        // Let them in if the token has been verified
        if (!mfaRequired || BooleanUtils.toBoolean((Boolean) mfaTokenVerified)) {
            filterChain.doFilter(request, response);
            return;
        }

        MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
        if (null == mfe) {
            mfe = multifactorAuthenticationService.createMfe(user.getUsername());
        }

        MFAStrategyI mfaStrategy;
        try {
            if (emailBackup == null) {
                String tempPreferredMfaBackup = mfe.getTempPreferredMfaBackup();
                if (tempPreferredMfaBackup != null) {
                    mfe.setPreferredMfas(tempPreferredMfaBackup);
                    mfe.setTempPreferredMfaBackup(null);
                    multifactorAuthenticationService.update(mfe);
                }
            }
            mfaStrategy = multifactorAuthenticationService.getPreferredMFAStrategy(user.getUsername());
        } catch (MFAStrategyNotFoundException e) {
            log.error("Failed to retrieve mfaStrategy for user: {}.", user.getUsername(), e);
            return;
        }

        if (user.isGuest()) {
            if (!shortUri.endsWith("/xapi/mfa/verify") && !shortUri.endsWith("/xapi/mfa") &&
                    !shortUri.endsWith("/xapi/mfa/exempt") && (mfaStrategy.getRegistrationTemplatePath().endsWith(shortUri)) ||
                    mfaStrategy.getVerificationTemplatePath().endsWith(shortUri)) {
                response.sendRedirect(mfaPreferences.getMfaRedirectPath());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Always allow these requests if they have logged in
        if (isUriAllowed(shortUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (BooleanUtils.toBooleanDefaultIfNull(mfe.isMfaExempted(), false)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("MFA Registered for this user: {}", user.getUsername());
        if (mfaStrategy.needsRegistration() && !mfe.isMfaRegistered()) {
            if (mfaStrategy.getRegistrationTemplatePath().endsWith(shortUri)) {
                filterChain.doFilter(request, response);
            } else {
                response.sendRedirect(mfaStrategy.getRegistrationTemplatePath());
            }
            return;
        }

        if (mfe.isMfaRegistered() || !mfaStrategy.needsRegistration()) {
            try {
                if (!BooleanUtils.toBooleanDefaultIfNull(mfaTokenSent, false)) {
                    multifactorAuthenticationService.sendCode(user);
                    httpSession.setAttribute(MFAConstants.MFA_TOKEN_SENT, true);
                }
            } catch (Exception e) {
                log.error("Failed to send MFA Token to user: {}. {}", user.getUsername(), e.getMessage(), e);
                return;
            }

            log.debug("Redirecting to the verification page for the strategy: {}", mfaStrategy.getVerificationTemplatePath());
            // Let them go to the "Verify Token" page
            if (mfaStrategy.getVerificationTemplatePath().endsWith(shortUri)) {
                filterChain.doFilter(request, response);
                return;
            }

            // If mfaVerified has not been set we need to prompt for the token.
            if (null == mfaTokenVerified) {
                response.sendRedirect(mfaStrategy.getVerificationTemplatePath());
            }
            // MFA is disabled for this user but the site requires MFA to be enabled
        } else {
            // Let them go to the Registration page if MFA is required.
            //Find the preferred MFA for the user. If a registration is required,
            //user should be redirected to Registration Page.
            //Each MFA Strategy would have their own Registration Page (if they need one)
            if (mfaStrategy.needsRegistration() && shortUri.equals(mfaStrategy.getRegistrationTemplatePath())) {
                filterChain.doFilter(request, response);
                return;
            }

            // Only let the user get the mfa config when mfaTokenVerified and mfaEnabled are false && requireMFA is true
            // (i.e. When MFA is required and the user has just logged in and are setting up MFA)
            // This is the only time someone should be seeing the secret key when they aren't fully authenticated with MFA.
            if (shortUri.endsWith("/xapi/mfa")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (mfaStrategy.needsRegistration()) {
                response.sendRedirect(mfaStrategy.getRegistrationTemplatePath());
                return;
            }
            response.sendRedirect(mfaPreferences.getMfaRedirectPath());
        }
    }

    private boolean isAliasToken(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Basic ")) {
            final String[] atoms = new String(Base64.decode(header.substring(6).getBytes(UTF_8)), UTF_8).split(":");
            if (AliasToken.isAliasFormat(atoms[0])) {
                final AliasToken alias = aliasTokenService.locateToken(atoms[0]);
                if (alias != null) {
                    // We don't care if the token is actually valid at this time.
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUriAllowed(String uri) {
        return StringUtils.endsWithAny(uri,
                "/xapi/mfa/verify",
                "/scripts/mfa/multifactorAuth.js",
                "/scripts/mfa/mfaEmailAuth.js",
                "/scripts/mfa/mfaGoogleAuth.js",
                "/scripts/mfa/qrcode.min.js",
                "/style/mfa/multifactorAuth.css",
                "/style/font-awesome.css",
                "/xapi/mfa/status",
                "/xapi/mfa/emailbackup",
                "/xapi/mfa/preference",
                "/xapi/mfa/switch_to_email",
                "/xapi/mfa/send_code");
    }
}