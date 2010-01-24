package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Composition;
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
        @ServiceDependency
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

    @Service(properties = { "foo=bar" }, factory=Factory.class, factoryMethod="createServiceProvider")
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
