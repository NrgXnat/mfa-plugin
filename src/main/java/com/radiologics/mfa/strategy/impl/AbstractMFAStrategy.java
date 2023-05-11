/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy.impl;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.jboss.aerogear.security.otp.api.Clock;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;

public abstract class AbstractMFAStrategy {
	protected String getTOTPCode(String secret, Clock clock) {
		return new Totp(secret, clock).now();
	}

	public void sendCode(PrincipalContactInformation user_info, MultifactorEntity mfe) throws Exception{
		return;
	}

	public String getVerificationTemplatePath() {
		return _verificationTemplatePath;
	}
	
	public void setVerificationTemplatePath(String templatePath) {
		_verificationTemplatePath = templatePath;
	}
	
	public String getRegistrationTemplatePath() {
		return _registrationTemplatePath;
	}

	public void setRegistrationTemplatePath(String registrationTemplatePath) {
		_registrationTemplatePath = registrationTemplatePath;
	}

	
	protected String _verificationTemplatePath;
	protected String _registrationTemplatePath;

}
