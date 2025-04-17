//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.MFACodeSendFailedException;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
import com.radiologics.mfa.exception.MFAUserExemptException;
import com.radiologics.mfa.exception.MFAUserNotFoundException;
import com.radiologics.mfa.manager.MFAStrategyManager;
import com.radiologics.mfa.model.MfaModel;
import com.radiologics.mfa.model.TokenPayload;
import com.radiologics.mfa.model.VerifiedTokenResponse;
import com.radiologics.mfa.preference.MFAPreferences;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.utils.MFAConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.Username;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.eventservice.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.mail.MessagingException;
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
    private final MFAPreferences mfaPreferences;

    @Autowired
    public MultifactorAuthenticationApi(final UserManagementServiceI userManagementService,
                                        final RoleHolder roleHolder,
                                        final MultifactorAuthenticationService multifactorAuthenticationService,
                                        final MFAPreferences mfaPreferences) {
        super(userManagementService, roleHolder);
        this.multifactorAuthenticationService = multifactorAuthenticationService;
        this.mfaPreferences = mfaPreferences;
    }

    @ApiOperation(response = VerifiedTokenResponse.class, value = "Verifies the MFA token",
            notes = "Returns the JsessionId and the xnat csrf token for the session.")
    @XapiRequestMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public ResponseEntity<VerifiedTokenResponse> verify(@RequestBody TokenPayload tokenPayload, HttpSession httpSession) {
        if (multifactorAuthenticationService.verifyToken(XDAT.getUserDetails().getUsername(), tokenPayload.getToken())) {
            httpSession.setAttribute(MFAConstants.MFA_TOKEN_VERIFIED, true);
            VerifiedTokenResponse responseObj = new VerifiedTokenResponse("Token Verified", httpSession.getId(), (String) httpSession.getAttribute("XNAT_CSRF"));
            return new ResponseEntity<>(responseObj, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @ApiOperation(value = "Configure MFA email back up")
    @XapiRequestMapping(value = {"/emailbackup"}, method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public void configureEmailBackup(
            @RequestParam(name = "emailBackup") final boolean emailBackup,
            @RequestParam(name = "mfaAdminEmailNotification") final boolean mfaAdminEmailNotification) {
        mfaPreferences.setEmailBackupEnabled(emailBackup);
        mfaPreferences.setMfaAdminEmailNotificationEnabled(mfaAdminEmailNotification);
    }

    @ApiOperation(value = "Get MFA email back up configuration. By default it is true")
    @XapiRequestMapping(value = {"/preference"}, method = RequestMethod.GET, restrictTo = AccessLevel.Authenticated)
    public MFAPreferences getMfaPreferences() {
        return mfaPreferences;
    }

    @ApiOperation(value = "Switch to email MFA")
    @XapiRequestMapping(value = {"/switch_to_email"}, method = RequestMethod.POST, restrictTo = AccessLevel.Authenticated)
    public void switchToEmail(HttpSession httpSession) throws MessagingException {
        if (!mfaPreferences.isEmailBackupEnabled()) {
            return;
        }
        final UserI user = XDAT.getUserDetails();
        MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
        mfe.setTempPreferredMfaBackup(mfe.getPreferredMfas());
        mfe.setPreferredMfas("Email");
        multifactorAuthenticationService.update(mfe);
        httpSession.setAttribute("EmailBackup", true);
        httpSession.setAttribute(MFAConstants.MFA_TOKEN_SENT, false);
        if (mfaPreferences.isMfaAdminEmailNotificationEnabled()) {
            sendAlertEmail(user.getUsername());
        }
    }

    @ApiOperation(value = "Configure Multi-factor Authentication")
    @XapiRequestMapping(value = {"/configure"}, method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public void configureMFA(@RequestParam(name = "requireMfa") final boolean requireMfa,
                             @RequestParam(name = "requireAdminMfa") final boolean requireAdminMfa) {
        boolean mfaRequired = mfaPreferences.isRequireMfa();
        boolean mfaAdminRequired = mfaPreferences.isRequireAdminMfa();

        if (requireMfa == mfaRequired && requireAdminMfa == mfaAdminRequired) {
            return;
        }
        mfaPreferences.setRequireMfa(requireMfa);
        mfaPreferences.setRequireAdminMfa(requireAdminMfa);

        //If the Site Admins need MFA, check the hibernate table and set all the Admin Roles to non-exempt
        Collection<String> allLogins = Users.getAllLogins();
        for (String aLogin : allLogins) {
            final MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(aLogin);
            if (Roles.isSiteAdmin(aLogin)) {
                if (requireAdminMfa) {
                    if (null == mfe) {
                        MultifactorEntity mfeNew = new MultifactorEntity(aLogin);
                        mfeNew.setMfaRegistered(false);
                        //Admin needs MFA = Exempted  = false
                        mfeNew.setMfaExempted(false);
                        mfeNew.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
                        multifactorAuthenticationService.create(mfeNew);
                    } else {
                        mfe.setMfaExempted(false);
                        multifactorAuthenticationService.update(mfe);
                    }
                } else { // does not require MFA
                    if (null != mfe && !mfe.isMfaExempted()) {
                        mfe.setMfaExempted(true);
                        multifactorAuthenticationService.update(mfe);
                    }
                }
            } else { //Non admin account
                if (requireMfa) {
                    if (null == mfe) {
                        MultifactorEntity mfeNew = new MultifactorEntity(aLogin);
                        mfeNew.setMfaRegistered(false);
                        //Non Admin needs MFA = Exempted  = false
                        mfeNew.setMfaExempted(false);
                        mfeNew.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
                        multifactorAuthenticationService.create(mfeNew);
                    } else {
                        if (mfe != null && mfe.isMfaExempted()) {
                            mfe.setMfaExempted(false);
                            multifactorAuthenticationService.update(mfe);
                        }
                    }
                } else {
                    if (mfe != null && !mfe.isMfaExempted()) {
                        mfe.setMfaExempted(true);
                        multifactorAuthenticationService.update(mfe);
                    }
                }
            }
        }
    }

    @ApiOperation(response = MfaModel.class, value = "Returns whether the user has registered for mfa")
    @XapiRequestMapping(value = {"/status", "/{username}/status"}, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public MfaModel getStatus(@ApiParam(value = "The username of the user we are interested in")
                                              @PathVariable(name = "username", required = false) @Username final String username) throws UserNotFoundException, UserInitException, UnauthorizedException {
        UserI targetUser = null;
        if (username != null) {
            targetUser = Users.getUser(username);
        }
        final UserI user = XDAT.getUserDetails();
        final boolean mfaUserRequired = mfaPreferences.isRequireMfa();
        final boolean mfaAdminRequired = mfaPreferences.isRequireAdminMfa();

        if (Roles.isSiteAdmin(user)) {
            if (targetUser != null) {
                if (Roles.isSiteAdmin(targetUser)) {
                    return getMfaModel(username, mfaAdminRequired);
                } else {
                    return getMfaModel(username, mfaUserRequired);
                }
            } else {
                return getMfaModel(user.getUsername(), mfaAdminRequired);
            }
        } else {
            if (targetUser != null && !username.equals(user.getUsername())) {
                throw new UnauthorizedException("User does not have permission access the MFA status");
            }
            return getMfaModel(user.getUsername(), mfaUserRequired);
        }
    }

    private MfaModel getMfaModel(String username, boolean mfaRequired) {
        if (mfaRequired) {
            MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username);
            if (mfe == null) {
                mfe = multifactorAuthenticationService.createMfe(username);
            }
            return multifactorAuthenticationService.toPojo(mfe);
        } else {
            return new MfaModel();
        }
    }

    @ApiOperation(response = MfaModel.class, value = "Gets the Multi-factor Authentication config for the user. If no configuration exists, a new configuration is created")
    @XapiRequestMapping(value = {""}, produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    public ResponseEntity<MfaModel> getMultifactorAuth() {
        final UserI user = XDAT.getUserDetails();
        MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(user.getUsername());
        final boolean mfaRequired = mfaPreferences.isRequireMfa();

        if (mfaRequired && (mfe == null)) {
            mfe = new MultifactorEntity(user.getUsername());
            String pref = multifactorAuthenticationService.getPreferredMFAMethod(mfe);
            if (pref != null) {
                mfe.setPreferredMfas(pref);
            }
            if (Roles.isSiteAdmin(user)) {
                mfe.setMfaExempted(!mfaPreferences.isRequireAdminMfa());
            } else {
                mfe.setMfaExempted(false);
            }
            mfe = multifactorAuthenticationService.create(mfe);
        }
        return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
    }


    @ApiOperation(response = MfaModel.class, value = "Unregister Device for the specified user",
            notes = "Returns the modified Multi-factor Authentication configuration for the user")
    @XapiRequestMapping(value = {"/{username}/unregister", "/unregister"}, method = RequestMethod.POST)
    public ResponseEntity<MfaModel> unregisterMFA(@ApiParam(value = "The username of the user we want to update")
                                                  @PathVariable(name = "username", required = false) @Username final String username) {
        final UserI user = XDAT.getUserDetails();
        final String _username = username == null ? user.getUsername() : username;

        if ((_username.equals(user.getUsername()) && mfaPreferences.isRequireMfa()) || Roles.isSiteAdmin(user)) {
            MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(_username);
            mfe.setMfaRegistered(false);
            mfe.setTempPreferredMfaBackup(null);
            mfe.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
            multifactorAuthenticationService.save(mfe);
            log.debug("Unregistered Multifactor Authentication for user: " + _username);
            return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(response = String.class, value = "Sends MFA Code to user")
    @XapiRequestMapping(value = {"/send_code"}, method = RequestMethod.POST)
    public ResponseEntity<Void> sendCode(HttpSession httpSession) {
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
        if (Roles.isSiteAdmin(user)) {
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
        if (Roles.isSiteAdmin(user)) {
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
                mfaPreferences.setPreferredMFAMethod(mfamethod);
                if (switchAll) {
                    //Now is all users are to be switched - update all MFA entries
                    List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
                    for (Object m : mfes) {
                        MultifactorEntity mfe = (MultifactorEntity) m;
                        mfe.setPreferredMfas(mfamethod);
                        //Users need to register again
                        mfe.setMfaRegistered(false);
                        multifactorAuthenticationService.saveOrUpdate(mfe);
                    }
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
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
        if (Roles.isSiteAdmin(user)) {
            String pref = mfaPreferences.getPreferredMFAMethod();
            if (pref != null) {
                return new ResponseEntity<>(pref, HttpStatus.OK);
            } else {
                log.debug("No site wide preferred MFA set");
                return new ResponseEntity<>(HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(response = String.class, value = "Gets the list of Users exempted from Multi-factor Authentication")
    @XapiRequestMapping(value = "/exempted", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = AccessLevel.Admin)
    public ResponseEntity<String> getMFAExemptedUser() {
        List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
        List<MultifactorEntity> exempted = new ArrayList<MultifactorEntity>();
        if (null != mfes && mfes.size() > 0) {
            for (Object m : mfes) {
                MultifactorEntity mfe = (MultifactorEntity) m;
                if (mfe.isMfaExempted()) {
                    exempted.add((MultifactorEntity) m);
                }
            }
        }

        return new ResponseEntity<>(buildExemptedMFAUsersJson(exempted, "exempted_users"), HttpStatus.OK);
    }

    @ApiOperation(response = String.class, value = "Gets the list of Users from Multi-factor Authentication")
    @XapiRequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, restrictTo = AccessLevel.Admin)
    public ResponseEntity<String> getMFAs() {
        List mfes = multifactorAuthenticationService.getAllMultifactorEntities();
        List<MultifactorEntity> users = new ArrayList<MultifactorEntity>();
        if (null != mfes && mfes.size() > 0) {
            for (Object m : mfes) {
                users.add((MultifactorEntity) m);
            }
        }

        return new ResponseEntity<>(buildExemptedMFAUsersJson(users, "mfa_users"), HttpStatus.OK);
    }

    @ApiOperation(response = MfaModel.class, value = "Sets a User  as exempted from Multi-factor Authentication")
    @XapiRequestMapping(value = "/exempt/{username}/{exempted}", method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public ResponseEntity<MfaModel> exemptUser(@PathVariable(name = "username") @Username final String username,
                                               @PathVariable(name = "exempted") final boolean exempted) {
        final UserI user = XDAT.getUserDetails();

        if (Roles.isSiteAdmin(user)) {
            if (mfaPreferences.isRequireMfa()) {
                MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username);
                if (null != mfe) {
                    mfe.setMfaExempted(exempted);
                    multifactorAuthenticationService.save(mfe);
                    return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
                } else {
                    mfe = new MultifactorEntity(username);
                    mfe.setMfaExempted(exempted);
                    String pref = multifactorAuthenticationService.getPreferredMFAMethod(mfe);
                    mfe.setPreferredMfas(pref);
                    multifactorAuthenticationService.create(mfe);
                    return new ResponseEntity<>(multifactorAuthenticationService.toPojo(mfe), HttpStatus.OK);
                }
            } else {
                new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(value = "Sets a CSV Username String  as exempted from Multi-factor Authentication")
    @XapiRequestMapping(value = "/exemptmultiple", method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public ResponseEntity<Void> exemptUsers(@RequestParam(name = "usernames", required = true) final String csvUsernames) {
        final UserI user = XDAT.getUserDetails();
        if (Roles.isSiteAdmin(user)) {
            String[] usernames = csvUsernames.split(",");
            for (String username : usernames) {
                if (Users.exists(username)) {
                    MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username.trim());
                    if (null != mfe) {
                        mfe.setMfaExempted(true);
                        multifactorAuthenticationService.save(mfe);
                    } else {
                        mfe = new MultifactorEntity(username);
                        mfe.setMfaExempted(true);
                        mfe.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
                        multifactorAuthenticationService.create(mfe);
                    }
                }
            }
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(value = "Enforces Multi-factor Authentication for multiple user")
    @XapiRequestMapping(value = "/enforcemfamultiple", method = RequestMethod.POST, restrictTo = AccessLevel.Admin)
    public ResponseEntity<Void> enforceMFAForMultipleUsers(@RequestParam(name = "usernames", required = true) final String csvUsernames) {
        final UserI user = XDAT.getUserDetails();

        if (Roles.isSiteAdmin(user)) {
            if (mfaPreferences.isRequireMfa()) {
                String[] usernames = csvUsernames.split(",");
                for (String username : usernames) {
                    if (Users.exists(username)) {
                        MultifactorEntity mfe = multifactorAuthenticationService.getMultifactorAuth(username.trim());
                        if (null != mfe) {
                            mfe.setMfaExempted(false);
                            multifactorAuthenticationService.save(mfe);
                        } else {
                            mfe = new MultifactorEntity(username);
                            mfe.setMfaExempted(false);
                            mfe.setMfaRegistered(false);
                            mfe.setPreferredMfas(mfaPreferences.getPreferredMFAMethod());
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
    public ResponseEntity<Void> setPreferredMFAMethodForUser(@PathVariable(name = "username") @Username final String username,
                                                             @PathVariable(name = "mfamethod") String mfamethod) {
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
                    if (null != mfe) {
                        mfe.setPreferredMfas(mfamethod);
                        multifactorAuthenticationService.saveOrUpdate(mfe);
                        return new ResponseEntity<>(HttpStatus.OK);
                    } else {
                        mfe = multifactorAuthenticationService.create(new MultifactorEntity(username));
                        mfe.setPreferredMfas(mfamethod);
                        multifactorAuthenticationService.save(mfe);
                        return new ResponseEntity<>(HttpStatus.OK);
                    }
                } else {
                    log.debug("Invalid MFA Method");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                log.debug("Insufficient privileges to set preferred MFA " + username);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (UserNotFoundException | UserInitException unfe) {
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
                if (null != code) {
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

    private void sendAlertEmail(String username) throws MessagingException {
        final String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
        final String emailSubject = XDAT.getSiteId() + ": Multi-Factor Authentication Alert";
        VelocityContext context = new VelocityContext();
        context.put("user", username);
        context.put("siteLogoPath", XDAT.getSiteLogoPath());
        context.put("system", TurbineUtils.GetSystemName());
        context.put("server", XDAT.getSiteUrl());
        context.put("contact_email", XDAT.getNotificationsPreferences().getHelpContactInfo());
        String body = AdminUtils.populateVmTemplate(context, mfaPreferences.getAdminAlertTextTemplate());

        try {
            XDAT.getMailService().sendHtmlMessage(adminEmail, adminEmail, emailSubject, body);
        } catch (Exception e) {
            log.error("Could not send admin alert Email.", e.getMessage());
            throw e;
        }
    }
}