/**
 * Copyright 2019 Radiologics, Inc
 *
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.utils;

public class MFAConstants {

    public static final String MFA_PREFERRED                      = "mfaPreferred"; //Expected in Site Config
    public static final String MFA_SITE_CONFIG_REQUIRE_MFA        = "requireMfa"; //Expected in Site Config
    public static final String MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA  = "requireAdminMfa"; //Expected in Site Config
    public static final String MFA_EMAIL_BACKUP_ENABLED           = "emailBackupEnabled";
    public static final String MFA_ADMIN_EMAIL_ADMIN_NOTIFICATION = "mfaAdminEmailNotificationEnabled";
    public static final String MFA_ERROR                          = "mfaError";
    public static final String MFA_REQUIRED                       = "mfaRequired";
    public static final String MFA_TOKEN_VERIFIED                 = "mfaTokenVerified";
    public static final String MFA_TOKEN_SENT                     = "mfaTokenSent";
    public static final String MFA_METHODS                        = "mfa_methods";
    public static final int    MFA_EMAIL_CLOCK_INTERVAL           = 600;
    public static final int    MFA_SMS_CLOCK_INTERVAL             = 180;

    public static final int MAX_CODE_RESEND_ATTEMPTS             = 5;
    public static final int CODE_RESEND_LOCKOUT_DURATION_MINUTES = 5;
}
