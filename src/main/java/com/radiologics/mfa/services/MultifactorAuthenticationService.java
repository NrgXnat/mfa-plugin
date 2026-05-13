//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.services;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.MFACodeSendFailedException;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
import com.radiologics.mfa.exception.MFAUserExemptException;
import com.radiologics.mfa.exception.MFAUserNotFoundException;
import com.radiologics.mfa.model.MfaModel;
import com.radiologics.mfa.principal.PrincipalContactInformation;
import com.radiologics.mfa.strategy.MFAStrategyI;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;

public interface MultifactorAuthenticationService extends BaseHibernateService<MultifactorEntity>{
	void save(MultifactorEntity mfe);
	void saveOrUpdate(MultifactorEntity mfe);
	MultifactorEntity getMultifactorAuth(String username);

	Boolean verifyToken(String username, String token);
	void sendCode(UserI user) throws MFAUserNotFoundException, MFAStrategyNotFoundException, MFAUserExemptException, MFACodeSendFailedException;

	MfaModel toPojo(MultifactorEntity mfe);
	MFAStrategyI getPreferredMFAStrategy(String username) throws MFAStrategyNotFoundException;
	MFAStrategyI getPreferredMFAStrategy(MultifactorEntity mfe) throws MFAStrategyNotFoundException;

	List<MultifactorEntity> getAllMultifactorEntities();
	PrincipalContactInformation getPrincipalContactInformation(UserI user);

	Boolean isMFARequired(String username);

	String getPreferredMFAMethod(UserI user);
	String getPreferredMFAMethod(String username);
	String getPreferredMFAMethod(MultifactorEntity mfe);
	MultifactorEntity createMfe(String username);

}
