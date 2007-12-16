/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.obr.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

/**
 * this class is used to manage all connections by wagon.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class RemoteFileManager {

    /**
     * save the connection.
     */
    private Wagon m_wagon;

    /**
     * the wagon manager.
     */
    private WagonManager m_wagonManager;

    /**
     * artifact repository.
     */
    private ArtifactRepository m_artifactRepository;

    /**
     * the project settings.
     */
    private Settings m_settings;

    /**
     * logger instance.
     */
    private Log m_log;

    /**
     * initialize main information.
     * @param ar ArtifactRepository provides by maven
     * @param wm WagonManager provides by maven
     * @param settings settings of the current project provides by maven
     * @param log logger
     */
    public RemoteFileManager(ArtifactRepository ar, WagonManager wm, Settings settings, Log log) {
        m_artifactRepository = ar;
        m_wagonManager = wm;
        m_settings = settings;
        m_log = log;
        m_wagon = null;
    }

    /**
     * disconnect the current object.
     *
     */
    public void disconnect() {
        if (m_wagon == null) {
            m_log.error("must be connected first!");
            return;
        }
        try {
            m_wagon.disconnect();
        } catch (ConnectionException e) {
            m_log.error("Error disconnecting wagon - ignored", e);
        }
    }

    /**
     * connect the current object to artifact repository given in constructor.
     * @throws MojoExecutionException if connection failed
     */
    public void connect() throws MojoExecutionException {
        String url = m_artifactRepository.getUrl();
        String id = m_artifactRepository.getId();

        Repository repository = new Repository(id, url);

        try {
            m_wagon = m_wagonManager.getWagon(repository);
            //configureWagon(m_wagon, repository.getId());
        } catch (UnsupportedProtocolException e) {
            throw new MojoExecutionException("Unsupported protocol: '" + repository.getProtocol() + "'", e);
        } catch (WagonConfigurationException e) {
            throw new MojoExecutionException("Unable to configure Wagon: '" + repository.getProtocol() + "'", e);
        }

        try {
            Debug debug = new Debug();
            m_wagon.addTransferListener(debug);

            ProxyInfo proxyInfo = getProxyInfo(m_settings);
            if (proxyInfo != null) {
                m_wagon.connect(repository, m_wagonManager.getAuthenticationInfo(id), proxyInfo);
            } else {
                m_wagon.connect(repository, m_wagonManager.getAuthenticationInfo(id));
            }

        } catch (ConnectionException e) {
            throw new MojoExecutionException("Error uploading file", e);
        } catch (AuthenticationException e) {
            throw new MojoExecutionException("Error uploading file", e);
        }
    }

    /**
     * get a file from the current repository connected.
     * @param url url to the targeted file
     * @return  get a file descriptor on the requiered resource
     * @throws IOException if an IO error occurs
     * @throws TransferFailedException  if the transfer failed 
     * @throws ResourceDoesNotExistException if the targeted resource doesn't exist
     * @throws AuthorizationException if the connection authorization failed
     */
    public File get(String url) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

        if (m_wagon == null) {
            m_log.error("must be connected first!");
            return null;
        }

        File file = File.createTempFile(String.valueOf(System.currentTimeMillis()), "tmp");
        m_wagon.get(url, file);
        return file;
    }

    /**
     * put a file on the current repository connected.
     * @param file file to upload
     * @param url url to copy file
     * @throws TransferFailedException if the transfer failed 
     * @throws ResourceDoesNotExistException if the targeted resource doesn't exist
     * @throws AuthorizationException if the connection authorization failed
     */
    public void put(File file, String url) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (m_wagon == null) {
            m_log.error("must be connected first!");
            return;
        }
        m_wagon.put(file, url);
    }

    /**
     * Convenience method to map a Proxy object from the user system settings to a ProxyInfo object.
     * @param settings project settings given by maven
     * @return a proxyInfo object instancied or null if no active proxy is define in the settings.xml
     */
    public static ProxyInfo getProxyInfo(Settings settings) {
        ProxyInfo proxyInfo = null;
        if (settings != null && settings.getActiveProxy() != null) {
            Proxy settingsProxy = settings.getActiveProxy();

            proxyInfo = new ProxyInfo();
            proxyInfo.setHost(settingsProxy.getHost());
            proxyInfo.setType(settingsProxy.getProtocol());
            proxyInfo.setPort(settingsProxy.getPort());
            proxyInfo.setNonProxyHosts(settingsProxy.getNonProxyHosts());
            proxyInfo.setUserName(settingsProxy.getUsername());
            proxyInfo.setPassword(settingsProxy.getPassword());
        }

        return proxyInfo;
    }

    /**
     * this method indicates if the targeted file is locked or not.
     * @param remote connection manager
     * @param fileName name targeted
     * @return  true if thr reuiered file is locked, else false
     * @throws MojoFailureException if the plugin failed
     */
    public boolean isLockedFile(RemoteFileManager remote, String fileName) throws MojoFailureException {
        File file = null;
        try {
            file = remote.get(fileName + ".lock");
        } catch (TransferFailedException e) {
            e.printStackTrace();
            throw new MojoFailureException("TransferFailedException");

        } catch (ResourceDoesNotExistException e) {
            return false;
        } catch (AuthorizationException e) {
            e.printStackTrace();
            throw new MojoFailureException("AuthorizationException");
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoFailureException("IOException");
        }
        if (file != null && file.length() == 0) { return false; }
        return true;
    }

}
