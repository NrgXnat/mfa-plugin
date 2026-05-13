/**
 * Copyright 2019 Radiologics, Inc
 *
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
import org.apache.commons.lang3.StringUtils;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.nrg.xdat.XDAT;

import java.util.Collections;
import java.util.List;

@Slf4j
@MFAHandler(handler = EmailAuthenticationStrategy.VECTOR)
public class EmailAuthenticationStrategy extends AbstractMFAStrategy implements MFAStrategyI {
    public static final String VECTOR = "Email";

    public EmailAuthenticationStrategy() {
        super(VECTOR, false);
    }

    @Override
    public void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) throws Exception {
        final String       username     = StringUtils.isBlank(userInfo.getUserFullname()) ? userInfo.getLogin() : userInfo.getUserFullname();
        final List<String> emails       = Collections.singletonList(userInfo.getEmail());
        final String       adminEmail   = XDAT.getSiteConfigPreferences().getAdminEmail();
        final String       code         = getTOTPCode(mfe.getSecret(), getClock());
        final String       emailBody    = getCodeSentMessage(username, code);
        final String       emailSubject = XDAT.getSiteId() + ": Multi-Factor Authentication";
        try {
            XDAT.getMailService().sendHtmlMessage(adminEmail, emails.toArray(new String[0]), emailSubject, emailBody);
        } catch (Exception e) {
            log.error("Could not send email to user {} at emails: {}", username, String.join(", ", emails), e);
            throw e;
        }
    }

    public Boolean verifyToken(MultifactorEntity mfe, String token) {
        try {
            return new Totp(mfe.getSecret(), getClock()).verify(token);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return false;
        }
    }

    private Clock getClock() {
        return new Clock(MFAConstants.MFA_EMAIL_CLOCK_INTERVAL);
    }
}
