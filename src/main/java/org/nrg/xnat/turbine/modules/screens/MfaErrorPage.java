/**
 * Copyright 2026 XNAT Works, Inc
 *
 * @author Rick Herrick (rick@xnatworks.io)
 */

package org.nrg.xnat.turbine.modules.screens;

import lombok.extern.slf4j.Slf4j;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

@SuppressWarnings("unused")
@Slf4j
public class MfaErrorPage extends SecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        log.debug("Building MFA error page");
    }

    @Override
    protected boolean isAuthorized(final RunData data) {
        return true;
    }
}
