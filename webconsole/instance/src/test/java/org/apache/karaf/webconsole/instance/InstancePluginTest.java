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
package org.apache.karaf.webconsole.instance;

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

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.easymock.EasyMock;

public class InstancePluginTest extends TestCase {
    public void testParseStringList() throws Exception {
        assertEquals(Arrays.asList("a", "b"), testParseStringList(" a ,b"));
        assertEquals(Collections.emptyList(), testParseStringList(null));
        assertEquals(Collections.singletonList("hello"), testParseStringList("hello"));
        assertEquals(Collections.singletonList("b"), testParseStringList(",b,"));
    }
    
    @SuppressWarnings("unchecked")
    private List<String> testParseStringList(String s) throws Exception {
        InstancePlugin ap = new InstancePlugin();
        Method m = ap.getClass().getDeclaredMethod("parseStringList", String.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(ap, s);
    }
    
    public void testDoPostCreate() throws Exception {
        InstanceSettings instanceSettings =
            new InstanceSettings(123, 456, 789,  null, null, Collections.singletonList("http://someURL"), Arrays.asList("abc", "def"));
        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.expect(instanceService.createInstance("instance1", instanceSettings, false)).andReturn(null);
        EasyMock.expect(instanceService.getInstances()).andReturn(new Instance[]{}).anyTimes();
        EasyMock.replay(instanceService);
        
        InstancePlugin ap = new InstancePlugin();
        ap.setInstanceService(instanceService);

        final Map<String, String> params = new HashMap<>();
        params.put("action", "create");
        params.put("name", "instance1");
        params.put("sshPort", "123");
        params.put("rmiRegistryPort", "456");
        params.put("rmiServerPort", "789");
        params.put("featureURLs", "http://someURL");
        params.put("features", "abc,def");
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getParameter(EasyMock.anyObject())).andAnswer(
                () -> params.get(EasyMock.getCurrentArguments()[0])).anyTimes();

        HttpServletResponse res = EasyMock.createNiceMock(HttpServletResponse.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        EasyMock.expect(res.getWriter()).andReturn(pw);
        
        EasyMock.replay(req);
        EasyMock.replay(res);
        ap.doPost(req, res);        
        EasyMock.verify(instanceService);
        
        // Check that the operation has succeeded. This will cause some information to be written to 
        // the outputstream...
        pw.flush();
        String s = new String(baos.toByteArray());
        assertTrue(s.contains("instance"));
    }
}
