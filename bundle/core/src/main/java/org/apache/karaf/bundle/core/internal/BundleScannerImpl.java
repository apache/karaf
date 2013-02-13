/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.karaf.bundle.core.BundleScanner;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleWatcher;
import org.ops4j.pax.url.mvn.Parser;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See {@link BundleScanner}
 */
public class BundleScannerImpl implements Runnable, BundleScanner {

	/**
	 * Matching bundles only with this qualifier
	 */
	private static final String QUALIFIER = "SNAPSHOT";
	private static final String THREAD_NAME = BundleScannerImpl.class.getName();

	private final Logger logger = LoggerFactory
			.getLogger(BundleScannerImpl.class);

	private volatile BundleContext bundleContext;
	private final PackageAdmin packageAdmin;
	private final MavenConfigService mavenConfigService;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile long interval = 1000L;
	private final Map<String, Pattern> matchingPatterns = new ConcurrentHashMap<String, Pattern>();

	/**
	 * Constructor
	 */
	@SuppressWarnings("deprecation")
	public BundleScannerImpl( //
			BundleContext bundleContext, //
			PackageAdmin packageAdmin, //
			MavenConfigService mavenConfigService //
	) {
		this.bundleContext = bundleContext;
		this.packageAdmin = packageAdmin;
		this.mavenConfigService = mavenConfigService;
	}

	/**
	 * Runs in dedicated thread.
	 */
	@Override
	public void run() {

		logger.debug("Bundle scanner thread started.");

		while (running.get()) {

			runCore();

			try {
				Thread.sleep(interval);
			} catch (InterruptedException ex) {
				running.set(false);
			}

		}

		logger.debug("Bundle scanner thread stopped.");

	}

	@SuppressWarnings("deprecation")
	private void runCore() {

		if (matchingPatterns.isEmpty()) {
			return;
		}

		Map<String, Bundle> matchingBundles = new HashMap<String, Bundle>();

		for (Bundle bundle : bundleContext.getBundles()) {

			String name = bundle.getSymbolicName();
			String qualifier = bundle.getVersion().getQualifier();

			if (!QUALIFIER.equalsIgnoreCase(qualifier)) {
				continue;
			}

			for (Pattern pattern : matchingPatterns.values()) {
				if (pattern.matcher(name).matches()) {
					matchingBundles.put(name, bundle);
				}
			}
		}

		if (matchingBundles.isEmpty()) {
			return;
		}

		File localRepository = mavenConfigService.getLocalRepository();

		List<Bundle> updatedBundles = getUpdatedBundles(localRepository,
				matchingBundles.values());

		packageAdmin.refreshPackages(updatedBundles
				.toArray(new Bundle[updatedBundles.size()]));

	}

	/**
	 * {@link ServiceConstants#PROTOCOL}
	 */
	private boolean isMavenURL(String url) {
		if (url == null) {
			return false;
		}
		return url.startsWith(ServiceConstants.PROTOCOL);
	}

	/**
	 * 
	 */
	private List<Bundle> getUpdatedBundles(File localRepository,
			Collection<Bundle> bundleList) {

		final List<Bundle> updatedList = new ArrayList<Bundle>();

		for (Bundle bundle : bundleList) {

			String bundleLocation = bundle.getLocation();

			if (!isMavenURL(bundleLocation)) {
				continue;
			}

			try {

				final File externalLocation = getBundleExternalLocation(
						localRepository, bundle);

				if (externalLocation == null || !externalLocation.exists()) {
					continue;
				}

				long bundleStamp = bundle.getLastModified();

				long currentStamp = externalLocation.lastModified();

				/**
				 * Force maven resolution, can be constrained by maven update
				 * policy.
				 */
				new URL(bundleLocation).openStream().close();

				long updatedStamp = externalLocation.lastModified();

				if (updatedStamp <= currentStamp) {

				}

				if (externalLocation.lastModified() > bundle.getLastModified()) {

					InputStream is = new FileInputStream(externalLocation);

					try {
						logger.info("[Scanner] Updating matching bundle: "
								+ bundle.getSymbolicName() + " ("
								+ bundle.getVersion() + ")");
						bundle.update(is);
						updatedList.add(bundle);
					} finally {
						is.close();
					}

				}

			} catch (Exception e) {
				logger.info(
						"[Scanner] Bundle update failure. "
								+ bundle.getSymbolicName() + " ("
								+ bundle.getVersion() + ")", e);
			}

		}

		return updatedList;

	}

	@Override
	public boolean add(String regex) {
		try {
			Pattern pattern = Pattern.compile(regex);
			matchingPatterns.put(regex, pattern);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean remove(String regex) {
		Pattern pattern = matchingPatterns.remove(regex);
		return pattern != null;
	}

	/**
	 * Extract bundleURI from "mvn:bundleURI"
	 */
	private String getBundleURI(Bundle bundle) {

		final String location = bundle.getLocation();

		if (!isMavenURL(location)) {
			throw new IllegalStateException("Non maven bundle location : "
					+ bundle);
		}

		String bundleURI = location.substring(ServiceConstants.PROTOCOL
				.length() + 1);

		return bundleURI;

	}

	/**
	 * Returns the location of the Bundle inside the local maven repository.
	 * 
	 * @param bundle
	 * @return
	 */
	private File getBundleExternalLocation(File localRepository, Bundle bundle) {
		try {

			Parser parser = new Parser(getBundleURI(bundle));

			String filePath = localRepository.getPath() + File.separator
					+ parser.getArtifactPath();

			return new File(filePath);

		} catch (Exception e) {
			logger.error(
					"Could not parse artifact path for bundle"
							+ bundle.getSymbolicName(), e);
		}
		return null;
	}

	@Override
	public boolean start() {
		if (running.compareAndSet(false, true)) {
			Thread thread = new Thread(this, THREAD_NAME);
			thread.start();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean stop() {
		if (running.compareAndSet(true, false)) {
			return true;
		} else {
			return false;
		}
	}

	public long getInterval() {
		return interval;
	}

	@Override
	public void setInterval(long interval) {
		this.interval = interval;
	}

	public boolean isRunning() {
		return running.get();
	}

}
