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
package org.apache.karaf.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.karaf.main.util.BootstrapLogManager;
import org.apache.karaf.main.util.PropertiesHelper;
import org.apache.karaf.main.util.StringMap;
import org.apache.karaf.main.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.startlevel.StartLevel;

/**
 * <p>
 * This class is the default way to instantiate and execute the framework. It is
 * not intended to be the only way to instantiate and execute the framework;
 * rather, it is one example of how to do so. When embedding the framework in a
 * host application, this class can serve as a simple guide of how to do so. It
 * may even be worthwhile to reuse some of its property handling capabilities.
 * This class is completely static and is only intended to start a single
 * instance of the framework.
 * </p>
 */
public class Main {

	/**
	 * The default name used for the system properties file.
	 */
	private static final String SYSTEM_PROPERTIES_FILE_NAME = "system.properties";
	/**
	 * The default name used for the configuration properties file.
	 */
	private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";
	/**
	 * The default name used for the startup properties file.
	 */
	private static final String STARTUP_PROPERTIES_FILE_NAME = "startup.properties";

	/**
	 * The property name prefix for the launcher's auto-install property.
	 */
	private static final String PROPERTY_AUTO_INSTALL = "karaf.auto.install";
	/**
	 * The property for auto-discovering the bundles
	 */
	private static final String PROPERTY_AUTO_START = "karaf.auto.start";
	/**
	 * The system property for specifying the Karaf home directory. The home
	 * directory hold the binary install of Karaf.
	 */
	private static final String PROP_KARAF_HOME = "karaf.home";
	/**
	 * The environment variable for specifying the Karaf home directory. The
	 * home directory hold the binary install of Karaf.
	 */
	private static final String ENV_KARAF_HOME = "KARAF_HOME";
	/**
	 * The system property for specifying the Karaf base directory. The base
	 * directory holds the configuration and data for a Karaf instance.
	 */
	private static final String PROP_KARAF_BASE = "karaf.base";
	/**
	 * The environment variable for specifying the Karaf base directory. The
	 * base directory holds the configuration and data for a Karaf instance.
	 */
	private static final String ENV_KARAF_BASE = "KARAF_BASE";
	/**
	 * The system property for specifying the Karaf data directory. The data
	 * directory holds the bundles data and cache for a Karaf instance.
	 */
	public static final String PROP_KARAF_DATA = "karaf.data";
	/**
	 * The system property for hosting the current Karaf version.
	 */
	private static final String PROP_KARAF_VERSION = "karaf.version";
	/**
	 * The environment variable for specifying the Karaf data directory. The
	 * data directory holds the bundles data and cache for a Karaf instance.
	 */
	public static final String ENV_KARAF_DATA = "KARAF_DATA";
	/**
	 * The system property for specifying the Karaf data directory. The data
	 * directory holds the bundles data and cache for a Karaf instance.
	 */
	private static final String PROP_KARAF_INSTANCES = "karaf.instances";
	/**
	 * The system property for specifying the Karaf data directory. The data
	 * directory holds the bundles data and cache for a Karaf instance.
	 */
	private static final String ENV_KARAF_INSTANCES = "KARAF_INSTANCES";

	/**
	 * Config property which identifies directories which contain bundles to be
	 * loaded by SMX
	 */
	private static final String BUNDLE_LOCATIONS = "bundle.locations";

    /**
     * Config property that indicates we want to convert bundle locations to
     * Maven style URLs
     */
	private static final String PROPERTY_CONVERT_TO_MAVEN_URL = "karaf.maven.convert";

	private static final String DEFAULT_REPO = "karaf.default.repository";

	private static final String KARAF_FRAMEWORK = "karaf.framework";

	public static final String KARAF_FRAMEWORK_FACTORY = "karaf.framework.factory";

	Logger LOG = Logger.getLogger(this.getClass().getName());

	private File karafHome;
	private File karafBase;
	private File karafData;
	private File karafInstances;
	private Properties configProps = null;
	Framework framework = null;
	private final String[] args;

	private LifecycleManager lifecycleManager;

	public Main(String[] args) {
		this.args = args;
	}

