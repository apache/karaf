package org.apache.felix.bundlerepository.impl.wrapper;

import java.net.URL;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Feb 25, 2010
 * Time: 11:50:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryWrapper implements org.osgi.service.obr.Repository {

    private final Repository repository;

    public RepositoryWrapper(Repository repository)
    {
        this.repository = repository;
    }

    public URL getURL() {
        return repository.getURL();
    }

    public org.osgi.service.obr.Resource[] getResources() {
        return Wrapper.wrap(repository.getResources());
    }

    public String getName() {
        return repository.getName();
    }

    public long getLastModified() {
        return repository.getLastModified();
    }

}
