package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service(properties = { "foo=bar" })
public class ServiceProvider implements ServiceInterface
{
    @ServiceDependency
    Sequencer m_sequencer;

    @ServiceDependency(removed="unbind")
    void bind(ServiceProvider2 provider2)
    {
        m_sequencer.next(4);
    }

    void unbind(ServiceProvider2 provider2)
    {
        m_sequencer.next(10);
    }

    @Start
    void start()
    {
        m_sequencer.next(5);
    }

    @Stop
    void stop()
    {
        m_sequencer.next(9);
    }

    public void doService()
    {
        m_sequencer.next(7);
    }
}
