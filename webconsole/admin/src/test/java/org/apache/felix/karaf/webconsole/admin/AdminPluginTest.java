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
package org.apache.felix.karaf.webconsole.admin;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.felix.karaf.admin.AdminService;
import org.apache.felix.karaf.admin.Instance;
import org.apache.felix.karaf.admin.InstanceSettings;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

public class AdminPluginTest extends TestCase {
    public void testParseStringList() throws Exception {
        assertEquals(Arrays.asList("a", "b"), testParseStringList(" a ,b"));
        assertEquals(Collections.emptyList(), testParseStringList(null));
        assertEquals(Arrays.asList("hello"), testParseStringList("hello"));
        assertEquals(Arrays.asList("b"), testParseStringList(",b,"));
    }
    
    @SuppressWarnings("unchecked")
    private List<String> testParseStringList(String s) throws Exception {
        AdminPlugin ap = new AdminPlugin();
        Method m = ap.getClass().getDeclaredMethod("parseStringList", new Class [] {String.class});
        m.setAccessible(true);
        return (List<String>) m.invoke(ap, s);
    }
    
    public void testDoPostCreate() throws Exception {
        InstanceSettings is = 
            new InstanceSettings(1234, null, Collections.singletonList("http://someURL"), Arrays.asList("abc", "def"));
        AdminService adminService = EasyMock.createMock(AdminService.class);
        EasyMock.expect(adminService.createInstance("instance1", is)).andReturn(null);
        EasyMock.expect(adminService.getInstances()).andReturn(new Instance[] {}).anyTimes();
        EasyMock.replay(adminService);
        
        AdminPlugin ap = new AdminPlugin();
        ap.setAdminService(adminService);

        final Map<String, String> params = new HashMap<String, String>();
        params.put("action", "create");
        params.put("name", "instance1");
        params.put("port", "1234");
        params.put("featureURLs", "http://someURL");
        params.put("features", "abc,def");
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getParameter((String) EasyMock.anyObject())).andAnswer(new IAnswer<String>() {
            public String answer() throws Throwable {
                return params.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        
        HttpServletResponse res = EasyMock.createNiceMock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        EasyMock.expect(res.getWriter()).andReturn(pw);
        
        EasyMock.replay(req);
        EasyMock.replay(res);
        ap.doPost(req, res);        
        EasyMock.verify(adminService);
        
        // Check that the operation has succeeded. This will cause some information to be written to 
        // the outputstream...
        pw.flush();
        String s = new String(baos.toByteArray());
        assertTrue(s.contains("instance"));
    }
}
