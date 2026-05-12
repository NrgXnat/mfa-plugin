/**
 * Copyright 2019 Radiologics, Inc
 *
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy.impl;

import com.radiologics.mfa.strategy.MFAStrategyI;
import org.apache.velocity.VelocityContext;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public abstract class AbstractMFAStrategy implements MFAStrategyI {
    protected AbstractMFAStrategy(final String vector, final boolean needsRegistration) {
        _vector = vector;
        _needsRegistration = needsRegistration;
    }

    @Override
    abstract public void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) throws Exception;

    @Override
    abstract public Boolean verifyToken(MultifactorEntity entity, String token);

    @Override
    public String getVector() {
        return _vector;
    }

    @Override
    public boolean needsRegistration() {
        return _needsRegistration;
    }

    @Override
    public String getVerificationTemplatePath() {
        return _verificationTemplatePath;
    }

    public void setVerificationTemplatePath(String templatePath) {
        _verificationTemplatePath = templatePath;
    }

    @Override
    public String getRegistrationTemplatePath() {
        return _registrationTemplatePath;
    }

    public void setRegistrationTemplatePath(String registrationTemplatePath) {
        _registrationTemplatePath = registrationTemplatePath;
    }

    @Override
    public String getBodyTemplatePath() {
        return _bodyTemplatePath;
    }

    public void setBodyTemplatePath(String bodyTemplatePath) {
        _bodyTemplatePath = bodyTemplatePath;
    }

    @Override
    public String getErrorTemplatePath() {
        return _errorTemplatePath;
    }

    public void setErrorTemplatePath(String errorTemplatePath) {
        _errorTemplatePath = errorTemplatePath;
    }

    protected String getTOTPCode(String secret, Clock clock) {
        return new Totp(secret, clock).now();
    }

    protected String getCodeSentMessage(String userFullName, String code) {
        final VelocityContext context = new VelocityContext();
        context.put("userFullName", userFullName);
        context.put("code", code);
        context.put("siteLogoPath", XDAT.getSiteLogoPath());
        context.put("process", XDAT.getSiteId() + " verification");
        context.put("system", TurbineUtils.GetSystemName());
        context.put("server", XDAT.getSiteUrl());
        context.put("contact_email", XDAT.getNotificationsPreferences().getHelpContactInfo());
        return AdminUtils.populateVmTemplate(context, getBodyTemplatePath());
    }

    private final String  _vector;
    private final boolean _needsRegistration;

    private String _verificationTemplatePath;
    private String _registrationTemplatePath;
    private String _bodyTemplatePath;
    private String _errorTemplatePath;
}
