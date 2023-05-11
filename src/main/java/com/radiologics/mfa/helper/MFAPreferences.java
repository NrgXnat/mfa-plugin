/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.helper;

import lombok.Data;

@Data
public class MFAPreferences {
	public String autoPreferredMFAStrategy;
	public String mfaRedirectPath;
}
