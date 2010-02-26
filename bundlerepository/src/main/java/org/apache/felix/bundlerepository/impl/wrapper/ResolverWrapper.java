package org.apache.felix.bundlerepository.impl.wrapper;

import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Feb 25, 2010
 * Time: 11:52:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResolverWrapper implements org.osgi.service.obr.Resolver {

    private final Resolver resolver;

    public ResolverWrapper(Resolver resolver)
    {
        this.resolver = resolver;
    }

    public void add(org.osgi.service.obr.Resource resource) {
        resolver.add(Wrapper.unwrap(resource));
    }

    public org.osgi.service.obr.Resource[] getAddedResources() {
        return Wrapper.wrap(resolver.getAddedResources());
    }

    public org.osgi.service.obr.Requirement[] getUnsatisfiedRequirements() {
        return Wrapper.wrap(resolver.getUnsatisfiedRequirements());
    }

    public org.osgi.service.obr.Resource[] getOptionalResources() {
        return Wrapper.wrap(resolver.getOptionalResources());
    }

    public org.osgi.service.obr.Requirement[] getReason(org.osgi.service.obr.Resource resource) {
        return Wrapper.wrap(resolver.getReason(Wrapper.unwrap(resource)));
    }

    public org.osgi.service.obr.Resource[] getResources(org.osgi.service.obr.Requirement requirement) {
        return Wrapper.wrap(resolver.getResources(Wrapper.unwrap(requirement)));
    }

    public org.osgi.service.obr.Resource[] getRequiredResources() {
        return Wrapper.wrap(resolver.getRequiredResources());
    }

    public boolean resolve() {
        return resolver.resolve();
    }

    public void deploy(boolean start) {
        resolver.deploy(start);
    }
}
