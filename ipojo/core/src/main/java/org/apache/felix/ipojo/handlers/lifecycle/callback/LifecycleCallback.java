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
package org.apache.felix.ipojo.handlers.lifecycle.callback;

import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;

/**
 * This class is the implementation of callback on lifecycle transition.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LifecycleCallback {
    
    /**
     * Invalid to Valid transition.
     */
    protected static final int VALIDATE = 1;
    
    /**
     * Valid to Invalid transition.
     */
    protected static final int INVALIDATE = 0;
    
    /**
     * Transition on which calling the callback.
     */
    private int m_transition;

    /**
     * Callback object.
     */
    private Callback m_callback;

    /**
     * LifecycleCallback constructor.
     * 
     * @param handler : the callback handler calling the callback
     * @param transition : transition on which calling the callback
     * @param method : method metadata to invoke
     */
    public LifecycleCallback(LifecycleCallbackHandler handler, int transition, MethodMetadata method) {
        m_transition = transition;
        m_callback = new Callback(method, handler.getInstanceManager());
    }
    
    /**
     * LifecycleCallback constructor.
     * 
     * @param handler : the callback handler calling the callback
     * @param transition : transition on which calling the callback
     * @param method : method name to invoke
     */
    public LifecycleCallback(LifecycleCallbackHandler handler, int transition, String method) {
        m_transition = transition;
        m_callback = new Callback(method, new String[0], false, handler.getInstanceManager());
    }

    /**
     * Call the callback method when the transition from inital tostate is
     * detected.
     * 
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        m_callback.call();
    }
    
    protected int getTransition() {
        return m_transition;
    }
    
    /**
     * Get the method name of the callback.
     * @return the method name
     */
    protected String getMethod() {
        return m_callback.getMethod();
    }

}
