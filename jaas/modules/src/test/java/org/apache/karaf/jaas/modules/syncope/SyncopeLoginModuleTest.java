/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jaas.modules.syncope;

import org.junit.Test;
import org.junit.Assert;

import java.util.List;
import java.util.Collections;
import java.util.Map;

public class SyncopeLoginModuleTest {

    @Test
    public void testRolesExtractionSyncope1() throws Exception {
        String syncopeResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<user>\n" +
                "    <attributes>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>cool</schema>\n" +
                "            <value>false</value>\n" +
                "        </attribute>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>email</schema>\n" +
                "            <value>karaf@example.net</value>\n" +
                "        </attribute>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>fullname</schema>\n" +
                "            <value>karaf</value>\n" +
                "        </attribute>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>gender</schema>\n" +
                "            <value>M</value>\n" +
                "        </attribute>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>surname</schema>\n" +
                "            <value>karaf</value>\n" +
                "        </attribute>\n" +
                "        <attribute>\n" +
                "            <readonly>false</readonly>\n" +
                "            <schema>userId</schema>\n" +
                "            <value>karaf@example.net</value>\n" +
                "        </attribute>\n" +
                "    </attributes>\n" +
                "    <derivedAttributes/>\n" +
                "    <id>100</id>\n" +
                "    <propagationStatuses/>\n" +
                "    <resources/>\n" +
                "    <virtualAttributes/>\n" +
                "    <creationDate>2014-08-12T18:37:09.202+02:00</creationDate>\n" +
                "    <failedLogins>0</failedLogins>\n" +
                "    <lastLoginDate>2014-08-13T09:38:02.204+02:00</lastLoginDate>\n" +
                "    <memberships>\n" +
                "        <membership>\n" +
                "            <attributes/>\n" +
                "            <derivedAttributes/>\n" +
                "            <id>100</id>\n" +
                "            <propagationStatuses/>\n" +
                "            <resources/>\n" +
                "            <virtualAttributes/>\n" +
                "            <resources/>\n" +
                "            <roleId>100</roleId>\n" +
                "            <roleName>admin</roleName>\n" +
                "        </membership>\n" +
                "        <membership>\n" +
                "            <attributes/>\n" +
                "            <derivedAttributes/>\n" +
                "            <id>101</id>\n" +
                "            <propagationStatuses/>\n" +
                "            <resources/>\n" +
                "            <virtualAttributes/>\n" +
                "            <resources/>\n" +
                "            <roleId>101</roleId>\n" +
                "            <roleName>another</roleName>\n" +
                "        </membership>\n" +
                "    </memberships>\n" +
                "    <password>36460D3A3C1E27C0DB2AF23344475EE712DD3C9D</password>\n" +
                "    <status>active</status>\n" +
                "    <username>karaf</username>\n" +
                "</user>\n";
        SyncopeLoginModule syncopeLoginModule = new SyncopeLoginModule();
        List<String> roles = syncopeLoginModule.extractingRolesSyncope1(syncopeResponse);
        Assert.assertEquals(2, roles.size());
        Assert.assertEquals("admin", roles.get(0));
        Assert.assertEquals("another", roles.get(1));
    }

    @Test
    public void testRolesExtractionSyncope2() throws Exception {
        String syncopeResponse = "{\n" + "\n"
                + "   \"@class\":\"org.apache.syncope.common.lib.to.UserTO\",\n" + "\n"
                + "   \"creator\":\"admin\",\n" + "\n"
                + "   \"creationDate\":\"2017-07-31T08:36:41.000+0000\",\n" + "\n"
                + "   \"lastModifier\":\"admin\",\n" + "\n"
                + "   \"lastChangeDate\":\"2017-08-01T08:46:19.236+0000\",\n" + "\n"
                + "   \"key\":\"e5a131b0-eb66-4115-a131-b0eb66511579\",\n" + "\n"
                + "   \"type\":\"USER\",\n" + "\n" + "   \"realm\":\"/karaf\",\n" + "\n"
                + "   \"status\":\"created\",\n" + "\n" + "   \"password\":null,\n" + "\n"
                + "   \"token\":null,\n" + "\n" + "   \"tokenExpireTime\":null,\n" + "\n"
                + "   \"username\":\"karaf\",\n" + "\n"
                + "   \"lastLoginDate\":\"2017-08-01T08:46:19.224+0000\",\n" + "\n"
                + "   \"changePwdDate\":null,\n" + "\n" + "   \"failedLogins\":0,\n" + "\n"
                + "   \"securityQuestion\":null,\n" + "\n" + "   \"securityAnswer\":null,\n" + "\n"
                + "   \"mustChangePassword\":false,\n" + "\n" + "   \"auxClasses\":[\n" + "\n"
                + " \n" + "\n" + "   ],\n" + "\n" + "   \"plainAttrs\":[\n" + "\n" + " \n" + "\n"
                + "   ],\n" + "\n" + "   \"derAttrs\":[\n" + "\n" + " \n" + "\n" + "   ],\n" + "\n"
                + "   \"virAttrs\":[\n" + "\n" + " \n" + "\n" + "   ],\n" + "\n"
                + "   \"resources\":[\n" + "\n" + " \n" + "\n" + "   ],\n" + "\n"
                + "   \"roles\":[\n" + "\n" + "      \"admin\", \"another\"\n" + "\n" + "   ],\n" +
                "\n"
                + "   \"dynRoles\":[\n" + "\n" + "      \"admin\"\n" + "\n" + "   ],\n" + "\n"
                + "   \"relationships\":[\n" + "\n" + " \n" + "\n" + "   ],\n" + "\n"
                + "   \"memberships\":[\n" + "\n" + "      {\n" + "\n"
                + "         \"type\":\"Membership\",\n" + "\n"
                + "         \"rightType\":\"GROUP\",\n" + "\n"
                + "         \"rightKey\":\"3847aa78-3202-4d8f-87aa-7832026d8fba\",\n" + "\n"
                + "         \"groupName\":\"manager\",\n" + "\n" + "         \"plainAttrs\":[\n"
                + "\n" + " \n" + "\n" + "         ],\n" + "\n" + "         \"derAttrs\":[\n" + "\n"
                + " \n" + "\n" + "         ],\n" + "\n" + "         \"virAttrs\":[\n" + "\n" + " \n"
                + "\n" + "         ]\n" + "\n" + "      }\n" + "\n" + "   ],\n" + "\n"
                + "   \"dynGroups\":[\n" + "\n" + " \n" + "\n" + "   ]\n" + "\n" + "}";
        SyncopeLoginModule syncopeLoginModule = new SyncopeLoginModule();
        Map<String, String> options = Collections.singletonMap(SyncopeLoginModule.USE_ROLES_FOR_SYNCOPE2, "true");
        syncopeLoginModule.initialize(null, null, Collections.emptyMap(), options);
        List<String> roles = syncopeLoginModule.extractingRolesSyncope2(syncopeResponse);
        Assert.assertEquals(2, roles.size());
        Assert.assertEquals("admin", roles.get(0));
        Assert.assertEquals("another", roles.get(1));
    }

}
