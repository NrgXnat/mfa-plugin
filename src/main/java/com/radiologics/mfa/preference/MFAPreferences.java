/**
 * Copyright 2019 Radiologics, Inc
 *
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 */

package com.radiologics.mfa.preference;

import com.radiologics.mfa.utils.MFAConstants;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventServiceI;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.preferences.EventTriggeringAbstractPreferenceBean;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@NrgPreferenceBean(
        toolId = com.radiologics.mfa.preference.MFAPreferences.MFA_TOOL_ID,
        toolName = "XNAT MFA plugin Preferences",
        description = "Manages mfa configurations and settings for the XNAT system.",
        properties = "config/mfa/xnat-mfa.properties"
)
public class MFAPreferences extends EventTriggeringAbstractPreferenceBean {
    public static final String MFA_TOOL_ID = "multifactor-auth";

    private final SiteConfigPreferences siteConfigPreferences;

    @Autowired
    protected MFAPreferences(
            final NrgPreferenceService preferenceService,
            final NrgEventServiceI eventService,
            ConfigPaths configFolderPaths,
            OrderedProperties initPrefs,
            SiteConfigPreferences siteConfigPreferences) {
        super(preferenceService, eventService, configFolderPaths, initPrefs);
        this.siteConfigPreferences = siteConfigPreferences;
    }

    @NrgPreference(defaultValue = "false")
    public boolean isRequireMfa() {
        return getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA);
    }

    public void setRequireMfa(final boolean requireMfa) {
        try {
            setBooleanValue(requireMfa, MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA);
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name initialized: something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "false")
    public boolean isRequireAdminMfa() {
        return getBooleanValue(MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA);
    }

    public void setRequireAdminMfa(final boolean requireAdminMfa) {
        try {
            setBooleanValue(requireAdminMfa, MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA);
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name initialized: something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "true")
    public boolean isEmailBackupEnabled() {
        return getBooleanValue(MFAConstants.MFA_EMAIL_BACKUP_ENABLED);
    }

    public void setEmailBackupEnabled(final boolean mfaEmailBackupEnabled) {
        try {
            setBooleanValue(mfaEmailBackupEnabled, MFAConstants.MFA_EMAIL_BACKUP_ENABLED);
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name initialized: something is very wrong here.", e);
        }
    }

    @NrgPreference(defaultValue = "true")
    public boolean isMfaAdminEmailNotificationEnabled() {
        return getBooleanValue(MFAConstants.MFA_ADMIN_EMAIL_ADMIN_NOTIFICATION);
    }

    public void setMfaAdminEmailNotificationEnabled(final boolean mfaAdminEmailNotification) {
        try {
            setBooleanValue(mfaAdminEmailNotification, MFAConstants.MFA_ADMIN_EMAIL_ADMIN_NOTIFICATION);
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name initialized: something is very wrong here.", e);
        }
    }

    @NrgPreference(property = "mfa.email.adminAlertTextTemplate")
    public String getAdminAlertTextTemplate() {
        return getValue("mfa.email.adminAlertTextTemplate");
    }

    @NrgPreference(property = "mfa.redirectPath")
    public String getMfaRedirectPath() {
        return siteConfigPreferences.getSiteUrl()+getValue("mfa.redirectPath");
    }

    @NrgPreference(property = "mfa.preferred")
    public String getPreferredMFAMethod() {
        return getValue("mfa.preferred");
    }

    public void setPreferredMFAMethod(String mfaStrategy) {
        try {
            set(mfaStrategy, "mfa.preferred");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'aliasTokenTimeout': something is very wrong here.", e);
        }
    }
}
