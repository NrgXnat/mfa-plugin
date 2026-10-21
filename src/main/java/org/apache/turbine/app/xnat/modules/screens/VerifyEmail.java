package org.apache.turbine.app.xnat.modules.screens;

import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.template.TemplateService;
import org.nrg.xdat.XDAT;
import org.nrg.xft.security.UserI;

public class VerifyEmail extends org.nrg.xnat.turbine.modules.screens.VerifyEmail{

    final MultifactorAuthenticationService mfaService =
            XDAT.getContextService().getBean(MultifactorAuthenticationService.class);

    @Override
    public void doRedirect(PipelineData pipelineData, String template) throws Exception {
        UserI user = XDAT.getUserDetails();
        if(user != null && !user.getUsername().equalsIgnoreCase("guest")){
            if(mfaService.isMFARequired(user.getUsername())){
                MFAStrategyI strategy = mfaService.getPreferredMFAStrategy(user.getUsername());
                String registrationTemplatePath = strategy.getRegistrationTemplatePath();
                template = registrationTemplatePath.substring(registrationTemplatePath.lastIndexOf("/"))
                                                   .replace("/","");
            }
        }
        // Turbine 7 removed the static TurbineTemplate facade; resolve TemplateService via the service broker
        // (mirrors xnat-web TurbineScreenRepresentation) and use the 3-arg TemplateScreen.doRedirect.
        final TemplateService templateService = (TemplateService) TurbineServices.getInstance().getService(TemplateService.SERVICE_NAME);
        super.doRedirect(pipelineData, templateService.getScreenName(template), template);
    }
}
