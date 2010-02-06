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
package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

public class MultipleAnnotationTest
{
    public static class Factory {
        public ServiceConsumer createServiceConsumer() {
            return new ServiceConsumer();
        }
        
        public ServiceProvider createServiceProvider() {
            return new ServiceProvider();
        }
        
        public ServiceProvider2 createServiceProvider2() {
            return new ServiceProvider2();
        }
    }
    
    public interface ServiceInterface {
        public void doService();
    }

    @Service(factory=Factory.class, factoryMethod="createServiceConsumer")
    static class ServiceConsumer
    {
        @ServiceDependency(filter="(test=multiple)")
        volatile Sequencer m_sequencer;

        @ServiceDependency(filter = "(foo=bar)")
        volatile ServiceInterface m_service;

        @Start
        void start()
        {
            m_sequencer.step(6);
            m_service.doService();
        }

        @Stop
        void stop()
        {
            m_sequencer.step(8);
        }
    }

    @Service(properties = {@Param(name="foo", value="bar") }, factory=Factory.class, factoryMethod="createServiceProvider")
    static class ServiceProvider implements ServiceInterface
    {
        @ServiceDependency(filter="(test=multiple)")
        Sequencer m_sequencer;

        @ServiceDependency(removed="unbind")
        void bind(ServiceProvider2 provider2)
        {
            m_sequencer.step(4);
        }

        void unbind(ServiceProvider2 provider2)
        {
            m_sequencer.step(10);
        }

        @Start
        void start()
        {
            m_sequencer.step(5);
        }

        @Stop
        void stop()
        {
            m_sequencer.step(9);
        }

        public void doService()
        {
            m_sequencer.step(7);
        }
    }

    @Service(provide = { ServiceProvider2.class }, factory=Factory.class, factoryMethod="createServiceProvider2")
    static class ServiceProvider2
    {
        Composite m_composite = new Composite();
        Sequencer m_sequencer;

        @ServiceDependency(required=false, filter="(foo=bar)")
        Runnable m_runnable;

        @ServiceDependency(service=Sequencer.class, filter="(test=multiple)")
        void bind(Sequencer seq)
        {
            m_sequencer = seq;
            m_sequencer.step(1);
        }

        @Start
        void start()
        {
            m_sequencer.step(3);
            m_runnable.run();
        }

        @Stop
        void stop()
        {
            m_sequencer.step(11);
        }

        @Composition
        Object[] getComposition()
        {
            return new Object[] { this, m_composite };
        }
    }

    static class Composite
    {
        void bind(Sequencer seq)
        {
            seq.step(2);
        }
    }
}
