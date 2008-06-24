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
package org.apache.servicemix.kernel.testing.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.test.provisioning.ArtifactLocator;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class AbstractIntegrationTest extends AbstractConfigurableBundleCreatorTests {

    static {
        try {
            File f = new File("target/smx4");
            f.mkdirs();
            System.setProperty("servicemix.home", f.getAbsolutePath());
            System.setProperty("servicemix.base", f.getAbsolutePath());
            System.setProperty("org.apache.servicemix.filemonitor.configDir", new File(f, "etc").getAbsolutePath());
            System.setProperty("org.apache.servicemix.filemonitor.monitorDir", new File(f, "deploy").getAbsolutePath());
            System.setProperty("org.apache.servicemix.filemonitor.generatedJarDir", new File(f, "data/generate-bundles").getAbsolutePath());
            System.setProperty("bundles.configuration.location", new File("src/test/conf").getAbsolutePath());
            System.setProperty("org.osgi.vendor.framework", "org.apache.servicemix.kernel.testing.support");
            PropertyConfigurator.configure("target/test-classes/log4j.properties");
        } catch (Throwable t) {}
    }

    private Properties dependencies;

    @Override
    protected String getPlatformName() {
        String systemProperty = System.getProperty(OSGI_FRAMEWORK_SELECTOR);
        if (logger.isTraceEnabled())
            logger.trace("system property [" + OSGI_FRAMEWORK_SELECTOR + "] has value=" + systemProperty);

        return (systemProperty == null ? SmxKernelPlatform.class.getName() : systemProperty);
    }

    protected String getBundle(String groupId, String artifactId) {
        return groupId + "," + artifactId + "," + getBundleVersion(groupId, artifactId);
    }

    protected String getBundleVersion(String groupId, String artifactId) {
        if (dependencies == null) {
            try {
                File f = new File(System.getProperty("basedir"), "target/classes/META-INF/maven/dependencies.properties");
                Properties prop = new Properties();
                prop.load(new FileInputStream(f));
                dependencies = prop;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load dependencies informations", e);
            }
        }
        String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
        if (version == null) {
            throw new IllegalStateException("Unable to find dependency information for: " + groupId + "/" + artifactId + "/version");
        }
        return version;
    }

    protected String[] getTestFrameworkBundlesNames() {
        return new String[] {
            getBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),
            getBundle("org.apache.felix", "org.osgi.compendium"),
            getBundle("org.apache.felix", "org.apache.felix.configadmin"),
            getBundle("org.ops4j.pax.logging", "pax-logging-api"),
            getBundle("org.ops4j.pax.logging", "pax-logging-service"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.junit"),
            getBundle("org.springframework", "spring-beans"),
            getBundle("org.springframework", "spring-core"),
            getBundle("org.springframework", "spring-context"),
            getBundle("org.springframework", "spring-aop"),
            getBundle("org.springframework", "spring-test"),
            getBundle("org.springframework.osgi", "spring-osgi-core"),
            getBundle("org.springframework.osgi", "spring-osgi-io"),
            getBundle("org.springframework.osgi", "spring-osgi-extender"),
            getBundle("org.springframework.osgi", "spring-osgi-test"),
            getBundle("org.springframework.osgi", "spring-osgi-annotation"),
            getBundle("org.apache.servicemix.kernel.testing", "org.apache.servicemix.kernel.testing.support"),
		};
    }

    protected void installBundle(String groupId, String artifactId, String classifier, String type) throws Exception {
        String version = getBundleVersion(groupId, artifactId);
        File loc = localMavenBundle(groupId, artifactId, version, classifier, type);
        Bundle bundle = bundleContext.installBundle(loc.toURI().toString());
        bundle.start();
    }

    protected Resource locateBundle(String bundleId) {
        Assert.hasText(bundleId, "bundleId should not be empty");

        // parse the String
        String[] artifactId = StringUtils.commaDelimitedListToStringArray(bundleId);

        Assert.isTrue(artifactId.length >= 3, "the CSV string " + bundleId + " contains too few values");
        // TODO: add a smarter mechanism which can handle 1 or 2 values CSVs
        for (int i = 0; i < artifactId.length; i++) {
            artifactId[i] = StringUtils.trimWhitespace(artifactId[i]);
        }

        File f;
        if (artifactId.length == 3) {
            f = localMavenBundle(artifactId[0], artifactId[1], artifactId[2], null, ArtifactLocator.DEFAULT_ARTIFACT_TYPE);
        } else {
            f = localMavenBundle(artifactId[0], artifactId[1], artifactId[2], null, artifactId[3]);
        }
        return new FileSystemResource(f);
    }


    protected File localMavenBundle(String groupId, String artifact, String version, String classifier, String type) {
        String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
        File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

        StringBuffer location = new StringBuffer(groupId.replace('.', '/'));
        location.append('/');
        location.append(artifact);
        location.append('/');
        location.append(getSnapshot(version));
        location.append('/');
        location.append(artifact);
        location.append('-');
        location.append(version);
        if (classifier != null) {
            location.append('-');
            location.append(classifier);
        }
        location.append(".");
        location.append(type);

        return new File(repositoryHome, location.toString());
    }

    protected static String getSnapshot(String version) {
        if (isTimestamped(version)) {
            return version.substring(0, version.lastIndexOf('-', version.lastIndexOf('-') - 1)) + "-SNAPSHOT";
        }
        return version;
    }

    protected static boolean isTimestamped(String version) {
        return version.matches(".+-\\d\\d\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d-\\d+");
    }

    protected static boolean isSnapshot(String version) {
        return version.matches(".+-SNAPSHOT");
    }

    public <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, DEFAULT_WAIT_TIME);
    }

    public <T> T getOsgiService(Class<T> type, long timeout) {
        // translate from seconds to miliseconds
        long time = timeout * 1000;

        // use the counter to make sure the threads block
        final Counter counter = new Counter("waitForOsgiService on bnd=" + type.getName());

        counter.increment();

        final List<T> services = new ArrayList<T>();

        ServiceListener listener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    services.add((T) bundleContext.getService(event.getServiceReference()));
                    counter.decrement();
                }
            }
        };

        String filter = OsgiFilterUtils.unifyFilter(type.getName(), null);
        OsgiListenerUtils.addServiceListener(bundleContext, listener, filter);

        if (logger.isDebugEnabled())
            logger.debug("start waiting for OSGi service=" + type.getName());

        try {
            if (counter.waitForZero(time)) {
                logger.warn("waiting for OSGi service=" + type.getName() + " timed out");
                throw new RuntimeException("Gave up waiting for OSGi service '" + type.getName() + "' to be created");
            }
            else if (logger.isDebugEnabled()) {
                logger.debug("found OSGi service=" + type.getName());
            }
            return services.get(0);
        }
        finally {
            // inform waiting thread
            bundleContext.removeServiceListener(listener);
        }
    }

    protected void checkBundleStarted(String name) {
        assertNotNull(bundleContext);
        for (int i = 0; i < bundleContext.getBundles().length; i++) {
            Bundle b = bundleContext.getBundles()[i];
            if (b.getSymbolicName().equals(name)) {
                assertEquals("Bundle '" + name + "' is not active", Bundle.ACTIVE, b.getState());
                return;
            }
        }
        fail("Bundle '" + name + "' not found");
    }


}