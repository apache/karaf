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

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.junit.Assert;
import org.ops4j.net.FreePort;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;

import static org.ops4j.pax.exam.CoreOptions.maven;

public class KarafTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafTestSupport.class);
    private static final int RMI_BASE_PORT = 33221;

    static final Long COMMAND_TIMEOUT = 10000L;
    static final Long SERVICE_TIMEOUT = 30000L;

    ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featuresService;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    @Configuration
    public Option[] config() {

        // ports to override values from etc/org.apache.karaf.management.cfg
        int rmiRegistryPort = new FreePort(RMI_BASE_PORT+1, RMI_BASE_PORT+100).getPort();
        int rmiServerPort = new FreePort(rmiRegistryPort+1, rmiRegistryPort+100).getPort();
        LOGGER.info("Port selected for Karaf RMI Registry: {}", rmiRegistryPort);
        LOGGER.info("Port selected for JMX Server connection: {}", rmiServerPort);

        return new Option[]{
                karafDistributionConfiguration().frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz"))
                        .karafVersion(MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf")).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                keepRuntimeFolder(),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", Integer.toString(rmiRegistryPort)),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", Integer.toString(rmiServerPort)),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
        return executeCommand(command, COMMAND_TIMEOUT, false);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent  Specifies if the command should be displayed in the screen.
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
        FutureTask<String> commandFuture = new FutureTask<String>(
                new Callable<String>() {
                    public String call() {
                        try {
                            if (!silent) {
                                System.err.println(command);
                            }
                            commandSession.execute(command);
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                        printStream.flush();
                        return byteArrayOutputStream.toString();
                    }
                });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        return response;
    }


    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

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

    /*
    * Explode the dictionary into a ,-delimited list of key=value pairs
    */
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
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
    }

    public JMXConnector getJMXConnector() throws Exception {
        String karafHome = System.getProperty("karaf.home");
        File managementProperties = new File(karafHome + "/etc", "org.apache.karaf.management.cfg");
        String rmiServerAddress = "";
        String rmiRegistryAddress = "localhost:1099";
        if (managementProperties.exists()) {
            Properties props = new Properties();
            props.load(new FileInputStream(managementProperties));
            if (props.containsKey("rmiServerPort")) {
                rmiServerAddress = "localhost:" + props.getProperty("rmiServerPort");
            }
            if (props.containsKey("rmiRegistryPort")) {
                rmiRegistryAddress = "localhost:" + props.getProperty("rmiRegistryPort");
            }
        }
        String jmxConnectionInfo = String.format("service:jmx:rmi://%s/jndi/rmi://%s/karaf-root", rmiServerAddress,
            rmiRegistryAddress);
        JMXServiceURL url = new JMXServiceURL(jmxConnectionInfo);
        Hashtable env = new Hashtable();
        String[] credentials = new String[]{"karaf", "karaf"};
        env.put("jmx.remote.credentials", credentials);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        return connector;
    }

    public void assertFeatureInstalled(String featureName) {
        Feature[] features = featuresService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureName.equals(feature.getName())) {
                return;
            }
        }
        Assert.fail("Feature " + featureName + " should be installed but is not");
    }

}
