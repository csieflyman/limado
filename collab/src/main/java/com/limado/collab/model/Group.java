/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Entity
@DiscriminatorValue("group")
public class Group extends Party {

    public static final String TYPE = "group";

    public Group() {
        setType(TYPE);
    }

    public Group(String identity) {
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
    public Set<Organization> getOrganizations() {
        Set<Party> children = getChildren();
        Set organizations = children.stream().filter(party -> party.getType().equals(Organization.TYPE)).collect(Collectors.toSet());
        return (Set<Organization>) organizations;
    }

    @JsonIgnore
    public Set<Group> getSubGroups() {
        Set<Party> children = getChildren();
        Set groups = children.stream().filter(party -> party.getType().equals(Group.TYPE)).collect(Collectors.toSet());
        return (Set<Group>) groups;
    }

    @JsonIgnore
    public Set<Group> getSuperGroups() {
        Set<Party> parents = getParents();
        Set groups = parents.stream().filter(party -> party.getType().equals(Group.TYPE)).collect(Collectors.toSet());
        return (Set<Group>) groups;
    }
}
