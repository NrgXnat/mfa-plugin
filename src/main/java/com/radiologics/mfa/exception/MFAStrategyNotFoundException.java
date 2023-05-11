/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */

package com.radiologics.mfa.exception;

public class MFAStrategyNotFoundException extends Exception {

	private static final long serialVersionUID = 1799869796264127411L;

	
	public MFAStrategyNotFoundException(String string,IllegalArgumentException illegalArgumentException) {
		super(string,illegalArgumentException);
	}

    public MFAStrategyNotFoundException() { }
}
