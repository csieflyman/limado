/*
 * Copyright © 2016. Limado Inc. All rights reserved
 */

package com.limado.collab.mvc.rest;

import com.google.common.collect.Sets;
import com.limado.collab.model.Group;
import com.limado.collab.model.Organization;
import com.limado.collab.model.Party;
import com.limado.collab.model.User;
import com.limado.collab.util.converter.json.JsonConverter;
import com.limado.collab.util.query.QueryParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author csieflyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = {"/spring-config.xml", "/spring-config-test.xml", "/mvc-config-test.xml"})
@DirtiesContext
public class PartyRestControllerTest {

    private static final Logger log = LogManager.getLogger(PartyRestControllerTest.class);

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private DebugLoggingResultHandler loggingResultHandler = new DebugLoggingResultHandler();
    @Autowired
    private OpenEntityManagerInViewFilter openEntityManagerInViewFilter;

    private static final String API_PATH = "/api/v1";
    private static Map<String, String> typePathMap = new HashMap<>();
    private static Map<String, Class> typeClassMap = new HashMap<>();
    static {
        typePathMap.put(User.TYPE, "users");
        typePathMap.put(Organization.TYPE, "organizations");
        typePathMap.put(Group.TYPE, "groups");

        typeClassMap.put(User.TYPE, User.class);
        typeClassMap.put(Organization.TYPE, Organization.class);
        typeClassMap.put(Group.TYPE, Group.class);
    }

