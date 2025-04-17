//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.entities;


import org.jboss.aerogear.security.otp.api.Base32;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
@Access(AccessType.FIELD)
public class MultifactorEntity extends AbstractHibernateEntity{

	private static final long serialVersionUID = -3561503604839095331L;
	
	private String   username;
	private String   secret;
	private String   authenticatorSecret;
	private boolean  mfaRegistered = false;
	private boolean  mfaExempted = false;
	private String   preferredMfas;
	private String   tempPreferredMfaBackup;
	
	public MultifactorEntity() { }

	public MultifactorEntity(String username, boolean mfaRegistered) {
		this.username            = username;
		this.mfaRegistered       = mfaRegistered;
		this.secret              = Base32.random();
		this.authenticatorSecret = Base32.random();
	}
	
	public MultifactorEntity(String username) {
		this.username            = username;
		this.secret              = Base32.random();
		this.authenticatorSecret = Base32.random();
	}
	
	@Column(unique = true, nullable = false)
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public boolean isMfaRegistered() {
		return mfaRegistered;
	}
	
	public void setMfaRegistered(boolean registered) {
		// If we are disabling MFA, make sure we generate a new secret for next time. 
		if(this.mfaRegistered && !registered) {
			this.secret              = Base32.random();
			this.authenticatorSecret = Base32.random();
		}
		this.mfaRegistered = registered;
	}

	@Column(nullable = false)
	public String getAuthenticatorSecret() {
		return authenticatorSecret;
	}

	@Column(nullable = false)
	public String getSecret() {
		return secret;
	}
	
	public void setSecret(String secret) { 
		this.secret = secret; 
	}

	public boolean isMfaExempted() {
		return mfaExempted;
	}

	public void setMfaExempted(boolean mfaExempted) {
		this.mfaExempted = mfaExempted;
	}

	public String getPreferredMfas() {
		return preferredMfas;
	}

	/**
	 * @return the preferredMfa
	 */
	public String getPreferredMfasAsJsonArrayString() {
		String[] mfas = preferredMfas.split(",");
		String pMFAAsJsonArray = "";
		for (int i=0; i < mfas.length; i++) {
			pMFAAsJsonArray += "\"" + mfas[i] + "\"," ;
		}
		if (pMFAAsJsonArray.endsWith(",")) {
			pMFAAsJsonArray = pMFAAsJsonArray.substring(0, pMFAAsJsonArray.length()-1);
		}
		return "[" + pMFAAsJsonArray + "]";
	}

	public String getTempPreferredMfaBackup() {
		return tempPreferredMfaBackup;
	}

	public void setTempPreferredMfaBackup(String tempPreferredMfaBackup) {
		this.tempPreferredMfaBackup = tempPreferredMfaBackup;
	}

	/**
	 * @param preferredMfa the preferredMfa to set
	 */
	public void setPreferredMfas(String preferredMfa) {
		this.preferredMfas = preferredMfa;
	}
}