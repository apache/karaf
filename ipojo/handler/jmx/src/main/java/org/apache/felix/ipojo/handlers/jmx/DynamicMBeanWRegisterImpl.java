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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;

/**
 * This class implements a 'wide' iPOJO DynamicMBean that can perform actions
 * before and after its registration and deregistration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DynamicMBeanWRegisterImpl extends DynamicMBeanImpl implements
        MBeanRegistration {

    /**
     * The preRegister method of MBeanRegistration interface.
     */
    private MethodMetadata m_preRegisterMeth;
    /**
     * The postRegister method of MBeanRegistration interface.
     */
    private MethodMetadata m_postRegisterMeth;
    /**
     * The preDeregister method of MBeanRegistration interface.
     */
    private MethodMetadata m_preDeregisterMeth;
    /**
     * The postDeregister method of MBeanRegistration interface.
     */
    private MethodMetadata m_postDeregisterMeth;
    /**
     * The effective name of the MBean.
     */
    private ObjectName m_objName;

    /**
     * Constructs a new DynamicMBeanWRegisterImpl.
     * 
     * @param properties the data extracted from the metadata.xml
     * @param instanceManager the instance manager
     * @param preRegisterMeth the method to call before MBean registration
     * @param postRegisterMeth the method to call after MBean registration
     * @param preDeregisterMeth the method to call before MBean deregistration
     * @param postDeregisterMeth the method to call after MBean registration
     */
    public DynamicMBeanWRegisterImpl(JmxConfigFieldMap properties,
            InstanceManager instanceManager, MethodMetadata preRegisterMeth,
            MethodMetadata postRegisterMeth, MethodMetadata preDeregisterMeth,
            MethodMetadata postDeregisterMeth) {
        super(properties, instanceManager);

        m_preRegisterMeth = preRegisterMeth;
        m_postRegisterMeth = postRegisterMeth;
        m_preDeregisterMeth = preDeregisterMeth;
        m_postDeregisterMeth = postDeregisterMeth;
    }

    /**
     * Returns the MBean name used to register it.
     * 
     * @return the MBean name used to register it.
     */
    public ObjectName getObjectName() {
        return m_objName;
    }

    /**
     * This method is executed before the MBean registration.
     * 
     * @param server the server on which the MBean will be registered
     * @param name the name of the MBean to expose
     * @throws Exception This exception will be caught by the MBean server and re-thrown as an MBeanRegistrationException.
     * @return the name with which the MBean will be registered
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
        m_objName = (ObjectName) callMethod(m_preRegisterMeth,
            MBeanHandler.PRE_REGISTER_METH_NAME, new Object[] { server, name });
        return m_objName;
    }

    /**
     * This method is executed after the MBean registration.
     * 
     * @param registrationDone indicates whether or not the MBean has been successfully registered in the MBean server.
     */
    public void postRegister(Boolean registrationDone) {
        callMethod(m_postRegisterMeth, MBeanHandler.POST_REGISTER_METH_NAME,
            new Object[] { registrationDone });
    }

    /**
     * This method is before after the MBean deregistration.
     * 
     * @throws Exception This exception will be caught by the MBean server and re-thrown as an MBeanRegistrationException.
     */
    public void preDeregister() throws Exception {
        callMethod(m_preDeregisterMeth, MBeanHandler.PRE_DEREGISTER_METH_NAME,
            null);
    }

    /**
     * This method is executed after the MBean deregistration.
     */
    public void postDeregister() {
        callMethod(m_postDeregisterMeth,
            MBeanHandler.POST_DEREGISTER_METH_NAME, null);
    }

    /**
     * Private method used to execute a given callback.
     * 
     * @param methodMetadata  the metadata description of the callback
     * @param methodName the name of the callback
     * @param params the parameters of the callback
     * @return the object eventually returned by the callback, or null if nothing's returned
     */
    private Object callMethod(MethodMetadata methodMetadata, String methodName,
            Object[] params) {
        Callback mc = new Callback(methodMetadata, m_instanceManager);
        try {
            if ((params == null) || (params.length == 0)) {
                return mc.call();
            } else {
                return mc.call(params);
            }
        } catch (NoSuchMethodException e) {
            // should never happen : method exists
            System.err.println("No such method : " + methodName);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.err.println("Illegal Access Exception");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Invocation Target Exception");
            e.printStackTrace();
        }
        return null;
    }
}
