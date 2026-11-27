package com.radiologics.mfa.security;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xnat.security.XnatExpiredPasswordFilter;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public final class MultifactorAuthConfigurer<B extends HttpSecurityBuilder<B>> extends AbstractHttpConfigurer<MultifactorAuthConfigurer<B>, B> {

    OncePerRequestFilter mfaFilter;

    public MultifactorAuthConfigurer(OncePerRequestFilter mfaFilter){
        this.mfaFilter = mfaFilter;
    }

    /**
     * Adds the MFA filter immediately before XNAT's expired-password filter.
     *
     * <p>The order matters. MFA has to be settled before XNAT lets a user change their password, or someone holding
     * a password that has expired could rotate it without the second factor. Because the expired-password filter then
     * runs second, the MFA filter has to hold back its check while MFA is pending; see
     * {@code MultifactorAuthenticationFilter.deferExpiryCheck}. Moving this to {@code addFilterAfter} brings back the
     * loop where each filter redirects the other's pages.</p>
     *
     * @param http The security builder to add the filter to.
     */
    @Override
    public void configure(final B http) {
        http.addFilterBefore(mfaFilter, XnatExpiredPasswordFilter.class);
    }
}
