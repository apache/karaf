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
package org.apache.karaf.deployer.features;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A deployment listener able to hot deploy (install/uninstall) a repository
 * descriptor as well as auto-install features.
 * <p>
 * Assumptions and Conventions:
 * <li>feature.xml file must have external file-name based on artifact-id
 * <li>feature.xml file must have internal root-name based on artifact-id
 * <li>feature.xml file must have file-extension managed by this component
 * <li>external file-name and internal root-name must be the same
 * <li>dependency features must be resolvable from available repositories
 * <li>dependency features must have a version
 */
public class FeatureDeploymentListener implements ArtifactUrlTransformer,
		SynchronousBundleListener {

	/** Repository feature.xml file extension managed by this component. */
	static final String EXTENSION = "xml";

	/** Features folder inside the wrapper bundle. */
	static final String FEATURE_PATH = "org.apache.karaf.shell.features";

	/** Features path inside the wrapper bundle jar. */
	static final String META_PATH = "/META-INF/" + FEATURE_PATH + "/";

	/** Deployer state properties file name. */
	static final String PROP_FILE = FeatureDeploymentListener.class.getName()
			+ "@repository.properties";

	/** Feature deployer protocol, used by default feature deployer. */
	static final String PROTOCOL = "feature";

	/** Root node in feature.xml */
	static final String ROOT_NODE = "features";

	private volatile BundleContext bundleContext;

	private volatile DocumentBuilderFactory dbf;

	private volatile FeaturesService featuresService;

	private final Logger logger = LoggerFactory
			.getLogger(FeatureDeploymentListener.class);

	/**
	 * Ensure all feature dependencies are installed into feature service.
	 */
	void assertDependencyInstalled(final Feature feature) throws Exception {
		final List<Dependency> depencencyList = feature.getDependencies();
		for (final Dependency depencency : depencencyList) {
			if (isInstalled(depencency)) {
				continue;
			} else {
				logger.error(
						"Expected feature dependency must be already installed: {} -> {}",
						feature, depencency);
				throw new IllegalStateException("Missing feature dependency.");
			}
		}
	}

	/**
	 * Ensure all feature dependencies are registered with feature service.
	 */
	void assertDependencyRegistered(final Feature feature) throws Exception {
		final List<Dependency> depencencyList = feature.getDependencies();
		for (final Dependency depencency : depencencyList) {
			if (isRegistered(depencency)) {
				continue;
			} else {
				logger.error(
						"Expected feature dependency must be already registered: {} -> {}",
						feature, depencency);
				throw new IllegalStateException("Missing feature dependency.");
			}
		}
	}

	@Override
	public void bundleChanged(final BundleEvent event) {

		final Bundle bundle = event.getBundle();

		if (!hasRepoDescriptor(bundle)) {
			return;
		}

		final BundleEventType type = BundleEventType.from(event);

		synchronized (Runnable.class) {
			try {
				switch (type) {
				default:
					return;
				case INSTALLED:
					logBundleEvent(event);
					repoCreate(bundle);
					break;
				case UNINSTALLED:
					logBundleEvent(event);
					repoDelete(bundle);
					break;
				case UPDATED:
					logBundleEvent(event);
					repoDelete(bundle);
					repoCreate(bundle);
				}
				logger.info("Success: " + type + " " + bundle);
			} catch (final Throwable e) {
				logger.error("Failure: " + type + " " + bundle, e);
			}
		}

	}

	@Override
	public boolean canHandle(final File file) {
		try {
			if (file.isFile() && file.getName().endsWith("." + EXTENSION)) {
				final Document doc = parse(file);
				final String name = doc.getDocumentElement().getLocalName();
				final String uri = doc.getDocumentElement().getNamespaceURI();
				if (ROOT_NODE.equals(name)) {
					if (isKnownFeaturesURI(uri)) {
						return true;
					} else {
						logger.error("Unknown features uri", new Exception(""
								+ uri));
					}
				}
			}
		} catch (final Exception e) {
			logger.error(
					"Unable to parse deployed file " + file.getAbsolutePath(),
					e);
		}
		return false;
	}

	/**
	 * Add all dependencies of a feature.
	 */
	void dependencyAdd(final Repository repo, final Feature feature)
			throws Exception {

		assertDependencyRegistered(feature);

		final List<Dependency> depencencyList = feature.getDependencies();
		for (final Dependency depencency : depencencyList) {
			featureAdd(repo, featureRegistered(depencency));
		}

	}

	/**
	 * Remove all dependencies of a feature.
	 */
	void dependencyRemove(final Repository repo, final Feature feature)
			throws Exception {

		assertDependencyInstalled(feature);

		final List<Dependency> depencencyList = feature.getDependencies();
		for (final Dependency depencency : depencencyList) {
			featureRemove(repo, featureInstalled(depencency));
		}

	}

	/**
	 * Component deactivate.
	 */
	public void destroy() throws Exception {
		bundleContext.removeBundleListener(this);
		logger.info("Deployer deactivate.");
	}

	/**
	 * Identity of feature/dependency based on name and version.
	 */
	boolean equals(final Feature feature, final Dependency depencency) {
		final boolean sameName = feature.getName().equals(depencency.getName());
		final boolean sameVersion = feature.getVersion().equals(
				depencency.getVersion());
		return sameName && sameVersion;
	}

	/**
	 * Activate auto-install features in a repository.
	 */
	void featureAdd(final Repository repo) throws Exception {
		final Feature[] featureArray = repo.getFeatures();
		for (final Feature feature : featureArray) {
			if (isAutoInstall(feature)) {
				featureAdd(repo, feature);
			}
		}
	}

	/**
	 * Increment counts, install feature when due.
	 */
	void featureAdd(final Repository repo, final Feature feature)
			throws Exception {

		final PropBean propBean = propBean();
		final boolean isMissing = isMissing(feature);

		if (propBean.checkIncrement(repo, feature)) {
			final int totalCount = propBean.countValue(null, feature);
			if (isMissing) {
				if (totalCount > 1) {
					logger.error(
							"Feature count error.",
							new IllegalStateException(
									"Feature is missing when should be present."));
				}
				dependencyAdd(repo, feature);
				featureInstall(feature);
			}
			logger.info("Feature added: {} @ {} {} {}", totalCount,
					repo.getName(), feature.getName(), feature.getVersion());
		} else {
			logger.error("Feature count error.", new IllegalStateException(
					"Trying to install feature already added."));
		}
	}

	/**
	 * Install feature.
	 */
	void featureInstall(final Feature feature) throws Exception {

		assertDependencyInstalled(feature);

		final String name = feature.getName();
		final String version = feature.getVersion();
		getFeaturesService().installFeature(name, version, options());

		logger.info("Feature installed: {} {}", name, version);

	}

	/**
	 * Find installed feature based on dependency identity.
	 */
	Feature featureInstalled(final Dependency depencency) throws Exception {
		final Feature[] featureArray = getFeaturesService()
				.listInstalledFeatures();
		for (final Feature feature : featureArray) {
			if (equals(feature, depencency)) {
				return feature;
			}
		}
		return null;
	}

	/**
	 * Find registered feature based on dependency identity.
	 */
	Feature featureRegistered(final Dependency depencency) throws Exception {
		final Feature[] featureArray = getFeaturesService().listFeatures();
		for (final Feature feature : featureArray) {
			if (equals(feature, depencency)) {
				return feature;
			}
		}
		return null;
	}

	/**
	 * Deactivate auto-install features in a repository.
	 */
	void featureRemove(final Repository repo) throws Exception {
		final Feature[] featureArray = repo.getFeatures();
		for (final Feature feature : featureArray) {
			if (isAutoInstall(feature)) {
				featureRemove(repo, feature);
			}
		}
	}

	/**
	 * Decrement counts, uninstall feature when due.
	 */
	void featureRemove(final Repository repo, final Feature feature)
			throws Exception {

		final PropBean propBean = propBean();
		final boolean isPresent = isPresent(feature);

		if (propBean.checkDecrement(repo, feature)) {
			final int totalCount = propBean.countValue(null, feature);
			if (totalCount == 0) {
				if (isPresent) {
					featureUninstall(feature);
					dependencyRemove(repo, feature);
				} else {
					logger.error(
							"Feature count error.",
							new IllegalStateException(
									"Feature is missing when should be present."));
				}
			}
			logger.info("Feature removed: {} @ {} {} {}", totalCount,
					repo.getName(), feature.getName(), feature.getVersion());
		} else {
			logger.error("Feature count error.", new IllegalStateException(
					"Trying to uninstall feature already removed."));
		}
	}

	/**
	 * Uninstall feature.
	 */
	void featureUninstall(final Feature feature) throws Exception {

		final String name = feature.getName();
		final String version = feature.getVersion();
		getFeaturesService().uninstallFeature(name, version);

		logger.info("Feature uninstalled: {} {}", name, version);

	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public FeaturesService getFeaturesService() {
		return featuresService;
	}

	/**
	 * Bundle contains stored feature.xml
	 */
	boolean hasRepoDescriptor(final Bundle bundle) {
		return repoUrl(bundle) != null;
	}

	/**
	 * Feature service contains named repository.
	 */
	boolean hasRepoRegistered(final String repoId) {
		return repo(repoId) != null;
	}

	/**
	 * Component activate.
	 */
	public void init() throws Exception {
		logger.info("Deployer activate.");
		bundleContext.addBundleListener(this);
	}

	/**
	 * Feature auto-install mode.
	 */
	boolean isAutoInstall(final Feature feature) {
		return Feature.DEFAULT_INSTALL_MODE.equals(feature.getInstall());
	}

	/**
	 * Verify if feature dependency is currently installed.
	 */
	boolean isInstalled(final Dependency depencency) throws Exception {
		return featureInstalled(depencency) != null;
	}

	/**
	 * Feature name space check.
	 */
	boolean isKnownFeaturesURI(final String uri) {
		if (uri == null) {
			return true;
		}
		if (FeaturesNamespaces.URI_0_0_0.equalsIgnoreCase(uri)) {
			return true;
		}
		if (FeaturesNamespaces.URI_1_0_0.equalsIgnoreCase(uri)) {
			return true;
		}
		if (FeaturesNamespaces.URI_1_1_0.equalsIgnoreCase(uri)) {
			return true;
		}
		if (FeaturesNamespaces.URI_1_2_0.equalsIgnoreCase(uri)) {
			return true;
		}
		if (FeaturesNamespaces.URI_CURRENT.equalsIgnoreCase(uri)) {
			return true;
		}
		return false;
	}

	/**
	 * Feature not installed.
	 */
	boolean isMissing(final Feature feature) {
		return !isPresent(feature);
	}

	/**
	 * Feature is installed.
	 */
	boolean isPresent(final Feature feature) {
		return getFeaturesService().isInstalled(feature);
	}

	/**
	 * Verify if feature dependency is present in any repository.
	 */
	boolean isRegistered(final Dependency depencency) throws Exception {
		return featureRegistered(depencency) != null;
	}

	void logBundleEvent(final BundleEvent event) {

		final Bundle bundle = event.getBundle();

		final BundleEventType type = BundleEventType.from(event);

		logger.info("Event: {} {}", type, bundle);

	}

	/**
	 * Default feature install options.
	 */
	EnumSet<Option> options() {
		return EnumSet.of(Option.Verbose, Option.PrintBundlesToRefresh);
	}

	/**
	 * Parse XML file.
	 */
	Document parse(final File artifact) throws Exception {
		if (dbf == null) {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
		}
		final DocumentBuilder db = dbf.newDocumentBuilder();
		db.setErrorHandler(new ErrorHandler() {
			@Override
			public void error(final SAXParseException exception)
					throws SAXException {
			}

			@Override
			public void fatalError(final SAXParseException exception)
					throws SAXException {
				throw exception;
			}

			@Override
			public void warning(final SAXParseException exception)
					throws SAXException {
			}
		});
		return db.parse(artifact);
	}

	/**
	 * Properties bean.
	 */
	PropBean propBean() {
		return new PropBean(propFile());
	}

	/**
	 * Properties file.
	 * <p>
	 * Use system bundle for global storage to permit self update.
	 */
	File propFile() {
		return getBundleContext().getBundle(0).getDataFile(PROP_FILE);
	}

	/**
	 * Find repository by name.
	 */
	Repository repo(final String repoName) {
		final Repository[] list = getFeaturesService().listRepositories();
		for (final Repository repo : list) {
			if (repoName.equals(repo.getName())) {
				return repo;
			}
		}
		return null;
	}

	/**
	 * Create repository, process auto-install features install.
	 */
	void repoCreate(final Bundle bundle) throws Exception {

		final String repoId = repoId(bundle);
		final URL repoUrl = repoUrl(bundle);

		logger.info("Repo create: {} {}", repoId, repoUrl);

		if (hasRepoRegistered(repoId)) {
			logger.error("Attemting to register a duplicate repository.");
			throw new IllegalStateException("Repo is present: " + repoId);
		}

		/** Register repository w/o any feature install. */
		getFeaturesService().addRepository(repoUrl.toURI(), false);

		if (!hasRepoRegistered(repoId)) {
			logger.error("Please verify repository file[name/version] vs xml[name/version].");
			throw new IllegalStateException("Can not register repo: " + repoId);
		}

		final Repository repo = repo(repoId);

		featureAdd(repo);

	}

	/**
	 * Delete repository, process auto-install features uninstall.
	 */
	void repoDelete(final Bundle bundle) throws Exception {

		final String repoId = repoId(bundle);
		final URL repoUrl = repoUrl(bundle);

		logger.info("Repo delete: {} {}", repoId, repoUrl);

		if (!hasRepoRegistered(repoId)) {
			logger.error("Attemting to unregister an unknown repository.");
			throw new IllegalStateException("Repo is missing: " + repoId);
		}

		final Repository repo = repo(repoId);

		featureRemove(repo);

		/** Unregister repository w/o any feature uninstall. */
		getFeaturesService().removeRepository(repo.getURI(), false);

		if (hasRepoRegistered(repoId)) {
			logger.error("Can not unregister repository from feature service.");
			throw new IllegalStateException("Repo is present: " + repoId);
		}

	}

	/**
	 * Repository ID stored in the bundle.
	 * <p>
	 * Currently it is an artifact id made from external feature.xml file name
	 * by the URL transformer.
	 */
	String repoId(final Bundle bundle) {
		return bundle.getSymbolicName();
	}

	/**
	 * Repository feature.xml stored in the bundle.
	 */
	URL repoUrl(final Bundle bundle) {
		final List<URL> list = repoUrlList(bundle);
		switch (list.size()) {
		case 0:
			/** Non wrapper bundle. */
			return null;
		case 1:
			/** Wrapper bundle. */
			return list.get(0);
		default:
			logger.error("Repository bundle should have single url entry.",
					new IllegalStateException(bundle.toString()));
			return null;
		}
	}

	/**
	 * Repository feature.xml stored in the bundle.
	 */
	List<URL> repoUrlList(final Bundle bundle) {

		final Enumeration<URL> entryEnum = bundle.findEntries(META_PATH, "*."
				+ EXTENSION, false);

		if (entryEnum == null || !entryEnum.hasMoreElements()) {
			return Collections.emptyList();
		}

		final List<URL> repoUrlList = new ArrayList<URL>();

		while (entryEnum.hasMoreElements()) {
			repoUrlList.add(entryEnum.nextElement());
		}

		return repoUrlList;

	}

	public void setBundleContext(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setFeaturesService(final FeaturesService featuresService) {
		this.featuresService = featuresService;
	}

	/**
	 * Convert to feature wrapper URL.
	 */
	@Override
	public URL transform(final URL artifact) {
		try {
			return new URL(PROTOCOL, null, artifact.toString());
		} catch (final Exception e) {
			logger.error("Unable to build wrapper bundle", e);
			return null;
		}
	}

}
