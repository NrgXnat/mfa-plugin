package com.radiologics.mfa.filter;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.preference.MFAPreferences;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the paths a session takes through {@link MultifactorAuthenticationFilter} between logging in with a password
 * and passing multi-factor authentication.
 */
public class MultifactorAuthenticationFilterTest {
    private static final String ERROR_PATH            = "/app/template/MfaVerificationError.vm";
    private static final String VERIFICATION_PATH     = "/app/template/MfaEmailVerification.vm";
    private static final String REGISTRATION_PATH     = "/app/template/MfaGoogleAuthenticatorRegister.vm";
    private static final String CHANGE_PASSWORD_PATH  = "/app/template/XDATScreen_UpdateUser.vm";
    private static final String PASSWORD_CHANGE_PATHS = CHANGE_PASSWORD_PATH
                                                        + ",/app/action/ModifyPassword"
                                                        + ",/app/template/Login.vm"
                                                        + ",/app/action/XDATLoginUser"
                                                        + ",/app/action/LogoutUser";
    private static final String REDIRECT_PATH         = "/app/template/Index.vm";

    /** A request for something that is none of the pages the filter treats specially. */
    private static final String ORDINARY_PATH = "/xapi/users/profile";

    private MultifactorAuthenticationService service;
    private MFAPreferences                   preferences;
    private MFAStrategyI                     strategy;
    private MultifactorEntity                entity;
    private UserI                            user;
    private MockHttpSession                  session;
    private MultifactorAuthenticationFilter  filter;
    private ExecutorService                  executor;

