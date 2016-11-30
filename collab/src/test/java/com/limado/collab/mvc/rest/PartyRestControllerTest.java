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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
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
    public void test() throws Exception {
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

        // retrieve
        user1 = getById(user1);
        org1 = getById(org1);
        group1 = getById(group1);

        // parents and children relations
        addChild(group1, org1);
        addChild(org1, user1);
        addChild(org1, user2);

        Set<Party> parents = getParents(org1);
        Assert.assertEquals(Sets.newHashSet(group1), parents);
        Set<Party> children = getChildren(org1);
        Assert.assertEquals(Sets.newHashSet(user1, user2), children);

        removeChild(group1, org1);
        removeChild(org1, user1);
        parents = getParents(org1);
        Assert.assertEquals(Collections.emptySet(), parents);
        children = getChildren(org1);
        Assert.assertEquals(Sets.newHashSet(user2), children);

        // update
        org1.setChildren(Sets.newHashSet(user1));
        org1.setParents(Sets.newHashSet(group1));
        org1.setName("org1 modified");
        String jsonObject = JsonConverter.getInstance().convertOut(org1);
        mockMvc.perform(put(API_PATH + "/organizations/" + org1.getId()).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonObject))
                .andExpect(status().isOk());

        // getById with parameters
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put(QueryParams.Q_FETCH_RELATIONS, Arrays.asList(Party.RELATION_PARENT, Party.RELATION_CHILDREN));
        mockMvc.perform(get(API_PATH + "/parties/" + org1.getId())
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .params(params))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.name").value("org1 modified"))
                .andExpect(jsonPath("$.parents[0].id").value(group1.getId().toString()))
                .andExpect(jsonPath("$.children[0].id").value(user1.getId().toString()))
                .andReturn();

        parents = getParents(org1);
        Assert.assertEquals(Sets.newHashSet(group1), parents);
        children = getChildren(org1);
        Assert.assertEquals(Sets.newHashSet(user1), children);

        mockMvc.perform(get(API_PATH + "/parties")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_ONLY_SIZE, "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(String.valueOf(size)));

        mockMvc.perform(get(API_PATH + "/parties")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param(QueryParams.Q_PREDICATES, "[id = " + org1.getId() + "]")
                .param(QueryParams.Q_FETCH_RELATIONS, Party.RELATION_PARENT + "," + Party.RELATION_CHILDREN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$[0].parents[0].id").value(group1.getId().toString()))
                .andExpect(jsonPath("$[0].children[0].id").value(user1.getId().toString()));

        List<UUID> uuids = Arrays.asList(user1.getId(), user2.getId(), org1.getId(), group1.getId());

        disable(uuids);
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.enabled == true)]").isEmpty());

        enable(uuids);
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.enabled == false)]").isEmpty());

        deleteByIds(uuids);
        mockMvc.perform(get(API_PATH + "/parties").accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private void enable(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(put(API_PATH + "/parties/enable").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private void disable(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(put(API_PATH + "/parties/disable").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
    }

    private void deleteByIds(List<UUID> uuids) throws Exception {
        String jsonArray = JsonConverter.getInstance().convertOut(uuids);
        mockMvc.perform(delete(API_PATH + "/parties").contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).content(jsonArray))
                .andExpect(status().isOk());
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

    private void addChild(Party parent, Party child) throws Exception {
        mockMvc.perform(post(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/child/" + child.getId()))
                .andExpect(status().isOk());
    }

    private void removeChild(Party parent, Party child) throws Exception {
        mockMvc.perform(delete(API_PATH + "/" + typePathMap.get(parent.getType()) + "/" + parent.getId() + "/child/" + child.getId()))
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
}