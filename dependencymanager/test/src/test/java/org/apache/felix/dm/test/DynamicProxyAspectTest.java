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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class DynamicProxyAspectTest extends Base {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    @Test
    public void testImplementGenericAspectWithDynamicProxy(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create two service providers, each providing a different service interface
        Service sp1 = m.createService().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Service sp2 = m.createService().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface2.class.getName(), null);
        
        // create a dynamic proxy based aspect and hook it up to both services
        Service a1 = m.createAspectService(ServiceInterface.class, null, 10, new Factory(e, ServiceInterface.class, "ServiceInterfaceProxy"), "create", "m_service", null);
        Service a2 = m.createAspectService(ServiceInterface2.class, null, 10, new Factory(e, ServiceInterface2.class, "ServiceInterfaceProxy2"), "create", "m_service", null);

        // create a client that invokes a method on boths services, validate that it goes
        // through the proxy twice
        Service sc = m.createService()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(true))
            ;
        
        // register both producers, validate that both services are started
        m.add(sp1);
        e.waitForStep(1, 2000);
        m.add(sp2);
        e.waitForStep(2, 2000);
        
        // add both aspects, and validate that both instances have been created
        m.add(a1);
        m.add(a2);
        e.waitForStep(4, 4000);
        
        // add the client, which will automatically invoke both services
        m.add(sc);
        
        // wait until both services have been invoked
        e.waitForStep(6, 4000);
        
        // make sure the proxy has been called twice
        Assert.assertEquals("Proxy should have been invoked this many times.", 2, DynamicProxyHandler.getCounter());
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable run);
    }
    
    static interface ServiceInterface2 {
        public void invoke(Runnable run);
    }
    
    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void start() {
            m_ensure.step(1);
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceProvider2 implements ServiceInterface2 {
        private final Ensure m_ensure;
        public ServiceProvider2(Ensure ensure) {
            m_ensure = ensure;
        }
        public void start() {
            m_ensure.step(2);
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private volatile ServiceInterface2 m_service2;
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 5));
            m_service2.invoke(Ensure.createRunnableStep(m_ensure, 6));
        }
    }
    
    static class DynamicProxyHandler implements InvocationHandler {
        public volatile Object m_service; // ISSUE, we cannot inject into "Object" at the moment
        private final String m_label;
        private static volatile int m_counter = 0;

        public DynamicProxyHandler(String label) {
            m_label = label;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (m_service == null) {
                Assert.fail("No service was injected into dynamic proxy handler " + m_label);
            }
            Method m = m_service.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (m == null) {
                Assert.fail("No method " + method.getName() + " was found in instance " + m_service + " in dynamic proxy handler " + m_label);
            }
            m_counter++;
            return m.invoke(m_service, args);
        }
        
        public static int getCounter() {
            return m_counter;
        }
    }
    
    static class Factory {
        private final String m_label;
        private Class m_class;
        private final Ensure m_ensure;
        
        public Factory(Ensure ensure, Class clazz, String label) {
            m_ensure = ensure;
            m_class = clazz;
            m_label = label;
        }
        
        public Object create() {
            m_ensure.step();
            return Proxy.newProxyInstance(m_class.getClassLoader(), new Class[] { m_class }, new DynamicProxyHandler(m_label));
        }
    }
}
