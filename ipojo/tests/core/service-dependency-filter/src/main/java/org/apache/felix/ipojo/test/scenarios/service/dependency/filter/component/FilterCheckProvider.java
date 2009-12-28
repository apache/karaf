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
package org.apache.felix.ipojo.test.scenarios.service.dependency.filter.component;

import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;

public class FilterCheckProvider implements CheckService, FooService {

    private String m_toto;
    
    private int bind;
    
    private int unbind;
    
    public FilterCheckProvider() {
        m_toto = "A";
    }
    
    public boolean check() {
        if (m_toto.equals("A")){
            m_toto="B";
            return true;
        } else {
            m_toto="A";
            return false;
        }
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("Bind", new Integer(bind-unbind));
        return null;
    }
    
    private void Bind() {
        bind++;
    }
    private void Unbind() {
        unbind++;
    }

    public boolean foo() {
        return true;
    }

    public Properties fooProps() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getBoolean() {
        // TODO Auto-generated method stub
        return false;
    }

    public double getDouble() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Boolean getObject() {
        // TODO Auto-generated method stub
        return null;
    }

}
