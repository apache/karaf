package org.apache.karaf.config.core;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;

public interface ConfigRepository {

    /**
     * Saves config to storage or ConfigurationAdmin.
     * @param pid
     * @param props
     * @param bypassStorage
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
    void update(String pid, Dictionary props, boolean bypassStorage) throws IOException;

    /**
     * Saves config to storage or ConfigurationAdmin.
     * @param pid
     * @param props
     * @param bypassStorage
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
    void update(String pid, Dictionary props) throws IOException;

    void delete(String pid) throws Exception;

    @SuppressWarnings("rawtypes")
    Dictionary getConfigProperties(String pid) throws IOException, InvalidSyntaxException;

    ConfigurationAdmin getConfigAdmin();
}
