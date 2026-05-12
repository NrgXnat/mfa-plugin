/**
 * Copyright 2019 Radiologics, Inc
 *
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;

public interface MFAStrategyI {
    void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) throws Exception;

    Boolean verifyToken(MultifactorEntity entity, String token);

    boolean needsRegistration();

    /**
     * Indicates the vector by which the MFA request is sent. This is something like
     * "email", "sms", and so on.
     *
     * @return The vector by which the MFA request is sent.
     */
    String getVector();

    String getBodyTemplatePath();

    String getRegistrationTemplatePath();

    String getVerificationTemplatePath();

    String getErrorTemplatePath();
}
