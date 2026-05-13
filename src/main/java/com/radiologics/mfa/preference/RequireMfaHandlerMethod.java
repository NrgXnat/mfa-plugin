package com.radiologics.mfa.preference;

import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xnat.event.listeners.methods.AbstractXnatPreferenceHandlerMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.radiologics.mfa.utils.MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA;
import static com.radiologics.mfa.utils.MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA;

@Component
public class RequireMfaHandlerMethod extends AbstractXnatPreferenceHandlerMethod {
    private final MultifactorAuthenticationService multifactorAuthenticationService;
    private final MFAPreferences                   mfaPreferences;

    @Autowired
    public RequireMfaHandlerMethod(final MultifactorAuthenticationService multifactorAuthenticationService, final MFAPreferences mfaPreferences) {
        super(MFAPreferences.class, MFA_SITE_CONFIG_REQUIRE_MFA, MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA);
        this.multifactorAuthenticationService = multifactorAuthenticationService;
        this.mfaPreferences                   = mfaPreferences;
    }

    @Override
    protected void handlePreferenceImpl(final String preference, final String value) {
        final boolean isAdminScoped = StringUtils.equals(preference, MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA);
        final boolean requireMfa    = Boolean.parseBoolean(value);

        // Filter based on setting: if for admins, only get admins, otherwise only get non-admins
        Users.getAllLogins().stream().filter(login -> Roles.isSiteAdmin(login) == isAdminScoped).forEach(login -> {
            // Get MFE for the user
            final MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(login);
            // If MFA was set to required...
            if (requireMfa) {
                // And the user doesn't have MFE configured
                if (null == mfe) {
                    // Then configure it
                    MultifactorEntity mfeNew = new MultifactorEntity(login);
                    mfeNew.setMfaRegistered(false);
                    //Admin needs MFA = Exempted  = false
                    mfeNew.setMfaExempted(false);
                    mfeNew.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
                    multifactorAuthenticationService.create(mfeNew);
                } else if (mfe.isMfaExempted()) {
                    // If the user DOES have MFE configured as exempted, then update it to be non-exempted
                    mfe.setMfaExempted(false);
                    multifactorAuthenticationService.update(mfe);
                }
            } else if (null != mfe && !mfe.isMfaExempted()) {
                // If the user has MFE configured as non-exempted, then update it to be exempted
                mfe.setMfaExempted(true);
                multifactorAuthenticationService.update(mfe);
            }
        });
    }
}
