/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.validator;

import com.limado.collab.model.Party;
import com.limado.collab.mvc.form.PartyForm;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

/**
 * @author csieflyman
 */
@Controller
public class PartyFormValidator implements Validator{
    private static final Logger log = LogManager.getLogger(PartyFormValidator.class);

    private static final Pattern IDENTITY_PATTERN = Pattern.compile("^[a-zA-z]([\\w\\_\\-])+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

    @Override
    public boolean supports(Class<?> clazz) {
        return PartyForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PartyForm<Party> form = (PartyForm) target;
        Party party = form.buildModel();
        if(!checkIdentity(party.getIdentity())) {
            errors.rejectValue("identity", "invalid.party.identity", new String[] {party.getIdentity()}, "invalid identity: " + party.getIdentity());
        }
        if(!checkName(party.getName())) {
            errors.rejectValue("name", "invalid.party.name", new String[] {party.getName()}, "invalid name: " + party.getName());
        }
        if(!checkEmail(party.getEmail())) {
            errors.rejectValue("email", "invalid.party.email", new String[] {party.getEmail()}, "invalid email: " + party.getEmail());
        }
    }

    private boolean checkIdentity(String identity) {
        return StringUtils.isNotBlank(identity) && identity.length() <= 16 && IDENTITY_PATTERN.matcher(identity).matches();
    }

    private boolean checkName(String name) {
        return StringUtils.isNotBlank(name) && name.length() <= 16;
    }

    private boolean checkEmail(String email) {
        return StringUtils.isEmpty(email) || (StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches());
    }
}
