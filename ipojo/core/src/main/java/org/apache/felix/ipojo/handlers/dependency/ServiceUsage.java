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
package org.apache.felix.ipojo.handlers.dependency;

import java.util.ArrayList;
import java.util.List;

/**
 * Object managing thread local copy of required services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceUsage extends ThreadLocal {
    
    /**
     * Structure contained in the Thread Local.
     */
    private class Usage {
        /**
         * Stack Size.
         */
        Integer m_stack = new Integer(0);
        /**
         * List of used service references.
         */
        List m_refs = new ArrayList();
        /**
         * List of used objects.
         */
        List m_objects = new ArrayList();
    }
    
    /**
     * Initialize the cached object.
     * @return an empty Usage object.
     * @see java.lang.ThreadLocal#initialValue()
     */
    public Object initialValue() {
        return new Usage();
    }
    
    /**
     * Get the list of stored references.
     * @return the list of stored references.
     */
    public List getReferences() {
        Usage use = (Usage) super.get();
        return use.m_refs;
    }
    
    /**
     * Get the lost of stored object.
     * @return the list of stored service objects.
     */
    public List getObjects() {
        Usage use = (Usage) super.get();
        return use.m_objects;
    }
    
    /**
     * Get the stack level.
     * @return the stack level.
     */
    public int getStackLevel() {
        Usage use = (Usage) super.get();
        return use.m_stack.intValue();
    }
    
    /**
     * Set the stack level.
     * @param level : the new stack level.
     */
    public void setStackLevel(int level) {
        Usage use = (Usage) super.get();
        use.m_stack = new Integer(level);
    }
    
    
    

}
