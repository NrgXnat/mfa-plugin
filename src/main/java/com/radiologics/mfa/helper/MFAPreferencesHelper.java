/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.helper;

import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.exceptions.UnknownToolId;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.utils.MFAConstants;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MFAPreferencesHelper {
	
	@Autowired
	public MFAPreferencesHelper(final SiteConfigPreferences siteConfig,
				    final MultifactorAuthenticationService mfaservice,
				    final MFAPreferences pref) {
		_siteConfig = siteConfig;
		_mfaService = mfaservice;	
		_pref = pref;
		initSiteConfigWithMFA();
	}
	
	private void initSiteConfigWithMFA() {
		String pref =  _siteConfig.getValue(MFAConstants.MFA_PREFERRED);
		if (pref == null) {
			try {
				setPreferredMFAMethod(_pref.getAutoPreferredMFAStrategy());
			}catch(Exception e) {log.error("Unable to init MFA preferences for  site configuration " + e.getMessage());}
		}		
	}

	public String getPreferredMFAMethod() {
		return (String) _siteConfig.getOrDefault(MFAConstants.MFA_PREFERRED, _pref.getAutoPreferredMFAStrategy());
	}

	public String setPreferredMFAMethod(String mfaStrategy) throws UnknownToolId, InvalidPreferenceName {
		return _siteConfig.set(mfaStrategy, MFAConstants.MFA_PREFERRED);
	}

        public String getPreferredMFAMethod(MultifactorEntity mfe) {
		String userOveriddenPreferredMethod = getPreferredMFAMethod();
		if (null == mfe) {
			log.debug("MFE is null - Returning UserOveriddenPreferredMethod: " + userOveriddenPreferredMethod);
			return userOveriddenPreferredMethod;
		}
		userOveriddenPreferredMethod = mfe.getPreferredMfas();
		if (null == userOveriddenPreferredMethod) {
			log.debug("MFE is not null but no preferredMFA" );
			userOveriddenPreferredMethod = getPreferredMFAMethod();
		}
		return userOveriddenPreferredMethod;
	}

	
	public String getPreferredMFAMethod(UserI user) {
		MultifactorEntity mfe = _mfaService.getMultifactorAuth(user.getUsername());	
		return getPreferredMFAMethod(mfe);
	}
	
	@Autowired
	private final MFAPreferences _pref;
	
	@Autowired
	private final SiteConfigPreferences _siteConfig;

	@Autowired
        private final MultifactorAuthenticationService _mfaService;

}
