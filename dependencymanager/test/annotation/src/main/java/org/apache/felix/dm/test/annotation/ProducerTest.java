package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service
public class ProducerTest implements Runnable
{
    @ServiceDependency(filter="(test=simple)")
    Sequencer m_sequencer;
    
    @Start
    protected void start() {
        m_sequencer.step(1);
    }
    
    @Stop
    protected void stop() {
        m_sequencer.step(5);
    }
    
    public void run()
    {
        m_sequencer.step(3);
    }
}
