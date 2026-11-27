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

    /**
     * Session attribute marking a session that has switched to the email backup method. It keeps the user on the
     * email method for the rest of the session, and because it is held on the session rather than on the user, their
     * own method comes back the next time they log in.
     */
    public static final String MFA_EMAIL_BACKUP_SESSION = "EmailBackup";

    /**
     * Session attribute set by this plugin while it is holding back XNAT's expired-password check, so that the check
     * can be restored once multi-factor authentication is complete.
     */
    public static final String MFA_DEFERRED_EXPIRY_CHECK = "mfaDeferredExpiryCheck";

    /**
     * Session attribute XNAT sets when a user logs in through a password reset or account reactivation link and must
     * change their password before doing anything else. Set in {@code XDAT.loginUser()}.
     */
    public static final String XNAT_FORCE_PASSWORD_CHANGE = "forcePasswordChange";

    /**
     * Session attribute XNAT caches the result of its password expiration check in. Read and written by
     * {@code XnatExpiredPasswordFilter}.
     */
    public static final String XNAT_PASSWORD_EXPIRED = "expired";
    public static final int    MFA_EMAIL_CLOCK_INTERVAL           = 600;
    public static final int    MFA_SMS_CLOCK_INTERVAL             = 180;

    public static final int MAX_CODE_RESEND_ATTEMPTS             = 5;
    public static final int CODE_RESEND_LOCKOUT_DURATION_MINUTES = 5;
}
