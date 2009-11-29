/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.test.scenarios.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;
import org.osgi.framework.BundleContext;

public class FooProviderType2 implements FooService {
    
    private int m_bar;  // Service property.
    private String m_foo;
    
    private BundleContext m_context;
    
    private static int count = 0;
    
  
    public boolean foo() {
        // Update
        if (m_foo.equals("foo")) {
            m_foo = "bar";
        } else {
            m_foo = "foo";
        }
        return true;
    }

    public Properties fooProps() {
        Properties p = new Properties();
        p.put("bar", new Integer(m_bar));
        if(m_foo != null) {
            p.put("foo", m_foo);
        }
        p.put("context", m_context);
        
        p.put("count", new Integer(count));
        return p;
    }
    
    public boolean getBoolean() { return true; }

    public double getDouble() { return 1.0; }

    public int getInt() { return 1; }

    public long getLong() { return 1; }

    public Boolean getObject() { return new Boolean(true); }
    
}
