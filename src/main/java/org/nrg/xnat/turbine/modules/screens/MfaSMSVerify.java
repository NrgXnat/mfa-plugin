/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class MfaSMSVerify extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context)
		throws Exception {
	}

    @Override
    protected boolean isAuthorized(RunData data) {
        return true;
    }
}
