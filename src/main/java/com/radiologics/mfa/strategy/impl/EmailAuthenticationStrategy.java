/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy.impl;

import com.radiologics.mfa.annotation.MFAHandler;
import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@MFAHandler(handler="Email")
public class EmailAuthenticationStrategy extends AbstractMFAStrategy implements MFAStrategyI {

	public String getRegistrationTemplatePath() {
		return null;
	}

	public Boolean needsRegistration() {
		return false;
	}
	
	
	public String getVerificationTemplatePath() {
		return _verificationTemplatePath;
	}
	
	
	public void setEmailBodyTemplatePath(String bodyTemplatePath) {
		_bodyTemplatePath = bodyTemplatePath;
	}

	@Override
	public void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) throws Exception{
		final String username     = userInfo.getUserFullname().trim().equals("") ? userInfo.getLogin() : userInfo.getUserFullname();
		final List<String> emails = Collections.singletonList(userInfo.getEmail());
		final String adminEmail   = XDAT.getSiteConfigPreferences().getAdminEmail();
		final String code         = getTOTPCode(mfe.getSecret(), getClock());
		final String emailBody    = getCodeSentMessage(username, code);
		final String emailSubject = XDAT.getSiteId() + ": Multi-Factor Authentication";
		try {
            XDAT.getMailService().sendHtmlMessage(adminEmail, emails.toArray(new String[emails.size()]), emailSubject, emailBody);
		}catch(Exception e) {
			log.error("Could not send Email.", e.getMessage());
			throw e;
		}
	}
	
	public Boolean verifyToken(MultifactorEntity mfe, String token) {
		try {
			return new Totp(mfe.getSecret(), getClock()).verify(token);
		}catch(Exception e) {
			log.debug(e.getMessage(), e);
			return false;
		}
	}

	private Clock getClock(){
		return new Clock(MFAConstants.MFA_EMAIL_CLOCK_INTERVAL);
	}
	
	private String getCodeSentMessage(String userFullName, String code) {
	        VelocityContext context = new VelocityContext();
	        context.put("userFullName",userFullName);
	        context.put("code", code);
	        context.put("siteLogoPath", XDAT.getSiteLogoPath());
	        context.put("process",XDAT.getSiteId() + " verification");
	        context.put("system",TurbineUtils.GetSystemName());
	        context.put("server",XDAT.getSiteUrl());
	        context.put("contact_email",XDAT.getNotificationsPreferences().getHelpContactInfo());
			String body = AdminUtils.populateVmTemplate(context, _bodyTemplatePath);
			return body;
	 }
	
	private String _bodyTemplatePath;
}
