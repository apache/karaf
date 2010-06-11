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

import java.util.Hashtable;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

/**
 * Tests for extra dependencies which are declared from service's init method.
 */
@RunWith(JUnit4TestRunner.class)
public class MultipleExtraDependencyTest2 extends Base {
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
    public void testMultipleExtraDependencies(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        Ensure e = new Ensure();
        
        Service sp2 = m.createService()
            .setImplementation(ServiceProvider2.class).setInterface(ServiceProvider2.class.getName(), null)
            .setCallbacks("init", "start", "stop", null)
            .setComposition("getComposition");
        
        Service sp = m.createService()
              .setImplementation(ServiceProvider.class)
              .setInterface(ServiceInterface.class.getName(), new Hashtable() {{ put("foo", "bar"); }})                            
              .setCallbacks("init", "start", "stop", null);
        
        Service sc = m.createService()
              .setImplementation(ServiceConsumer.class)
              .setCallbacks("init", "start", "stop", null);
        
        // Provide the Sequencer service to the MultipleAnnotationsTest class.
        Service sequencer = 
            m.createService().setImplementation(new SequencerImpl(e))
                             .setInterface(Sequencer.class.getName(), null);
        m.add(sp2);
        m.add(sp);
        m.add(sc);
        m.add(sequencer);
        
        // Check if the test.annotation components have been initialized orderly
        e.waitForStep(7, 10000);
        
        // Stop the test.annotation bundle
        m.remove(sequencer);
        m.remove(sp);
        m.remove(sp2);
        m.remove(sc);
        
//        m.remove(sp2);
//        m.remove(sc);
//        m.remove(sp);
//        m.remove(sequencer);
        

        
        // And check if the test.annotation bundle has been deactivated orderly
        e.waitForStep(11, 10000);
    }
    
    public interface Sequencer
    {
        void step();
        void step(int step);
        void waitForStep(int step, int timeout);
    }
    
    public static class SequencerImpl implements Sequencer {
        Ensure m_ensure;
        
        public SequencerImpl(Ensure e)
        {
            m_ensure = e;
        }
        
        public void step()
        {
            m_ensure.step();
        }

        public void step(int step)
        {
            m_ensure.step(step);
        }

        public void waitForStep(int step, int timeout)
        {
            m_ensure.waitForStep(step, timeout);
        }  
    }
    
    public interface ServiceInterface
    {
        public void doService();
    }
    
    public static class ServiceConsumer
    {
        volatile Sequencer m_sequencer;
        volatile ServiceInterface m_service;
        ServiceDependency m_d1, m_d2;

        public void init(Object serviceInstance, DependencyManager m, Service s)
        {
           s.add(m_d1 = m.createServiceDependency()
                   .setService(Sequencer.class)
                   .setRequired(true)
                   .setInstanceBound(true)
                   .setAutoConfig("m_sequencer"));
           s.add(m_d2 = m.createServiceDependency()
                   .setService(ServiceInterface.class, "(foo=bar)")
                   .setRequired(true)
                   .setInstanceBound(true)
                   .setAutoConfig("m_service"));
        }
        
        void start()
        {
            m_d1.setInstanceBound(false);
            m_d2.setInstanceBound(false);
            m_sequencer.step(6);
            m_service.doService();
        }

        void stop()
        {
            m_sequencer.step(8);
        }
    }
    
    public static class ServiceProvider implements ServiceInterface
    {
        Sequencer m_sequencer;
        ServiceProvider2 m_serviceProvider2;
        ServiceDependency m_d1, m_d2;

        public void init(Object serviceInstance, DependencyManager m, Service s)
        {
            s.add(m_d1 = m.createServiceDependency()
                  .setService(Sequencer.class)
                  .setRequired(true)
                  .setInstanceBound(true)
                  .setAutoConfig("m_sequencer"));
            s.add(m_d2 = m.createServiceDependency()
                  .setService(ServiceProvider2.class)
                  .setRequired(true)
                  .setInstanceBound(true)
                  .setCallbacks("bind", "unbind"));
        }
        
        void bind(ServiceProvider2 provider2)
        {
            m_serviceProvider2 = provider2;
        }

        void start()
        {
            m_d1.setInstanceBound(false);
            m_d2.setInstanceBound(false);
            m_serviceProvider2.step(4);
            m_sequencer.step(5);
        }

        void stop()
        {
            m_sequencer.step(9);
        }

        void unbind(ServiceProvider2 provider2)
        {
            m_sequencer.step(10);
        }

        public void doService()
        {
            m_sequencer.step(7);
        }
    }

    public static class ServiceProvider2
    {
        Composite m_composite = new Composite();
        Sequencer m_sequencer;
        Runnable m_runnable;
        ServiceDependency m_d1, m_d2;

        public void init(Object serviceInstance, DependencyManager m, Service s)
        {
            s.add(m_d1 = m.createServiceDependency()
                  .setService(Runnable.class, "(foo=bar)")
                  .setRequired(false)
                  .setInstanceBound(true)
                  .setAutoConfig("m_runnable"));
            s.add(m_d2 = m.createServiceDependency()
                  .setService(Sequencer.class)
                  .setRequired(true)
                  .setInstanceBound(true)
                  .setCallbacks("bind", null));
        }
        
        void bind(Sequencer seq)
        {
            m_sequencer = seq;
            m_sequencer.step(1);
        }

        void start()
        {
            m_d1.setInstanceBound(false);
            m_d2.setInstanceBound(false);
            m_sequencer.step(3);
            m_runnable.run(); // NullObject
        }

        public void step(int step) // called by ServiceProvider.start() method 
        { 
            m_sequencer.step(step);
        }
        
        void stop()
        {
            m_sequencer.step(11);
        }

        Object[] getComposition()
        {
            return new Object[] { this, m_composite };
        }
    }
    
    public static class Composite
    {
        void bind(Sequencer seq)
        {
            seq.step(2);
        }
    }
}
