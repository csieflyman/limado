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
@DiscriminatorValue("organization")
public class Organization extends Party {

    public static final String TYPE = "organization";

    public Organization() {
        setType(TYPE);
    }

    public Organization(String identity) {
        setIdentity(identity);
        setType(TYPE);
    }

    @JsonIgnore
    public Set<User> getUsers() {
        Set<Party> children = getChildren();
        Set users = children.stream().filter(party -> party.getType().equals(User.TYPE)).collect(Collectors.toSet());
        return (Set<User>) users;
    }

    @JsonIgnore
    public Set<Organization> getSubOrganizations() {
        Set<Party> children = getChildren();
        Set organizations = children.stream().filter(party -> party.getType().equals(Organization.TYPE)).collect(Collectors.toSet());
        return (Set<Organization>) organizations;
    }

    @JsonIgnore
    public Organization getSuperOrganization() {
        Set<Party> parents = getParents();
        Optional<Party> organization = parents.stream().filter(party -> party.getType().equals(Organization.TYPE)).findAny();
        return organization.isPresent() ? (Organization) organization.get() : null;
    }
}
