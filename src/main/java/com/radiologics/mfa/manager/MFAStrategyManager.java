/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nrg.xdat.XDAT;
import org.springframework.stereotype.Component;

import com.radiologics.mfa.annotation.MFAHandler;
import com.radiologics.mfa.strategy.MFAStrategyI;

import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
public class MFAStrategyManager {
	
	public static List<MFAStrategyI> GetAvailableStrategies() {
		List<MFAStrategyI> strategies = new ArrayList<MFAStrategyI>();
		try {
	        Map<String, MFAStrategyI> strategyMap =  XDAT.getContextService().getBeansOfType(MFAStrategyI.class);
	        if (strategyMap != null) {
	            strategies = new ArrayList<MFAStrategyI>(strategyMap.values());
	        }
	    } catch (Exception e) {
	        log.error("Unable to retrieve injected MFA Strategy beans", e);
	    }
		return strategies;
	}
	
	public static List<String> GetAvailableStrategyAnnotationCodes() {
		List<MFAStrategyI> strategies = GetAvailableStrategies();
		List<String>  strategyAnnotation = new ArrayList<String>();
		for (MFAStrategyI s : strategies) {
   			final MFAHandler annotation = s.getClass().getAnnotation(MFAHandler.class);
   			if (null != annotation && annotation.handler() != null) {
   				strategyAnnotation.add(annotation.handler());
   			}
   		}
		return strategyAnnotation;
	}

}
