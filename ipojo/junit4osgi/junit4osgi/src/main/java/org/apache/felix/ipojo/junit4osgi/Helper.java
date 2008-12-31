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
package org.apache.felix.ipojo.junit4osgi;

import org.osgi.framework.BundleContext;


/**
 * Helper abstract class.
 * Helper objects aim to facilitate Test writing.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class Helper {
    
    /**
     * The bundle context.
     */
    protected BundleContext m_context;
    
    /**
     * The OSGi Test case. 
     */
    protected OSGiTestCase m_testcase;
          
    /**
     * Creates a Helper.
     * Registers the helper.
     * Sub-classes must initialize the session.
     * @param tc the OSGi Test Case
     */
    public Helper(OSGiTestCase tc) {
        m_testcase = tc;
        m_context = m_testcase.getBundleContext();
        tc.addHelper(this);
    }
    
    /**
     * Rolls back the session. 
     */
    public abstract void dispose();

}
