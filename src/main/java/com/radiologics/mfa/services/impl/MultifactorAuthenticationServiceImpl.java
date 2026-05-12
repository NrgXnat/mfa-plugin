//Copyright 2019 Radiologics, Inc
//Author: James Ransford <ransfordj@radiologics.com>
//Author: Mohana Ramaratnam <mohana@radiologics.com>
package com.radiologics.mfa.services.impl;

import com.radiologics.mfa.dao.MultifactorDAO;
import com.radiologics.mfa.entities.MultifactorEntity;
import com.radiologics.mfa.exception.MFACodeSendFailedException;
import com.radiologics.mfa.exception.MFAStrategyNotFoundException;
import com.radiologics.mfa.exception.MFAUserExemptException;
import com.radiologics.mfa.exception.MFAUserNotFoundException;
import com.radiologics.mfa.manager.MFAStrategyFactory;
import com.radiologics.mfa.model.MfaModel;
import com.radiologics.mfa.preference.MFAPreferences;
import com.radiologics.mfa.principal.PrincipalContactInformation;
import com.radiologics.mfa.services.MultifactorAuthenticationService;
import com.radiologics.mfa.strategy.MFAStrategyI;
import com.radiologics.mfa.strategy.impl.GoogleAuthenticatorStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.jboss.aerogear.security.otp.api.Base32;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.entities.UserRegistrationData;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.services.UserRegistrationDataService;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
@Slf4j
public class MultifactorAuthenticationServiceImpl extends AbstractHibernateEntityService<MultifactorEntity, MultifactorDAO> implements MultifactorAuthenticationService {
    private final MFAPreferences mfaPreferences;
    private final UserRegistrationDataService userRegistrationDataService;

    public MultifactorAuthenticationServiceImpl(final MFAPreferences mfaPreferences, final UserRegistrationDataService userRegistrationDataService) {
        this.mfaPreferences = mfaPreferences;
        this.userRegistrationDataService = userRegistrationDataService;
    }

    public List<MultifactorEntity> getAllMultifactorEntities() {
        return getDao().getMultifactorEntities();
    }

    public MultifactorEntity getMultifactorAuth(String username) {
        return this.getDao().findByUniqueProperty("username", username);
    }

    public void saveOrUpdate(MultifactorEntity mfe) {
        if (null != mfe) {
            this.getDao().saveOrUpdate(mfe);
        }
    }

    public void save(MultifactorEntity mfe) {
        if (null != mfe) {
            this.update(mfe);
        }
    }

    public Boolean verifyToken(String username, String token) {
        final MultifactorEntity mfe = this.getMultifactorAuth(username);
        if (mfe.isMfaExempted()) {
            return true;
        }

        final MFAStrategyI mfaStrategy;
        try {
            mfaStrategy = getPreferredMFAStrategy(mfe);
        } catch (MFAStrategyNotFoundException e) {
            log.error(e.getMessage(), e);
            return false;
        }

        final Boolean valid = mfaStrategy.verifyToken(mfe, token);
        if (valid && mfaStrategy.needsRegistration()) {
            // Register the user if they aren't registered with the mfa service.
            mfe.setMfaRegistered(true);
            save(mfe);
        }
        return valid;
    }

    public void sendCode(UserI user) throws MFAUserNotFoundException, MFAStrategyNotFoundException, MFAUserExemptException, MFACodeSendFailedException {
        final String username = user.getUsername();
        final MultifactorEntity mfe = getMultifactorAuth(username);

        if (mfe == null) {
            log.debug("Code could not be sent. User ({}) not setup for MFA", username);
            throw new MFAUserNotFoundException();
        }

        if (mfe.isMfaExempted()) {
            log.debug("User ({}) exempted from MFA. Code not sent.", username);
            throw new MFAUserExemptException();
        }

        final MFAStrategyI mfaStrategy = getPreferredMFAStrategy(mfe);

        try {
            mfe.setSecret(Base32.random()); // Make sure we get a new code every time.
            mfaStrategy.sendCode(getPrincipalContactInformation(user), mfe);
            log.debug("Multi-factor Authentication code sent to user: {}", username);
            update(mfe);
        } catch (Exception e) {
            throw new MFACodeSendFailedException();
        }
    }

    public MFAStrategyI getPreferredMFAStrategy(String username) throws MFAStrategyNotFoundException {
        return getPreferredMFAStrategy(this.getMultifactorAuth(username));
    }

