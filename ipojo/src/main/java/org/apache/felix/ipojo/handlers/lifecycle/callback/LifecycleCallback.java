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

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Callback;

/**
 * This class is the implementation of callback on lifecycle transition.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class LifecycleCallback {

    /**
     * Initial state of the transition.
     */
    private int m_initialState;

    /**
     * Final state of the transition.
     */
    private int m_finalState;

    /**
     * Callback object.
     */
    private Callback m_callback;

    /**
     * Method called by the callback.
     */
    private String m_method;

    /**
     * LifecycleCallback constructor.
     * 
     * @param hh : the callback handler calling the callback
     * @param initialState : initial state of the callback
     * @param finalState : finali state of the callback
     * @param method : method to invoke
     * @param isStatic : is the method static ?
     */
    public LifecycleCallback(LifecycleCallbackHandler hh, String initialState, String finalState, String method, boolean isStatic) {
        if (initialState.equals("VALID")) {
            m_initialState = InstanceManager.VALID;
        }
        if (initialState.equals("INVALID")) {
            m_initialState = InstanceManager.INVALID;
        }
        if (finalState.equals("VALID")) {
            m_finalState = InstanceManager.VALID;
        }
        if (finalState.equals("INVALID")) {
            m_finalState = InstanceManager.INVALID;
        }

        m_method = method;
        m_callback = new Callback(method, new String[0], isStatic, hh.getInstanceManager());
    }

    /**
     * Call the callback method when the transition from inital to final state is
     * detected.
     * 
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        m_callback.call();
    }

    public int getFinalState() {
        return m_finalState;
    }

    public int getInitialState() {
        return m_initialState;
    }

    public String getMethod() {
        return m_method;
    }

}
