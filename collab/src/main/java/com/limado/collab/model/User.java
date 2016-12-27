package com.limado.collab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Entity
@DiscriminatorValue("user")
public class User extends Party {

    public static final String TYPE = "user";

    public User() {
        setType(TYPE);
    }

    public User(String identity) {
        setIdentity(identity);
        setType(TYPE);
    }

    @JsonIgnore
    public Organization getOrganization() {
        Set<Party> parents = getParents();
        Optional<Party> organization = parents.stream().filter(party -> party.getType().equals(Organization.TYPE)).findAny();
        return organization.isPresent() ? (Organization) organization.get() : null;
    }

    @JsonIgnore
    public Set<Group> getGroups() {
        Set<Party> parents = getParents();
        Set groups = parents.stream().filter(party -> party.getType().equals(Group.TYPE)).collect(Collectors.toSet());
        return (Set<Group>) groups;
    }
}
