package org.apache.felix.bundlerepository.impl.wrapper;

import java.net.URL;
import java.util.Map;

import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Requirement;

public class ResourceWrapper implements org.osgi.service.obr.Resource {

    final Resource resource;

    public ResourceWrapper(Resource resource) {
        this.resource = resource;
    }

    public Map getProperties() {
        return resource.getProperties();
    }

    public String getSymbolicName() {
        return resource.getSymbolicName();
    }

    public String getPresentationName() {
        return resource.getPresentationName();
    }

    public Version getVersion() {
        return resource.getVersion();
    }

    public String getId() {
        return resource.getId();
    }

    public URL getURL() {
        return resource.getURL();
    }

    public Requirement[] getRequirements() {
        return Wrapper.wrap(resource.getRequirements());
    }

    public Capability[] getCapabilities() {
        return Wrapper.wrap(resource.getCapabilities());
    }

    public String[] getCategories() {
        return resource.getCategories();
    }

    public Repository getRepository() {
        return Wrapper.wrap(resource.getRepository());
    }
}
