package org.apache.felix.dm.samples.annotation;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

@Service
public class ServiceProvider implements ServiceInterface
{
    public ServiceProvider()
    {
    }

    @Start
    public void start()
    {
        System.out.println("ServiceProvider.start");
    }

    @Stop
    public void stop()
    {
        System.out.println("ServiceProvider.stop");
    }

    public void doService()
    {
        System.out.println("ServiceProvider.doService()");
    }
}
