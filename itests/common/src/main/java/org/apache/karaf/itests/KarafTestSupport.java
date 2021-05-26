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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
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

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.RerunTestException;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.container.remote.RBCRemoteTargetOptions;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
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

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class KarafTestSupport {

    private static final EnumSet<FeaturesService.Option> NO_AUTO_REFRESH = EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);
    public static final String MIN_RMI_SERVER_PORT = "44444";
    public static final String MAX_RMI_SERVER_PORT = "65534";
    public static final String MIN_HTTP_PORT = "9080";
    public static final String MAX_HTTP_PORT = "9999";
    public static final String MIN_RMI_REG_PORT = "1099";
    public static final String MAX_RMI_REG_PORT = "9999";
    public static final String MIN_SSH_PORT = "8101";
    public static final String MAX_SSH_PORT = "8888";

    static final Long COMMAND_TIMEOUT = 360000L;
    static final Long SERVICE_TIMEOUT = 360000L;
    static final long BUNDLE_TIMEOUT = 360000L;

    private static Logger LOG = LoggerFactory.getLogger(KarafTestSupport.class);

    @Rule
    public KarafTestWatcher baseTestWatcher = new KarafTestWatcher();

    ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featureService;

    @Inject
    protected SessionFactory sessionFactory;

    @Inject
    protected ConfigurationAdmin configurationAdmin;


    /**
     * To make sure the tests run only when the boot features are fully installed
     */
    @Inject
    BootFinished bootFinished;

    public static class Retry implements TestRule {
        private static boolean retry = true;

        public Retry(boolean retry) {
            Retry.retry = retry;
        }

        public Statement apply(Statement base, Description description) {
            return statement(base, description);
        }

        private Statement statement(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // implement retry logic here
                    // retry once to honor the FeatureService refresh
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        LOG.debug(t.getMessage(), t);
                        if (retry && !(t instanceof org.junit.AssumptionViolatedException)) {
                            retry = false;
                            throw new RerunTestException("rerun this test pls", t);
                        } else {
                            throw t;
                        }
                    }

                }
            };
        }
    }

    @Rule
    public Retry retry = new Retry(true);

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

    /**
     * Override this method if you want to change the Karaf distribution in use.
     */
    public MavenArtifactUrlReference getKarafDistribution() {
        return CoreOptions.maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");
    }

    @Configuration
    public Option[] config() {
        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));
        String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        if (localRepository == null) {
            localRepository = "";
        }

        // org.ops4j.pax.exam.spi.PaxExamRuntime.defaultTestSystemOptions() does it implicitly when pax.exam.system = test
        ConfigurationManager cm = new ConfigurationManager();
        String logging = cm.getProperty(org.ops4j.pax.exam.Constants.EXAM_LOGGING_KEY, org.ops4j.pax.exam.Constants.EXAM_LOGGING_PAX_LOGGING);
        Option[] examOptions = new Option[] {
                bootDelegationPackage("sun.*"),
                frameworkStartLevel(org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                when(logging.equals(org.ops4j.pax.exam.Constants.EXAM_LOGGING_PAX_LOGGING)).useOptions(
                        url("link:classpath:META-INF/links/org.ops4j.pax.logging.api.link").startLevel(START_LEVEL_SYSTEM_BUNDLES)),

                url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                // but we don't want geronimo atinject...
//                url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES)
                // we want SMX inject 1_3
                url("link:classpath:org.apache.servicemix.bundles.javax-inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES)
        };

        Option[] testOptions = null;
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            testOptions = new Option[] {
                // debugConfiguration("8889", true),
                KarafDistributionOption.karafDistributionConfiguration().frameworkUrl(getKarafDistribution()).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                KarafDistributionOption.configureSecurity().disableKarafMBeanServerBuilder(),
                KarafDistributionOption.configureConsole().ignoreLocalConsole(),
                KarafDistributionOption.keepRuntimeFolder(),
                KarafDistributionOption.logLevel(LogLevel.INFO),
                CoreOptions.systemTimeout(3600000),
                RBCRemoteTargetOptions.waitForRBCFor(3600000),
                CoreOptions.mavenBundle().groupId("org.awaitility").artifactId("awaitility").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),
                //replaceConfigurationFile("etc/host.key", getConfigFile("/etc/host.key")),
                KarafDistributionOption.replaceConfigurationFile("etc/users.properties", getConfigFile("/etc/users.properties")),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "updateSnapshots", "none"),
                KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepository),
                KarafDistributionOption.editConfigurationFilePut("etc/branding.properties", "welcome", ""), // No welcome banner
                KarafDistributionOption.editConfigurationFilePut("etc/branding-ssh.properties", "welcome", ""),
                new VMOption("--add-reads=java.xml=java.logging"),
                new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                new VMOption("--patch-module"),
                new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-"
                    + System.getProperty("karaf.version") + ".jar"),
                new VMOption("--patch-module"), new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-"
                    + System.getProperty("karaf.version") + ".jar"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.security=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.net=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.lang=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.util=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.file=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.ftp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.content.text=ALL-UNNAMED"),
                new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                new VMOption("-classpath"),
                new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"
                    + File.pathSeparator + "lib/endorsed/*")
                
            };
        } else {
            testOptions = new Option[] {
                //debugConfiguration("8889", true),
                KarafDistributionOption.karafDistributionConfiguration().frameworkUrl(getKarafDistribution()).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                KarafDistributionOption.configureSecurity().disableKarafMBeanServerBuilder(),
                KarafDistributionOption.configureConsole().ignoreLocalConsole(),
                KarafDistributionOption.keepRuntimeFolder(),
                KarafDistributionOption.logLevel(LogLevel.INFO),
                CoreOptions.systemTimeout(3600000),
                RBCRemoteTargetOptions.waitForRBCFor(3600000),
                CoreOptions.mavenBundle().groupId("org.awaitility").artifactId("awaitility").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").versionAsInProject(),
                //replaceConfigurationFile("etc/host.key", getConfigFile("/etc/host.key")),
                KarafDistributionOption.replaceConfigurationFile("etc/users.properties", getConfigFile("/etc/users.properties")),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "updateSnapshots", "none"),
                KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
                KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepository),
                KarafDistributionOption.editConfigurationFilePut("etc/branding.properties", "welcome", ""), // No welcome banner
                KarafDistributionOption.editConfigurationFilePut("etc/branding-ssh.properties", "welcome", "")
            };
        }

        return combine(examOptions, testOptions);
    }

    public static int getAvailablePort(int min, int max) {
        for (int i = min; i <= max; i++) {
            try (ServerSocket socket = new ServerSocket(i)) {
                return socket.getLocalPort();
            } catch (Exception e) {
                System.err.println("Port " + i + " not available, trying next one");
                continue; // try next port
            }
        }
        throw new IllegalStateException("Can't find available network ports");
    }

    /**
     * Executes a shell command and returns output as a String.
     *
     * @param command The command to execute
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    public String executeCommand(final String command, Principal ... principals) {
        return executeCommand(command, COMMAND_TIMEOUT, false, principals);
    }

    /**
     * Executes a shell command and returns output as a String.
     *
     * @param command    The command to execute.
     * @param timeout    The amount of time in millis to wait for the command to execute.
     * @param silent     Specifies if the command should be displayed in the screen.
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    public String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal ... principals) {
        waitForCommandService(command);

        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final SessionFactory sessionFactory = getOsgiService(SessionFactory.class);
        final Session session = sessionFactory.create(System.in, printStream, System.err);

        final Callable<String> commandCallable = () -> {
            try {
                if (!silent) {
                    System.err.println(command);
                }
                Object result = session.execute(command);
                if (result != null) {
                    session.getConsole().println(result.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            printStream.flush();
            return byteArrayOutputStream.toString();
        };

        FutureTask<String> commandFuture;
        if (principals.length == 0) {
            commandFuture = new FutureTask<>(commandCallable);
        } else {
            // If principals are defined, run the command callable via Subject.doAs()
            commandFuture = new FutureTask<>(() -> {
                Subject subject = new Subject();
                subject.getPrincipals().addAll(Arrays.asList(principals));
                return Subject.doAs(subject, (PrivilegedExceptionAction<String>) commandCallable::call);
            });
        }

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? (e.getCause().getCause() != null ? e.getCause().getCause() : e.getCause()) : e;
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return response;
    }

    public void assertServiceAvailable(String type) {
        Assert.assertNotNull(getOsgiService(type));
    }

    public void assertServiceAvailable(Class type) {
        Assert.assertNotNull(getOsgiService(type));
    }

    public void assertServiceAvailable(Class type, long timeout) {
        Assert.assertNotNull(getOsgiService(type, timeout));
    }

    public void assertServiceAvailable(Class type, String filter, long timeout) {
        Assert.assertNotNull(getOsgiService(type, filter, timeout));
    }

    public <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    public <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    public Object getOsgiService(String type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object getOsgiService(String type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = tracker.waitForService(timeout);
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
            return svc;
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> T getOsgiService(Class<T> type, String filter, long timeout) {
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
        String scope = (colonIndx > 0) ? command.substring(0, colonIndx) : "*";
        String name  = (colonIndx > 0) ? command.substring(colonIndx + 1) : command;
        try {
            long start = System.currentTimeMillis();
            long cur   = start;
            while (cur - start < SERVICE_TIMEOUT) {
                if (sessionFactory.getRegistry().getCommand(scope, name) != null) {
                    return;
                }
                Thread.sleep(100);
                cur = System.currentTimeMillis();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void waitForService(String filter, long timeout) throws InvalidSyntaxException, InterruptedException {
        ServiceTracker<Object, Object> st = new ServiceTracker<>(bundleContext, bundleContext.createFilter(filter), null);
        try {
            st.open();
            st.waitForService(timeout);
        } finally {
            st.close();
        }
    }

    public Bundle waitBundleState(String symbolicName, int state) {
        long endTime = System.currentTimeMillis() + BUNDLE_TIMEOUT;
        while (System.currentTimeMillis() < endTime) {
            Bundle bundle = findBundleByName(symbolicName);
            if (bundle != null && bundle.getState() == state) {
                return bundle;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        Assert.fail("Manadatory bundle " + symbolicName + " not found.");
        throw new IllegalStateException("Should not be reached");
    }

    /*
     * Explode the dictionary into a ,-delimited list of key=value pairs
     */
    @SuppressWarnings("rawtypes")
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuilder result = new StringBuilder();
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
        return references != null ? Arrays.asList(references) : Collections.emptyList();
    }

    public JMXConnector getJMXConnector() throws Exception {
        return getJMXConnector("karaf", "karaf");
    }

    public JMXConnector getJMXConnector(String userName, String passWord) throws Exception {
        JMXServiceURL url = new JMXServiceURL(getJmxServiceUrl());
        Hashtable<String, Object> env = new Hashtable<>();
        String[] credentials = new String[]{ userName, passWord };
        env.put("jmx.remote.credentials", credentials);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        return connector;
    }

    public String getJmxServiceUrl() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.management", null);
        if (configuration != null) {
            return configuration.getProcessedProperties(null).get("serviceUrl").toString();
        }
        return "service:jmx:rmi:///jndi/rmi://localhost:" + MIN_RMI_SERVER_PORT + "/karaf-root";
    }

    public String getSshPort() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.shell", null);
        if (configuration != null) {
            return configuration.getProcessedProperties(null).get("sshPort").toString();
        }
        return "8101";
    }

    public String getHttpPort() throws Exception {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.web", null);
        if (configuration != null) {
            return configuration.getProcessedProperties(null).get("org.osgi.service.http.port").toString();
        }
        return "8181";
    }

    public void assertFeatureInstalled(String featureName) throws Exception {
        String name;
        String version;
        if (featureName.contains("/")) {
            name = featureName.substring(0, featureName.indexOf("/"));
            version = featureName.substring(featureName.indexOf("/") + 1);
        } else {
            name = featureName;
            version = null;
        }
        assertFeatureInstalled(name, version);
    }

    public void assertFeatureInstalled(String featureName, String featureVersion) throws Exception {
        Feature featureToAssert = featureService.getFeatures(featureName, featureVersion)[0];
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureToAssert.equals(feature)) {
                return;
            }
        }

        Assert.fail("Feature " + featureName + (featureVersion != null ? "/" + featureVersion : "") + " should be installed but is not");
    }

    public void assertFeaturesInstalled(String ... expectedFeatures) throws Exception {
        Set<String> expectedFeaturesSet = new HashSet<>(Arrays.asList(expectedFeatures));
        Feature[] features = featureService.listInstalledFeatures();
        Set<String> installedFeatures = new HashSet<>();
        for (Feature feature : features) {
            installedFeatures.add(feature.getName());
        }
        String msg = "Expecting the following features to be installed : " + expectedFeaturesSet + " but found " + installedFeatures;
        Assert.assertTrue(msg, installedFeatures.containsAll(expectedFeaturesSet));
    }

    public void assertFeatureNotInstalled(String featureName) throws Exception {
        String name;
        String version;
        if (featureName.contains("/")) {
            name = featureName.substring(0, featureName.indexOf("/"));
            version = featureName.substring(featureName.indexOf("/") + 1);
        } else {
            name = featureName;
            version = null;
        }
        assertFeatureNotInstalled(name, version);
    }

    public void assertFeatureNotInstalled(String featureName, String featureVersion) throws Exception {
        Feature featureToAssert = featureService.getFeatures(featureName, featureVersion)[0];
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureToAssert.equals(feature)) {
                Assert.fail("Feature " + featureName + (featureVersion != null ? "/" + featureVersion : "") + " is installed whereas it should not be");
            }
        }
    }

    public void assertContains(String expectedPart, String actual) {
        Assert.assertTrue("Should contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

    public void assertContainsNot(String expectedPart, String actual) {
        Assert.assertFalse("Should not contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

    public void assertBundleInstalled(String name) {
        Assert.assertNotNull("Bundle " + name + " should be installed", findBundleByName(name));
    }

    public void assertBundleNotInstalled(String name) {
        Assert.assertNull("Bundle " + name + " should not be installed", findBundleByName(name));
    }

    public void installBundle(String bundleLocation, boolean start) throws Exception {
        Bundle bundle = bundleContext.installBundle(bundleLocation);
        if (start) {
            bundle.start();
        }
    }

    public Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    public void addFeaturesRepository(String featuresRepository) throws Exception {
        featureService.addRepository(new URI(featuresRepository));
    }

    public void installAndAssertFeature(String feature) throws Exception {
        featureService.installFeature(feature, NO_AUTO_REFRESH);
        assertFeatureInstalled(feature);
    }

    public void installAssertAndUninstallFeature(String feature, String version) throws Exception {
        installAssertAndUninstallFeatures(feature + "/" + version);
    }

    public void installAssertAndUninstallFeatures(String... feature) throws Exception {
        boolean success = false;
        Set<String> features = new HashSet<>(Arrays.asList(feature));
        try {
            System.out.println("Installing " + features);
            featureService.installFeatures(features, NO_AUTO_REFRESH);
            for (String curFeature : feature) {
                assertFeatureInstalled(curFeature);
            }
            success = true;
        } finally {
            System.out.println("Uninstalling " + features);
            try {
                featureService.uninstallFeatures(features, NO_AUTO_REFRESH);
            } catch (Exception e) {
                if (success) {
                    throw e;
                }
            }
        }
    }

    /**
     * The feature service does not uninstall feature dependencies when uninstalling a single feature.
     * So we need to make sure we uninstall all features that were newly installed.
     *
     * @param featuresBefore
     * @throws Exception
     */
    public void uninstallNewFeatures(Set<Feature> featuresBefore) throws Exception {
        Feature[] features = featureService.listInstalledFeatures();
        Set<String> uninstall = new HashSet<>();
        for (Feature curFeature : features) {
            if (!featuresBefore.contains(curFeature)) {
                uninstall.add(curFeature.getId());
            }
        }
        try {
            System.out.println("Uninstalling " + uninstall);
            featureService.uninstallFeatures(uninstall, NO_AUTO_REFRESH);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void close(Closeable closeAble) {
        if (closeAble != null) {
            try {
                closeAble.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
