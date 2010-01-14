package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service
public class ServiceConsumer
{
    @ServiceDependency
    volatile Sequencer m_sequencer;

    @ServiceDependency(filter = "(foo=bar)")
    volatile ServiceInterface m_service;

    @Start
    void start()
    {
        m_sequencer.next(6);
        m_service.doService();
    }

    @Stop
    void stop()
    {
        m_sequencer.next(8);
    }
}
