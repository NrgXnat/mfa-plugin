package org.apache.turbine.app.xnat.modules.screens;

import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.services.template.TurbineTemplate;
import org.apache.turbine.util.RunData;
import org.nrg.xdat.XDAT;
import org.nrg.xft.security.UserI;

/**
 * Sends a user who has just verified their email address on to multi-factor authentication, rather than into XNAT.
 */
@Slf4j
public class VerifyEmail extends org.nrg.xnat.turbine.modules.screens.VerifyEmail {

    final MultifactorAuthenticationService mfaService =
            XDAT.getContextService().getBean(MultifactorAuthenticationService.class);

    @Override
    public void doRedirect(RunData data, String template) throws Exception {
        final String mfaTemplate = getMfaTemplate();
        if (StringUtils.isNotBlank(mfaTemplate)) {
            template = mfaTemplate;
        }
        super.doRedirect(data, TurbineTemplate.getScreenName(template), template);
    }

    /**
     * Returns the name of the MFA page this user has to go through next, or null if there isn't one.
     *
     * <p>Only methods that pair with a device, such as Google Authenticator, have a registration page. The email and
     * SMS methods send a code instead, so they go straight to their verification page.</p>
     *
     * @return The template name of the next MFA page, or null.
     */
    private String getMfaTemplate() {
        try {
            final UserI user = XDAT.getUserDetails();
            if (user == null || user.isGuest()) {
                return null;
            }
            if (!mfaService.isMFARequired(user.getUsername())) {
                return null;
            }
            final MFAStrategyI strategy = mfaService.getPreferredMFAStrategy(user.getUsername());
            final String       path     = strategy.needsRegistration()
                                          ? strategy.getRegistrationTemplatePath()
                                          : strategy.getVerificationTemplatePath();
            if (StringUtils.isBlank(path)) {
                return null;
            }
            return StringUtils.substringAfterLast(path, "/");
        } catch (Exception e) {
            log.error("Failed to work out the multi-factor authentication page to send this user to. Continuing to the page they asked for.", e);
            return null;
        }
    }
}
