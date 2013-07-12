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
package org.apache.karaf.features.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.Resolver;
import org.apache.karaf.region.persist.RegionsPersistence;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleManager {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(BundleManager.class);
	private final BundleContext bundleContext;
	private final RegionsPersistence regionsPersistence;
	private final long refreshTimeout;

	public BundleManager(BundleContext bundleContext) {
		this(bundleContext, null);
	}

	public BundleManager(BundleContext bundleContext,
			RegionsPersistence regionsPersistence) {
		this(bundleContext, regionsPersistence, 5000);
	}

	public BundleManager(BundleContext bundleContext,
			RegionsPersistence regionsPersistence, long refreshTimeout) {
		this.bundleContext = bundleContext;
		this.regionsPersistence = regionsPersistence;
		this.refreshTimeout = refreshTimeout;
	}

	public BundleInstallerResult installBundleIfNeeded(String bundleLocation,
			int startLevel, String regionName) throws IOException,
			BundleException {
		BundleInstallerResult result = doInstallBundleIfNeeded(bundleLocation,
				startLevel);
		installToRegion(regionName, result.bundle, result.isNew);
		return result;
	}

	private BundleInstallerResult doInstallBundleIfNeeded(
			String bundleLocation, int startLevel) throws IOException,
			BundleException {
		InputStream is = getInputStreamForBundle(bundleLocation);
		try {
			is.mark(256 * 1024);
			JarInputStream jar = new JarInputStream(is);
			Manifest m = jar.getManifest();
			if (m == null) {
				throw new BundleException(
						"Manifest not present in the first entry of the zip "
								+ bundleLocation);
			}
			String sn = m.getMainAttributes().getValue(
					Constants.BUNDLE_SYMBOLICNAME);
			if (sn == null) {
				throw new BundleException(
						"Jar is not a bundle, no Bundle-SymbolicName "
								+ bundleLocation);
			}
			// remove attributes from the symbolic name (like
			// ;blueprint.graceperiod:=false suffix)
			int attributeIndexSep = sn.indexOf(';');
			if (attributeIndexSep != -1) {
				sn = sn.substring(0, attributeIndexSep);
			}
			String vStr = m.getMainAttributes().getValue(
					Constants.BUNDLE_VERSION);
			Version v = vStr == null ? Version.emptyVersion : Version
					.parseVersion(vStr);
			Bundle existingBundle = findInstalled(sn, v);
			if (existingBundle != null) {
				LOGGER.debug("Found installed bundle: " + existingBundle);
				return new BundleInstallerResult(existingBundle, false);
			}
			try {
				is.reset();
			} catch (IOException e) {
				is.close();
				is = new URL(bundleLocation).openStream();
				// is = new BufferedInputStream(new
				// URL(bundleLocation).openStream());
			}
			is = new BufferedInputStream(new FilterInputStream(is) {
				@Override
				public int read(byte b[], int off, int len) throws IOException {
					if (Thread.currentThread().isInterrupted()) {
						throw new InterruptedIOException();
					}
					return super.read(b, off, len);
				}
			});

			LOGGER.debug("Installing bundle " + bundleLocation);
			Bundle b = bundleContext.installBundle(bundleLocation, is);

			if (startLevel > 0) {
				b.adapt(BundleStartLevel.class).setStartLevel(startLevel);
			}

			return new BundleInstallerResult(b, true);
		} finally {
			is.close();
		}
	}

	private Bundle findInstalled(String symbolicName, Version version) {
		String vStr;
		for (Bundle b : bundleContext.getBundles()) {
			if (b.getSymbolicName() != null
					&& b.getSymbolicName().equals(symbolicName)) {
				vStr = (String) b.getHeaders().get(Constants.BUNDLE_VERSION);
				Version bv = vStr == null ? Version.emptyVersion : Version
						.parseVersion(vStr);
				if (version.equals(bv)) {
					return b;
				}
			}
		}
		return null;
	}

	private InputStream getInputStreamForBundle(String bundleLocation)
			throws MalformedURLException, IOException {
		InputStream is;
		LOGGER.debug("Checking " + bundleLocation);
		try {
			int protocolIndex = bundleLocation.indexOf(":");
			if (protocolIndex != -1) {
				String protocol = bundleLocation.substring(0, protocolIndex);
				waitForUrlHandler(protocol);
			}
			URL bundleUrl = new URL(bundleLocation);
			is = new BufferedInputStream(bundleUrl.openStream());
		} catch (RuntimeException e) {
			LOGGER.error(e.getMessage());
			throw e;
		}
		return is;
	}

	private void installToRegion(String region, Bundle b, boolean isNew)
			throws BundleException {
		if (region != null && isNew) {
			if (regionsPersistence != null) {
				regionsPersistence.install(b, region);
			} else {
				throw new RuntimeException(
						"Unable to find RegionsPersistence service, while installing "
								+ region);
			}
		}
	}

	/**
	 * Will wait for the {@link URLStreamHandlerService} service for the
	 * specified protocol to be registered.
	 * 
	 * @param protocol
	 */
	private void waitForUrlHandler(String protocol) {
		try {
			Filter filter = bundleContext.createFilter("(&("
					+ Constants.OBJECTCLASS + "="
					+ URLStreamHandlerService.class.getName()
					+ ")(url.handler.protocol=" + protocol + "))");
			if (filter == null) {
				return;
			}
			ServiceTracker<URLStreamHandlerService, URLStreamHandlerService> urlHandlerTracker = new ServiceTracker<URLStreamHandlerService, URLStreamHandlerService>(
					bundleContext, filter, null);
			try {
				urlHandlerTracker.open();
				urlHandlerTracker.waitForService(30000);
			} catch (InterruptedException e) {
				LOGGER.debug(
						"Interrupted while waiting for URL handler for protocol {}.",
						protocol);
			} finally {
				urlHandlerTracker.close();
			}
		} catch (Exception ex) {
			LOGGER.error("Error creating service tracker.", ex);
		}
	}

	protected Set<Bundle> findBundlesToRefresh(Set<Bundle> existing,
			Set<Bundle> installed) {
		Set<Bundle> bundles = new HashSet<Bundle>();
		bundles.addAll(findBundlesWithOptionalPackagesToRefresh(existing,
				installed));
		bundles.addAll(findBundlesWithFragmentsToRefresh(existing, installed));
		return bundles;
	}

	protected Set<Bundle> findBundlesWithFragmentsToRefresh(
			Set<Bundle> existing, Set<Bundle> installed) {
		Set<Bundle> bundles = new HashSet<Bundle>();
		Set<Bundle> oldBundles = new HashSet<Bundle>(existing);
		oldBundles.removeAll(installed);
		if (!oldBundles.isEmpty()) {
			for (Bundle b : installed) {
				String hostHeader = (String) b.getHeaders().get(
						Constants.FRAGMENT_HOST);
				if (hostHeader != null) {
					Clause[] clauses = Parser.parseHeader(hostHeader);
					if (clauses != null && clauses.length > 0) {
						Clause path = clauses[0];
						for (Bundle hostBundle : oldBundles) {
							if (hostBundle.getSymbolicName().equals(
									path.getName())) {
								String ver = path
										.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
								if (ver != null) {
									VersionRange v = VersionRange
											.parseVersionRange(ver);
									if (v.contains(hostBundle.getVersion())) {
										bundles.add(hostBundle);
									}
								} else {
									bundles.add(hostBundle);
								}
							}
						}
					}
				}
			}
		}
		return bundles;
	}

	protected Set<Bundle> findBundlesWithOptionalPackagesToRefresh(
			Set<Bundle> existing, Set<Bundle> installed) {
		// First pass: include all bundles contained in these features
		Set<Bundle> bundles = new HashSet<Bundle>(existing);
		bundles.removeAll(installed);
		if (bundles.isEmpty()) {
			return bundles;
		}
		// Second pass: for each bundle, check if there is any unresolved
		// optional package that could be resolved
		Map<Bundle, List<Clause>> imports = new HashMap<Bundle, List<Clause>>();
		for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
			Bundle b = it.next();
			String importsStr = (String) b.getHeaders().get(
					Constants.IMPORT_PACKAGE);
			List<Clause> importsList = getOptionalImports(importsStr);
			if (importsList.isEmpty()) {
				it.remove();
			} else {
				imports.put(b, importsList);
			}
		}
		if (bundles.isEmpty()) {
			return bundles;
		}
		// Third pass: compute a list of packages that are exported by our
		// bundles and see if
		// some exported packages can be wired to the optional imports
		List<Clause> exports = new ArrayList<Clause>();
		for (Bundle b : installed) {
			String exportsStr = (String) b.getHeaders().get(
					Constants.EXPORT_PACKAGE);
			if (exportsStr != null) {
				Clause[] exportsList = Parser.parseHeader(exportsStr);
				exports.addAll(Arrays.asList(exportsList));
			}
		}
		for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
			Bundle b = it.next();
			List<Clause> importsList = imports.get(b);
			for (Iterator<Clause> itpi = importsList.iterator(); itpi.hasNext();) {
				Clause pi = itpi.next();
				boolean matching = false;
				for (Clause pe : exports) {
					if (pi.getName().equals(pe.getName())) {
						String evStr = pe
								.getAttribute(Constants.VERSION_ATTRIBUTE);
						String ivStr = pi
								.getAttribute(Constants.VERSION_ATTRIBUTE);
						Version exported = evStr != null ? Version
								.parseVersion(evStr) : Version.emptyVersion;
						VersionRange imported = ivStr != null ? VersionRange
								.parseVersionRange(ivStr)
								: VersionRange.ANY_VERSION;
						if (imported.contains(exported)) {
							matching = true;
							break;
						}
					}
				}
				if (!matching) {
					itpi.remove();
				}
			}
			if (importsList.isEmpty()) {
				it.remove();
			} else {
				LOGGER.debug(
						"Refeshing bundle {} ({}) to solve the following optional imports",
						b.getSymbolicName(), b.getBundleId());
				for (Clause p : importsList) {
					LOGGER.debug("    {}", p);
				}

			}
		}
		return bundles;
	}

	/*
	 * Get the list of optional imports from an OSGi Import-Package string
	 */
	protected List<Clause> getOptionalImports(String importsStr) {
		Clause[] imports = Parser.parseHeader(importsStr);
		List<Clause> result = new LinkedList<Clause>();
		for (Clause anImport : imports) {
			String resolution = anImport
					.getDirective(Constants.RESOLUTION_DIRECTIVE);
			if (Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
				result.add(anImport);
			}
		}
		return result;
	}

	protected void refreshPackages(Collection<Bundle> bundles) {
		final Object refreshLock = new Object();
		FrameworkWiring wiring = bundleContext.getBundle().adapt(
				FrameworkWiring.class);
		if (wiring != null) {
			synchronized (refreshLock) {
				wiring.refreshBundles(bundles, new FrameworkListener() {
					public void frameworkEvent(FrameworkEvent event) {
						if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
							synchronized (refreshLock) {
								refreshLock.notifyAll();
							}
						}
					}
				});
				try {
					refreshLock.wait(refreshTimeout);
				} catch (InterruptedException e) {
					LOGGER.warn(e.getMessage(), e);
				}
			}
		}
	}

	public void uninstall(Set<Bundle> bundles) {
		for (Bundle b : bundles) {
			try {
				b.uninstall();
			} catch (Exception e2) {
				// Ignore
			}
		}
		refreshPackages(null);
	}

        public void uninstall(List<Bundle> bundles) {
                for (Bundle b : bundles) {
                        try {
                                b.uninstall();
                        } catch (Exception e2) {
                                // Ignore
                        }
                }
                refreshPackages(null);
        }


	public void uninstallById(Set<Long> bundles) throws BundleException,
			InterruptedException {
		for (long bundleId : bundles) {
			Bundle b = bundleContext.getBundle(bundleId);
			if (b != null) {
				b.uninstall();
			}
		}
		refreshPackages(null);
	}

	public File getDataFile(String fileName) {
		return bundleContext.getDataFile(fileName);
	}

	EventAdminListener createAndRegisterEventAdminListener() {
		EventAdminListener listener = null;
		try {
			getClass().getClassLoader().loadClass(
					"org.bundles.service.event.EventAdmin");
			listener = new EventAdminListener(bundleContext);
		} catch (Throwable t) {
			// Ignore, if the EventAdmin package is not available, just don't
			// use it
			LOGGER.debug("EventAdmin package is not available, just don't use it");
		}
		return listener;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ServiceTracker createServiceTrackerForResolverName(String resolver)
			throws InvalidSyntaxException {
		String filter = "(&(" + Constants.OBJECTCLASS + "="
				+ Resolver.class.getName() + ")(name=" + resolver + "))";
		return new ServiceTracker(bundleContext,
				FrameworkUtil.createFilter(filter), null);
	}

	public void refreshBundles(Set<Bundle> existing, Set<Bundle> installed,
			EnumSet<Option> options) {
		boolean print = options.contains(Option.PrintBundlesToRefresh);
		boolean refresh = !options.contains(Option.NoAutoRefreshBundles);
		if (print || refresh) {
			Set<Bundle> bundlesToRefresh = findBundlesToRefresh(existing,
					installed);
			StringBuilder sb = new StringBuilder();
			for (Bundle b : bundlesToRefresh) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(b.getSymbolicName()).append(" (")
						.append(b.getBundleId()).append(")");
			}
			LOGGER.debug("Bundles to refresh: {}", sb.toString());
			if (!bundlesToRefresh.isEmpty()) {
				if (print) {
					if (refresh) {
						System.out.println("Refreshing bundles "
								+ sb.toString());
					} else {
						System.out
								.println("The following bundles may need to be refreshed: "
										+ sb.toString());
					}
				}
				if (refresh) {
					LOGGER.debug("Refreshing bundles: {}", sb.toString());
					refreshPackages(bundlesToRefresh);
				}
			}
		}
	}

	public BundleContext getBundleContext() {
	    return this.bundleContext;
	}
	
	public static class BundleInstallerResult {
		Bundle bundle;
		boolean isNew;

		public BundleInstallerResult(Bundle bundle, boolean isNew) {
			super();
			this.bundle = bundle;
			this.isNew = isNew;
		}

	}

}
