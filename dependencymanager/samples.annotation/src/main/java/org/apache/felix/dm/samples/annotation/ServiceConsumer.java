package org.apache.felix.dm.samples.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service
public class ServiceConsumer
{
    @ServiceDependency
    private volatile ServiceInterface m_service;

    @ServiceDependency
    protected void bind(ServiceInterface si)
    {
    }

    @Start
    protected void start()
    {
        System.out.println("ServiceConsumer.start");
        m_service.doService();
    }

    @Stop
    protected void stop()
    {
        System.out.println("ServiceConsumer.stop");
    }
}
