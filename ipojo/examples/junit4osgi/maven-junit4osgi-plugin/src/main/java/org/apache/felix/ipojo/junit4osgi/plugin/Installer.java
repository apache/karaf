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
package org.apache.felix.ipojo.junit4osgi.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Bundle Activator installing bundles in the embedded OSGi.
 * Installed bundles are the junit4osgi framework, the required bundle and the artifact bundle (if enable).
 * Bundles are installed from the local maven repository.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Installer implements BundleActivator {
    
    /**
     * The list of artifact containing bundles for the junit4osgi framework.
     */
    private List m_artifacts;
    
    /**
     * The current maven project. 
     */
    private MavenProject m_project;
    
    /**
     * Flag enabling/disabling the deployment of the current
     * project artifact.
     */
    private boolean m_deployCurrent;
    
    /**
     * List of bundle URLs to install. 
     */
    private List m_bundles;
    
    
    /**
     * Creates a Installer.
     * @param artifacts the list of artifact containing bundles for the junit4osgi framework.
     * @param bundles the list of bundle URLs to install 
     * @param project the current maven project
     * @param deployCurrentArtifact flag enabling/disabling the deployment of the current project artifact
     */
    public Installer(List artifacts, List bundles, MavenProject project, boolean deployCurrentArtifact) {
        this.m_artifacts = artifacts;
        this.m_project = project;
        m_deployCurrent = deployCurrentArtifact;
        this.m_bundles = bundles;
    }
    
    /**
     * Installs and starts the iPOJO bundle.
     * @param context  the bundle context.
     * @throws BundleException when the bundle cannot be installed or started correctly
     */
    private void installIPOJO(BundleContext context) throws BundleException {
        String path = getUrlByArtifactId("org.apache.felix.ipojo").toString();
        Bundle bundle = context.installBundle(path);
        bundle.start();
    }
    
    /**
     * Installs and starts the Junit4OSGi bundle.
     * @param context  the bundle context.
     * @throws BundleException when the bundle cannot be installed or started correctly
     */
    private void installJunit(BundleContext context) throws BundleException {
        String path = getUrlByArtifactId("org.apache.felix.ipojo.junit4osgi").toString();
        Bundle bundle = context.installBundle(path);
        bundle.start();
    }
    
    /**
     * Installs and starts the iPOJO Extender Handler bundle.
     * @param context  the bundle context.
     * @throws BundleException when the bundle cannot be installed or started correctly
     */
    private void installExtender(BundleContext context) throws BundleException {
        String path = getUrlByArtifactId("org.apache.felix.ipojo.handler.extender").toString();
        Bundle bundle = context.installBundle(path);
        bundle.start();
    }
    
    /**
     * Installs and Starts required bundles.
     * @param context the bundle context used to deploy bundles.
     * @throws BundleException when a bundle cannot be installed or started correctly
     */
    private void deployBundles(BundleContext context) throws BundleException {
        for (int i = 0; i < m_bundles.size(); i++) {
            URL url = (URL) m_bundles.get(i);
            Bundle bundle = context.installBundle(url.toString());
            bundle.start();
        }
    }
    
    /**
     * Gets the bundle URL from the artifact list.
     * @param id the dependency id.
     * @return the bundle URL or <code>null</code> if the URL cannot
     * be found.
     */
    private URL getUrlByArtifactId(String id) {
        for (int i = 0; i < m_artifacts.size(); i++) {
            Artifact artifact = (Artifact) m_artifacts.get(i);
            if (artifact.getArtifactId().equals(id)) {
                try {
                    return artifact.getFile().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Deploys the current bundle if enable.
     * @param context the bundle context
     * @throws BundleException when the bundle cannot be installed or started correctly.
     */
    private void deployProjectArtifact(BundleContext context)
        throws BundleException {
        if (!m_deployCurrent) {
            return;
        }

        File file = m_project.getArtifact().getFile();
        try {
            if (file.exists()) {
                if (file.getName().endsWith("jar")) {
                    JarFile jar = new JarFile(file);
                    if (jar.getManifest().getMainAttributes().getValue("Bundle-ManifestVersion") != null) {
                        Bundle bundle = context.installBundle(file.toURL().toString());
                        bundle.start();
                    } else {
                        System.err.println("The current artifact " + file.getName() + " is not a valid bundle");
                    }
                } else {
                    System.err.println("The current artifact " + file.getName() + " is not a Jar file.");
                }
            } else {
                System.err.println("The current artifact " + file.getName() + " does not exist.");
            }
        } catch (Exception e) {
            throw new BundleException("The current project artifact cannot be installed (" + e.getMessage() + ")");
        }
        
        
    }


    /**
     * Start bundles.
     * @param context the bundle context.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) {
        try {
            installIPOJO(context);
            installExtender(context);
            installJunit(context);
            deployBundles(context);
            deployProjectArtifact(context);
        } catch (BundleException e) {
            System.err.println("Cannot start the framework : " + e.getMessage());
            return;
        }
    }

    /**
     * Stopping methods.
     * @param context the bundle context.
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) {
        // Do nothing.
    }

}
