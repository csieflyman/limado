package com.limado.collab.mvc.form;

import com.limado.collab.model.Party;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Set;
import java.util.UUID;

/**
 * @author csieflyman
 */
public abstract class PartyForm<T extends Party> implements Form<T> {

    private UUID id;

    private Long version;

    private String type;

    private String identity;

    private String name;

    private String email;

    private Boolean enabled = true;

    private Set<Party> parents;

    private Set<Party> children;

    protected void populatePartyProperties(T party) {
        party.setId(getId());
        party.setVersion(getVersion());
        party.setName(getName());
        party.setIdentity(getIdentity());
        party.setEmail(getEmail());
        party.setEnabled(getEnabled());
        party.setParents(getParents());
        party.setChildren(getChildren());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Party> getParents() {
        return parents;
    }

    public void setParents(Set<Party> parents) {
        this.parents = parents;
    }

    public Set<Party> getChildren() {
        return children;
    }

    public void setChildren(Set<Party> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