    @Before
    public void setUp() throws Exception {
        service     = mock(MultifactorAuthenticationService.class);
        preferences = mock(MFAPreferences.class);
        strategy    = mock(MFAStrategyI.class);
        user        = mock(UserI.class);
        entity      = new MultifactorEntity("tester");
        session     = new MockHttpSession();

        when(user.getUsername()).thenReturn("tester");
        when(user.getLogin()).thenReturn("tester");
        when(user.isGuest()).thenReturn(false);

        when(preferences.getMfaRedirectPath()).thenReturn(REDIRECT_PATH);

        // The email method: a code is sent, and there is no device to register, so there is no registration page.
        when(strategy.needsRegistration()).thenReturn(false);
        when(strategy.getRegistrationTemplatePath()).thenReturn(null);
        when(strategy.getVerificationTemplatePath()).thenReturn(VERIFICATION_PATH);
        when(strategy.getErrorTemplatePath()).thenReturn(ERROR_PATH);

        when(service.isMFARequired("tester")).thenReturn(true);
        when(service.getMultifactorAuth("tester")).thenReturn(entity);
        when(service.getPreferredMFAStrategy(entity)).thenReturn(strategy);

        // One filter bean serves every request in production, so the tests share one too.
        filter = new MultifactorAuthenticationFilter(service, preferences, mock(AliasTokenService.class),
                                                     Collections.singletonList(strategy), ERROR_PATH,
                                                     CHANGE_PASSWORD_PATH, PASSWORD_CHANGE_PATHS) {
            @Override
            protected UserI currentUser() {
                return user;
            }
        };
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void passesRequestsThroughOnceTheCodeHasBeenVerified() throws Exception {
        session.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
        assertTrue("A verified session should reach the rest of XNAT", run(request(ORDINARY_PATH)).reachedXnat());
        verify(service, never()).isMFARequired("tester");
    }

    @Test
    public void sendsAnyOtherRequestToTheVerificationPage() throws Exception {
        assertEquals(VERIFICATION_PATH, run(request(ORDINARY_PATH)).redirectedTo());
        verify(service, times(1)).sendCode(user);
    }

    @Test
    public void showsTheVerificationPageItself() throws Exception {
        assertTrue("The verification page has to render for the user to enter their code",
                   run(request(VERIFICATION_PATH)).reachedXnat());
    }

    /**
     * XNAT's scripts and stylesheets are public, and the MFA pages can't draw their form without them. Redirecting
     * them meant the pages only worked if the browser already had them cached.
     */
    @Test
    public void servesStaticAssetsWhileWaitingForTheCode() throws Exception {
        for (final String path : new String[]{"/scripts/xnat/init.js", "/style/app.css", "/images/logo.png",
                                              "/themes/custom/css/theme.css", "/favicon.ico"}) {
            assertTrue(path + " should be served while MFA is pending", run(request(path)).reachedXnat());
        }
        verify(service, never()).sendCode(any(UserI.class));
        verify(service, never()).getMultifactorAuth("tester");
    }

    /**
     * Turbine reads trailing path segments as parameters, so a longer path is a request for something other than the
     * page it starts with and must not be matched as that page.
     */
    @Test
    public void matchesItsOwnPagesExactly() throws Exception {
        assertEquals(VERIFICATION_PATH, run(request(VERIFICATION_PATH + "/action/SomeAction")).redirectedTo());
    }

    @Test
    public void sendsAUserWhoNeedsToRegisterADeviceToTheRegistrationPage() throws Exception {
        when(strategy.needsRegistration()).thenReturn(true);
        when(strategy.getRegistrationTemplatePath()).thenReturn(REGISTRATION_PATH);

        assertEquals(REGISTRATION_PATH, run(request(ORDINARY_PATH)).redirectedTo());
        assertTrue("The registration page has to render", run(request(REGISTRATION_PATH)).reachedXnat());
        verify(service, never()).sendCode(any(UserI.class));
    }

    @Test
    public void turnsGuestsAwayFromTheMfaPages() throws Exception {
        when(user.isGuest()).thenReturn(true);

        assertEquals("A guest has no MFA to complete, so the pages would only render an empty form",
                     REDIRECT_PATH, run(request(VERIFICATION_PATH)).redirectedTo());
        assertTrue("Everything else a guest asks for is XNAT's business, not the plugin's",
                   run(request("/app/template/Login.vm")).reachedXnat());
        verify(service, never()).isMFARequired(any());
    }

    // Password resets and expired passwords.

    /**
     * A reset link logs the user in with a forced password change. XNAT's expired-password filter then sends every
     * request to the password page while this filter sends every request to the verification page, and neither the
     * password change nor the MFA check can finish.
     */
    @Test
    public void letsAUserWithAResetLinkChangeTheirPassword() throws Exception {
        session.setAttribute(MFAConstants.XNAT_FORCE_PASSWORD_CHANGE, true);

        assertTrue("The password page has to render", run(request(CHANGE_PASSWORD_PATH)).reachedXnat());
        assertTrue("The password change has to go through", run(post("/app/action/ModifyPassword")).reachedXnat());
        assertTrue("Logging out has to work", run(request("/app/action/LogoutUser")).reachedXnat());
        assertTrue("The login page has to render", run(request("/app/template/Login.vm")).reachedXnat());
        assertTrue("Logging in has to work", run(post("/app/action/XDATLoginUser")).reachedXnat());

        verify(service, never()).sendCode(any(UserI.class));
    }

    @Test
    public void letsAUserWithAResetLinkReachNothingElse() throws Exception {
        session.setAttribute(MFAConstants.XNAT_FORCE_PASSWORD_CHANGE, true);

        assertEquals(CHANGE_PASSWORD_PATH, run(request(ORDINARY_PATH)).redirectedTo());
        assertEquals(CHANGE_PASSWORD_PATH, run(request("/data/projects")).redirectedTo());
        assertEquals("The verification page is not reachable until the password has been changed",
                     CHANGE_PASSWORD_PATH, run(request(VERIFICATION_PATH)).redirectedTo());

        // The Referer header is set by the client, and XNAT's expired-password filter trusts it to decide that a
        // request came from the password page. This filter must not.
        final MockHttpServletRequest spoofed = request(ORDINARY_PATH);
        spoofed.addHeader("Referer", "https://host.example.org" + CHANGE_PASSWORD_PATH);
        assertEquals(CHANGE_PASSWORD_PATH, run(spoofed).redirectedTo());

        verify(service, never()).sendCode(any(UserI.class));
    }

    /**
     * XNAT's expired-password filter runs straight after this one and would send the verification page itself to the
     * password page. Its check is held back until the code has been verified, and then restored so that XNAT can ask
     * for a new password on the first request after that.
     */
    @Test
    public void holdsBackTheExpiredPasswordCheckUntilTheCodeIsVerified() throws Exception {
        run(request(VERIFICATION_PATH));
        assertEquals("XNAT must not redirect the verification page while the code is pending",
                     Boolean.FALSE, session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED));
        assertEquals(Boolean.TRUE, session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));

