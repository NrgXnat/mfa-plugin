//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.*;
import com.radiologics.mfa.helper.MFAPreferencesHelper;
import com.radiologics.mfa.manager.MFAStrategyManager;
import com.radiologics.mfa.model.MfaModel;
import com.radiologics.mfa.model.TokenPayload;
import com.radiologics.mfa.model.VerifiedTokenResponse;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.exceptions.UnknownToolId;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.Username;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Api("Multifactor Authentication Api")
@XapiRestController
@RequestMapping(value = "/mfa")
@Slf4j
public class MultifactorAuthenticationApi extends AbstractXapiRestController {

	private final MultifactorAuthenticationService multifactorAuthenticationService;
	private final SiteConfigPreferences            siteConfigPreferences;
	private final MFAPreferencesHelper             mfaPreferencesHelper;

	@Autowired
	public MultifactorAuthenticationApi(final UserManagementServiceI userManagementService, 
										final RoleHolder roleHolder,
										final MultifactorAuthenticationService multifactorAuthenticationService,
										final MFAPreferencesHelper mfaPreferencesHelper,
										final SiteConfigPreferences siteConfigPreferences) {
		super(userManagementService, roleHolder);

		this.multifactorAuthenticationService = multifactorAuthenticationService;
		this.siteConfigPreferences            = siteConfigPreferences;
		this.mfaPreferencesHelper             = mfaPreferencesHelper;
	}

