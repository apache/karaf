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
package org.apache.felix.deploymentadmin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarInputStream;

import org.apache.felix.deploymentadmin.spi.CommitResourceCommand;
import org.apache.felix.deploymentadmin.spi.DeploymentSessionImpl;
import org.apache.felix.deploymentadmin.spi.DropBundleCommand;
import org.apache.felix.deploymentadmin.spi.DropResourceCommand;
import org.apache.felix.deploymentadmin.spi.GetStorageAreaCommand;
import org.apache.felix.deploymentadmin.spi.ProcessResourceCommand;
import org.apache.felix.deploymentadmin.spi.SnapshotCommand;
import org.apache.felix.deploymentadmin.spi.StartBundleCommand;
import org.apache.felix.deploymentadmin.spi.StartCustomizerCommand;
import org.apache.felix.deploymentadmin.spi.StopBundleCommand;
import org.apache.felix.deploymentadmin.spi.UpdateCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

public class DeploymentAdminImpl implements DeploymentAdmin {

    public static final String PACKAGE_DIR = "packages";
    public static final String TEMP_DIR = "temp";
    public static final String PACKAGECONTENTS_DIR = "contents";
    public static final String PACKAGEINDEX_FILE = "index.txt";
    public static final String TEMP_PREFIX = "pkg";
    public static final String TEMP_POSTFIX = "";

    private static final long TIMEOUT = 10000;

    private BundleContext m_context; /* will be injected by dependencymanager */
    private PackageAdmin m_packageAdmin;    /* will be injected by dependencymanager */
    private EventAdmin m_eventAdmin; /* will be injected by dependencymanager */
    private LogService m_log;        /* will be injected by dependencymanager */
    private DeploymentSessionImpl m_session = null;
    private final Map m_packages = new HashMap();
    private final List m_commandChain = new ArrayList();
    private final Semaphore m_semaphore = new Semaphore();

    /**
     * Create new instance of this <code>DeploymentAdmin</code>.
     */
    public DeploymentAdminImpl() {
        GetStorageAreaCommand getStorageAreaCommand = new GetStorageAreaCommand();
        m_commandChain.add(getStorageAreaCommand);
        m_commandChain.add(new StopBundleCommand());
        m_commandChain.add(new SnapshotCommand(getStorageAreaCommand));
        m_commandChain.add(new UpdateCommand());
        m_commandChain.add(new StartCustomizerCommand());
        CommitResourceCommand commitCommand = new CommitResourceCommand();
        m_commandChain.add(new ProcessResourceCommand(commitCommand));
        m_commandChain.add(new DropResourceCommand(commitCommand));
        m_commandChain.add(new DropBundleCommand());
        m_commandChain.add(commitCommand);
        m_commandChain.add(new StartBundleCommand());
    }

    // called automatically once dependencies are satisfied
    public void start() throws DeploymentException {
        File packageDir = m_context.getDataFile(PACKAGE_DIR);
        if (packageDir == null) {
            throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not create directories needed for deployment package persistence");
        } else {
            packageDir.mkdirs();
            File[] packages = packageDir.listFiles();
            for(int i = 0; i < packages.length; i++) {
                if (packages[i].isDirectory()) {
                    try {
                        File index = new File(packages[i], PACKAGEINDEX_FILE);
                        File contents = new File(packages[i], PACKAGECONTENTS_DIR);
                        FileDeploymentPackage dp = new FileDeploymentPackage(index, contents, m_context);
                        m_packages.put(dp.getName(), dp);
                    }
                    catch (IOException e) {
                        m_log.log(LogService.LOG_WARNING, "Could not read deployment package from disk, skipping: '" + packages[i].getAbsolutePath() + "'");
                        continue;
                    }
                }
            }
        }
    }


    public void stop() {
    	cancel();
    }

    public boolean cancel() {
        if (m_session != null) {
            m_session.cancel();
            return true;
        }
        return false;
    }

    public DeploymentPackage getDeploymentPackage(String symbName) {
        if (symbName == null) {
            throw new IllegalArgumentException("Symbolic name may not be null");
        }
        return (DeploymentPackage) m_packages.get(symbName);
    }

