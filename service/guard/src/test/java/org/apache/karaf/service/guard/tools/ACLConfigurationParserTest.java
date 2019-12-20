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
package org.apache.karaf.service.guard.tools;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.karaf.service.guard.tools.ACLConfigurationParser.Specificity;
import org.junit.Test;

public class ACLConfigurationParserTest {
    @Test
    public void testParseRoles() {
        assertEquals(Collections.singletonList("some_role"),
                ACLConfigurationParser.parseRoles(" some_role   "));
        assertEquals(Arrays.asList("a","b","C"),
                ACLConfigurationParser.parseRoles("a,b,C"));
        assertEquals(Collections.emptyList(),
                ACLConfigurationParser.parseRoles("# test comment"));
    }

    @Test
    public void testGetRolesForInvocation() {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put("foo", "r1, r2");
        config.put("bar(java.lang.String, int)[/aa/,/42/]", "ra");
        config.put("bar(java.lang.String, int)[/bb/,/42/]", "rb");
        config.put("bar(java.lang.String, int)[\"cc\", \"17\"]", "rc");
        config.put("bar(java.lang.String, int)", "rd");
        config.put("bar(java.lang.String)", "re");
        config.put("bar", "rf");
        config.put("ba*", "rg #Wildcard");

        List<String> roles1 = new ArrayList<>();
        assertEquals(Specificity.NAME_MATCH,
                ACLConfigurationParser.getRolesForInvocation("foo", new Object [] {}, new String [] {}, config, roles1));
        assertEquals(Arrays.asList("r1", "r2"), roles1);

        List<String> roles2 = new ArrayList<>();
        assertEquals(Specificity.NAME_MATCH,
                ACLConfigurationParser.getRolesForInvocation("foo", new Object [] {"test"}, new String [] {"java.lang.String"}, config, roles2));
        assertEquals(Arrays.asList("r1", "r2"), roles2);

        List<String> roles3 = new ArrayList<>();
        assertEquals(Specificity.NO_MATCH,
                ACLConfigurationParser.getRolesForInvocation("test", new Object [] {}, new String [] {}, config, roles3));
        assertEquals(0, roles3.size());

        List<String> roles4 = new ArrayList<>();
        assertEquals(Specificity.ARGUMENT_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {"aa", 42}, new String [] {"java.lang.String", "int"}, config, roles4));
        assertEquals(Collections.singletonList("ra"), roles4);

        List<String> roles5 = new ArrayList<>();
        assertEquals(Specificity.ARGUMENT_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {"bb", 42}, new String [] {"java.lang.String", "int"}, config, roles5));
        assertEquals(Collections.singletonList("rb"), roles5);

        List<String> roles6 = new ArrayList<>();
        assertEquals(Specificity.ARGUMENT_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {"cc", 17}, new String [] {"java.lang.String", "int"}, config, roles6));
        assertEquals(Collections.singletonList("rc"), roles6);

        List<String> roles7 = new ArrayList<>();
        assertEquals(Specificity.SIGNATURE_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {"aaa", 42}, new String [] {"java.lang.String", "int"}, config, roles7));
        assertEquals(Collections.singletonList("rd"), roles7);

        List<String> roles8 = new ArrayList<>();
        assertEquals(Specificity.SIGNATURE_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {"aa"}, new String [] {"java.lang.String"}, config, roles8));
        assertEquals(Collections.singletonList("re"), roles8);

        List<String> roles9 = new ArrayList<>();
        assertEquals(Specificity.NAME_MATCH,
                ACLConfigurationParser.getRolesForInvocation("bar", new Object [] {42}, new String [] {"int"}, config, roles9));
        assertEquals(Collections.singletonList("rf"), roles9);

        List<String> roles10 = new ArrayList<>();
        assertEquals(Specificity.WILDCARD_MATCH,
                ACLConfigurationParser.getRolesForInvocation("barr", new Object [] {42}, new String [] {"int"}, config, roles10));
        assertEquals(Collections.singletonList("rg"), roles10);
    }
}
