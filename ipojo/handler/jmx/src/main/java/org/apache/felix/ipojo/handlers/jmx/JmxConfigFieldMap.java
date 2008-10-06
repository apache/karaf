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

package org.apache.felix.ipojo.handlers.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JmxConfigFieldMap : use to store the informations needed to build the Dynamic
 * MBean.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JmxConfigFieldMap {

    /**
     * The exposed attributes.
     */
    private Map < String, PropertyField > m_properties = new HashMap < String, PropertyField >();
    /**
     * The exposed methods.
     */
    private Map < String, MethodField[] > m_methods = new HashMap < String, MethodField[] >();
    /**
     * The allowed notifications.
     */
    private Map < String, NotificationField > m_notifications = new HashMap < String, NotificationField >();
    /**
     * The description of the Mbean.
     */
    private String m_description;

    /**
     * Constructor.
     */
    public JmxConfigFieldMap() {

    }

    /**
     * Gets the description of the MBean.
     * 
     * @return the description of the MBean
     */
    public String getDecription() {
        return m_description;
    }

    /**
     * Sets the description of the MBean.
     * 
     * @param description a String which describes the Mbean
     */
    public void setDescription(String description) {
        this.m_description = description;
    }

    /**
     * Adds a new attribute exposed in the Mbean.
     * 
     * @param name the name of the new property
     * @param propertyField the field which describes the property
     */
    public void addPropertyFromName(String name, PropertyField propertyField) {
        m_properties.put(name, propertyField);
    }

    /**
     * Gets all of the properties exposed.
     * 
     * @return the collection of all properties
     */
    public Collection < PropertyField > getProperties() {
        if (m_properties != null) {
            return m_properties.values();
        } else {
            return null;
        }
    }

    /**
     * Gets the property by the name.
     * 
     * @param name the name of the required property
     * @return the field required or null if is not found
     */
    public PropertyField getPropertyFromName(String name) {
        PropertyField prop = m_properties.get(name);
        return prop;
    }

    /**
     * Gets the property by the field.
     * 
     * @param field the required field
     * @return the property by the field
     */
    public PropertyField getPropertyFromField(String field) {
        PropertyField property = null;
        Iterator < PropertyField > it = m_properties.values().iterator();
        while (it.hasNext()) {
            PropertyField p = it.next();
            if (p.getField().compareTo(field) == 0) {
                if (property != null) {
                    System.err.println("a field already exists");
                } else {
                    property = p;
                }
            }
        }
        return property;
    }

    /**
     * Adds a new method descriptor from its name.
     * 
     * @param name the name of the method
     * @param methodField the description of the method
     */
    public void addMethodFromName(String name, MethodField methodField) {
        MethodField[] mf;
        if (!m_methods.containsKey(name)) {
            mf = new MethodField[1];
            mf[0] = methodField;
        } else {
            MethodField[] temp = m_methods.get(name);
            mf = new MethodField[temp.length + 1];
            for (int i = 0; i < temp.length; i++) {
                mf[i] = temp[i];
            }
            mf[temp.length] = methodField;
        }
        m_methods.put(name, mf);
    }

    /**
     * Adds new methods descriptors from one name. (the method must have the same name but different signature).
     * 
     * @param name the name of the method
     * @param methodsField the description of the methods
     */
    public void addMethodFromName(String name, MethodField[] methodsField) {
        MethodField[] mf;
        if (!m_methods.containsKey(name)) {
            mf = methodsField;
        } else {
            MethodField[] temp = m_methods.get(name);
            mf = new MethodField[temp.length + methodsField.length];
            for (int i = 0; i < temp.length; i++) {
                mf[i] = temp[i];
            }
            for (int i = 0; i < methodsField.length; i++) {
                mf[i + temp.length] = methodsField[i];
            }
        }
        m_methods.put(name, mf);
    }

    /**
     * Adds a method from name and erases the older if exists.
     * 
     * @param name the name of the method
     * @param methodField the method to be added
     */
    public void overrideMethodFromName(String name, MethodField methodField) {
        MethodField[] mf = new MethodField[1];
        mf[0] = methodField;
        m_methods.put(name, mf);
    }

    /**
     * Add methods from name and erases the olders if exists.
     * 
     * @param name the name of the method
     * @param methodsField the array of methods to be added
     */
    public void overrideMethodFromName(String name, MethodField[] methodsField) {
        m_methods.put(name, methodsField);
    }

    /**
     * Returns the method(s) with the given name.
     * 
     * @param name the name of the methods
     * @return the list of methods with the given name
     */
    public MethodField[] getMethodFromName(String name) {
        MethodField[] prop = m_methods.get(name);
        return prop;
    }

    /**
     * Gets the method with the good signature.
     * 
     * @param operationName the name of the method requiered
     * @param signature the required signature
     * @return the method which the same signature or null if not found
     */
    public MethodField getMethodFromName(String operationName,
            String[] signature) {
        MethodField[] methods = m_methods.get(operationName);
        for (int i = 0; i < methods.length; i++) {
            if (isSameSignature(signature, methods[i].getSignature())) {
                return methods[i];
            }
        }
        return null;
    }

    /**
     * Compares two method signature.
     * 
     * @param sig1 the first signature
     * @param sig2 the second signature
     * @return true if the signature are similar false otherwise
     */
    private boolean isSameSignature(String[] sig1, String[] sig2) {
        if (sig1.length != sig2.length) {
            return false;
        } else {
            for (int i = 0; i < sig1.length; i++) {
                if (!sig1[i].equals(sig2[i])) {
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Returns all methods store.
     * 
     * @return the collection of methodField[]
     */
    public Collection < MethodField[] > getMethods() {
        if (m_methods != null) {
            return m_methods.values();
        } else {
            return null;
        }
    }

    /**
     * Adds a notification.
     * 
     * @param name the name of the notification
     * @param notificationField the field involved with the notification.
     */
    public void addNotificationFromName(String name,
            NotificationField notificationField) {
        m_notifications.put(name, notificationField);
    }

    /**
     * Returns the notification with the given name.
     * 
     * @param name the name of the notification to return
     * @return the notification if it exists, {@code null} otherwise
     */
    public NotificationField getNotificationFromName(String name) {
        NotificationField prop = m_notifications.get(name);
        return prop;
    }

    /**
     * Gets all notifications defined.
     * 
     * @return the collection of NotificationField
     */
    public Collection < NotificationField > getNotifications() {
        if (m_notifications != null) {
            return m_notifications.values();
        } else {
            return null;
        }
    }
}
