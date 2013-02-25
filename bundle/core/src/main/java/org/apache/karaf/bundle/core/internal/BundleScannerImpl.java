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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.karaf.bundle.core.BundleScanner;
import org.ops4j.pax.url.mvn.Parser;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See {@link BundleScanner}.
 */
public class BundleScannerImpl implements Runnable, BundleScanner {

	/**
	 * Matching bundles only with given qualifier.
	 */
	private static final String QUALIFIER = "SNAPSHOT";

	private static final String THREAD_NAME = BundleScannerImpl.class.getName();

	private final Logger logger = LoggerFactory
			.getLogger(BundleScannerImpl.class);

	private volatile BundleContext bundleContext;
	private final PackageAdmin packageAdmin;
	private final MavenConfigService mavenConfigService;

	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile long interval = 1 * 1000L;

	/**
	 * Registered matching patterns.
	 * <p>
	 * [original-regex : compiled-pattern]
	 */
	private final Map<String, Pattern> matchingPatterns = new ConcurrentHashMap<String, Pattern>();

	/**
	 * Collected bundle update statistics.
	 * <p>
	 * [bundle-symbolic-name : number-of-updates]
	 */
	private final Map<String, AtomicInteger> updateStats = new ConcurrentHashMap<String, AtomicInteger>();

	/**
	 * Constructor.
	 */
	@SuppressWarnings("deprecation")
	public BundleScannerImpl( //
			final BundleContext bundleContext, //
			final PackageAdmin packageAdmin, //
			final MavenConfigService mavenConfigService //
	) {
		this.bundleContext = bundleContext;
		this.packageAdmin = packageAdmin;
		this.mavenConfigService = mavenConfigService;
	}

	@Override
	public Map<String, Integer> getStatistics() {
		Map<String, Integer> statsMap = new TreeMap<String, Integer>();
		for (Map.Entry<String, AtomicInteger> entry : updateStats.entrySet()) {
			statsMap.put(entry.getKey(), entry.getValue().get());
		}
		return statsMap;
	}

	@Override
	public void clearStatistics() {
		updateStats.clear();
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
			} catch (final InterruptedException ex) {
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

		final Map<String, Bundle> matchingBundles = new HashMap<String, Bundle>();

		for (final Bundle bundle : bundleContext.getBundles()) {

			final String name = bundle.getSymbolicName();
			final String qualifier = bundle.getVersion().getQualifier();

			if (!QUALIFIER.equalsIgnoreCase(qualifier)) {
				continue;
			}

			for (final Pattern pattern : matchingPatterns.values()) {
				if (pattern.matcher(name).matches()) {
					matchingBundles.put(name, bundle);
				}
			}
		}

		if (matchingBundles.isEmpty()) {
			return;
		}

		final File localRepository = mavenConfigService.getLocalRepository();

		final List<Bundle> updatedBundles = getUpdatedBundles(localRepository,
				matchingBundles.values());

		packageAdmin.refreshPackages(updatedBundles
				.toArray(new Bundle[updatedBundles.size()]));

		for (final Bundle bundle : updatedBundles) {
			final String name = bundle.getSymbolicName();
			final AtomicInteger count = updateStats.get(name);
			if (count == null) {
				updateStats.put(name, new AtomicInteger(0));
			}
			count.getAndIncrement();
		}

	}

	/**
	 * Maven URL detector. See {@link ServiceConstants#PROTOCOL}.
	 */
	private boolean isMavenURL(final String url) {
		if (url == null) {
			return false;
		} else {
			return url.startsWith(ServiceConstants.PROTOCOL);
		}
	}

	/**
	 * Force maven artifact resolution, update bundle when artifact is updated,
	 * and produce list of affected bundles.
	 */
	private List<Bundle> getUpdatedBundles(final File localRepository,
			final Collection<Bundle> bundleList) {

		final List<Bundle> updatedList = new ArrayList<Bundle>();

		for (final Bundle bundle : bundleList) {

			try {

				final String bundleLocation = bundle.getLocation();

				if (!isMavenURL(bundleLocation)) {
					continue;
				}

				final File externalLocation = getBundleExternalLocation(
						localRepository, bundle);

				if (externalLocation == null || !externalLocation.exists()) {
					continue;
				}

				/**
				 * Force maven resolution, update artifact in local maven
				 * repository. Will be constrained by global or per-repository
				 * maven repository update policy.
				 */
				new URL(bundleLocation).openStream().close();

				final long bundleStamp = bundle.getLastModified();

				final long artifactStamp = externalLocation.lastModified();

				if (bundleStamp >= artifactStamp) {
					continue;
				}

				InputStream stream = null;
				try {
					stream = new FileInputStream(externalLocation);
					logger.info("Updating bundle: " + bundle);
					bundle.update(stream);
					updatedList.add(bundle);
				} finally {
					if (stream != null) {
						stream.close();
					}
				}

			} catch (final Exception e) {
				logger.error("Bundle update failure: " + bundle, e);
			}

		}

		return updatedList;

	}

	@Override
	public boolean add(final String regex) {
		try {
			if (matchingPatterns.containsKey(regex)) {
				return false;
			} else {
				final Pattern pattern = Pattern.compile(regex);
				matchingPatterns.put(regex, pattern);
				return true;
			}
		} catch (final Exception e) {
			return false;
		}
	}

	@Override
	public boolean remove(final String regex) {
		final Pattern pattern = matchingPatterns.remove(regex);
		return pattern != null;
	}

	@Override
	public List<String> getPatterns() {
		List<String> list = new ArrayList<String>(matchingPatterns.keySet());
		Collections.sort(list);
		return list;
	}

	@Override
	public void clearPatterns() {
		matchingPatterns.clear();
	}

	/**
	 * Extract bundleURI from "mvn:bundleURI"
	 */
	private String getBundleURI(final Bundle bundle) {

		final String location = bundle.getLocation();

		if (!isMavenURL(location)) {
			throw new IllegalStateException("Non maven bundle location : "
					+ bundle);
		}

		final String bundleURI = location.substring(ServiceConstants.PROTOCOL
				.length() + 1);

		return bundleURI;

	}

	/**
	 * Returns the location of the Bundle inside the local maven repository.
	 * 
	 * @param bundle
	 * @return
	 */
	private File getBundleExternalLocation(final File localRepository,
			final Bundle bundle) throws Exception {

		final Parser parser = new Parser(getBundleURI(bundle));

		final String filePath = localRepository.getPath() + File.separator
				+ parser.getArtifactPath();

		return new File(filePath);

	}

	@Override
	public void start() {
		if (running.compareAndSet(false, true)) {
			final Thread thread = new Thread(this, THREAD_NAME);
			thread.start();
		}
	}

	@Override
	public void stop() {
		if (running.compareAndSet(true, false)) {
		}
	}

	public long getInterval() {
		return interval;
	}

	@Override
	public void setInterval(final long interval) {
		this.interval = interval;
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

}
