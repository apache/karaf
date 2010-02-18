package org.apache.felix.dm.test.bundle.annotation.aspect;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.Constants;

@AspectService(filter = "(!(" + Constants.SERVICE_RANKING + "=*))", properties = { @Param(name = Constants.SERVICE_RANKING, value = "1") })
public class ServiceProviderAspect implements ServiceInterface
{
    @ServiceDependency(filter = "(test=aspect.ServiceProviderAspect)")
    protected Sequencer m_sequencer;

    public void run()
    {
        m_sequencer.step(2);
    }
}