    public DeploymentPackage getDeploymentPackage(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle can not be null");
        }
        for (Iterator i = m_packages.values().iterator(); i.hasNext();) {
            DeploymentPackage dp = (DeploymentPackage) i.next();
            if (dp.getBundle(bundle.getSymbolicName()) != null) {
                return dp;
            }
        }
        return null;
    }

    public DeploymentPackage installDeploymentPackage(InputStream input) throws DeploymentException {
        if (input == null) {
            throw new IllegalArgumentException("Inputstream may not be null");
        }
        try {
            if (!m_semaphore.tryAcquire(TIMEOUT)) {
                throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Timeout exceeded while waiting to install deployment package (" + TIMEOUT + "msec)");
            }
        }
        catch (InterruptedException ie) {
            throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Thread interrupted");
        }

        JarInputStream jarInput = null;
        File tempPackage = null;
        File tempIndex = null;
        File tempContents = null;

        try {
            try {
                File tempDir = m_context.getDataFile(TEMP_DIR);
                tempDir.mkdirs();
                tempPackage = File.createTempFile(TEMP_PREFIX, TEMP_POSTFIX, tempDir);
                tempPackage.delete();
                tempPackage.mkdirs();
                tempIndex = new File(tempPackage, PACKAGEINDEX_FILE);
                tempContents = new File(tempPackage, PACKAGECONTENTS_DIR);
                tempContents.mkdirs();
                input = new ExplodingOutputtingInputStream(input, tempIndex, tempContents);
            }
            catch (IOException e) {
                m_log.log(LogService.LOG_ERROR, "Error writing package to disk", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Error writing package to disk", e);
            }
            try {
                jarInput = new JarInputStream(input);
            }
            catch (IOException e) {
                m_log.log(LogService.LOG_ERROR, "Stream does not contain a valid Jar", e);
                throw new DeploymentException(DeploymentException.CODE_NOT_A_JAR, "Stream does not contain a valid Jar", e);
            }
        }
        finally {
            m_semaphore.release();
        }

        StreamDeploymentPackage source = new StreamDeploymentPackage(jarInput, m_context);
        sendStartedEvent(source.getName());

        boolean succeeded = false;
        try {
            AbstractDeploymentPackage target = (AbstractDeploymentPackage) getDeploymentPackage(source.getName());
            boolean newPackage = (target == null);
            if (newPackage) {
                target = AbstractDeploymentPackage.emptyPackage;
            }
            if (source.isFixPackage() && ((newPackage) || (!source.getVersionRange().isInRange(target.getVersion())))) {
                succeeded = false;
                m_log.log(LogService.LOG_ERROR, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
            }
            try {
                m_session = new DeploymentSessionImpl(source, target, m_commandChain, this);
                m_session.call();
            }
            catch (DeploymentException de) {
                succeeded = false;
                throw de;
            }
            try {
                jarInput.close();
            }
            catch (IOException e) {
                // nothing we can do
                m_log.log(LogService.LOG_WARNING, "Could not close stream properly", e);
            }

            File targetContents = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName() + File.separator + PACKAGECONTENTS_DIR);
            File targetIndex = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName() + File.separator + PACKAGEINDEX_FILE);
            if (source.isFixPackage()) {
                try {
                    ExplodingOutputtingInputStream.merge(targetIndex, targetContents, tempIndex, tempContents);
                }
                catch (IOException e) {
                    succeeded = false;
                    m_log.log(LogService.LOG_ERROR, "Could not merge source fix package with target deployment package", e);
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not merge source fix package with target deployment package", e);
                }
            } else {
                File targetPackage = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName());
                targetPackage.mkdirs();
                ExplodingOutputtingInputStream.replace(targetPackage, tempPackage);
            }
            FileDeploymentPackage fileDeploymentPackage = null;
            try {
                fileDeploymentPackage = new FileDeploymentPackage(targetIndex, targetContents, m_context);
                m_packages.put(source.getName(), fileDeploymentPackage);
            }
            catch (IOException e) {
                succeeded = false;
                m_log.log(LogService.LOG_ERROR, "Could not create installed deployment package from disk", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not create installed deployment package from disk", e);
            }
            succeeded = true;
            return fileDeploymentPackage;
        }
        finally {
        	delete(tempPackage);
            sendCompleteEvent(source.getName(), succeeded);
            m_semaphore.release();
        }
    }

    private void delete(File target) {
    	if (target.isDirectory()) {
            File[] childs = target.listFiles();
            for (int i = 0; i < childs.length; i++) {
                delete(childs[i]);
            }
        }
    	target.delete();
	}

	public DeploymentPackage[] listDeploymentPackages() {
        Collection packages = m_packages.values();
        return (DeploymentPackage[]) packages.toArray(new DeploymentPackage[packages.size()]);
    }

    /**
     * Returns reference to this bundle's <code>BundleContext</code>
     *
     * @return This bundle's <code>BundleContext</code>
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Returns reference to the current logging service defined in the framework.
     *
     * @return Currently active <code>LogService</code>.
     */
    public LogService getLog() {
        return m_log;
    }

    /**
     * Returns reference to the current package admin defined in the framework.
     *
     * @return Currently active <code>PackageAdmin</code>.
     */
    public PackageAdmin getPackageAdmin() {
        return m_packageAdmin;
    }

    private void sendStartedEvent(String name) {
        Dictionary props = new Properties();
        props.put(Constants.EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME, name);
        Event completeEvent = new Event(Constants.EVENTTOPIC_INSTALL, props);
        m_eventAdmin.postEvent(completeEvent);
    }

    private void sendCompleteEvent(String name, boolean success) {
        Dictionary props = new Hashtable();
        props.put(Constants.EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME, name);
        props.put(Constants.EVENTPROPERTY_SUCCESSFUL, new Boolean(success));
        Event completeEvent = new Event(Constants.EVENTTOPIC_COMPLETE, props);
        m_eventAdmin.postEvent(completeEvent);
    }

}