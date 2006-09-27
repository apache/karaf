/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


public class CoreController implements CoreControllerMBean {
	private AgentContext ac;

	public CoreController(AgentContext ac) {
		super();
		this.ac = ac;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#startBundle(java.lang.String)
	 */
	public void startBundle(String bundleSymbolicName) throws BundleException,
			BundleNotAvailableException {
		String[] s = bundleSymbolicName.split(";");
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getBundleContext().getBundle(id).start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchStartBundles(java.lang.String[])
	 */
	public void batchStartBundles(String[] bundleSymbolicNames)
			throws BundleException, BundleNotAvailableException {
		if (bundleSymbolicNames == null)
			throw new IllegalArgumentException(
					"Array of bundles cannot be null");
		for (int i = 0; i < bundleSymbolicNames.length; i++) {
			this.startBundle(bundleSymbolicNames[i]);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#stopBundle(java.lang.String)
	 */
	public void stopBundle(String bundleSymbolicName) throws BundleException,
			BundleNotAvailableException {
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getBundleContext().getBundle(id).stop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchStopBundles(java.lang.String[])
	 */
	public void batchStopBundles(String[] bundleSymbolicNames)
			throws BundleException, BundleNotAvailableException {
		if (bundleSymbolicNames == null)
			throw new IllegalArgumentException(
					"Array of bundles cannot be null");
		for (int i = 0; i < bundleSymbolicNames.length; i++) {
			this.stopBundle(bundleSymbolicNames[i]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#updateBundle(java.lang.String)
	 */
	public void updateBundle(String bundleSymbolicName) throws BundleException,
			BundleNotAvailableException {
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getBundleContext().getBundle(id).update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchUpdateBundles(java.lang.String[])
	 */
	public void batchUpdateBundles(String[] bundleSymbolicNames)
			throws BundleException, BundleNotAvailableException {
		if (bundleSymbolicNames == null)
			throw new IllegalArgumentException(
					"Array of bundles cannot be null");
		for (int i = 0; i < bundleSymbolicNames.length; i++) {
			this.updateBundle(bundleSymbolicNames[i]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#updateBundleFromUrl(java.lang.String,
	 *      java.lang.String)
	 */
	public void updateBundleFromUrl(String bundleSymbolicName, String url)
			throws BundleException, BundleNotAvailableException,
			MalformedURLException, IOException {
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getBundleContext().getBundle(id).update((new URL(url)).openStream());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchUpdateBundleFromUrl(java.lang.String[],
	 *      java.lang.String[])
	 */
	public void batchUpdateBundleFromUrl(String[] bundleSymbolicNames,
			String[] urls) throws BundleException, BundleNotAvailableException,
			MalformedURLException, IOException {
		if (bundleSymbolicNames == null || urls == null)
			throw new IllegalArgumentException("arguments cannot be null");
		if (urls.length != bundleSymbolicNames.length)
			throw new IllegalArgumentException(
					"Each bundle needs a corresponding url");
		for (int i = 0; i < bundleSymbolicNames.length; i++) {
			this.updateBundleFromUrl(bundleSymbolicNames[i], urls[i]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#installBundle(java.lang.String)
	 */
	public void installBundle(String bundleLocation) throws BundleException {
		ac.getBundleContext().installBundle(bundleLocation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchInstallBundle(java.lang.String[])
	 */
	public void batchInstallBundle(String[] bundleLocations)
			throws BundleException {
		for (int i = 0; i < bundleLocations.length; i++) {
			this.installBundle(bundleLocations[i]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#setBundleStartLevel(java.lang.String,
	 *      int)
	 */
	public void setBundleStartLevel(String bundleSymbolicName, int newlevel)
			throws BundleNotAvailableException, ServiceNotAvailableException {
		StartLevel sl = ac.getStartLevel();
		if (sl == null) {
			ac.debug("tried to modify startlevel, but no service found");
			throw new ServiceNotAvailableException(
					"Start Level service not available");
		}
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getStartLevel().setBundleStartLevel(
				ac.getBundleContext().getBundle(id), newlevel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#batchSetBundleStartLevel(java.lang.String[],
	 *      int[])
	 */
	public void batchSetBundleStartLevel(String[] bundleSymbolicNames,
			int[] newlevels) {
		if (bundleSymbolicNames == null || newlevels == null)
			throw new IllegalArgumentException("arguments cannot be null");
		if (newlevels.length != bundleSymbolicNames.length)
			throw new IllegalArgumentException(
					"Each bundle needs a corresponding new level");
		for (int i = 0; i < newlevels.length; i++) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#refreshPackages(java.lang.String[])
	 */
	public void refreshPackages(String[] bundleSymbolicNames)
			throws BundleNotAvailableException, ServiceNotAvailableException {
		if (bundleSymbolicNames == null)
			throw new IllegalArgumentException("argument cannot be null");
		Bundle[] bundles = new Bundle[bundleSymbolicNames.length];
		for (int i = 0; i < bundles.length; i++) {
			long id = InstrumentationSupport.getBundleId(
					bundleSymbolicNames[i], ac);
			bundles[i] = ac.getBundleContext().getBundle(id);
			if (bundles[i] == null)
				throw new BundleNotAvailableException(
						"could not get bundle whose id" + id);
		}
		ac.getPackageadmin().refreshPackages(bundles);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#resolveBundles(java.lang.String[])
	 */
	public void resolveBundles(String[] bundleSymbolicNames)
			throws BundleNotAvailableException, ServiceNotAvailableException {
		if (bundleSymbolicNames == null)
			throw new IllegalArgumentException("argument cannot be null");
		Bundle[] bundles = new Bundle[bundleSymbolicNames.length];
		for (int i = 0; i < bundles.length; i++) {
			long id = InstrumentationSupport.getBundleId(
					bundleSymbolicNames[i], ac);
			bundles[i] = ac.getBundleContext().getBundle(id);
			if (bundles[i] == null)
				throw new BundleNotAvailableException(
						"could not get bundle whose id" + id);
		}
		ac.getPackageadmin().resolveBundles(bundles);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#setPlatformStartLevel(int)
	 */
	public void setPlatformStartLevel(int newlevel)
			throws ServiceNotAvailableException {
		ac.getStartLevel().setStartLevel(newlevel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#getPlatformStartLevel()
	 */
	public int getPlatformStartLevel() throws ServiceNotAvailableException {
		return ac.getStartLevel().getStartLevel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#setInitialBundleStartLevel(int)
	 */
	public void setInitialBundleStartLevel(int newlevel)
			throws ServiceNotAvailableException {
		ac.getStartLevel().setInitialBundleStartLevel(newlevel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#getInitialBundleStartLevel()
	 */
	public int getInitialBundleStartLevel() throws ServiceNotAvailableException {
		return ac.getStartLevel().getInitialBundleStartLevel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#restartFramework()
	 */
	public void restartFramework() throws NotImplementedException {
		try {
			ac.getBundleContext().getBundle(0).update();
		} catch (BundleException be) {
			throw new NotImplementedException(
					"Restarting not implemented in this framework", be);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#shutdownFramework()
	 */
	public void shutdownFramework() throws NotImplementedException {
		try {
			ac.getBundleContext().getBundle(0).stop();
		} catch (BundleException be) {
			throw new NotImplementedException(
					"Shutting down not implemented in this framework", be);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.CoreControllerMBean#updateFramework()
	 */
	public void updateFramework() throws NotImplementedException {
		// TODO
		throw new NotImplementedException(
				"Feature not implemented for this framework");
	}

	public void uninstallBundle(String bundleSymbolicName)
			throws BundleNotAvailableException, BundleException {
		if (bundleSymbolicName == null)
			throw new IllegalArgumentException("argument cannot be null");
		String[] s = bundleSymbolicName.split(";");
		long id = InstrumentationSupport.getBundleId(bundleSymbolicName, ac);
		ac.getBundleContext().getBundle(id).uninstall();

	}
}
