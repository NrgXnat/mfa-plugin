/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class MfaGoogleAuthenticatorVerify extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context) {}

    @Override
    protected boolean isAuthorized(RunData data) {
        return true;
    }

}