    public MFAStrategyI getPreferredMFAStrategy(MultifactorEntity mfe) throws MFAStrategyNotFoundException {
        String preferredMFA = mfaPreferences.getPreferredMFAMethod();
        if (null != mfe) {
            preferredMFA = mfe.getPreferredMfas(); //User preferred MFA
        }
        if (null == preferredMFA) {
            preferredMFA = mfaPreferences.getPreferredMFAMethod();
        }

        final MFAStrategyI strategy = MFAStrategyFactory.GetMFAStrategyByMFAHandlerAnnotation(preferredMFA);
        if (null == strategy) {
            throw new MFAStrategyNotFoundException();
        }
        return strategy;
    }

    public String getPreferredMFAMethod(UserI user) {
        return getPreferredMFAMethod(getMultifactorAuth(user.getUsername()));
    }

    public String getPreferredMFAMethod(String username) {
        return getPreferredMFAMethod(getMultifactorAuth(username));
    }

    public String getPreferredMFAMethod(MultifactorEntity mfe) {
        String userOveriddenPreferredMethod = mfaPreferences.getPreferredMFAMethod();
        if (null == mfe) {
            log.debug("MFE is null - Returning UserOveriddenPreferredMethod: " + userOveriddenPreferredMethod);
            return userOveriddenPreferredMethod;
        }
        userOveriddenPreferredMethod = mfe.getPreferredMfas();
        if (null == userOveriddenPreferredMethod) {
            log.debug("MFE is not null but no preferredMFA");
            userOveriddenPreferredMethod = mfaPreferences.getPreferredMFAMethod();
        }
        return userOveriddenPreferredMethod;
    }

    public MfaModel toPojo(MultifactorEntity mfe) {
        try {
            final MFAStrategyI mfaStrategy = getPreferredMFAStrategy(mfe);
            final MfaModel model = new MfaModel();
            model.setUsername(mfe.getUsername());
            model.setMfaExempted(mfe.isMfaExempted());
            model.setMfaRegistered(mfe.isMfaRegistered());
            model.setMfaNeedsDeviceRegistration(mfaStrategy.needsRegistration());
            model.setMfaPreferred(mfe.getPreferredMfas());

            if (!mfe.isMfaRegistered() && mfaStrategy.needsRegistration() && mfaStrategy instanceof GoogleAuthenticatorStrategy) {
                model.setQrCodeUrl(((GoogleAuthenticatorStrategy) mfaStrategy).generateQRCode(mfe));
            }

            return model;
        } catch (MFAStrategyNotFoundException msNFE) {
            return new MfaModel();
        }
    }

    public PrincipalContactInformation getPrincipalContactInformation(UserI user) {
        try {
            final PrincipalContactInformation contact = new PrincipalContactInformation();
            contact.setLogin(user.getLogin());
            contact.setUsername(user.getUsername());
            contact.setUserFirstName(user.getFirstname());
            contact.setUserLastName(user.getLastname());
            contact.setUserFullname(user.getFirstname() + " " + user.getLastname());
            contact.setEmail(user.getEmail());
            UserRegistrationData userRegistrationData = userRegistrationDataService.getUserRegistrationData(user);
            log.debug(" User Registration data is null {}", (null == userRegistrationData));
            if (null != userRegistrationData) {
                log.debug("Looking up phone number " + userRegistrationData.getPhone());
                contact.setPhonenumber(userRegistrationData.getPhone());
            }
            log.debug("After setting the registration {}", contact.getPhonenumber());
            return contact;
        } catch (Exception e) {
            log.debug("Could not look up user to extract principal contact {}", e.getMessage(), e);
        }
        return null;
    }

    public Boolean isMFARequired(String username) {
        return ((BooleanUtils.toBooleanDefaultIfNull(mfaPreferences.getBooleanValue("requireMfa"), false) && !Roles.isSiteAdmin(username)) || (BooleanUtils.toBooleanDefaultIfNull(mfaPreferences.getBooleanValue("requireAdminMfa"), false) && Roles.isSiteAdmin(username)));
    }

    public MultifactorEntity createMfe(String username) {
        String pref = mfaPreferences.getPreferredMFAMethod();
        MultifactorEntity mfe = new MultifactorEntity(username);
        mfe.setMfaExempted(false);
        if (pref != null) {
            mfe.setPreferredMfas(pref);
        }
        create(mfe);
        return mfe;
    }
}
