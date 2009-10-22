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
package org.apache.felix.karaf.admin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.felix.karaf.admin.InstanceSettings;

public class AdminServiceImplTest extends TestCase {
    public void testHandleFeatures() throws Exception {
        AdminServiceImpl as = new AdminServiceImpl();
        
        File f = File.createTempFile(getName(), ".test");
        try {
            Properties p = new Properties();
            p.put("featuresBoot", "abc,def ");
            p.put("featuresRepositories", "somescheme://xyz");
            OutputStream os = new FileOutputStream(f);
            try {
                p.store(os, "Test comment");
            } finally {
                os.close();
            }
            
            InstanceSettings s = new InstanceSettings(8122, null, null, Arrays.asList("test"));
            as.handleFeatures(f, s);
            
            Properties p2 = new Properties();
            InputStream is = new FileInputStream(f);
            try {
                p2.load(is);
            } finally {
                is.close();
            }
            assertEquals(2, p2.size());
            assertEquals("abc,def,test", p2.get("featuresBoot"));
            assertEquals("somescheme://xyz", p2.get("featuresRepositories"));
        } finally {
            f.delete();
        }
    }
}
