package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.TemporalServiceDependency;

@Service(provide={})
public class TemporalServiceDependencyTest implements Runnable
{
    Thread m_thread;
    
    @ServiceDependency(filter="(test=temporal)")
    Sequencer m_sequencer;
    
    @TemporalServiceDependency(timeout=4000L,filter="(test=temporal)")
    Runnable m_service;
    
    @Start
    protected void start() {
        m_thread = new Thread(this);
        m_thread.start();
    }
    
    protected void stop() { 
        m_thread.interrupt();
        try
        {
            m_thread.join();
        }
        catch (InterruptedException e)
        {
        }
    }

    public void run()
    {
        m_service.run();
        m_sequencer.waitForStep(2, 15000);
        m_service.run(); // we should block here
        m_sequencer.waitForStep(4, 15000);
        try {
            m_service.run(); // should raise IllegalStateException
        } catch (IllegalStateException e) {
            m_sequencer.step(5);
        }
    }
}