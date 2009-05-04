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
package org.apache.felix.karaf.gshell.itests;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.configProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.profile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenConfiguration;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractIntegrationTest {

    public static final long DEFAULT_TIMEOUT = 30000;

    @Inject
    protected BundleContext bundleContext;

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
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
            tracker.open();
            // Note that the tracker is not closed to keep the reference
            // This is buggy, has the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
        return bundleContext.installBundle(mvnUrl.getURL());
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // install log service using pax runners profile abstraction (there are more profiles, like DS)
            profile("log").version("1.3.0"),
            profile("felix.config").version("1.0.10"),
            profile("spring.dm").version("1.2.0-rc1"),

            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
            systemProperty("karaf.name").value("root"),
            systemProperty("karaf.base").value("target/smx.base"),
            systemProperty("karaf.startLocalConsole").value("false"),
            systemProperty("karaf.startRemoteShell").value("false"),

            // hack system packages
            systemPackages("org.apache.felix.karaf.main.spi;version=1.0.0", "org.apache.felix.karaf.jaas.boot"),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework(),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework(),

            // Bundles
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jline"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-httpclient"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-jexl"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-vfs"),
            mavenBundle("org.apache.mina", "mina-core"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.oro"),
            mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.config"),
            mavenBundle("org.apache.sshd", "sshd-core"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.core"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.osgi"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.log").noStart(),


//            mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository"),

            scanFeatures(
                    maven().groupId("org.apache.felix.karaf").artifactId("apache-felix-karaf").type("xml").classifier("features").versionAsInProject(),
                    "obr", "wrapper"
            ),

            felix()
        );
        return options;
    }

}
