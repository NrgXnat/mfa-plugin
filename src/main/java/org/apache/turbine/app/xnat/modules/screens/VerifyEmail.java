package org.apache.turbine.app.xnat.modules.screens;

import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import org.apache.turbine.services.template.TurbineTemplate;
import org.apache.turbine.util.RunData;
import org.nrg.xdat.XDAT;
import org.nrg.xft.security.UserI;

public class VerifyEmail extends org.nrg.xnat.turbine.modules.screens.VerifyEmail{

    final MultifactorAuthenticationService mfaService =
            XDAT.getContextService().getBean(MultifactorAuthenticationService.class);

    @Override
    public void doRedirect(RunData data, String template) throws Exception {
        UserI user = XDAT.getUserDetails();
        if(user != null && !user.getUsername().equalsIgnoreCase("guest")){
            if(mfaService.isMFARequired(user.getUsername())){
                MFAStrategyI strategy = mfaService.getPreferredMFAStrategy(user.getUsername());
                String registrationTemplatePath = strategy.getRegistrationTemplatePath();
                template = registrationTemplatePath.substring(registrationTemplatePath.lastIndexOf("/"))
                                                   .replace("/","");
            }
        }
        super.doRedirect(data, TurbineTemplate.getScreenName(template), template);
    }
}
