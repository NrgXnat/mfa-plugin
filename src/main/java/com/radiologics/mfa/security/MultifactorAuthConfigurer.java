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

    @Override
    public void configure(final B http) {
        http.addFilterBefore(mfaFilter, XnatExpiredPasswordFilter.class);
    }
}
