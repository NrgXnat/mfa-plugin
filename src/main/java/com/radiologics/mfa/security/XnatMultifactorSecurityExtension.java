//Copyright 2019 Radiologics, Inc
//Author: James <ransfordj@radiologics.com>
package com.radiologics.mfa.security;

import org.nrg.xnat.security.BaseXnatSecurityExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;
import com.radiologics.mfa.filter.MultifactorAuthenticationFilter;

@Component
public class XnatMultifactorSecurityExtension extends BaseXnatSecurityExtension{

	@Autowired
	MultifactorAuthenticationFilter multifactorAuthenticationFilter;

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.apply(new MultifactorAuthConfigurer<>(multifactorAuthenticationFilter));
	}

	@Override
	public String getAuthMethod() {
		return "Multifactor Authentication";
	}
}
