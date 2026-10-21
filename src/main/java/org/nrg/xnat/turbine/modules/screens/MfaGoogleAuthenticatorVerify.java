/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.pipeline.PipelineData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class MfaGoogleAuthenticatorVerify extends SecureScreen {

	@Override
	protected void doBuildTemplate(PipelineData pipelineData, Context context) {}

    @Override
    protected boolean isAuthorized(PipelineData pipelineData) {
        return true;
    }

}