//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.plugin;

import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import com.radiologics.mfa.helper.MFAPreferences;
import com.radiologics.mfa.strategy.impl.EmailAuthenticationStrategy;
import com.radiologics.mfa.strategy.impl.GoogleAuthenticatorStrategy;
import com.radiologics.mfa.strategy.impl.SMSAuthenticationStrategy;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@PropertySource("classpath:/config/mfa/xnat-mfa.properties")
@XnatPlugin(value = "MfaPlugin", name  = "Multi-factor Authentication Plugin",
			description = "Adds multi-factor authentication support to XNAT.",
			logConfigurationFile = "META-INF/resources/mfa-logback.xml",
			entityPackages = { "com.radiologics.mfa.entities" })
@ComponentScan({"com.radiologics.mfa.dao", "com.radiologics.mfa.helper",
				"com.radiologics.mfa.api", "com.radiologics.mfa.security",
				"com.radiologics.mfa.services", "com.radiologics.mfa.filter"})
public class MfaPlugin { 
	
	@Bean
	MFAPreferences mfaPreferences(@Value("${mfa.preferred}") String mfaPreferred, @Value("${mfa.redirectPath}") String redirectPath) {
		final MFAPreferences preferences = new MFAPreferences();
		preferences.setAutoPreferredMFAStrategy(mfaPreferred);
		preferences.setMfaRedirectPath(redirectPath);
		return preferences;
	}
	
	@Bean
	GoogleAuthenticatorStrategy googleAuthenticatorStrategy(@Value("${mfa.googleAuthenticator.qrCodeUrlTemplate}") String qrCodeUrlTemplate,
															@Value("${mfa.googleAuthenticator.verificationPath}")  String verificationPath,
															@Value("${mfa.googleAuthenticator.registerPath}") String registerPath) {
		final GoogleAuthenticatorStrategy googleAuthenticator = new GoogleAuthenticatorStrategy();
		googleAuthenticator.setQrCodeUrlTemplate(qrCodeUrlTemplate);
		googleAuthenticator.setVerificationTemplatePath(verificationPath);
		googleAuthenticator.setRegistrationTemplatePath(registerPath);
		return googleAuthenticator;
	}

	@Bean
	EmailAuthenticationStrategy emailAuthenticationStrategy(@Value("${mfa.email.codeSentTextTemplate}") String codeSentEmailTemplate,
															@Value("${mfa.email.verificationPath}")    String verificationPath) {
		final EmailAuthenticationStrategy emailAuthenticationStrategy = new EmailAuthenticationStrategy();
		emailAuthenticationStrategy.setVerificationTemplatePath(verificationPath);
		emailAuthenticationStrategy.setEmailBodyTemplatePath(codeSentEmailTemplate);
		return emailAuthenticationStrategy;
	}

	@Bean
	SMSAuthenticationStrategy smsAuthenticationStrategy(@Value("${mfa.sms.codeSentTextTemplate}") String codeSentEmailTemplate,
														@Value("${mfa.sms.verificationPath}")    String verificationPath) {
		final SMSAuthenticationStrategy smsAuthenticationStrategy = new SMSAuthenticationStrategy();
		smsAuthenticationStrategy.setVerificationTemplatePath(verificationPath);
		smsAuthenticationStrategy.setSMSBodyTemplatePath(codeSentEmailTemplate);
		return smsAuthenticationStrategy;
	}
}