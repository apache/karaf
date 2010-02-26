package org.apache.felix.bundlerepository.impl.wrapper;

import java.util.Map;

import org.apache.felix.bundlerepository.Capability;

public class CapabilityWrapper implements org.osgi.service.obr.Capability {

    final Capability capability;

    public CapabilityWrapper(Capability capability)
    {
        this.capability = capability;
    }

    public String getName() {
        return capability.getName();
    }

    public Map getProperties() {
        return capability.getProperties();
    }
}
