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
 * JmxConfigFieldMap : use to store the informations needed to build the Dynamic MBean.
 *  
 *  @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JmxConfigFieldMap {

    /** 
     * m_properties : use to store the attributes exposed.
     */
    private Map < String, PropertyField > m_properties = new HashMap < String, PropertyField >();
    /** 
     * m_methods : use to store the methods exposed.
     */
    private Map < String, MethodField[] > m_methods = new HashMap < String, MethodField[] >();
    /** 
     * m_notification : use to store the notification allowed.
     */
    private Map < String, NotificationField > m_notifications = new HashMap < String, NotificationField >();
    /** 
     * m_description : description of the Mbean.
     */
    private String m_description;
    
    
    /** 
     * JmxConfigFieldMap : constructor.
     */
    public JmxConfigFieldMap() {
        
    }
    
    /** 
     * getDescription : get the descritpion of the MBean.
     * @return String : Decription of the MBean
     */
    public String getDecription() {
        return m_description;
    }
    
    /** 
     * setDescription : set the descritpion of the MBean.
     * @param description : String which describe the Mbean
     */
    public void setDescription(String description) {
        this.m_description = description;
    }
    
    /** 
     * addPropertyFromName : add a new attribute exposed in the Mbean.
     * @param name : name of the new property
     * @param propertyField : Field which describe the property
     */
    public void addPropertyFromName(String name, PropertyField propertyField) {
        m_properties.put(name, propertyField);
    }
    
    /** 
     * getProperties : get all of the properties exposed.
     * @return : collection of all properties
     */
    public Collection<PropertyField> getProperties() {
        if (m_properties != null) {
            return m_properties.values();
        } else {
            return null;
        }     
    }
    
    /** 
     * getPropertyFromName : get the property by the name.
     * @param name : name of the requiered property
     * @return PropertyField : the field requiered or null if is not found
     */
    public PropertyField getPropertyFromName(String name) {
        PropertyField prop = m_properties.get(name);
        return prop;
    }
    
    /** 
     * getPropertyFromField : get the property by the field.
     * @param field : the requiered field
     * @return PropertyField : 
     */
    public PropertyField getPropertyFromField(String field) {
        PropertyField property = null;
        Iterator<PropertyField> it = m_properties.values().iterator();
        while (it.hasNext()) {
            PropertyField p = it.next();
            if (p.getField().compareTo(field) == 0) {
                if (property != null) {
                    System.err.println("a field already exist");
                } else {
                    property = p;
                }
            }
        }
        return property;   
    }
    
    
    /** 
     * addMethodFromName : add a new method descriptor from its name.
     * @param name : name of the method
     * @param methodField : descritpion of the method
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
     * addMethodFromName : add new methods descriptors from one name.
     * (the method muste have the same name but different signature).
     * @param name : name of the method
     * @param methodsField : descritpion of the methods
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
     * DynamicMBeanImpl : add methods from name and erase the older if exist.
     * @param name : name of the method
     * @param methodField : method to be added
     */
    public void overrideMethodFromName(String name, MethodField methodField) {
        MethodField[] mf = new MethodField[1];
        mf[0] = methodField;
        m_methods.put(name, mf);
    }
    
    /** 
     * DynamicMBeanImpl : add methods from name and erase the older if exist.
     * @param name : name of the method
     * @param methodsField : array of methods to be added
     */
    public void overrideMethodFromName(String name, MethodField[] methodsField) {
        m_methods.put(name, methodsField);  
    }
    
    /** 
     * getMethodFromName : return the metod(s) which are similar.
     * @param name : name of requiered method
     * @return MethodField[] : list of returned methods
     */
    public MethodField[] getMethodFromName(String name) {
        MethodField[] prop = m_methods.get(name);
        return prop;
    }
    
    /** 
     * getMethodFromName : get the method which the good signature.
     * @param operationName : name of the method requiered
     * @param signature : signature requiered
     * @return MethodField : the method which the same signature or null if not found
     */
    public MethodField getMethodFromName(String operationName, String[] signature) {
        MethodField[] methods = m_methods.get(operationName);
        for (int i = 0; i < methods.length; i++) {
            if (isSameSignature(signature, methods[i].getSignature())) {
                return methods[i];
            }
        }
        return null;
    }
    
    /** 
     * isSameSignature : compare two method signature.
     * @param sig1 : first signature
     * @param sig2 : second signature
     * @return boolean : return true if the signature are similar
     *                   fale else
     */
    private boolean isSameSignature(String[] sig1, String[] sig2) {
        if (sig1.length != sig2.length) {
            return false;
        } else {
            for (int i = 0; i < sig1.length; i++) {
                //System.out.println(sig1[i] +" == "+ sig2[i]);
                if (!sig1[i].equals(sig2[i])) {
                    return false;
                }
            }
            
        }
        return true;
    }
    
    /** 
     * getMethods : return all methods store.
     * @return Collection : collection of methodField[]
     */
    public Collection<MethodField[]> getMethods() {
        if (m_methods != null) {
            return m_methods.values();
        } else {
            return null;
        }
    }   
    
    /** 
     * addNotificationFromName : add a notification .
     * @param name : 
     * @param notificationField :
     */
    public void addNotificationFromName(String name, NotificationField notificationField) {
        m_notifications.put(name, notificationField);
    }
    
    /** 
     * getNotificationFromName : return the notification with requiered name.
     * @param name : name requiered
     * @return NotificationField : return the notification if exist, null else
     */
    public NotificationField getNotificationFromName(String name) {
        NotificationField prop = m_notifications.get(name);
        return prop;
    }
    
    /** 
     * getNotification : get all notifications define.
     * @return Collection : return collection of NotificationField
     */
    public Collection<NotificationField> getNotifications() {
        if (m_notifications != null) {
            return m_notifications.values();
        } else {
            return null;
        }
    }
}
