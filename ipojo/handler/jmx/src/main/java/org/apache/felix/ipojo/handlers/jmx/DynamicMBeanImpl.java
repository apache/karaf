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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;

/**
 * This class implements iPOJO DynamicMBean. it builds the dynamic MBean
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DynamicMBeanImpl extends NotificationBroadcasterSupport implements
        DynamicMBean {

    /**
     * The instance manager. Used to store the InstanceManager instance.
     */
    protected final InstanceManager m_instanceManager;

    /**
     * The JmxConfigDFieldMap. Stors the data extracted from metadata.xml.
     */
    private JmxConfigFieldMap m_configMap;

    /**
     * The MBeanInfo. The class storing the MBean Informations.
     */
    private MBeanInfo m_mBeanInfo;

    /**
     * The class name. Constant storing the name of the class.
     */
    private String m_className = this.getClass().getName();

    /**
     * The sequence number. Used to calculate unique id to notification.
     */
    private int m_sequenceNumber;

    /**
     * Constructor.
     *
     * @param properties the data extracted from metadat.xml file
     * @param instanceManager the InstanceManager instance
     */
    public DynamicMBeanImpl(JmxConfigFieldMap properties,
            InstanceManager instanceManager) {
        m_configMap = properties;
        m_instanceManager = instanceManager;
        this.buildMBeanInfo();
    }

    /**
     * Gets the value of the required attribute.
     *
     * @param arg0 the name of required attribute
     * @throws AttributeNotFoundException if the attribute doesn't exist
     * @throws MBeanException if something bad occures
     * @throws ReflectionException if something bad occures
     * @return the object attribute
     */
    public Object getAttribute(String arg0) throws AttributeNotFoundException,
        MBeanException, ReflectionException {
        PropertyField attribute = m_configMap.getPropertyFromName(arg0);

        if (attribute == null) {
            throw new AttributeNotFoundException(arg0 + " not found");
        } else {
            return attribute.getValue();
        }
    }

    /**
     * Gets values of required attributes.
     *
     * @param attributeNames the names of the required attributes
     * @return return the list of the attribute
     */
    public AttributeList getAttributes(String[] attributeNames) {

        if (attributeNames == null) {
            throw new IllegalArgumentException(
                "attributeNames[] cannot be null");
        }

        AttributeList resultList = new AttributeList();
        for (int i = 0; i < attributeNames.length; i++) {
            PropertyField propertyField = (PropertyField) m_configMap
                .getPropertyFromField((String) attributeNames[i]);

            if (propertyField != null) {
                resultList.add(new Attribute(attributeNames[i], propertyField
                    .getValue()));
            }
        }
        return resultList;
    }

    /**
     * Returns the MBean Class builded.
     *
     * @return return MBeanInfo class constructed by buildMBeanInfo
     */
    public MBeanInfo getMBeanInfo() {
        return m_mBeanInfo;
    }

    /**
     * Invokes the required method on the targeted POJO.
     *
     * @param operationName the name of the method called
     * @param params the parameters given to the method
     * @param signature the determine which method called
     * @return the object return by the method
     * @throws MBeanException if something bad occures
     * @throws ReflectionException if something bad occures
     */
    public Object invoke(String operationName, Object[] params,
            String[] signature) throws MBeanException, ReflectionException {

        MethodField method = m_configMap.getMethodFromName(operationName,
            signature);
        if (method != null) {
            MethodMetadata methodCall = method.getMethod();
            Callback mc = new Callback(methodCall, m_instanceManager);
            try {
                return mc.call(params);
            } catch (NoSuchMethodException e) {
                throw new ReflectionException(e);
            } catch (IllegalAccessException e) {
                throw new ReflectionException(e);
            } catch (InvocationTargetException e) {
                throw new MBeanException(e);
            }
        } else {
            throw new ReflectionException(new NoSuchMethodException(
                operationName), "Cannot find the operation " + operationName
                    + " in " + m_className);
        }
    }

    /**
     * Changes specified attribute value.
     *
     * @param attribute the attribute with new value to be changed
     * @throws AttributeNotFoundException if the required attribute was not found
     * @throws InvalidAttributeValueException if the value is inccorrect type
     * @throws MBeanException if something bad occures
     * @throws ReflectionException if something bad occures
     */
    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException,
        MBeanException, ReflectionException {

        // Check attribute is not null to avoid NullPointerException later on
        if (attribute == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute cannot be null"), "Cannot invoke a setter of "
                    + m_className + " with null attribute");
        }
        String name = attribute.getName();
        Object value = attribute.getValue();

        if (name == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute name cannot be null"),
                "Cannot invoke the setter of " + m_className
                        + " with null attribute name");
        }
        // Check for a recognized attribute name and call the corresponding
        // setter
        //

        PropertyField propertyField = (PropertyField) m_configMap
            .getPropertyFromName(name);
        if (propertyField == null) {
            // unrecognized attribute name:
            throw new AttributeNotFoundException("Attribute " + name
                    + " not found in " + m_className);
        }
        if (!propertyField.isWritable()) {
            throw new InvalidAttributeValueException("Attribute " + name
                    + " can not be set");
        }

        if (value == null) {
            try {
                m_instanceManager.onSet(null, propertyField.getField(), null);
            } catch (Exception e) {
                throw new InvalidAttributeValueException(
                    "Cannot set attribute " + name + " to null");
            }
        } else { // if non null value, make sure it is assignable to the
            // attribute
            if (true /* TODO type.class.isAssignableFrom(value.getClass()) */) {
                // propertyField.setValue(value);
                // setValue(attributeField.getField(),null);
                m_instanceManager.onSet(null, propertyField.getField(), value);
            } else {
                throw new InvalidAttributeValueException(
                    "Cannot set attribute " + name + " to a "
                            + value.getClass().getName()
                            + " object, String expected");
            }
        }

    }

    /**
     * Changes all the attributes value.
     *
     * @param attributes the list of attribute value to be changed
     * @return the list of new attribute
     */
    public AttributeList setAttributes(AttributeList attributes) {

        // Check attributes is not null to avoid NullPointerException later on
        if (attributes == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "AttributeList attributes cannot be null"),
                "Cannot invoke a setter of " + m_className);
        }
        AttributeList resultList = new AttributeList();

        // if attributeNames is empty, nothing more to do
        if (attributes.isEmpty()) {
            return resultList;
        }

        // for each attribute, try to set it and add to the result list if
        // successful
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            try {
                setAttribute(attr);
                String name = attr.getName();
                Object value = getAttribute(name);
                resultList.add(new Attribute(name, value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }

    /**
     * Builds the MBean information on initialization. This
     * value doesn't change further.
     */
    private void buildMBeanInfo() {

        // generate infos for attributes
        MBeanAttributeInfo[] dAttributes = null;

        if (m_configMap == null) {
            return;
        }

        String dDescription = m_configMap.getDecription();

        if (m_configMap.getProperties() != null) {
            List < MBeanAttributeInfo > lAttributes = null;
            lAttributes = new ArrayList < MBeanAttributeInfo >();

            Iterator < PropertyField > iterator = m_configMap.getProperties()
                .iterator();
            while (iterator.hasNext()) {
                PropertyField propertyField = iterator.next();
                lAttributes.add(new MBeanAttributeInfo(propertyField.getName(),
                    propertyField.getType(), propertyField.getDescription(),
                    propertyField.isReadable(), propertyField.isWritable(),
                    false));
            }
            dAttributes = lAttributes
                .toArray(new MBeanAttributeInfo[lAttributes.size()]);
        }

        MBeanOperationInfo[] dOperations = null;
        if (m_configMap.getMethods() != null) {

            List < MBeanOperationInfo > lOperations = new ArrayList < MBeanOperationInfo >();

            Iterator < MethodField[] > iterator = m_configMap.getMethods()
                .iterator();
            while (iterator.hasNext()) {
                MethodField[] method = iterator.next();
                for (int i = 0; i < method.length; i++) {
                    lOperations.add(new MBeanOperationInfo(method[i].getName(),
                        method[i].getDescription(), method[i].getParams(),
                        method[i].getReturnType(), MBeanOperationInfo.UNKNOWN));
                }
                dOperations = lOperations
                    .toArray(new MBeanOperationInfo[lOperations.size()]);
            }
        }

        MBeanNotificationInfo[] dNotification = new MBeanNotificationInfo[0];
        if (m_configMap.getMethods() != null) {

            List < MBeanNotificationInfo > lNotifications = new ArrayList < MBeanNotificationInfo >();

            Iterator < NotificationField > iterator = m_configMap
                .getNotifications().iterator();
            while (iterator.hasNext()) {
                NotificationField notification = iterator
                    .next();
                lNotifications.add(notification.getNotificationInfo());
            }
            dNotification = lNotifications
                .toArray(new MBeanNotificationInfo[lNotifications.size()]);
        }

        m_mBeanInfo = new MBeanInfo(this.m_className, dDescription,
            dAttributes, null, // No constructor
            dOperations, dNotification);
    }

    /**
     * Gets the notification informations (use by JMX).
     *
     * @return the structure which describe the notifications
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        MBeanNotificationInfo[] dNotification = new MBeanNotificationInfo[0];
        if (m_configMap.getMethods() != null) {

            List < MBeanNotificationInfo > lNotifications = new ArrayList < MBeanNotificationInfo >();

            Iterator < NotificationField > iterator = m_configMap
                .getNotifications().iterator();
            while (iterator.hasNext()) {
                NotificationField notification = iterator
                    .next();
                lNotifications.add(notification.getNotificationInfo());
            }
            dNotification = lNotifications
                .toArray(new MBeanNotificationInfo[lNotifications.size()]);
        }
        return dNotification;
    }

    /**
     * Sends a notification to a subscriber.
     *
     * @param msg the msg to send
     * @param attributeName the name of the attribute
     * @param attributeType the type of the attribute
     * @param oldValue the old value of the attribute
     * @param newValue the new value of the attribute
     */
    public void sendNotification(String msg, String attributeName,
            String attributeType, Object oldValue, Object newValue) {

        long timeStamp = System.currentTimeMillis();

        if (newValue.equals(oldValue)) {
            return;
        }
        m_sequenceNumber++;
        Notification notification = new AttributeChangeNotification(this,
            m_sequenceNumber, timeStamp, msg, attributeName, attributeType,
            oldValue, newValue);
        sendNotification(notification);
        m_instanceManager.getFactory().getLogger().log(Logger.INFO,
            "Notification sent");
    }
}