        session.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
        run(request(REDIRECT_PATH));
        assertNull("XNAT has to check the password's age again once MFA is done",
                   session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED));
        assertNull(session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));
    }

    /**
     * An answer XNAT cached itself has to be left alone, because there is nothing of ours to undo.
     */
    @Test
    public void leavesXnatsOwnExpiryAnswerAlone() throws Exception {
        session.setAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED, Boolean.FALSE);

        run(request(VERIFICATION_PATH));
        assertNull(session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));

        session.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
        run(request(REDIRECT_PATH));
        assertEquals(Boolean.FALSE, session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED));
    }

    @Test
    public void leavesTheExpiredPasswordCheckAloneForUsersWhoDoNotNeedMfa() throws Exception {
        when(service.isMFARequired("tester")).thenReturn(false);
        run(request(ORDINARY_PATH));
        assertNull(session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED));
        assertNull(session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));
    }

    @Test
    public void restoresTheExpiredPasswordCheckForExemptUsers() throws Exception {
        run(request(ORDINARY_PATH));
        assertEquals(Boolean.TRUE, session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));

        entity.setMfaExempted(true);
        run(request(ORDINARY_PATH));
        assertNull(session.getAttribute(MFAConstants.XNAT_PASSWORD_EXPIRED));
        assertNull(session.getAttribute(MFAConstants.MFA_DEFERRED_EXPIRY_CHECK));
    }

    // One code per login.

    @Test
    public void recordsThatACodeWasSentBeforeSendingIt() throws Exception {
        doAnswer(invocation -> {
            assertEquals("A request arriving while the code is being sent must not send a second one",
                         Boolean.TRUE, session.getAttribute(MFAConstants.MFA_TOKEN_SENT));
            return null;
        }).when(service).sendCode(user);

        run(request(ORDINARY_PATH));
        verify(service, times(1)).sendCode(user);
    }

    /**
     * A page makes several requests at once. Each code replaces the one before it, so sending a code per request left
     * the user with a mailbox of codes of which only the last one worked.
     */
    @Test
    public void sendsOneCodeForRequestsThatArriveTogether() throws Exception {
        session.setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Object());

        final int            requests = 8;
        final CyclicBarrier  start    = new CyclicBarrier(requests);
        final CountDownLatch sending  = new CountDownLatch(1);

        // Hold the first sender inside sendCode, so the others are all inside the filter at the same time.
        doAnswer(invocation -> sending.await(5, TimeUnit.SECONDS)).when(service).sendCode(user);

        executor = Executors.newFixedThreadPool(requests);
        final List<Callable<Void>> calls = new ArrayList<>();
        for (int index = 0; index < requests; index++) {
            calls.add(() -> {
                start.await(5, TimeUnit.SECONDS);
                run(request("/xapi/access/displays/browseable"));
                return null;
            });
        }

        final List<Future<Void>> results = executor.invokeAll(calls, 15, TimeUnit.SECONDS);
        sending.countDown();
        for (final Future<Void> result : results) {
            // Rethrows whatever a request threw, and fails rather than passing quietly if one never finished.
            result.get(5, TimeUnit.SECONDS);
        }

        verify(service, times(1)).sendCode(user);
    }

    @Test
    public void showsTheErrorPageWhenTheCodeCannotBeSent() throws Exception {
        doThrow(new RuntimeException("no mail server")).when(service).sendCode(user);

        assertEquals(ERROR_PATH, run(request(ORDINARY_PATH)).redirectedTo());
        assertEquals(Boolean.TRUE, session.getAttribute(MFAConstants.MFA_ERROR));
        assertEquals("A failed send must not count as a code the user has",
                     Boolean.FALSE, session.getAttribute(MFAConstants.MFA_TOKEN_SENT));
        assertTrue("The error page itself has to render", run(request(ERROR_PATH)).reachedXnat());
    }

    @Test
    public void doesNotShowTheErrorPageWithoutAnError() throws Exception {
        assertEquals(VERIFICATION_PATH, run(request(ERROR_PATH)).redirectedTo());
    }

    // Switching to the email backup.

    @Test
    public void keepsTheEmailBackupForTheRestOfTheSession() throws Exception {
        entity.setPreferredMfas("Email");
        entity.setTempPreferredMfaBackup("GoogleAuthenticator");
        session.setAttribute(MFAConstants.MFA_EMAIL_BACKUP_SESSION, true);

        run(request(ORDINARY_PATH));

        assertEquals("A session that switched to the email backup has to stay on email",
                     "Email", entity.getPreferredMfas());
        assertEquals("GoogleAuthenticator", entity.getTempPreferredMfaBackup());
    }

    @Test
    public void putsTheUsersOwnMethodBackOnTheirNextSession() throws Exception {
        entity.setPreferredMfas("Email");
        entity.setTempPreferredMfaBackup("GoogleAuthenticator");

        run(request(ORDINARY_PATH));

        assertEquals("GoogleAuthenticator", entity.getPreferredMfas());
        assertNull(entity.getTempPreferredMfaBackup());
        verify(service).update(entity);
    }

    // Deployments under a context path, such as https://host.example.org/xnat.

    @Test
    public void addsTheContextPathToRedirects() throws Exception {
        final MockHttpServletRequest request = request(ORDINARY_PATH);
        request.setContextPath("/xnat");
        assertEquals("/xnat" + VERIFICATION_PATH, run(request).redirectedTo());
    }

    @Test
    public void matchesItsOwnPagesUnderAContextPath() throws Exception {
        final MockHttpServletRequest request = request(VERIFICATION_PATH);
        request.setContextPath("/xnat");
        assertTrue("The verification page has to render under a context path", run(request).reachedXnat());
    }

    // Helpers.

    private MockHttpServletRequest request(final String path) {
        return request("GET", path);
    }

    private MockHttpServletRequest post(final String path) {
        return request("POST", path);
    }

    /**
     * Builds a request the way a servlet container hands one to a filter: the servlet's own mapping in the servlet
     * path, and the rest of the path in the path info.
     */
    private MockHttpServletRequest request(final String method, final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setSession(session);
        final int separator = path.indexOf('/', 1);
        if (separator > 0) {
            request.setServletPath(path.substring(0, separator));
            request.setPathInfo(path.substring(separator));
        } else {
            request.setServletPath(path);
        }
        return request;
    }

    private Result run(final MockHttpServletRequest request) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain         chain    = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Result(response, chain);
    }

    private static class Result {
        private final MockHttpServletResponse response;
        private final MockFilterChain         chain;

        private Result(final MockHttpServletResponse response, final MockFilterChain chain) {
            this.response = response;
            this.chain    = chain;
        }

        /**
         * Asserts that the response is a redirect, and returns where it points.
         */
        private String redirectedTo() {
            assertEquals("Expected a redirect", HttpServletResponse.SC_FOUND, response.getStatus());
            return response.getRedirectedUrl();
        }

        /**
         * Indicates whether the request was passed on to the rest of XNAT rather than answered by the filter.
         */
        private boolean reachedXnat() {
            return chain.getRequest() != null;
        }
    }
}
