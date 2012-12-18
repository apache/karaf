/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.jasypt.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;

import de.kalpatec.pojosr.framework.PojoServiceRegistryFactoryImpl;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;
import junit.framework.TestCase;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;

public class EncryptablePropertyPlaceholderTest extends TestCase {

    public static final long DEFAULT_TIMEOUT = 30000;

    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {

        StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
        EnvironmentStringPBEConfig env = new EnvironmentStringPBEConfig();
        env.setAlgorithm("PBEWithMD5AndDES");
        env.setPassword("password");
        enc.setConfig(env);
        String val = enc.encrypt("bar");
        System.setProperty("foo", val);

        System.setProperty("org.osgi.framework.storage", "target/osgi/" + System.currentTimeMillis());
        System.setProperty("karaf.name", "root");

        List<BundleDescriptor> bundles = new ClasspathScanner().scanForBundles("(Bundle-SymbolicName=*)");
        bundles.add(getBundleDescriptor(
                "target/jasypt.jar",
                newBundle().add("OSGI-INF/blueprint/karaf-jaas-jasypt.xml", getClass().getResource("/OSGI-INF/blueprint/karaf-jaas-jasypt.xml"))
                           .set("Manifest-Version", "2")
                           .set("Bundle-ManifestVersion", "2")
                           .set("Bundle-SymbolicName", "jasypt")
                           .set("Bundle-Version", "0.0.0")));
        bundles.add(getBundleDescriptor(
                "target/test.jar",
                newBundle().add("OSGI-INF/blueprint/test.xml", getClass().getResource("test.xml"))
                           .set("Manifest-Version", "2")
                           .set("Bundle-ManifestVersion", "2")
                           .set("Bundle-SymbolicName", "test")
                           .set("Bundle-Version", "0.0.0")));

        Map config = new HashMap();
        config.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, bundles);
        PojoServiceRegistry reg = new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry(config);
        bundleContext = reg.getBundleContext();
    }

    private BundleDescriptor getBundleDescriptor(String path, TinyBundle bundle) throws Exception {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        copy(bundle.build(), fos);
        fos.close();
        JarInputStream jis = new JarInputStream(new FileInputStream(file));
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry entry : jis.getManifest().getMainAttributes().entrySet()) {
            headers.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return new BundleDescriptor(
                getClass().getClassLoader(),
                new URL("jar:" + file.toURI().toString() + "!/"),
                headers);
    }

    @After
    public void tearDown() throws Exception {
        bundleContext.getBundle().stop();
    }

    @Test
    public void testPlaceholder() throws Exception {

        for (Bundle bundle: bundleContext.getBundles()) {
            System.out.println(bundle.getSymbolicName() + " / " + bundle.getVersion());
        }

        String encoded = getOsgiService(String.class, "(encoded=*)");
        assertEquals("bar", encoded);
    }


    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter) {
        return getOsgiService(type, filter, DEFAULT_TIMEOUT);
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

    /*
     * Provides an iterable collection of references, even if the original array is null
     */
    private static final Collection<ServiceReference> asCollection(ServiceReference[] references) {
        List<ServiceReference> result = new LinkedList<ServiceReference>();
        if (references != null) {
            for (ServiceReference reference : references) {
                result.add(reference);
            }
        }
        return result;
    }

    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        return copy(input, output, 8024);
    }

    public static long copy(final InputStream input, final OutputStream output, int buffersize) throws IOException {
        final byte[] buffer = new byte[buffersize];
        int n;
        long count=0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

}
