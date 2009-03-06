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
package org.apache.servicemix.kernel.gshell.features.management;

import java.util.HashSet;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;

/**
 * Management Agent that registers MBeans with JMX MBeanServer.
 */
public class ManagementAgent implements DisposableBean {

    private static final transient Log LOG = LogFactory.getLog(ManagementAgent.class);

    private MBeanServer mbeanServer;
    private MetadataMBeanInfoAssembler assembler;
    private Set<ObjectName> mbeans = new HashSet<ObjectName>();

    public ManagementAgent() {
        assembler = new MetadataMBeanInfoAssembler();
        assembler.setAttributeSource(new AnnotationJmxAttributeSource());
    }

    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public void destroy() throws Exception {
        // Using the array to hold the busMBeans to avoid the
        // CurrentModificationException
        Object[] mBeans = mbeans.toArray();
        int caught = 0;
        for (Object name : mBeans) {
            mbeans.remove((ObjectName)name);
            try {
                unregister((ObjectName)name);
            } catch (JMException jmex) {
                LOG.info("Exception unregistering MBean", jmex);
                caught++;
            }
        }
        if (caught > 0) {
            LOG.warn("A number of " + caught
                     + " exceptions caught while unregistering MBeans during stop operation.  "
                     + "See INFO log for details.");
        }
    }

    public void register(Object obj, ObjectName name) throws JMException {
        register(obj, name, false);
    }

    public void register(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        try {
            registerMBeanWithServer(obj, name, forceRegistration);
        } catch (NotCompliantMBeanException e) {
            // If this is not a "normal" MBean, then try to deploy it using JMX
            // annotations
            ModelMBeanInfo mbi = assembler.getMBeanInfo(obj, name.toString());
            RequiredModelMBean mbean = (RequiredModelMBean) mbeanServer.instantiate(RequiredModelMBean.class.getName());
            mbean.setModelMBeanInfo(mbi);
            try {
                mbean.setManagedResource(obj, "ObjectReference");
            } catch (InvalidTargetObjectTypeException itotex) {
                throw new JMException(itotex.getMessage());
            }
            registerMBeanWithServer(mbean, name, forceRegistration);
        }
    }

    public synchronized void unregister(ObjectName name) throws JMException {
        if (mbeans.contains(name)) {
            //check if this bean already get removed in destory method
            mbeanServer.unregisterMBean(name);
        }
    }

    private void registerMBeanWithServer(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        ObjectInstance instance = null;
        try {
            instance = mbeanServer.registerMBean(obj, name);
        } catch (InstanceAlreadyExistsException e) {
            if (forceRegistration) {
                mbeanServer.unregisterMBean(name);
                instance = mbeanServer.registerMBean(obj, name);
            } 
        } catch (NotCompliantMBeanException e) {
            throw e;
        }

        if (instance != null) {
            mbeans.add(name);
        }
    }

}