/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.strategy;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.principal.PrincipalContactInformation;

public interface  MFAStrategyI  {
	Boolean verifyToken(MultifactorEntity entity, String token);
	String getRegistrationTemplatePath();
	Boolean needsRegistration();
	void sendCode(PrincipalContactInformation user_info, MultifactorEntity mfe) throws Exception;
	String getVerificationTemplatePath();
}
