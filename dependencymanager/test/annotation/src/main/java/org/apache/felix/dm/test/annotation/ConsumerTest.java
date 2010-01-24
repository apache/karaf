package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service
public class ConsumerTest
{
    @ServiceDependency
    Runnable m_runnable;
    
    @ServiceDependency(filter="(test=simple)")
    Sequencer m_sequencer;

    @Start
    protected void start() {
        m_sequencer.step(2);
        m_runnable.run();
    }
    
    @Stop
    protected void stop() {
        m_sequencer.step(4);
    }
}