    private Map<String, Party> userMap;
    private Map<String, Party> orgMap;
    private Map<String, Party> groupMap;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(openEntityManagerInViewFilter).build();
        initTestData();
    }

    private void initTestData() {
        userMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "user" + i;
            User user = new User(identity);
            user.setName(" I am " + identity);
            user.setEmail(identity + "@example.com");
            userMap.put(identity, user);
        }
        orgMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "org" + i;
            Organization org = new Organization(identity);
            org.setName(" I am " + identity);
            orgMap.put(identity, org);
        }
        groupMap = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String identity = "group" + i;
            Group group = new Group(identity);
            group.setName(" I am " + identity);
            groupMap.put(identity, group);
        }
    }

    @Test
    public void testBadRequestException() throws Exception{
        User user1 = new User();
        user1.setIdentity("123");
        user1.setName("");
        user1.setEmail("wrong email");

        String jsonObject = JsonConverter.getInstance().convertOut(user1);
        mockMvc.perform(post(API_PATH + "/" + typePathMap.get(user1.getType())).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        String invalidFormatId = "12345";
        mockMvc.perform(get(API_PATH + "/parties/" + invalidFormatId).accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Test
    public void testResourceNotFoundException() throws Exception{
        String randomId = UUID.randomUUID().toString();
        mockMvc.perform(get(API_PATH + "/parties/" + randomId).accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Test
    public void testParentsAndChildren() throws Exception {
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party org1 = orgMap.get("org1");
        Party org2 = orgMap.get("org2");
        Party group1 = groupMap.get("group1");
        Party group2 = groupMap.get("group2");

        // create
        user1 = createParty(user1);
        user2 = createParty(user2);
        org1 = createParty(org1);
        org2 = createParty(org2);
        group1 = createParty(group1);
        group2 = createParty(group2);

        // group
        addChild(group1, user1);
        addChildren(group1, Sets.newHashSet(org1));
        addParents(group2, Sets.newHashSet(group1));
        // org
        addChild(org1, user1);
        addChildren(org2, Sets.newHashSet(user2));
        addParents(org2, Sets.newHashSet(group2, org1));
        // user
        addParents(user2, Sets.newHashSet(group2));

        Assert.assertEquals(Sets.newHashSet(group2, org1), getParents(org2));
        Assert.assertEquals(Sets.newHashSet(org2, user2), getChildren(group2));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        Assert.assertEquals(Sets.newHashSet(group2, org1, org2, user1, user2), getDescendants(group1, params));
        params.add(QueryParams.Q_PREDICATES, "[TYPE(party) in (User)]");
        Assert.assertEquals(Sets.newHashSet(user1, user2), getDescendants(group1, params));
        params.add(QueryParams.Q_ONLY_SIZE, "true");
        Assert.assertEquals(2, getDescendants(group1, params));

        params = new LinkedMultiValueMap<>();
        Assert.assertEquals(Sets.newHashSet(group1, group2, org2, org1), getAscendants(user2, params));
        params.add(QueryParams.Q_PREDICATES, "[TYPE(party) in (Organization)]");
        Assert.assertEquals(Sets.newHashSet(org2, org1), getAscendants(user2, params));
        params.add(QueryParams.Q_ONLY_SIZE, "true");
        Assert.assertEquals(2, getAscendants(user2, params));

        // group
        removeChild(group1, user1);
        removeChildren(group1, Sets.newHashSet(org1));
        removeParents(group2, Sets.newHashSet(group1));
        Assert.assertEquals(Collections.emptySet(), getParents(org1));
        Assert.assertEquals(Collections.emptySet(), getChildren(group1));

        // org
        movePartyToOrganization((Organization) org2, user1);
        Assert.assertEquals(Sets.newHashSet(org2), getChildren(org1));
        Assert.assertEquals(Sets.newHashSet(user1, user2), getChildren(org2));
        removeChild(org2, user1);
        removeChildren(org2, Sets.newHashSet(user2));
        removeParents(org2, Sets.newHashSet(group2, org1));
        Assert.assertEquals(Collections.emptySet(), getParents(org2));
        Assert.assertEquals(Collections.emptySet(), getChildren(org1));


        // user
        removeParents(user2, Sets.newHashSet(group2));
        Assert.assertEquals(Collections.emptySet(), getParents(user2));
        Assert.assertEquals(Collections.emptySet(), getChildren(group2));

        List<UUID> uuids = Arrays.asList(user1.getId(), user2.getId(), org1.getId(), org2.getId(), group1.getId(), group2.getId());
        deleteByIds(uuids);
    }

    @Test
    public void testFind() throws Exception{
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party org1 = orgMap.get("org1");
        Party group1 = groupMap.get("group1");
        int size = 4;

        // create
        user1 = createParty(user1);
        user2 = createParty(user2);
        org1 = createParty(org1);
        group1 = createParty(group1);

        addChild(group1, org1);
        addChildren(org1, Sets.newHashSet(user1, user2));

        // findSize
        mockMvc.perform(get(API_PATH + "/parties")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_ONLY_SIZE, "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(String.valueOf(size)));

        // find with paging, sort
        mockMvc.perform(get(API_PATH + "/parties")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_OFFSET, "0")
                .param(QueryParams.Q_LIMIT, "3")
                .param(QueryParams.Q_SORT, "-identity"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$[0].identity").value("user2"))
                .andExpect(jsonPath("$[1].identity").value("user1"))
                .andExpect(jsonPath("$[2].identity").value("org1"))
                .andExpect(jsonPath("$.length()").value(3));

        // findById
        mockMvc.perform(get(API_PATH + "/parties")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_PREDICATES, "[id = " + org1.getId() + "]")
                .param(QueryParams.Q_FETCH_RELATIONS, Party.RELATION_PARENT + "," + Party.RELATION_CHILDREN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$[0].id").value(org1.getId().toString()))
                .andExpect(jsonPath("$[0].parents[0].id").value(group1.getId().toString()))
                .andExpect(jsonPath("$[0].children.length()").value(2));

        // getById with parents, children
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put(QueryParams.Q_FETCH_RELATIONS, Arrays.asList(Party.RELATION_PARENT, Party.RELATION_CHILDREN));
        mockMvc.perform(get(API_PATH + "/parties/" + org1.getId())
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(org1.getId().toString()))
                .andExpect(jsonPath("$.parents[0].id").value(group1.getId().toString()))
                .andExpect(jsonPath("$.children.length()").value(2))
                .andReturn();

        List<UUID> uuids = Arrays.asList(user1.getId(), user2.getId(), org1.getId(), group1.getId());
        deleteByIds(uuids);
    }

    @Test
    public void testUpdate() throws Exception{
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party org1 = orgMap.get("org1");
        Party group1 = groupMap.get("group1");

        // create
        user1 = createParty(user1);
        user2 = createParty(user2);
        org1 = createParty(org1);
        group1 = createParty(group1);

        addChild(group1, org1);
        addChildren(org1, Sets.newHashSet(user1, user2));

        // update org
        org1 = getById(org1);
        org1.setChildren(Sets.newHashSet(user1));
        org1.setParents(Sets.newHashSet(group1));
        org1.setName("org1 modified");
        String jsonObject = JsonConverter.getInstance().convertOut(org1);
        mockMvc.perform(put(API_PATH + "/organizations/" + org1.getId()).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isOk());
        org1 = getById(org1);
        Assert.assertEquals("org1 modified", org1.getName());
        Assert.assertEquals(Sets.newHashSet(group1), getParents(org1));
        Assert.assertEquals(Sets.newHashSet(user1), getChildren(org1));

        // update group
        group1 = getById(group1);
        group1.setChildren(Sets.newHashSet(user1, user2));
        group1.setParents(Collections.emptySet());
        group1.setName("group1 modified");
        jsonObject = JsonConverter.getInstance().convertOut(group1);
        mockMvc.perform(put(API_PATH + "/groups/" + group1.getId()).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isOk());
        group1 = getById(group1);
        Assert.assertEquals("group1 modified", group1.getName());
        Assert.assertEquals(Collections.emptySet(), getParents(group1));
        Assert.assertEquals(Sets.newHashSet(user1, user2), getChildren(group1));

        // update user
        user1 = getById(user1);
        user1.setParents(Sets.newHashSet(org1));
        user1.setName("user1 modified");
        jsonObject = JsonConverter.getInstance().convertOut(user1);
        mockMvc.perform(put(API_PATH + "/users/" + user1.getId()).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isOk());
        user1 = getById(user1);
        Assert.assertEquals("user1 modified", user1.getName());
        Assert.assertEquals(Sets.newHashSet(org1), getParents(user1));
        Assert.assertEquals(Collections.emptySet(), getChildren(user1));

        List<UUID> uuids = Arrays.asList(user1.getId(), user2.getId(), org1.getId(), group1.getId());
        deleteByIds(uuids);
    }

    @Test
    public void testEnableAndDisableAndDelete() throws Exception{
        Party user1 = userMap.get("user1");
        Party user2 = userMap.get("user2");
        Party org1 = orgMap.get("org1");
        Party group1 = groupMap.get("group1");

        // create
        user1 = createParty(user1);
        user2 = createParty(user2);
        org1 = createParty(org1);
        group1 = createParty(group1);

        List<UUID> uuids = Arrays.asList(user1.getId(), user2.getId(), org1.getId(), group1.getId());
        disable(uuids);
        enable(uuids);
        deleteByIds(uuids);
    }

    private void enable(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(put(API_PATH + "/parties/enable").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_PREDICATES, "[id in (" + StringUtils.join(uuids, ",") + ")]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.enabled == false)]").isEmpty());
    }

    private void disable(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(put(API_PATH + "/parties/disable").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_PREDICATES, "[id in (" + StringUtils.join(uuids, ",") + ")]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.enabled == true)]").isEmpty());
    }

    private void deleteByIds(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(delete(API_PATH + "/parties").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_PREDICATES, "[id in (" + StringUtils.join(uuids, ",") + ")]"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private Party createParty(Party party) throws Exception {
        String jsonObject = JsonConverter.getInstance().convertOut(party);
        MvcResult result = mockMvc.perform(post(API_PATH + "/" + typePathMap.get(party.getType())).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        String responseJsonObject = result.getResponse().getContentAsString();
        return (Party) JsonConverter.getInstance().convertIn(responseJsonObject, typeClassMap.get(party.getType()));
    }

    private Party getById(Party party) throws Exception {
        MvcResult result = mockMvc.perform(get(API_PATH + "/parties/" + party.getId()).accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(party.getId().toString()))
                .andReturn();
        String responseJsonObject = result.getResponse().getContentAsString();
        return (Party) JsonConverter.getInstance().convertIn(responseJsonObject, typeClassMap.get(party.getType()));
    }

    private void movePartyToOrganization(Organization parent, Party child) throws Exception {
        mockMvc.perform(put(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/child/" + child.getId()))
                .andExpect(status().isOk());
    }

    private void addChild(Party parent, Party child) throws Exception {
        mockMvc.perform(post(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/child/" + child.getId()))
                .andExpect(status().isOk());
    }

    private void removeChild(Party parent, Party child) throws Exception {
        mockMvc.perform(delete(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/child/" + child.getId()))
                .andExpect(status().isOk());
    }

    private void addChildren(Party parent, Set<Party> children) throws Exception {
        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        String jsonArray = JsonConverter.getInstance().convertOut(childrenIds);
        mockMvc.perform(post(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/children")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private void removeChildren(Party parent, Set<Party> children) throws Exception {
        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        String jsonArray = JsonConverter.getInstance().convertOut(childrenIds);
        mockMvc.perform(delete(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/children")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private void addParents(Party parent, Set<Party> children) throws Exception {
        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        String jsonArray = JsonConverter.getInstance().convertOut(childrenIds);
        mockMvc.perform(post(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/parents")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private void removeParents(Party parent, Set<Party> children) throws Exception {
        Set<UUID> childrenIds = children.stream().map(Party::getId).collect(Collectors.toSet());
        String jsonArray = JsonConverter.getInstance().convertOut(childrenIds);
        mockMvc.perform(delete(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/parents")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private Set<Party> getChildren(Party party) throws Exception {
        MvcResult result = mockMvc.perform(get(API_PATH + "/parties/" + party.getId() + "/children").accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();
        String responseJsonArray = result.getResponse().getContentAsString();
        return  JsonConverter.getInstance().convertInToSet(responseJsonArray, Party.class);
    }

    private Set<Party> getParents(Party party) throws Exception {
        MvcResult result = mockMvc.perform(get(API_PATH + "/parties/" + party.getId() + "/parents").accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();
        String responseJsonArray = result.getResponse().getContentAsString();
        return JsonConverter.getInstance().convertInToSet(responseJsonArray, Party.class);
    }

    private Object getDescendants(Party party, MultiValueMap params) throws Exception {
        MvcResult result = mockMvc.perform(get(API_PATH + "/parties/" + party.getId() + "/descendants")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE).params(params))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();
        if(params.containsKey(QueryParams.Q_ONLY_SIZE) && params.getFirst(QueryParams.Q_ONLY_SIZE).equals("true")) {
            return Integer.parseInt(result.getResponse().getContentAsString());
        }
        String responseJsonArray = result.getResponse().getContentAsString();
        return  JsonConverter.getInstance().convertInToSet(responseJsonArray, Party.class);
    }

    private Object getAscendants(Party party, MultiValueMap params) throws Exception {
        MvcResult result = mockMvc.perform(get(API_PATH + "/parties/" + party.getId() + "/ascendants")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE).params(params))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();
        if(params.containsKey(QueryParams.Q_ONLY_SIZE) && params.getFirst(QueryParams.Q_ONLY_SIZE).equals("true")) {
            return Integer.parseInt(result.getResponse().getContentAsString());
        }
        String responseJsonArray = result.getResponse().getContentAsString();
        return JsonConverter.getInstance().convertInToSet(responseJsonArray, Party.class);
    }
}