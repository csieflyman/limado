package com.limado.collab.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * author flyman
 */
@Entity
@Table(name = "party")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = User.class, name = User.TYPE),
        @JsonSubTypes.Type(value = Organization.class, name = Organization.TYPE),
        @JsonSubTypes.Type(value = Group.class, name = Group.TYPE)})
public abstract class Party implements Identifiable<UUID> {

    public static final String RELATION_CHILDREN = "children";
    public static final String RELATION_PARENT = "parents";

    @Id
    @GeneratedValue
    private UUID id;

    @Version
    private Long version;

    @Basic(optional = false)
    private String identity;

    @Basic(optional = false)
    private String name;

    private String email;

    @Basic(optional = false)
    private Boolean enabled = true;

    @Column(insertable = false, updatable = false)
    private String type;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", insertable = false, updatable = false)
    private Date creationDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modification_date", insertable = false, updatable = false)
    private Date modificationDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "party_rel", joinColumns = {@JoinColumn(name = "parent")}, inverseJoinColumns = {@JoinColumn(name = "children")})
    private Set<Party> children = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "children")
    private Set<Party> parents = new HashSet<>();

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
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

    public Date getCreationDate() {
        return creationDate == null ? null : new Date(creationDate.getTime());
    }

    public Date getModificationDate() {
        return modificationDate == null ? null : new Date(modificationDate.getTime());
    }

    public Set<Party> getChildren() {
        return children;
    }

    public void setChildren(Set<Party> children) {
        this.children = children;
    }

    public Set<Party> getParents() {
        return parents;
    }

    public void setParents(Set<Party> parents) {
        this.parents = parents;
    }

    public void addChild(Party child) {
        if (children == null)
            children = new HashSet<>();
        children.add(child);
        child.addParent(this);
    }

    public void removeChild(Party child) {
        if (children == null)
            children = new HashSet<>();
        children.remove(child);
        child.removeParent(this);
    }

    private void addParent(Party parent) {
        if (parents == null)
            parents = new HashSet<>();
        parents.add(parent);
    }

    private void removeParent(Party parent) {
        if (parents == null)
            parents = new HashSet<>();
        parents.remove(parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Party party = (Party) o;
        return new EqualsBuilder().append(this.getType(), party.getType()).append(this.getIdentity(), party.getIdentity()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.getType()).append(this.getIdentity()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("type", getType()).append("identity", identity).append("enabled", enabled)
                .append("children", children == null ? "[]" : children.stream().map(child -> child.getType() + "/" + child.getIdentity() + "/" + child.getId()).collect(Collectors.toSet()))
                .append("parents", parents == null ? "[]" : parents.stream().map(parent -> parent.getType() + "/" + parent.getIdentity() + "/" + parent.getId()).collect(Collectors.toSet()))
                .toString();
    }
}
