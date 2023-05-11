/**
 * Copyright 2019 Radiologics, Inc
 * @author Mohana Ramaratnam (mohana@radiologics.com)
 *
 */
package com.radiologics.mfa.principal;

import lombok.Data;
import org.nrg.xft.security.UserI;

@Data
public class PrincipalContactInformation {
	private String email;
	private String phonenumber;
	private String ipaddress;
	private String userFirstName;
	private String userLastName;
	private String userFullname;
	private String login;
	private String username;
	
	public PrincipalContactInformation() { }
	
	public PrincipalContactInformation(UserI user) {
		this.setEmail(user.getEmail());
		this.setLogin(user.getLogin());
		this.setUserFirstName(user.getFirstname());
		this.setUserLastName(user.getLastname());
		this.setUserFullname(user.getFirstname() + " " + user.getLastname());
		this.setUsername(user.getUsername());
	}
}
