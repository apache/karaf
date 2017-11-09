/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.management.internal;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;
import java.util.Objects;
import java.util.Set;

public class EventAdminMBeanServerWrapper implements MBeanServer {

    private static final String[] NO_ARGS_SIG = new String[] {ObjectName.class.getName()};
    private static final String[] OBJECT_NAME_ONLY_SIG = new String[] {ObjectName.class.getName()};

    private static final String CREATE_MBEAN = "createMBean";
    private static final String[] CREATE_MBEAN_SIG_1 = new String[] {String.class.getName(), ObjectName.class.getName()};
    private static final String[] CREATE_MBEAN_SIG_2 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName()};
    private static final String[] CREATE_MBEAN_SIG_3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    private static final String[] CREATE_MBEAN_SIG_4 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String REGISTER_MBEAN = "registerMBean";
    private static final String[] REGISTER_MBEAN_SIG = new String[] {Object.class.getName(), ObjectName.class.getName()};


    private static final String UNREGISTER_MBEAN = "unregisterMBean";
    private static final String[] UNREGISTER_MBEAN_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_OBJECT_INSTANCE = "getObjectInstance";
    private static final String[] GET_OBJECT_INSTANCE_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String QUERY_MBEANS = "queryMBeans";
    private static final String[] QUERY_MBEANS_SIG = new String[] {ObjectName.class.getName(), QueryExp.class.getName()};

    private static final String QUERY_NAMES = "queryMBeans";
    private static final String[] QUERY_NAMES_SIG = QUERY_MBEANS_SIG;

    private static final String IS_REGISTERED = "isRegistered";
    private static final String[] IS_REGISTERED_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_MBEAN_COUNT = "getMBeanCount";
    private static final String[] GET_MBEAN_COUNT_SIG = NO_ARGS_SIG;

    private static final String GET_ATTRIBUTE = "getAttribute";
    private static final String[] GET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    private static final String GET_ATTRIBUTES = "getAttributes";
    private static final String[] GET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), String[].class.getName()};

    private static final String SET_ATTRIBUTE = "setAttribute";
    private static final String[] SET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), Attribute.class.getName()};

    private static final String SET_ATTRIBUTES = "setAttributes";
    private static final String[] SET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), AttributeList.class.getName()};

    private static final String INVOKE = "invoke";
    private static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String GET_DEFAULT_DOMAIN = "getDefaultDomain";
    private static final String[] GET_DEFAULT_DOMAIN_SIG = NO_ARGS_SIG;

    private static final String GET_DOMAINS = "getDomains";
    private static final String[] GET_DOMAINS_SIG = NO_ARGS_SIG;

    private static final String ADD_NOTIFICATION_LISTENER  = "addNotificationListener";
    private static final String[] ADD_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    private static final String[] ADD_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    private static final String REMOVE_NOTIFICATION_LISTENER  = "addNotificationListener";
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), ObjectName.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_3 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_4 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    private static final String GET_MBEAN_INFO = "getMBeanInfo";
    private static final String[] GET_MBEAN_INFO_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String IS_INSTANCE_OF = "isInstanceOf";
    private static final String[] IS_INSTANCE_OF_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    private static final String INSTANTIATE = "instantiate";
    private static final String[] INSTANTIATE_SIG1 = new String[] {String.class.getName()};
    private static final String[] INSTANTIATE_SIG2 = new String[] {String.class.getName(), ObjectName.class.getName()};
    private static final String[] INSTANTIATE_SIG3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    private static final String[] INSTANTIATE_SIG4 = new String[] {String.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String DESERIALIZE = "deserialize";
    private static final String[] DESERIALIZE_SIG1 = new String[] {ObjectName.class.getName(), byte[].class.getName()};
    private static final String[] DESERIALIZE_SIG2 = new String[] {String.class.getName(), byte[].class.getName()};
    private static final String[] DESERIALIZE_SIG3 = new String[] {String.class.getName(), ObjectName.class.getName(), byte[].class.getName()};

    private static final String GET_CLASSLOADER_FOR = "getClassLoaderFor";
    private static final String[] GET_CLASSLOADER_FOR_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_CLASSLOADER = "getClassLoader";
    private static final String[] GET_CLASSLOADER_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_CLASSLOADER_REPOSITORY = "getClassLoaderRepository";
    private static final String[] GET_CLASSLOADER_REPOSITORY_SIG = NO_ARGS_SIG;

    private final MBeanServer delegate;
    private final EventAdminLogger logger;

    public EventAdminMBeanServerWrapper(MBeanServer delegate, EventAdminLogger logger) {
        this.delegate = Objects.requireNonNull(delegate);
        this.logger = Objects.requireNonNull(logger);
    }

    private void log(String methodName, String[] signature, Object result, Throwable error, Object... params) {
        logger.log(methodName, signature, result, error, params);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.createMBean(className, name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(CREATE_MBEAN, CREATE_MBEAN_SIG_1, result, error, className, name);
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.createMBean(className, name, loaderName);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(CREATE_MBEAN, CREATE_MBEAN_SIG_2, result, error, className, name, loaderName);
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.createMBean(className, name, params, signature);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(CREATE_MBEAN, CREATE_MBEAN_SIG_3, result, error, className, name, params, signature);
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.createMBean(className, name, loaderName, params, signature);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(CREATE_MBEAN, CREATE_MBEAN_SIG_4, result, error, className, name, loaderName, params, signature);
        }
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.registerMBean(object, name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(REGISTER_MBEAN, REGISTER_MBEAN_SIG, result, error, object, name);
        }
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        Throwable error = null;
        try {
            delegate.unregisterMBean(name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(UNREGISTER_MBEAN, UNREGISTER_MBEAN_SIG, null, error, name);
        }
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        Throwable error = null;
        ObjectInstance result = null;
        try {
            return result = delegate.getObjectInstance(name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_OBJECT_INSTANCE, GET_OBJECT_INSTANCE_SIG, result, error, name);
        }
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Throwable error = null;
        Set<ObjectInstance> result = null;
        try {
            return result = delegate.queryMBeans(name, query);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(QUERY_MBEANS, QUERY_MBEANS_SIG, result, error, name, query);
        }
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Throwable error = null;
        Set<ObjectName> result = null;
        try {
            return result = delegate.queryNames(name, query);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(QUERY_NAMES, QUERY_NAMES_SIG, result, error, name, query);
        }
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        Throwable error = null;
        Boolean result = null;
        try {
            return result = delegate.isRegistered(name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(IS_REGISTERED, IS_REGISTERED_SIG, result, error, name);
        }
    }

    @Override
    public Integer getMBeanCount() {
        Throwable error = null;
        Integer result = null;
        try {
            return result = delegate.getMBeanCount();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_MBEAN_COUNT, GET_MBEAN_COUNT_SIG, result, error);
        }
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.getAttribute(name, attribute);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_ATTRIBUTE, GET_ATTRIBUTE_SIG, result, error, name, attribute);
        }
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        Throwable error = null;
        AttributeList result = null;
        try {
            return result = delegate.getAttributes(name, attributes);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_ATTRIBUTES, GET_ATTRIBUTES_SIG, result, error, name, attributes);
        }
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Throwable error = null;
        try {
            delegate.setAttribute(name, attribute);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(SET_ATTRIBUTE, SET_ATTRIBUTE_SIG, null, error, name, attribute);
        }
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        Throwable error = null;
        AttributeList result = null;
        try {
            return result = delegate.setAttributes(name, attributes);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(SET_ATTRIBUTES, SET_ATTRIBUTES_SIG, result, error, name, attributes);
        }
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.invoke(name, operationName, params, signature);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(INVOKE, INVOKE_SIG, result, error, name, operationName, params, signature);
        }
    }

    @Override
    public String getDefaultDomain() {
        Throwable error = null;
        String result = null;
        try {
            return result = delegate.getDefaultDomain();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_DEFAULT_DOMAIN, GET_DEFAULT_DOMAIN_SIG, result, error);
        }
    }

    @Override
    public String[] getDomains() {
        Throwable error = null;
        String[] result = null;
        try {
            return result = delegate.getDomains();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_DOMAINS, GET_DOMAINS_SIG, result, error);
        }
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        Throwable error = null;
        try {
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_1, null, error, name, listener, filter, handback);
        }
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        Throwable error = null;
        try {
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_2, null, error, name, listener, filter, handback);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        try {
            delegate.removeNotificationListener(name, listener);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_1, null, error, name, listener);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        try {
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_2, null, error, name, listener, filter, handback);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        try {
            delegate.removeNotificationListener(name, listener);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_3, null, error, name, listener);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        try {
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_4, null, error, name, listener, filter, handback);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        Throwable error = null;
        MBeanInfo result = null;
        try {
            return result = delegate.getMBeanInfo(name);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_MBEAN_INFO, GET_MBEAN_INFO_SIG, result, error, name);
        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        Throwable error = null;
        Boolean result = null;
        try {
            return result = delegate.isInstanceOf(name, className);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(IS_INSTANCE_OF, IS_INSTANCE_OF_SIG, result, error, name, className);
        }
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.instantiate(className);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(INSTANTIATE, INSTANTIATE_SIG1, result, error, className);
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.instantiate(className, loaderName);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(INSTANTIATE, INSTANTIATE_SIG2, result, error, className, loaderName);
        }
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.instantiate(className, params, signature);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(INSTANTIATE, INSTANTIATE_SIG3, result, error, className, params, signature);
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        Throwable error = null;
        Object result = null;
        try {
            return result = delegate.instantiate(className, loaderName, params, signature);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(INSTANTIATE, INSTANTIATE_SIG4, result, error, className, loaderName, params, signature);
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        Throwable error = null;
        ObjectInputStream result = null;
        try {
            return result = delegate.deserialize(name, data);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(DESERIALIZE, DESERIALIZE_SIG1, result, error, name, data);
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        ObjectInputStream result = null;
        try {
            return result = delegate.deserialize(className, data);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(DESERIALIZE, DESERIALIZE_SIG2, result, error, className, data);
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        ObjectInputStream result = null;
        try {
            return result = delegate.deserialize(className, loaderName, data);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(DESERIALIZE, DESERIALIZE_SIG3, result, error, className, loaderName, data);
        }
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        Throwable error = null;
        ClassLoader result = null;
        try {
            return result = delegate.getClassLoaderFor(mbeanName);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_CLASSLOADER_FOR, GET_CLASSLOADER_FOR_SIG, result, error, mbeanName);
        }
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        Throwable error = null;
        ClassLoader result = null;
        try {
            return result = delegate.getClassLoader(loaderName);
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_CLASSLOADER, GET_CLASSLOADER_SIG, result, error, loaderName);
        }
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        Throwable error = null;
        ClassLoaderRepository result = null;
        try {
            return result = delegate.getClassLoaderRepository();
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            log(GET_CLASSLOADER_REPOSITORY, GET_CLASSLOADER_REPOSITORY_SIG, result, error);
        }
    }

    public MBeanServer getDelegate() {
        return delegate;
    }
}
