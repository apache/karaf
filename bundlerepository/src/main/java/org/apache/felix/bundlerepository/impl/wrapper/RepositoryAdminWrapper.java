package org.apache.felix.bundlerepository.impl.wrapper;

import java.net.URL;

import org.apache.felix.bundlerepository.RepositoryAdmin;

public class RepositoryAdminWrapper implements org.osgi.service.obr.RepositoryAdmin
{

    private final RepositoryAdmin admin;

    public RepositoryAdminWrapper(RepositoryAdmin admin) 
    {
        this.admin = admin;
    }

    public org.osgi.service.obr.Resource[] discoverResources(String filterExpr) {
        return Wrapper.wrap(admin.discoverResources(filterExpr));
    }

    public org.osgi.service.obr.Resolver resolver() {
        return Wrapper.wrap(admin.resolver());
    }

    public org.osgi.service.obr.Repository addRepository(URL repository) throws Exception {
        return Wrapper.wrap(admin.addRepository(repository));
    }

    public boolean removeRepository(URL repository) {
        return admin.removeRepository(repository);
    }

    public org.osgi.service.obr.Repository[] listRepositories() {
        return Wrapper.wrap(admin.listRepositories());
    }

    public org.osgi.service.obr.Resource getResource(String s) {
        throw new UnsupportedOperationException();
    }

}
