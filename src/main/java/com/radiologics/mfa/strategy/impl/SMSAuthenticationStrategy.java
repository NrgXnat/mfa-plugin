/**
 * Copyright 2019 Radiologics Inc
 *
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy.impl;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.common.collect.Maps;
import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import java.util.Map;

@Slf4j
public class SMSAuthenticationStrategy extends AbstractMFAStrategy implements MFAStrategyI {
    public SMSAuthenticationStrategy() {
        super("SMS", false);
    }

    @Override
    public void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) {
        final String username   = StringUtils.isBlank(userInfo.getUserFullname()) ? userInfo.getLogin() : userInfo.getUserFullname();
        final String phone      = userInfo.getPhonenumber();
        final String code       = getTOTPCode(mfe.getSecret(), getClock());
        final String smsMessage = getCodeSentMessage(username, code);
        sendSMSMessage(smsMessage, phone);
    }

    private void sendSMSMessage(String message, String phoneNumber) {
        //noinspection deprecation
        final AmazonSNSClient                    snsClient             = new AmazonSNSClient();
        final Map<String, MessageAttributeValue> smsAttributes         = Maps.newHashMap();
        final MessageAttributeValue              messageAttributeValue = new MessageAttributeValue().withStringValue("Transactional").withDataType("String");

        smsAttributes.put("AWS.SNS.SMS.SMSType", messageAttributeValue);
        final PublishRequest request = new PublishRequest().withMessage(message).withPhoneNumber(phoneNumber).withMessageAttributes(smsAttributes);

        try {
            final PublishResult result = snsClient.publish(request);
            log.debug("SMS Message sent {}", result);
        } catch (Exception e) {
            log.error("Could not send SMS", e);
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
        return new Clock(MFAConstants.MFA_SMS_CLOCK_INTERVAL);
    }
}
