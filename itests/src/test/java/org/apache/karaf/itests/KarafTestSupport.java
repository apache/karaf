/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.URL;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.junit.Rule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafTestSupport {

    public static final String MIN_RMI_SERVER_PORT = "44444";
    public static final String MAX_RMI_SERVER_PORT = "66666";
    public static final String MIN_HTTP_PORT = "9080";
    public static final String MAX_HTTP_PORT = "9999";
    public static final String MIN_RMI_REG_PORT = "1099";
    public static final String MAX_RMI_REG_PORT = "9999";
    public static final String MIN_SSH_PORT = "8101";
    public static final String MAX_SSH_PORT = "8888";

    static final Long COMMAND_TIMEOUT = 30000L;
    static final Long SERVICE_TIMEOUT = 30000L;

    private static Logger LOG = LoggerFactory.getLogger(KarafTestSupport.class);

    @Rule
    public KarafTestWatcher baseTestWatcher = new KarafTestWatcher();

    ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featureService;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    /**
     * To make sure the tests run only when the boot features are fully installed
     */
    @Inject
    BootFinished bootFinished;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    public File getConfigFile(String path) {
        URL res = this.getClass().getResource(path);
        if (res == null) {
            throw new RuntimeException("Config resource " + path + " not found");
        }
    	return new File(res.getFile());
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");

        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));

        return new Option[]{
            // KarafDistributionOption.debugConfiguration("8889", true),
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
            // the KarafMBeanServerBuilder doesn't exist anymore
            configureSecurity().disableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
				logLevel(LogLevel.INFO),
            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "config,standard,region,package,kar,management"),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
            editConfigurationFilePut("etc/system.properties", "spring31.version", System.getProperty("spring31.version")),
            editConfigurationFilePut("etc/system.properties", "spring32.version", System.getProperty("spring32.version")),
            editConfigurationFilePut("etc/system.properties", "spring40.version", System.getProperty("spring40.version")),
            editConfigurationFilePut("etc/system.properties", "spring41.version", System.getProperty("spring41.version")),
            editConfigurationFilePut("etc/system.properties", "spring42.version", System.getProperty("spring42.version")),
        };
    }

    private int getAvailablePort(int min, int max) {
        for (int i = min; i <= max; i++) {
            try {
                ServerSocket serverSocket = new ServerSocket(i);
                return serverSocket.getLocalPort();
            } catch (Exception e) {
                System.err.println("Port " + i + " not available, trying next one");
                continue; // try next port
            }
        }
        throw new IllegalStateException("Can't find available network ports");
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    protected String executeCommand(final String command, Principal ... principals) {
        return executeCommand(command, COMMAND_TIMEOUT, false, principals);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command    The command to execute.
     * @param timeout    The amount of time in millis to wait for the command to execute.
     * @param silent     Specifies if the command should be displayed in the screen.
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal ... principals) {
        waitForCommandService(command);

        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);

        final Callable<String> commandCallable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    if (!silent) {
                        System.err.println(command);
                    }
                    final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
                    final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
                    commandSession.execute(command);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                printStream.flush();
                return byteArrayOutputStream.toString();
            }
        };

        FutureTask<String> commandFuture;
        if (principals.length == 0) {
            commandFuture = new FutureTask<String>(commandCallable);
        } else {
            // If principals are defined, run the command callable via Subject.doAs()
            commandFuture = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Subject subject = new Subject();
                    subject.getPrincipals().addAll(Arrays.asList(principals));
                    return Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
                        @Override
                        public String run() throws Exception {
                            return commandCallable.call();
                        }
                    });
                }
            });
        }

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause().getCause();
            throw new RuntimeException(cause.getMessage(), cause);
	} catch (InterruptedException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
        return response;
    }


    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                Dictionary dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref);
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref);
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForCommandService(String command) {
        // the commands are represented by services. Due to the asynchronous nature of services they may not be
        // immediately available. This code waits the services to be available, in their secured form. It
        // means that the code waits for the command service to appear with the roles defined.

        if (command == null || command.length() == 0) {
            return;
        }

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0) {
            command = command.substring(0, spaceIdx);
        }
        int colonIndx = command.indexOf(':');

        try {
            if (colonIndx > 0) {
                String scope = command.substring(0, colonIndx);
                String function = command.substring(colonIndx + 1);
                waitForService("(&(osgi.command.scope=" + scope + ")(osgi.command.function=" + function + "))", SERVICE_TIMEOUT);
            } else {
                waitForService("(osgi.command.function=" + command + ")", SERVICE_TIMEOUT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForService(String filter, long timeout) throws InvalidSyntaxException, InterruptedException {
        ServiceTracker st = new ServiceTracker(bundleContext, bundleContext.createFilter(filter), null);
        try {
            st.open();
            st.waitForService(timeout);
        } finally {
            st.close();
        }
    }

    /*
    * Explode the dictionary into a ,-delimited list of key=value pairs
    */
    @SuppressWarnings("rawtypes")
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /**
     * Provides an iterable collection of references, even if the original array is null
     */
    @SuppressWarnings("rawtypes")
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
    }

    public JMXConnector getJMXConnector() throws Exception {
        return getJMXConnector("karaf", "karaf");
    }

    public JMXConnector getJMXConnector(String userName, String passWord) throws Exception {
        JMXServiceURL url = new JMXServiceURL(getJmxServiceUrl());
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        String[] credentials = new String[]{ userName, passWord };
        env.put("jmx.remote.credentials", credentials);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        return connector;
    }

    public String getJmxServiceUrl() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.management", null);
        if (configuration != null) {
            return configuration.getProperties().get("serviceUrl").toString();
        }
        return "service:jmx:rmi:///jndi/rmi://localhost:" + MIN_RMI_SERVER_PORT + "/karaf-root";
    }

    public String getSshPort() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.shell", null);
        if (configuration != null) {
            return configuration.getProperties().get("sshPort").toString();
        }
        return "8101";
    }

    public void assertFeatureInstalled(String featureName) {
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureName.equals(feature.getName())) {
                return;
            }
        }
        Assert.fail("Feature " + featureName + " should be installed but is not");
    }

    public void assertFeatureInstalled(String featureName, String featureVersion) {
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureName.equals(feature.getName()) && featureVersion.equals(feature.getVersion())) {
                return;
            }
        }
        Assert.fail("Feature " + featureName + "/" + featureVersion + " should be installed but is not");
    }

    public void assertFeatureNotInstalled(String featureName) {
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureName.equals(feature.getName())) {
                Assert.fail("Feature " + featureName + " is installed whereas it should not be");
                return;
            }
        }
    }

    public void assertFeaturesInstalled(String ... expectedFeatures) {
        Set<String> expectedFeaturesSet = new HashSet<String>(Arrays.asList(expectedFeatures));
        Feature[] features = featureService.listInstalledFeatures();
        Set<String> installedFeatures = new HashSet<String>();
        for (Feature feature : features) {
            installedFeatures.add(feature.getName());
        }
        String msg = "Expecting the following features to be installed : " + expectedFeaturesSet + " but found " + installedFeatures;
        Assert.assertTrue(msg, installedFeatures.containsAll(expectedFeaturesSet));
    }

    public void assertContains(String expectedPart, String actual) {
        assertTrue("Should contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

    public void assertContainsNot(String expectedPart, String actual) {
        Assert.assertFalse("Should not contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

	protected void assertBundleInstalled(String name) {
	    Assert.assertNotNull("Bundle " + name + " should be installed", findBundleByName(name));
	}

	protected void assertBundleNotInstalled(String name) {
	    Assert.assertNull("Bundle " + name + " should not be installed", findBundleByName(name));
	}

	protected Bundle findBundleByName(String symbolicName) {
	    for (Bundle bundle : bundleContext.getBundles()) {
	        if (bundle.getSymbolicName().equals(symbolicName)) {
	            return bundle;
	        }
	    }
	    return null;
	}

    protected void installAndAssertFeature(String feature) throws Exception {
        featureService.installFeature(feature, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        assertFeatureInstalled(feature);
    }

    protected void installAndAssertFeature(String feature, String version) throws Exception {
        featureService.installFeature(feature, version, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        assertFeatureInstalled(feature, version);
    }

    protected void installAssertAndUninstallFeatures(String... feature) throws Exception {
    	Set<Feature> featuresBefore = new HashSet<Feature>(Arrays.asList(featureService.listInstalledFeatures()));
    	try {
			for (String curFeature : feature) {
				featureService.installFeature(curFeature, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
			    assertFeatureInstalled(curFeature);
			}
		} finally {
			uninstallNewFeatures(featuresBefore);
		}
    }

    protected void installAssertAndUninstallFeature(String feature) throws Exception {
        Set<Feature> featuresBefore = new HashSet<Feature>(Arrays.asList(featureService.listInstalledFeatures()));
        try {
            featureService.installFeature(feature, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
            assertFeatureInstalled(feature);
        } finally {
            uninstallNewFeatures(featuresBefore);
        }
    }

    protected void installAssertAndUninstallFeature(String feature, String version) throws Exception {
        Set<Feature> featuresBefore = new HashSet<Feature>(Arrays.asList(featureService.listInstalledFeatures()));
        try {
            featureService.installFeature(feature, version, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
            assertFeatureInstalled(feature, version);
        } finally {
            uninstallNewFeatures(featuresBefore);
        }
    }

    /**
     * The feature service does not uninstall feature dependencies when uninstalling a single feature.
     * So we need to make sure we uninstall all features that were newly installed.
     *
     * @param featuresBefore
     * @throws Exception
     */
	protected void uninstallNewFeatures(Set<Feature> featuresBefore)
			throws Exception {
		Feature[] features = featureService.listInstalledFeatures();
        for (Feature curFeature : features) {
			if (!featuresBefore.contains(curFeature)) {
				try {
					System.out.println("Uninstalling " + curFeature.getName());
					featureService.uninstallFeature(curFeature.getName(), curFeature.getVersion(), EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

    protected void close(Closeable closeAble) {
    	if (closeAble != null) {
    		try {
				closeAble.close();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
    	}
    }

}
