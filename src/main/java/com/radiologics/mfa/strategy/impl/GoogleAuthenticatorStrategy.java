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
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.Totp;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@MFAHandler(handler = "GoogleAuthenticator")
public class GoogleAuthenticatorStrategy extends AbstractMFAStrategy implements MFAStrategyI {
    public GoogleAuthenticatorStrategy() {
        super("Google Authenticator", true);
    }

    @Override
    public void sendCode(PrincipalContactInformation userInfo, MultifactorEntity mfe) {
        log.debug("Google Authenticator MFA doesn't send codes");
    }

    // Generate this on the fly so the URL doesn't get saved in the database.
    // If a user changes the URL Template, we want the URLs to update as well.
    public String generateQRCode(MultifactorEntity mfe) {
        if (mfe != null && _qrCodeUrlTemplate != null) {
            return _qrCodeUrlTemplate.replaceAll("MFA_SECRET", mfe.getAuthenticatorSecret())
                                     .replaceAll("MFA_USERNAME", mfe.getUsername())
                                     .replaceAll("MFA_ISSUER", _siteConfig.getSiteId());
        }
        return "";
    }

    public void setQrCodeUrlTemplate(String urlTemplate) {
        _qrCodeUrlTemplate = urlTemplate;
    }

    public Boolean verifyToken(MultifactorEntity mfe, String token) {
        try {
            return new Totp(mfe.getAuthenticatorSecret()).verify(token);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Autowired
    private SiteConfigPreferences _siteConfig;

    private String _qrCodeUrlTemplate;
}

