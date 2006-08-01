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
package org.apache.felix.ipojo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;



/**
 * A callback allows calling a method on the component.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Callback {

	/**
	 * Name of the method to call.
	 */
	private String m_method;

	/**
	 * Is the method a static method ?
	 */
	private boolean m_isStatic;

	/**
	 * Reference on the component manager.
	 */
	private ComponentManager m_manager;

	 /**
     * LifecycleCallback constructor.
     * @param method : the name of the method to call
     * @param isStatic : is the method a static method
     * @param cm : the component manager of the component containing the method
     */
    public Callback(String method, boolean isStatic, ComponentManager cm) {
        m_method = method;
        m_isStatic = isStatic;
        m_manager = cm;
    }

    /**
     * Call the method.
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    public void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Activator.getLogger().log(Level.INFO, "[" + m_manager.getComponentMetatada().getClassName() + "] Call an callback method : " + m_method);
        Method method = m_manager.getClazz().getMethod(m_method, new Class[] {});
        method.setAccessible(true);

        if (m_isStatic) { method.invoke(null, new Object[]{}); }
        else {
        	// Two cases :
        	// - if instances already exists : call on each instances
        	// - if no instance exists : create an instance
        	if (m_manager.getInstances().length == 0) {
        		Activator.getLogger().log(Level.INFO, "[" + m_manager.getComponentMetatada().getClassName() + "] Create the first instance " + m_manager.getInstance());
        		method.invoke(m_manager.getInstance(), new Object[]{});
        	} else {
        		for (int i = 0; i < m_manager.getInstances().length; i++) {
            		Activator.getLogger().log(Level.INFO, "[" + m_manager.getComponentMetatada().getClassName() + "] Call the callback on the instance " + m_manager.getInstances()[i]);
        			method.invoke(m_manager.getInstances()[i], new Object[]{});
        		}
        	}
        }
    }

    /**
     * Call the callback on the method with the argument given in parameter.
     * @param arg : the parameters
     * @throws NoSuchMethodException : the callback method is not found
     * @throws IllegalAccessException : the callbback method cannot be called
     * @throws InvocationTargetException : an error occurs inside the called method
     */
    public void call(Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Activator.getLogger().log(Level.INFO, "[" + m_manager.getComponentMetatada().getClassName() + "] Call an callback method : " + m_method);

    	// Build an array of call for arg :
    	Class[] classes = new Class[arg.length];
    	for (int i = 0; i < arg.length; i++) {
    		classes[i] = arg[i].getClass();
    	}
        Method method = m_manager.getClazz().getMethod(m_method, classes);
        method.setAccessible(true);

        if (m_isStatic) { method.invoke(null, arg); }
        else {
        	// Two cases :
        	// - if instances already exists : call on each instances
        	// - if no instance exists : create an instance
        	if (m_manager.getInstances().length == 0) {
        		Activator.getLogger().log(Level.INFO, "[" + m_manager.getComponentMetatada().getClassName() + "] Create the first instance " + m_manager.getInstance());
        		method.invoke(m_manager.getInstance(), new Object[]{});
        	} else {
        		for (int i = 0; i < m_manager.getInstances().length; i++) {
        			method.invoke(m_manager.getInstances()[i], arg);
        		}
        	}
        }
    }
}
