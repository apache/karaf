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

public class SyncopeLoginModuleTest {

    @Test
    public void testRolesExtraction() throws Exception {
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
        List<String> roles = syncopeLoginModule.extractingRoles(syncopeResponse);
        Assert.assertEquals(2, roles.size());
        Assert.assertEquals("admin", roles.get(0));
        Assert.assertEquals("another", roles.get(1));
    }

}