	@ApiOperation(response = VerifiedTokenResponse.class, value = "Verifies the MFA token",
				  notes = "Returns the JsessionId and the xnat csrf token for the session.")
	@XapiRequestMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public ResponseEntity<VerifiedTokenResponse> verify(@RequestBody TokenPayload tokenPayload, HttpSession httpSession) {
		if(multifactorAuthenticationService.verifyToken(XDAT.getUserDetails().getUsername(), tokenPayload.getToken())) {
			httpSession.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
			VerifiedTokenResponse responseObj = new VerifiedTokenResponse("Token Verified", httpSession.getId(), (String) httpSession.getAttribute("XNAT_CSRF"));
			return new ResponseEntity<>(responseObj,HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	
	@ApiOperation(value = "Configure Multi-factor Authentication")
	@XapiRequestMapping(value = {"/configure"},  method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
	public ResponseEntity<Void> configureMFA(@RequestParam(name="requireMfa")       final boolean requireMfa,
											 @RequestParam(name="requireAdminMfa")  final boolean requireAdminMfa) {
		boolean mfaRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA), false);
		boolean mfaAdminRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA), false);
		boolean changeDetected = false;

		try{
			if (requireMfa != mfaRequired) {
				changeDetected = true;
				siteConfigPreferences.setBooleanValue(requireMfa, MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA);
			}
			if (requireAdminMfa != mfaAdminRequired) {
				changeDetected = true;
				siteConfigPreferences.setBooleanValue(requireAdminMfa, MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA);
			}
		}catch(UnknownToolId | InvalidPreferenceName e ){
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!changeDetected) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		//If the Site Admins need MFA, check the hibernate table and set all the Admin Roles to non-exempt
		Collection<String> allLogins = Users.getAllLogins();
		for (String aLogin : allLogins) {
			final MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(aLogin);
			if (Roles.isSiteAdmin(aLogin)) {
				if (requireAdminMfa ) {
					if (null == mfe) {
						MultifactorEntity mfeNew = new MultifactorEntity(aLogin);
						mfeNew.setMfaRegistered(false);
						//Admin needs MFA = Exempted  = false
						mfeNew.setMfaExempted(false);
						mfeNew.setPreferredMfas(mfaPreferencesHelper.getPreferredMFAMethod());
						multifactorAuthenticationService.create(mfeNew);
					}else {
						mfe.setMfaExempted(false);
						multifactorAuthenticationService.update(mfe);
					}
				}else { // does not require MFA
					if (null != mfe && !mfe.isMfaExempted()) {
						mfe.setMfaExempted(true);
						multifactorAuthenticationService.update(mfe);
					}
				}
			}else { //Non admin account
				if (requireMfa) {
					if (null == mfe) {
						MultifactorEntity mfeNew = new MultifactorEntity(aLogin);
						mfeNew.setMfaRegistered(false);
						//Non Admin needs MFA = Exempted  = false
						mfeNew.setMfaExempted(false);
						mfeNew.setPreferredMfas(mfaPreferencesHelper.getPreferredMFAMethod());
						multifactorAuthenticationService.create(mfeNew);
					}else {
						if (mfe != null && mfe.isMfaExempted()) {
							mfe.setMfaExempted(false);
							multifactorAuthenticationService.update(mfe);
						}
					}
				}else {
						if (mfe != null && !mfe.isMfaExempted()) {
							mfe.setMfaExempted(true);
							multifactorAuthenticationService.update(mfe);
						}
				}
			}
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ApiOperation(response = MfaModel.class, value = "Returns whether the user has registered for mfa")
	@XapiRequestMapping(value = {"/status", "/{username}/status"}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
	public ResponseEntity<MfaModel> getStatus(@ApiParam(value = "The username of the user we are interested in")
											  @PathVariable(name = "username", required = false)  @Username final String username) {
		final UserI user = XDAT.getUserDetails();
		boolean mfaRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA), false);

		if(username != null) {
			if(Roles.isSiteAdmin(user)){
				final MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username);
				if(mfe != null) {
					if (mfaRequired) {
						return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe),HttpStatus.OK);
					}else {
						return new ResponseEntity<>(HttpStatus.NO_CONTENT);
					}
				}else {
					//Is MFA enforced? If yes, setup the user for MFA
					if (mfaRequired) {
						String pref = mfaPreferencesHelper.getPreferredMFAMethod();
						MultifactorEntity mfe1 = new MultifactorEntity(username);
						mfe1.setMfaExempted(false);
						if (pref != null) { 
							mfe1.setPreferredMfas(pref);
						}
						 multifactorAuthenticationService.create(mfe1);
						return	new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe1),HttpStatus.OK);
					}else {
						return new ResponseEntity<>(HttpStatus.NO_CONTENT);
					}
				}
			}
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		
		MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
		if(mfe == null) {
			if (mfaRequired) {
				String pref  = multifactorAuthenticationService.getPreferredMFAMethod(user.getUsername());
				mfe = new MultifactorEntity(user.getUsername());
				if (pref != null) { 
					mfe.setPreferredMfas(pref);
				}
				mfe.setMfaExempted(false);
				mfe = multifactorAuthenticationService.create(mfe);
				return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe),HttpStatus.OK);
			}else {
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}
		}else {
			if (mfaRequired) {
				return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe),HttpStatus.OK);
			}else {
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}
		}
	}
	
	
	@ApiOperation(response = MfaModel.class, value = "Gets the Multi-factor Authentication config for the user. If no configuration exists, a new configuration is created")
	@XapiRequestMapping(value = {""}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
	public ResponseEntity<MfaModel> getMultifactorAuth() {
		final UserI user = XDAT.getUserDetails();
		MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
		final boolean mfaRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA), false);

		if(mfaRequired && (mfe == null)) {
			mfe = new MultifactorEntity(user.getUsername());
			String pref  = multifactorAuthenticationService.getPreferredMFAMethod(mfe);
			if (pref != null) {
				mfe.setPreferredMfas(pref);
			}
			if (Roles.isSiteAdmin(user)) {
				Boolean siteConfigAdminRequireMFA = BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_ADMIN_REQUIRE_MFA), false);
				boolean doesAdminRequireMFA = BooleanUtils.toBooleanDefaultIfNull(siteConfigAdminRequireMFA, false);
				mfe.setMfaExempted(!doesAdminRequireMFA);
			}else {
				mfe.setMfaExempted(false);
			}
 			mfe = multifactorAuthenticationService.create(mfe);
		}
		return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
	}

	
	@ApiOperation(response = MfaModel.class, value = "Unregister Device for the specified user",
				 notes = "Returns the modified Multi-factor Authentication configuration for the user")
	@XapiRequestMapping(value = {"/{username}/unregister","/unregister"},  method = RequestMethod.POST)
	public ResponseEntity<MfaModel> unregisterMFA(@ApiParam(value = "The username of the user we want to update")
												  @PathVariable(name = "username", required = false)  @Username final String username) {
		final UserI user = XDAT.getUserDetails();
		final String _username = username == null ? user.getUsername() : username;
		
		if((_username.equals(user.getUsername()) && !BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue("mfaRequired"),false)) || Roles.isSiteAdmin(user)) {
			MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(_username);
			mfe.setMfaRegistered(false);
			multifactorAuthenticationService.save(mfe);
			log.debug("Unregistered Multifactor Authentication for user: " + _username);
			return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
		}	
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ApiOperation(response = String.class, value = "Sends MFA Code to user")
	@XapiRequestMapping(value = {"/send_code"},  method = RequestMethod.POST)
	public ResponseEntity<Void> sendCode( HttpSession httpSession ) {
		try {
			multifactorAuthenticationService.sendCode(XDAT.getUserDetails());
			httpSession.setAttribute(MFAConstants.MFA_TOKEN_SENT, true);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (MFAUserNotFoundException | MFAUserExemptException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (MFAStrategyNotFoundException | MFACodeSendFailedException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@ApiOperation(response = String.class, value = "Gets the Site wide Multi-factor Authentication Methods")
	@XapiRequestMapping(value = {"/methods"}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
	public ResponseEntity<String> getAvailableMFAMethods() {
		//Only Admins get this information
		final UserI user = XDAT.getUserDetails();
		if(Roles.isSiteAdmin(user)){
			List<MFAStrategyI> strategies = MFAStrategyManager.GetAvailableStrategies();
			return new ResponseEntity<>(this.generateMfaMethodResponseJson(strategies), HttpStatus.OK);
		}		
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ApiOperation(value = "Sets  the Site wide preferred Multi-factor Authentication Method")
	@XapiRequestMapping(value = "/preferred/{mfamethod}", method = RequestMethod.POST)
    public ResponseEntity<Void> setPreferredMFAMethod(@PathVariable final String mfamethod,
													  @RequestParam final boolean switchAll) {
		//Only Admins can set this information
		final UserI user = XDAT.getUserDetails();
		if(Roles.isSiteAdmin(user)){
	   		List<String> annotationCodes = MFAStrategyManager.GetAvailableStrategyAnnotationCodes();
	   		//Is it an existing code
	   		boolean existing = false;
	   		if (annotationCodes.size() > 0) {
		   		for (String code : annotationCodes) {
		   			if (code.equals(mfamethod)) {
		   				existing = true; 
		   				break;
		   			}
		   		}
	   		}
	   		if (existing) {
	   			try {
	   				mfaPreferencesHelper.setPreferredMFAMethod(mfamethod);
	   			    if (switchAll) {
		   				//Now is all users are to be switched - update all MFA entries
	   			    	List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
	   			    	for (Object m : mfes) {
	   			    		MultifactorEntity mfe = (MultifactorEntity)m;
	   			    		mfe.setPreferredMfas(mfamethod);
	   			    		//Users need to register again
	   			    		mfe.setMfaRegistered(false);
	   			    		multifactorAuthenticationService.saveOrUpdate(mfe);
	   			    	}
	   			    }
	   			}catch(InvalidPreferenceName ipn) {
					log.error("Unable to save preference", ipn);
					return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	   			}
				return new ResponseEntity<>(HttpStatus.OK);
	   		}else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	   		}
		}		
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	
	@ApiOperation(response = String.class, value = "Gets  the Site wide preferred Multi-factor Authentication Method")
	@XapiRequestMapping(value = "/preferred", method = RequestMethod.GET)
    public ResponseEntity<String> getPreferredMFAMethod() {
		//Only Admins can set this information
		final UserI user = XDAT.getUserDetails();
		if(Roles.isSiteAdmin(user)){
	   		String pref = mfaPreferencesHelper.getPreferredMFAMethod();
	   		if (pref != null) {
				   return new ResponseEntity<>(pref,HttpStatus.OK);
	   		}else {
				   log.debug("No site wide preferred MFA set");
				   return new ResponseEntity<>(HttpStatus.OK);
	   		}
		}		
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ApiOperation(response = String.class, value = "Gets the list of Users exempted from Multi-factor Authentication")
	@XapiRequestMapping(value = "/exempted", method = RequestMethod.GET,  produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = AccessLevel.Admin)
    public ResponseEntity<String> getMFAExemptedUser() {
		List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
		List<MultifactorEntity> exempted = new ArrayList<MultifactorEntity>();
		if (null != mfes && mfes.size() > 0) {
			for (Object m : mfes) {
				MultifactorEntity mfe = (MultifactorEntity)m;
				if (mfe.isMfaExempted()) {
					exempted.add((MultifactorEntity)m);
				}
			}
		}
		
		return new ResponseEntity<>(buildExemptedMFAUsersJson(exempted,"exempted_users"), HttpStatus.OK);
	}

	@ApiOperation(response = String.class, value = "Gets the list of Users from Multi-factor Authentication")
	@XapiRequestMapping(value = "/users", method = RequestMethod.GET,  produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = AccessLevel.Admin)
    public ResponseEntity<String> getMFAs() {
		List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
		List<MultifactorEntity> users = new ArrayList<MultifactorEntity>();
		if (null != mfes && mfes.size() > 0) {
			for (Object m : mfes) {
				users.add((MultifactorEntity)m);
			}
		}
		
		return new ResponseEntity<>(buildExemptedMFAUsersJson(users,"mfa_users"), HttpStatus.OK);
	}

	@ApiOperation(response = MfaModel.class, value = "Sets a User  as exempted from Multi-factor Authentication")
	@XapiRequestMapping(value = "/exempt/{username}/{exempted}", method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public ResponseEntity<MfaModel> exemptUser(@PathVariable(name="username") @Username final String username,
											   @PathVariable(name="exempted") final boolean exempted){
		final UserI user = XDAT.getUserDetails();
		final boolean mfaRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA), false);

		if(Roles.isSiteAdmin(user)){
			if (mfaRequired) {
				MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username);
				if (null != mfe) {
					mfe.setMfaExempted(exempted);
					multifactorAuthenticationService.save(mfe);
					return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
				}else {
					mfe = new MultifactorEntity(username);
					mfe.setMfaExempted(exempted);
					String pref =  multifactorAuthenticationService.getPreferredMFAMethod(mfe);
					mfe.setPreferredMfas(pref);
					multifactorAuthenticationService.create(mfe);
					return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
				}
			}else {
				new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ApiOperation(value = "Sets a CSV Username String  as exempted from Multi-factor Authentication")
	@XapiRequestMapping(value = "/exemptmultiple", method = RequestMethod.POST,   restrictTo = AccessLevel.Admin)
    public ResponseEntity<Void> exemptUsers(@RequestParam(name="usernames", required = true)  final String csvUsernames){
		final UserI user = XDAT.getUserDetails();
		if(Roles.isSiteAdmin(user)){
			String[] usernames = csvUsernames.split(",");
			for (String username : usernames) {
				if (Users.exists(username)) {
					MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username.trim());
					if (null != mfe) {
						mfe.setMfaExempted(true);
						multifactorAuthenticationService.save(mfe);
					}else {
						mfe = new MultifactorEntity(username);
						mfe.setMfaExempted(true);
						String pref =  mfaPreferencesHelper.getPreferredMFAMethod();
						mfe.setPreferredMfas(pref);
						multifactorAuthenticationService.create(mfe);
					}
				}
			}
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@ApiOperation(	value = "Enforces Multi-factor Authentication for multiple user")
	@XapiRequestMapping(value = "/enforcemfamultiple", method = RequestMethod.POST,  restrictTo = AccessLevel.Admin)
    public ResponseEntity<Void> enforceMFAForMultipleUsers(@RequestParam(name="usernames", required = true)  final String csvUsernames){
		final UserI user = XDAT.getUserDetails();
		final boolean mfaRequired =  BooleanUtils.toBooleanDefaultIfNull(siteConfigPreferences.getBooleanValue(MFAConstants.MFA_SITE_CONFIG_REQUIRE_MFA), false);

		if(Roles.isSiteAdmin(user)){
			if (mfaRequired) {
				String[] usernames = csvUsernames.split(",");
				for (String username : usernames) {
					if (Users.exists(username)) {
						MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username.trim());
						if (null != mfe) {
							mfe.setMfaExempted(false);
							multifactorAuthenticationService.save(mfe);
						}else {
							mfe = new MultifactorEntity(username);
							mfe.setMfaExempted(false);
							mfe.setMfaRegistered(false);
							String pref =  mfaPreferencesHelper.getPreferredMFAMethod();
							mfe.setPreferredMfas(pref);
							multifactorAuthenticationService.create(mfe);
						}
					}
				}
				return new ResponseEntity<>(HttpStatus.OK);
			}
		}
		log.debug("User enforcement status could not be modified. Only site admins can enforce.");
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	
	@ApiOperation(value = "Sets  the Preferred Multi-factor Authentication Method for user")
	@XapiRequestMapping(value = "/preferred/user/{username}/method/{mfamethod}", method = RequestMethod.POST)
    public ResponseEntity<Void> setPreferredMFAMethodForUser(@PathVariable(name="username") @Username final String username,
															   @PathVariable(name="mfamethod") String mfamethod) {
		final UserI user = XDAT.getUserDetails();
		try {
			UserI requestedUser = Users.getUser(username);
			if (Roles.isSiteAdmin(user) || username.equals(user.getUsername())) {
				List<String> annotationCodes = MFAStrategyManager.GetAvailableStrategyAnnotationCodes();
				//Is it an existing code
				boolean existing = false;
				if (annotationCodes.size() > 0) {
					for (String code : annotationCodes) {
						if (code.equals(mfamethod)) {
							existing = true;
							break;
						}
					}
				}
				if (existing) {
						MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(requestedUser.getUsername());
						if(null != mfe) {
							mfe.setPreferredMfas(mfamethod);
							multifactorAuthenticationService.saveOrUpdate(mfe);
							return new ResponseEntity<>( HttpStatus.OK);
						}else {
							mfe = multifactorAuthenticationService.create(new MultifactorEntity(username));
							mfe.setPreferredMfas(mfamethod);
							multifactorAuthenticationService.save(mfe);
							return new ResponseEntity<>(HttpStatus.OK);
						}
				}else {
				   log.debug("Invalid MFA Method");
				   return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}else {
				log.debug("Insufficient privileges to set preferred MFA " + username);
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
		} catch(UserNotFoundException | UserInitException unfe) {
			log.debug("Invalid Username " + username);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	
	private String buildExemptedMFAUsersJson(List<MultifactorEntity> mfes, String rootNode) {
		if (null != mfes && mfes.size() > 0) {
	   		ObjectMapper mapper = new ObjectMapper();
	   		ObjectNode users = mapper.createObjectNode();
	   		ArrayNode usersNode = mapper.createArrayNode();
	   		for (MultifactorEntity mfe : mfes) {
	   			JsonNode node = mapper.valueToTree(mfe);
	   			usersNode.add(node);
	   		}
	   		users.set(rootNode, usersNode);
	   		try {
	   			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(users);
	   			return json;
	   		} catch (Exception e) {
	   		   log.error("Could not convert the MFA Methods to JSON: {}", e.getMessage(), e);
	   		}	
	   	}
	   	return "{}";
	}
	
	private String generateMfaMethodResponseJson(List<MFAStrategyI> strategies) {
	   	if (null != strategies && strategies.size() > 0) {
	   		ObjectMapper mapper = new ObjectMapper();
	   		ObjectNode mfaMethods = mapper.createObjectNode();
	   		ArrayNode mfaMethodsNode = mapper.createArrayNode();
	   		List<String> annotationCodes = MFAStrategyManager.GetAvailableStrategyAnnotationCodes();
	   		for (String code : annotationCodes) {
	   			if (null != code ) {
	   				mfaMethodsNode.add(code);
	   			}
	   		}
	   		mfaMethods.set(MFAConstants.MFA_METHODS, mfaMethodsNode);
	   		try {
	   			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mfaMethods);
	   			return json;
	   		} catch (Exception e) {
	   		   log.error("Could not convert the MFA Methods to JSON: {}", e.getMessage(), e);
	   		}	
	   	}
	   	return "{}";
	}
}