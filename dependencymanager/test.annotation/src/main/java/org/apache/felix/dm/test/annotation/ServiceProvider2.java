package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service(provide = { ServiceProvider2.class })
public class ServiceProvider2
{
    Composite m_composite = new Composite();
    Sequencer m_sequencer;

    @ServiceDependency(required=false, filter="(foo=bar)")
    Runnable m_runnable;

    @ServiceDependency(service=Sequencer.class)
    void bind(Sequencer seq)
    {
        m_sequencer = seq;
        m_sequencer.next(1);
    }

    @Start
    void start()
    {
        m_sequencer.next(3);
        m_runnable.run();
    }

    @Stop
    void stop()
    {
        m_sequencer.next(11);
    }

    @Composition
    Object[] getComposition()
    {
        return new Object[] { this, m_composite };
    }
}

class Composite
{
    void bind(Sequencer seq)
    {
        seq.next(2);
    }
}

