/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.handlers.lifecycle.callback;

import java.lang.reflect.InvocationTargetException;
import org.apache.felix.ipojo.Callback;

/**
 * This class is the implementation of callback on lifecycle transition.
 * @author Clement Escoffier
 *
 */
public class LifecycleCallback {


    /**
     * Metadata of the callback.
     */
    private LifecycleCallbackMetadata m_metadata;

    /**
     * Callback object.
     */
    private Callback m_callback;

    /**
     * LifecycleCallback constructor.
     * @param hh : the callback handler calling the callback
     * @param hm : the callback metadata
     */
    public LifecycleCallback(LifecycleCallbackHandler hh, LifecycleCallbackMetadata hm) {
        m_metadata = hm;
        m_callback = new Callback(hm.getMethod(), hm.isStatic(), hh.getComponentManager());
    }

    /**
     * @return : the metadata of the hook
     */
    public LifecycleCallbackMetadata getMetadata() {
        return m_metadata;
    }

    /**
     * Call the hook method when the transition from inital to final state is detected.
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	m_callback.call();
    }

}