	public LifecycleManager launch() throws Exception {
		karafHome = Utils.getKarafHome(Main.class, Main.PROP_KARAF_HOME,
				Main.ENV_KARAF_HOME);
		karafBase = Utils.getKarafDirectory(Main.PROP_KARAF_BASE,
				Main.ENV_KARAF_BASE, karafHome, false, true);
		karafData = Utils.getKarafDirectory(Main.PROP_KARAF_DATA,
				Main.ENV_KARAF_DATA, new File(karafBase, "data"), true, true);

		if (Boolean.getBoolean("karaf.restart.clean")) {
			Utils.deleteDirectory(karafData);
		}

		karafInstances = Utils.getKarafDirectory(Main.PROP_KARAF_INSTANCES,
				Main.ENV_KARAF_INSTANCES, new File(karafHome, "instances"),
				false, false);

		Package p = Package.getPackage("org.apache.karaf.main");
		if (p != null && p.getImplementationVersion() != null)
			System.setProperty(PROP_KARAF_VERSION, p.getImplementationVersion());
		System.setProperty(PROP_KARAF_HOME, karafHome.getPath());
		System.setProperty(PROP_KARAF_BASE, karafBase.getPath());
		System.setProperty(PROP_KARAF_DATA, karafData.getPath());
		System.setProperty(PROP_KARAF_INSTANCES, karafInstances.getPath());

		File etcFolder = new File(karafBase, "etc");
		if (!etcFolder.exists()) {
			throw new FileNotFoundException("etc folder not found: "
					+ etcFolder.getAbsolutePath());
		}
		Properties sysProps = PropertiesHelper.loadPropertiesFile(etcFolder,
				SYSTEM_PROPERTIES_FILE_NAME, false);
		PropertiesHelper.updateSystemProperties(sysProps);
		InstanceInfoManager.updateInstanceInfo(karafHome, karafBase);

		configProps = PropertiesHelper.loadPropertiesFile(etcFolder,
				CONFIG_PROPERTIES_FILE_NAME, false);
		PropertiesHelper.substituteVariables(configProps);

		BootstrapLogManager.setProperties(configProps);
		LOG.addHandler(BootstrapLogManager.getDefaultHandler());

		PropertiesHelper.copySystemProperties(configProps);

		ClassLoader classLoader = createClassLoader(configProps);

		processSecurityProperties(configProps);

		if (configProps.getProperty(Constants.FRAMEWORK_STORAGE) == null) {
			File storage = new File(karafData.getPath(), "cache");
			try {
				storage.mkdirs();
			} catch (SecurityException se) {
				throw new Exception(se.getMessage());
			}
			configProps.setProperty(Constants.FRAMEWORK_STORAGE,
					storage.getAbsolutePath());
		}

		// Start up the OSGI framework
		String factoryClass = configProps.getProperty(KARAF_FRAMEWORK_FACTORY);
		if (factoryClass == null) {
			InputStream is = classLoader
					.getResourceAsStream("META-INF/services/"
							+ FrameworkFactory.class.getName());
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					"UTF-8"));
			factoryClass = br.readLine();
			br.close();
		}
		FrameworkFactory factory = (FrameworkFactory) classLoader.loadClass(
				factoryClass).newInstance();
		framework = factory.newFramework(new StringMap(configProps, false));
		framework.init();

		List<File> bundleDirs = getBundleDirs(configProps);
		Properties startupProps = PropertiesHelper.loadPropertiesFile(
				etcFolder, STARTUP_PROPERTIES_FILE_NAME, true);
		Main.processConfigurationProperties(configProps, startupProps,
				bundleDirs);
		processAutoProperties(framework.getBundleContext());
		framework.start();
		lifecycleManager = new LifecycleManager(configProps, framework);
		lifecycleManager.start();
		return lifecycleManager;
	}

	private List<File> getBundleDirs(Properties configProps)
			throws FileNotFoundException {
		List<File> bundleDirs = new ArrayList<File>();
		String defaultRepo = System.getProperty(DEFAULT_REPO, "system");
		if (karafBase.equals(karafHome)) {
			File systemRepo = new File(karafHome, defaultRepo);
			if (!systemRepo.exists()) {
				throw new FileNotFoundException("system repo not found: "
						+ systemRepo.getAbsolutePath());
			}
			bundleDirs.add(systemRepo);
		} else {
			File baseSystemRepo = new File(karafBase, defaultRepo);
			File homeSystemRepo = new File(karafHome, defaultRepo);
			if (!baseSystemRepo.exists() && !homeSystemRepo.exists()) {
				throw new FileNotFoundException("system repos not found: "
						+ baseSystemRepo.getAbsolutePath() + " "
						+ homeSystemRepo.getAbsolutePath());
			}
			bundleDirs.add(baseSystemRepo);
			bundleDirs.add(homeSystemRepo);
		}
		String locations = configProps.getProperty(BUNDLE_LOCATIONS);
		addLocationsToBundleDirs(bundleDirs, locations);
		return bundleDirs;
	}

	private void addLocationsToBundleDirs(List<File> bundleDirs,
			String locations) {
		if (locations != null) {
			StringTokenizer st = new StringTokenizer(locations, "\" ", true);
			if (st.countTokens() > 0) {
				String location;
				do {
					location = PropertiesHelper.nextLocation(st);
					if (location != null) {
						File f = new File(karafBase, location);
						if (f.exists() && f.isDirectory()) {
							bundleDirs.add(f);
						} else {
							System.err.println("Bundle location " + location
									+ " does not exist or is not a directory.");
						}
					}
				} while (location != null);
			}
		}
	}

	/**
	 * Process properties to customize default felix behavior
	 * 
	 * @param configProps
	 *            properties loaded from etc/config.properties
	 * @param startupProps
	 *            properties loaded from etc/startup.properties
	 * @param bundleDirs
	 *            location to load bundles from (usually system/)
	 */
	private static void processConfigurationProperties(Properties configProps,
			Properties startupProps, List<File> bundleDirs) throws Exception {
		if (bundleDirs == null) {
			return;
		}
		boolean hasErrors = false;
		if ("all".equals(configProps.getProperty(PROPERTY_AUTO_START, "")
				.trim())) {
			configProps.remove(PROPERTY_AUTO_START);
			ArrayList<File> jars = new ArrayList<File>();

			// We should start all the bundles in the system dir.
			for (File bundleDir : bundleDirs) {
				Utils.findJars(bundleDir, jars);
			}

			StringBuffer sb = new StringBuffer();

			for (File jar : jars) {
				try {
					sb.append("\"").append(jar.toURI().toURL().toString())
							.append("\" ");
				} catch (MalformedURLException e) {
					System.err.print("Ignoring " + jar.toString() + " (" + e
							+ ")");
				}
			}

			configProps.setProperty(PROPERTY_AUTO_START, sb.toString());

		} else if (STARTUP_PROPERTIES_FILE_NAME.equals(configProps.getProperty(
				PROPERTY_AUTO_START, "").trim())) {
			configProps.remove(PROPERTY_AUTO_START);
			// We should start the bundles in the startup.properties file.
			HashMap<Integer, StringBuffer> levels = new HashMap<Integer, StringBuffer>();
			for (Object o : startupProps.keySet()) {
				String name = (String) o;
				File file = Utils.findFile(bundleDirs, name);

				if (file != null) {
					Integer level;
					try {
						level = new Integer(startupProps.getProperty(name)
								.trim());
					} catch (NumberFormatException e1) {
						System.err.print("Ignoring " + file.toString()
								+ " (run level must be an integer)");
						continue;
					}
					StringBuffer sb = levels.get(level);
					if (sb == null) {
						sb = new StringBuffer(256);
						levels.put(level, sb);
					}
					try {
						sb.append("\"").append(file.toURI().toURL().toString())
								.append("|").append(name).append("\" ");
					} catch (MalformedURLException e) {
						System.err.print("Ignoring " + file.toString() + " ("
								+ e + ")");
					}
				} else {
					System.err.println("Bundle listed in "
							+ STARTUP_PROPERTIES_FILE_NAME
							+ " configuration not found: " + name);
					hasErrors = true;
				}
			}

			for (Map.Entry<Integer, StringBuffer> entry : levels.entrySet()) {
				configProps.setProperty(
						PROPERTY_AUTO_START + "." + entry.getKey(), entry
								.getValue().toString());
			}
		}
		if (hasErrors) {
			throw new Exception("Aborting due to missing startup bundles");
		}
	}

	/**
	 * <p>
	 * This method performs the main task of constructing an framework instance
	 * and starting its execution. The following functions are performed when
	 * invoked:
	 * </p>
	 * <ol>
	 * <li><i><b>Read the system properties file.<b></i> This is a file
	 * containing properties to be pushed into <tt>System.setProperty()</tt>
	 * before starting the framework. This mechanism is mainly shorthand for
	 * people starting the framework from the command line to avoid having to
	 * specify a bunch of <tt>-D</tt> system property definitions. The only
	 * properties defined in this file that will impact the framework's behavior
	 * are the those concerning setting HTTP proxies, such as
	 * <tt>http.proxyHost</tt>, <tt>http.proxyPort</tt>, and
	 * <tt>http.proxyAuth</tt>.</li>
	 * <li><i><b>Perform system property variable substitution on system
	 * properties.</b></i> Any system properties in the system property file
	 * whose value adheres to <tt>${&lt;system-prop-name&gt;}</tt> syntax will
	 * have their value substituted with the appropriate system property value.</li>
	 * <li><i><b>Read the framework's configuration property file.</b></i> This
	 * is a file containing properties used to configure the framework instance
	 * and to pass configuration information into bundles installed into the
	 * framework instance. The configuration property file is called
	 * <tt>config.properties</tt> by default and is located in the
	 * <tt>conf/</tt> directory of the Felix installation directory, which is
	 * the parent directory of the directory containing the <tt>felix.jar</tt>
	 * file. It is possible to use a different location for the property file by
	 * specifying the desired URL using the <tt>felix.config.properties</tt>
	 * system property; this should be set using the <tt>-D</tt> syntax when
	 * executing the JVM. Refer to the <a
	 * href="Felix.html#Felix(java.util.Map, java.util.List)"> <tt>Felix</tt>
	 * </a> constructor documentation for more information on the framework
	 * configuration options.</li>
	 * <li><i><b>Perform system property variable substitution on configuration
	 * properties.</b></i> Any configuration properties whose value adheres to
	 * <tt>${&lt;system-prop-name&gt;}</tt> syntax will have their value
	 * substituted with the appropriate system property value.</li>
	 * <li><i><b>Ensure the default bundle cache has sufficient information to
	 * initialize.</b></i> The default implementation of the bundle cache
	 * requires either a profile name or a profile directory in order to start.
	 * The configuration properties are checked for at least one of the
	 * <tt>felix.cache.profile</tt> or <tt>felix.cache.profiledir</tt>
	 * properties. If neither is found, the user is asked to supply a profile
	 * name that is added to the configuration property set. See the <a
	 * href="cache/DefaultBundleCache.html"><tt>DefaultBundleCache</tt></a>
	 * documentation for more details its configuration options.</li>
	 * <li><i><b>Creates and starts a framework instance.</b></i> A case
	 * insensitive <a href="util/StringMap.html"><tt>StringMap</tt></a> is
	 * created for the configuration property file and is passed into the
	 * framework.</li>
	 * </ol>
	 * <p>
	 * It should be noted that simply starting an instance of the framework is
	 * not enough to create an interactive session with it. It is necessary to
	 * install and start bundles that provide an interactive impl; this is
	 * generally done by specifying an "auto-start" property in the framework
	 * configuration property file. If no interactive impl bundles are installed
	 * or if the configuration property file cannot be found, the framework will
	 * appear to be hung or deadlocked. This is not the case, it is executing
	 * correctly, there is just no way to interact with it. Refer to the <a
	 * href="Felix.html#Felix(java.util.Map, java.util.List)"> <tt>Felix</tt>
	 * </a> constructor documentation for more information on framework
	 * configuration options.
	 * </p>
	 * 
	 * @param args
	 *            An array of arguments, all of which are ignored.
	 * @throws Exception
	 *             If an error occurs.
	 **/
	public static void main(String[] args) throws Exception {
		while (true) {
			boolean restart = false;
			System.setProperty("karaf.restart", "false");
			final Main main = new Main(args);
			LifecycleManager manager = null;
			int exitCode = 0;
			try {
				manager = main.launch();
			} catch (Throwable ex) {
                if (main.lifecycleManager != null) {
                    main.lifecycleManager.destroyKaraf();
                }
                exitCode = -1;
				System.err.println("Could not create framework: " + ex);
				ex.printStackTrace();
			}
			try {
				if (manager != null) {
					manager.awaitShutdown();
					boolean stopped = main.lifecycleManager.destroyKaraf();
					restart = Boolean.getBoolean("karaf.restart");
					if (!stopped) {
						if (restart) {
							System.err
									.println("Timeout waiting for framework to stop.  Restarting now.");
						} else {
							System.err
									.println("Timeout waiting for framework to stop.  Exiting VM.");
							exitCode = -3;
						}
					}
				}
			} catch (Throwable ex) {
				exitCode = -2;
				System.err.println("Error occured shutting down framework: "
						+ ex);
				ex.printStackTrace();
			} finally {
				if (!restart) {
					System.exit(exitCode);
				}
			}
		}
	}

	private static void processSecurityProperties(Properties m_configProps) {
		String prop = m_configProps
				.getProperty("org.apache.karaf.security.providers");
		if (prop != null) {
			String[] providers = prop.split(",");
			for (String provider : providers) {
				addProvider(provider);
			}
		}
	}

	private static void addProvider(String provider) {
		try {
			Security.addProvider((Provider) Class.forName(provider)
					.newInstance());
		} catch (Throwable t) {
			System.err.println("Unable to register security provider: " + t);
		}
	}

	/**
	 * <p/>
	 * Processes the auto-install and auto-start properties from the specified
	 * configuration properties.
	 * 
	 * @param context
	 *            the system bundle context
	 */
	private void processAutoProperties(BundleContext context) {
		// Check if we want to convert URLs to maven style
		boolean convertToMavenUrls = Boolean.parseBoolean(configProps
				.getProperty(PROPERTY_CONVERT_TO_MAVEN_URL, "true"));

		// Retrieve the Start Level service, since it will be needed
		// to set the start level of the installed bundles.
		StartLevel sl = (StartLevel) context
				.getService(context
						.getServiceReference(org.osgi.service.startlevel.StartLevel.class
								.getName()));

		// Set the default bundle start level
		int ibsl = 60;
		try {
			String str = configProps.getProperty("karaf.startlevel.bundle");
			if (str != null) {
				ibsl = Integer.parseInt(str);
			}
		} catch (Throwable t) {
		}
		sl.setInitialBundleStartLevel(ibsl);

		// If we have a clean state, install everything
		if (framework.getBundleContext().getBundles().length == 1) {
			// The auto-install property specifies a space-delimited list of
			// bundle URLs to be automatically installed into each new profile;
			// the start level to which the bundles are assigned is specified by
			// appending a ".n" to the auto-install property name, where "n" is
			// the desired start level for the list of bundles.
			autoInstall(PROPERTY_AUTO_INSTALL, context, sl, convertToMavenUrls,
					false);

			// The auto-start property specifies a space-delimited list of
			// bundle URLs to be automatically installed and started into each
			// new profile; the start level to which the bundles are assigned
			// is specified by appending a ".n" to the auto-start property name,
			// where "n" is the desired start level for the list of bundles.
			// The following code starts bundles in one pass, installing bundles
			// for a given level, then starting them, then moving to the next
			// level.
			autoInstall(PROPERTY_AUTO_START, context, sl, convertToMavenUrls,
					true);
		}
	}

	private List<Bundle> autoInstall(String propertyPrefix,
			BundleContext context, StartLevel sl, boolean convertToMavenUrls,
			boolean start) {
		Map<Integer, String> autoStart = new TreeMap<Integer, String>();
		List<Bundle> bundles = new ArrayList<Bundle>();
		for (Object o : configProps.keySet()) {
			String key = (String) o;
			// Ignore all keys that are not the auto-start property.
			if (!key.startsWith(propertyPrefix)) {
				continue;
			}
			// If the auto-start property does not have a start level,
			// then assume it is the default bundle start level, otherwise
			// parse the specified start level.
			int startLevel = sl.getInitialBundleStartLevel();
			if (!key.equals(propertyPrefix)) {
				try {
					startLevel = Integer.parseInt(key.substring(key
							.lastIndexOf('.') + 1));
				} catch (NumberFormatException ex) {
					System.err.println("Invalid property: " + key);
				}
			}
			autoStart.put(startLevel, configProps.getProperty(key));
		}
		for (Integer startLevel : autoStart.keySet()) {
			StringTokenizer st = new StringTokenizer(autoStart.get(startLevel),
					"\" ", true);
			if (st.countTokens() > 0) {
				String location;
				do {
					location = PropertiesHelper.nextLocation(st);
					if (location != null) {
						try {
							String[] parts = Utils.convertToMavenUrlsIfNeeded(
									location, convertToMavenUrls);
							Bundle b = context.installBundle(parts[0], new URL(
									parts[1]).openStream());
							sl.setBundleStartLevel(b, startLevel);
							bundles.add(b);
						} catch (Exception ex) {
							System.err.println("Error installing bundle  "
									+ location + ": " + ex);
						}
					}
				} while (location != null);
			}
		}
		// Now loop through and start the installed bundles.
		if (start) {
			startBundles(bundles);
		}
		return bundles;
	}

	private void startBundles(List<Bundle> bundles) {
		for (Bundle b : bundles) {
			try {
				String fragmentHostHeader = (String) b.getHeaders().get(
						Constants.FRAGMENT_HOST);
				if (fragmentHostHeader == null
						|| fragmentHostHeader.trim().length() == 0) {
					b.start();
				}
			} catch (Exception ex) {
				System.err.println("Error starting bundle "
						+ b.getSymbolicName() + ": " + ex);
			}
		}
	}

	private ClassLoader createClassLoader(Properties configProps)
			throws Exception {
		String framework = configProps.getProperty(KARAF_FRAMEWORK);
		if (framework == null) {
			throw new IllegalArgumentException("Property " + KARAF_FRAMEWORK
					+ " must be set in the etc/" + CONFIG_PROPERTIES_FILE_NAME
					+ " configuration file");
		}
		String bundle = configProps.getProperty(KARAF_FRAMEWORK + "."
				+ framework);
		if (bundle == null) {
			throw new IllegalArgumentException("Property " + KARAF_FRAMEWORK
					+ "." + framework + " must be set in the etc/"
					+ CONFIG_PROPERTIES_FILE_NAME + " configuration file");
		}
		File bundleFile = new File(karafBase, bundle);
		if (!bundleFile.exists()) {
			bundleFile = new File(karafHome, bundle);
		}
		if (!bundleFile.exists()) {
			throw new FileNotFoundException(bundleFile.getAbsolutePath());
		}

		List<URL> urls = new ArrayList<URL>();
		urls.add(bundleFile.toURI().toURL());
		File[] libs = new File(karafHome, "lib").listFiles();
		if (libs != null) {
			for (File f : libs) {
				if (f.isFile() && f.canRead() && f.getName().endsWith(".jar")) {
					urls.add(f.toURI().toURL());
				}
			}
		}

		return new URLClassLoader(urls.toArray(new URL[urls.size()]),
				Main.class.getClassLoader());
	}

	/**
	 * Retrieve the arguments used when launching Karaf
	 * 
	 * @return the arguments of the main karaf process
	 */
	public String[] getArgs() {
		return args;
	}

	public Framework getFramework() {
		return framework;
	}

	public static void shutdown() throws IOException, FileNotFoundException,
			Exception, UnknownHostException {
		File karafHome = Utils.getKarafHome(Main.class, Main.PROP_KARAF_HOME,
				Main.ENV_KARAF_HOME);
		File karafBase = Utils.getKarafDirectory(PROP_KARAF_BASE,
				ENV_KARAF_BASE, karafHome, false, true);
		File karafData = Utils.getKarafDirectory(PROP_KARAF_DATA,
				ENV_KARAF_DATA, new File(karafBase.getPath(), "data"), true,
				true);

		System.setProperty(Main.PROP_KARAF_HOME, karafHome.getPath());
		System.setProperty(Main.PROP_KARAF_BASE, karafBase.getPath());
		System.setProperty(Main.PROP_KARAF_DATA, karafData.getPath());

		File etcFolder = new File(karafBase, "etc");
		if (!etcFolder.exists()) {
			throw new FileNotFoundException("etc folder not found: "
					+ etcFolder.getAbsolutePath());
		}
		// Load system properties.
		Properties sysProps = PropertiesHelper.loadPropertiesFile(etcFolder,
				Main.SYSTEM_PROPERTIES_FILE_NAME, false);
		PropertiesHelper.updateSystemProperties(sysProps);
		Properties props = PropertiesHelper.loadPropertiesFile(etcFolder,
				Main.CONFIG_PROPERTIES_FILE_NAME, false);
		PropertiesHelper.copySystemProperties(props);
		PropertiesHelper.substituteVariables(props);

		LifecycleManager.shutDown(props);
	}

}
