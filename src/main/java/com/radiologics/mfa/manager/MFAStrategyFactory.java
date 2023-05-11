/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import com.radiologics.mfa.annotation.MFAHandler;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
import com.radiologics.mfa.strategy.MFAStrategyI;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MFAStrategyFactory {

	//TODO = Improve this as this will be queried too often
	public static MFAStrategyI GetMFAStrategyByMFAHandlerAnnotation(String mfahandler) throws MFAStrategyNotFoundException{
		List<MFAStrategyI> mfaStrategies = null;
		MFAStrategyI mfaStrategy = null;
		try {
	    	mfaStrategies = MFAStrategyManager.GetAvailableStrategies();
	    } catch (Exception e) {
	        log.error("Unable to retrieve injected MFA Strategy beans", e);
	        throw new MFAStrategyNotFoundException("Could not retrieve exporter of class " + MFAStrategyI.class.getName(),new IllegalArgumentException());
	    }

	    if (mfaStrategies == null || mfaStrategies.isEmpty()) {
	        log.trace("No MFA beans");
	        throw new MFAStrategyNotFoundException("No export beans ",new IllegalArgumentException());
	    }

	    for (MFAStrategyI e : mfaStrategies) {
	        final MFAHandler annotation = e.getClass().getAnnotation(MFAHandler.class);
	        if (annotation != null) {
	        	if (mfahandler.equals(annotation.handler())) {
	        		mfaStrategy = e;
	        		break;
	        	}
	        }

	    }
	    if (null == mfaStrategy) {
	        log.trace("No MFA Strategy for " + mfahandler);
	        throw new MFAStrategyNotFoundException("No MFA Strategy for  " + mfahandler,new IllegalArgumentException());
	    }
	    return mfaStrategy;
	}
	
}
