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


import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** this class implements iPOJO Handler.
 * it builds the dynamic MBean from metadata.xml and expose it to the MBean Server.
 *  
 *  @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MBeanHandler extends PrimitiveHandler {
    /** 
     * InstanceManager: use to store the InstanceManager instance.
     */
    private InstanceManager m_instanceManager;
    /**
     * ServiceRegistration : use to register and deregister the Dynamic MBean.
     */
    private ServiceRegistration m_serviceRegistration;
    /**
     * JmxConfigFieldMap : use to store data when parsing metadata.xml.
     */
    private JmxConfigFieldMap m_jmxConfigFieldMap;
    /**
     * DynamicMBeanImpl : store the Dynamic MBean.
     */
    private DynamicMBeanImpl m_MBean;
    /**
     * String : constant which store the name of the class.
     */
    private String m_NAMESPACE = this.getClass().getName();

    /** 
     * configure : construct the structure JmxConfigFieldMap.and the Dynamic Mbean.
     * @param metadata Element
     * @param dict Dictionnary
     */
    public void configure(Element metadata, Dictionary dict) {
        
        ManipulationMetadata manipulation = new ManipulationMetadata(metadata);
        
        m_instanceManager = getInstanceManager();


        m_jmxConfigFieldMap = new JmxConfigFieldMap();

        // Build the hashmap
        Element[] mbeans = metadata.getElements("config", m_NAMESPACE);

        if (mbeans.length != 1) { return; }
        
       
        
        // set property 
        Element[] attributes = mbeans[0].getElements("property");
        //String[] fields = new String[attributes.length];
        FieldMetadata[] fields = new FieldMetadata[attributes.length];
        for (int i = 0 ; i < attributes.length ; i++) {
            boolean notif = false;
            String rights;
            String name;
            String field = attributes[i].getAttribute("field");
            
            if (attributes[i].containsAttribute("name")) {
                name = attributes[i].getAttribute("name");
            } else {
                name = field;
            }
            if (attributes[i].containsAttribute("rights")) {
                rights = attributes[i].getAttribute("rights");
            } else {
                rights = "w";
            }
            
            PropertyField property = new PropertyField(name, field, rights, getTypeFromAttributeField(field, manipulation));
            
            if (attributes[i].containsAttribute("notification")) {
                notif = Boolean.parseBoolean(attributes[i].getAttribute("notification"));
            }
            
            property.setNotifiable(notif);
            
            if (notif) {
                //add the new notifiable property in structure
                NotificationField notification = new NotificationField(name, this.getClass().getName() + "." + field, null);
                m_jmxConfigFieldMap.addNotificationFromName(name, notification);
            }
            m_jmxConfigFieldMap.addPropertyFromName(name, property);
            fields[i] = manipulation.getField(field);
            System.out.println("DEBUG: property exposed:" + name + " " + field + ":" 
                    + getTypeFromAttributeField(field, manipulation) + " " + rights 
                    + ", Notif=" + notif);
        }
        
        //set methods 
        Element[] methods = mbeans[0].getElements("method");
        for (int i = 0 ; i < methods.length ; i++) {
            String name = methods[i].getAttribute("name");
            String description = null;
            if (methods[i].containsAttribute("description")) {
                description = methods[i].getAttribute("description");
            }
            
            MethodField[] method = getMethodsFromName(name, manipulation, description);
            
            for (int j = 0 ; j < method.length ; j++) {
                m_jmxConfigFieldMap.addMethodFromName(name, method[j]);
            
                System.out.println("DEBUG: method exposed:" + method[j].getReturnType() + " " + name);
            }
        }

        m_instanceManager.register(this, fields, null);
        
    }
    /**
     * start : register the Dynamic Mbean.
     */
    public void start() {
        
//      create the corresponding MBean
        m_MBean = new DynamicMBeanImpl(m_jmxConfigFieldMap, m_instanceManager);
        if (m_serviceRegistration != null) { m_serviceRegistration.unregister(); }

        // Register the ManagedService
        BundleContext bundleContext = m_instanceManager.getContext();
        Properties properties = new Properties();
        properties.put("jmxagent.objectName", "HandlerJMX:type=" 
                + m_instanceManager.getClassName() 
                + ",ServiceId=" 
                + m_instanceManager.getInstanceName());

        m_serviceRegistration = bundleContext.registerService(javax.management.DynamicMBean.class.getName(), m_MBean, properties);
    }

    /** 
     * stop : deregister the Dynamic Mbean.
     */
    public void stop() {
        if (m_serviceRegistration != null) { m_serviceRegistration.unregister(); }
        
        
    }
    
    
    /** 
     * setterCallback : call when a POJO member is modified externally.
     * @param fieldName : name of the modified field 
     * @param value     : new value of the field
     */
    public void setterCallback(String fieldName, Object value) {
        // Check if the field is a configurable property

        PropertyField propertyField = (PropertyField) m_jmxConfigFieldMap.getPropertyFromField(fieldName);
        if (propertyField != null) {
            if (propertyField.isNotifiable()) {
                                            
                m_MBean.sendNotification(propertyField.getName() + " changed", propertyField.getName(),
                                         propertyField.getType(), propertyField.getValue(), value);
            }
            propertyField.setValue(value);
        }
    }

    /** 
     * getterCallback : call when a POJO member is modified by the MBean.
     * @param fieldName : name of the modified field 
     * @param value     : old value of the field
     * @return          : new value of the field
     */
    public Object getterCallback(String fieldName, Object value) {
        
        
        // Check if the field is a configurable property
        PropertyField propertyField = (PropertyField) m_jmxConfigFieldMap.getPropertyFromField(fieldName);
        if (propertyField != null) { 
            m_instanceManager.setterCallback(fieldName, propertyField.getValue());
            return propertyField.getValue();
        }
        m_instanceManager.setterCallback(fieldName, value);
        return value;
    }
    
    /** 
     * getTypeFromAttributeField : get the type from a field name.
     * @param fieldRequire : name of the requiered field 
     * @param manipulation : metadata extract from metadata.xml file
     * @return          : type of the field or null if it wasn't found
     */
    private static String getTypeFromAttributeField(String fieldRequire, ManipulationMetadata manipulation) {
        
        FieldMetadata field = manipulation.getField(fieldRequire);
        if (field == null) {
            return null;
        } else {
            return field.getReflectionType();
        }
    }
    
    /** 
     * getMethodsFromName : get all the methods available which get this name.
     * @param methodName : name of the requiered methods
     * @param manipulation : metadata extract from metadata.xml file
     * @param description  : description which appears in jmx console
     * @return          : array of methods with the right name
     */
    private MethodField[] getMethodsFromName(String methodName, ManipulationMetadata manipulation, String description) {
        
        MethodMetadata[] fields = manipulation.getMethods(methodName);
        if (fields.length == 0) {
            return null;
        }
        
        MethodField[] ret = new MethodField[fields.length];
        
        if (fields.length == 1) {
            ret[0] = new MethodField(fields[0], description);
            return ret;
        } else {
            for (int i = 0 ; i < fields.length ; i++) {
                ret[i] = new MethodField(fields[i], description);
            }
            return ret;
        }
    }
    

}
