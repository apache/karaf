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
package org.apache.karaf.itests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import aQute.lib.strings.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JtaTest extends BaseTest {

    @Configuration
    public Option[] config() {
        File originalConfig = new File("../../assemblies/features/base/src/main/filtered-resources/resources/etc/config.properties");
        Properties conf = new Properties();
        try (FileReader fr = new FileReader(originalConfig)) {
            conf.load(fr);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        String bd = conf.getProperty("org.osgi.framework.bootdelegation");
        String[] packages = bd.split("\\s*,\\s*");
        String[] filtered = Arrays.stream(packages).filter(p -> !p.contains("jakarta.transaction")).toArray(String[]::new);

        // unfortunately this doesn't work. editConfigurationFilePut() for config.properties is overwritten by
        // bootDelegation() options which always come last
        // so I can't perform tests without boot delegation of jakarta.transaction packages
        return combine(super.config(), editConfigurationFilePut("etc/config.properties", "org.osgi.framework.bootdelegation", Strings.join(", ", filtered)));
    }

    @Test
    public void noSpecialFeatures() throws Exception {
        ClassLoader cl = FrameworkUtil.getBundle(this.getClass()).adapt(BundleWiring.class).getClassLoader();

        if (isJDK8OrEarlier()) {
            // these classes should be boot delegated because they should be part of JDK8, all used ONLY
            // in com.sun.corba.se.impl.javax.rmi.CORBA.Util.mapSystemException()
            ensureLoadedFromSystem(cl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromSystem(cl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromSystem(cl, "jakarta.transaction.TransactionRolledbackException");
        } else {
            // JDK9+ doesn't provide jakarta.transaction package at all
            ensureNotFound(cl, "jakarta.transaction.InvalidTransactionException");
            ensureNotFound(cl, "jakarta.transaction.TransactionRequiredException");
            ensureNotFound(cl, "jakarta.transaction.TransactionRolledbackException");
        }

        // whatever the JDK, these classes should be available
        ensureLoadedFromSystem(cl, "javax.transaction.xa.XAException");
        ensureLoadedFromSystem(cl, "javax.transaction.xa.XAResource");
        ensureLoadedFromSystem(cl, "javax.transaction.xa.Xid");

        // whatever the JDK, these classes should NOT be available
        ensureNotFound(cl, "jakarta.transaction.UserTransaction");
        ensureNotFound(cl, "jakarta.transaction.TransactionManager");
    }

    @Test
    public void jakartaTransaction2_0() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.features/enterprise/" + System.getProperty("karaf.version") + "/xml/features");
        // this feature installs jakarta.transaction/jakarta.transaction-api/2.0.1
        installAndAssertFeature("transaction-api");

        ClassLoader myCl = FrameworkUtil.getBundle(this.getClass()).adapt(BundleWiring.class).getClassLoader();
        ClassLoader jtaCl = FrameworkUtil.getBundle(myCl.loadClass("jakarta.transaction.UserTransaction")).adapt(BundleWiring.class).getClassLoader();

        if (isJDK8OrEarlier()) {
            // these classes should be boot delegated
            ensureLoadedFromSystem(myCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRolledbackException");
        } else {
            // these classes ARE boot delegated, but can't be found in JDK, so they're loaded from the API bundle
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRolledbackException");
        }

        // whatever the JDK, these classes should be available from system CL, even if jakarta.transaction-api/2.0.1
        // exports javax.transaction.xa package
        ensureLoadedFromSystem(myCl, "javax.transaction.xa.XAException");
        ensureLoadedFromSystem(myCl, "javax.transaction.xa.XAResource");
        ensureLoadedFromSystem(myCl, "javax.transaction.xa.Xid");
        ensureLoadedFromSystem(jtaCl, "javax.transaction.xa.XAException");
        ensureLoadedFromSystem(jtaCl, "javax.transaction.xa.XAResource");
        ensureLoadedFromSystem(jtaCl, "javax.transaction.xa.Xid");

        // these classes should be loaded from JTA API bundle
        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionManager");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.TransactionManager");
    }

    @Test
    public void jakartaTransaction2_0AndDBCP2() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.features/enterprise/" + System.getProperty("karaf.version") + "/xml/features");
        // this feature installs jakarta.transaction/jakarta.transaction-api/2.0.1
        installAndAssertFeature("transaction-api");

        Bundle pool2 = bundleContext.installBundle("mvn:org.apache.commons/commons-pool2/2.9.0");
        // DBCP2 has:
        //  - Import-Package: jakarta.transaction;version="2.0"
        //  - Import-Package: javax.transaction.xa;version="1.1";partial=true;mandatory:=partial
        // Karaf provides special Export-Package: javax.transaction.xa;version="1.1";partial=true;mandatory:=partial
        // from system bundle just for DBCP2
        // jakarta.transaction package is exported from system bundle (in JDK8) to prevent wiring this package
        // requirement to system bundle - full JTA API is required and jakarta.transaction-api/2.0.1 does this
        Bundle dbcp2 = bundleContext.installBundle("mvn:org.apache.commons/commons-dbcp2/2.8.0");
        boolean resolved = bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Collections.singletonList(dbcp2));
        assertTrue(resolved);

        ClassLoader myCl = FrameworkUtil.getBundle(this.getClass()).adapt(BundleWiring.class).getClassLoader();
        ClassLoader dbcp2Cl = dbcp2.adapt(BundleWiring.class).getClassLoader();
        ClassLoader jtaCl = FrameworkUtil.getBundle(myCl.loadClass("jakarta.transaction.UserTransaction")).adapt(BundleWiring.class).getClassLoader();

        if (isJDK8OrEarlier()) {
            // these classes should be boot delegated
            ensureLoadedFromSystem(myCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRolledbackException");
        } else {
            // these classes ARE boot delegated, but can't be found in JDK, so they're loaded from the API bundle
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRolledbackException");
        }

        // These classes should come from system - whether loaded from my, jta or dbcp2 CL, even if dbcp2 wires
        // directly to system through Import-Package: javax.transaction.xa;partial=true;mandatory:=partial
        // It works because:
        //  - jakarta.transaction-api/2.0.1 provides the jakarta.transaction package
        //  - javax.transaction.xa is a JDK package and always comes from the system bundle
        // see:
        // - https://github.com/ops4j/org.ops4j.pax.transx/issues/33
        // - https://issues.apache.org/jira/browse/DBCP-571
        ensureLoadedFromSystem(myCl, "javax.transaction.xa.Xid");
        ensureLoadedFromSystem(jtaCl, "javax.transaction.xa.Xid");
        ensureLoadedFromSystem(dbcp2Cl, "javax.transaction.xa.Xid");

        // these classes should be loaded from JTA API bundle
        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionManager");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.TransactionManager");
        ensureLoadedFromCl(dbcp2Cl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(dbcp2Cl, jtaCl, "jakarta.transaction.TransactionManager");
    }

    @Test
    public void jakartaTransaction2_0_1AndDBCP2() throws Exception {
        // this set of bundles matches Karaf's transaction-api feature, using jakarta.transaction-api/2.0.1
        bundleContext.installBundle("mvn:jakarta.interceptor/jakarta.interceptor-api/2.1.0");
        bundleContext.installBundle("mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.javax-inject/1_2");
        bundleContext.installBundle("mvn:jakarta.el/jakarta.el-api/3.0.3");
        bundleContext.installBundle("mvn:jakarta.enterprise/jakarta.enterprise.cdi-api/4.0.1");
        // this bundle doesn't have Require-Bundle: system.bundle, but jakarta.transaction package (the 3 exception
        // classes provided by JDK8) is still bootdelegated by Karaf
        Bundle jta = bundleContext.installBundle("mvn:jakarta.transaction/jakarta.transaction-api/2.0.1");

        Bundle pool2 = bundleContext.installBundle("mvn:org.apache.commons/commons-pool2/2.9.0");
        Bundle dbcp2 = bundleContext.installBundle("mvn:org.apache.commons/commons-dbcp2/2.8.0");
        // this won't resolve if system bundle doesn't export javax.transaction.xa package without mandatory "partial"
        // attribute because jakarta.transaction-api/2.0.1 exporting jakarta.transaction is needed to resolve dbcp2
        boolean resolved = bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Collections.singletonList(dbcp2));
        assertTrue(resolved);

        ClassLoader myCl = FrameworkUtil.getBundle(this.getClass()).adapt(BundleWiring.class).getClassLoader();
        ClassLoader dbcp2Cl = dbcp2.adapt(BundleWiring.class).getClassLoader();
        ClassLoader jtaCl = FrameworkUtil.getBundle(myCl.loadClass("jakarta.transaction.UserTransaction")).adapt(BundleWiring.class).getClassLoader();

        if (isJDK8OrEarlier()) {
            // these classes should be boot delegated
            ensureLoadedFromSystem(myCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromSystem(myCl, "jakarta.transaction.TransactionRolledbackException");
        } else {
            // these classes ARE boot delegated, but can't be found in JDK, so they're loaded from the API bundle
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.InvalidTransactionException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRequiredException");
            ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionRolledbackException");
        }

        ensureLoadedFromSystem(jtaCl, "javax.transaction.xa.Xid");
        ensureLoadedFromSystem(myCl, "javax.transaction.xa.Xid");
        ensureLoadedFromSystem(dbcp2Cl, "javax.transaction.xa.Xid");

        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(myCl, jtaCl, "jakarta.transaction.TransactionManager");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(jtaCl, jtaCl, "jakarta.transaction.TransactionManager");
        ensureLoadedFromCl(dbcp2Cl, jtaCl, "jakarta.transaction.UserTransaction");
        ensureLoadedFromCl(dbcp2Cl, jtaCl, "jakarta.transaction.TransactionManager");
    }

    private void ensureLoadedFromSystem(ClassLoader cl, String className) {
        try {
            Class<?> c = cl.loadClass(className);
            assertTrue(c != null && (c.getClassLoader() == null || c.getClassLoader().getClass().getName().contains("jdk.internal")));
        } catch (ClassNotFoundException e) {
            fail("Can't load " + className);
        }
    }

    private void ensureLoadedFromCl(ClassLoader initiatingCl, ClassLoader loadingCl, String className) {
        try {
            Class<?> c = initiatingCl.loadClass(className);
            assertTrue(c != null && c.getClassLoader() == loadingCl);
        } catch (ClassNotFoundException e) {
            fail("Can't load " + className);
        }
    }

    private void ensureNotFound(ClassLoader cl, String className) {
        try {
            Class<?> c = cl.loadClass(className);
            fail("Class " + className + " should not be available");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private boolean isJDK8OrEarlier() {
        String v = System.getProperty("java.specification.version");
        try {
            if (v.contains(".")) {
                float f = Float.parseFloat(v);
                return f < 1.9F;
            } else {
                int i = Integer.parseInt(v);
                return i < 9;
            }
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

}
