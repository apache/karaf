/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.management.mbeans.dev.internal;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.management.mbeans.dev.DevMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of the DevMBean.
 */
public class DevMBeanImpl extends StandardMBean implements DevMBean {

    private BundleContext bundleContext;

    public DevMBeanImpl() throws NotCompliantMBeanException {
        super(DevMBean.class);
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String framework() throws Exception {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return "Felix";
        } else {
            return "Equinox";
        }
    }

    public void frameworkOptions(boolean debug, String framework) throws Exception {
        Properties properties = new Properties(new File(System.getProperty("karaf.etc"), "config.properties"));
        if (framework != null) {
            // switch the framework is use
            if (!framework.equalsIgnoreCase("felix") && !framework.equalsIgnoreCase("equinox")) {
                throw new IllegalArgumentException("Unsupported framework " + framework);
            }
            properties.put("karaf.framework", framework.toLowerCase());
        }
        if (debug) {
            properties.put("felix.log.level", "4");
            properties.put("osgi.debug", "etc/equinox-debug.properties");
            // TODO populate the equinox-debug.properties file with the one provided in shell/dev module
        } else {
            properties.remove("felix.log.level");
            properties.remove("osgi.debug");
        }
        properties.save();
    }

    public void restart(boolean clean) throws Exception {
        System.setProperty("karaf.restart", "true");
        System.setProperty("karaf.restart.clean", Boolean.toString(clean));
        bundleContext.getBundle(0).stop();
    }

    public Map getProperties(boolean unset, boolean dumpToFile) throws Exception {
        Map<String, String> result = new HashMap<String, String>();

        java.util.Properties props = (java.util.Properties) System.getProperties().clone();

        String def = null;
        if (unset) {
            def = "unset";
        }

        setProperty(props, Constants.FRAMEWORK_BEGINNING_STARTLEVEL, def);
        setProperty(props, Constants.FRAMEWORK_BOOTDELEGATION, def);
        setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT, def);
        setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_APP, def);
        setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT, def);
        setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_EXT, def);
        setProperty(props, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK, def);
        setProperty(props, Constants.FRAMEWORK_EXECPERMISSION, def);
        setProperty(props, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, def);
        setProperty(props, Constants.FRAMEWORK_LANGUAGE, def);
        setProperty(props, Constants.FRAMEWORK_LIBRARY_EXTENSIONS, def);
        setProperty(props, Constants.FRAMEWORK_OS_NAME, def);
        setProperty(props, Constants.FRAMEWORK_OS_VERSION, def);
        setProperty(props, Constants.FRAMEWORK_PROCESSOR, def);
        setProperty(props, Constants.FRAMEWORK_SECURITY, def);
        setProperty(props, Constants.FRAMEWORK_SECURITY_OSGI, def);
        setProperty(props, Constants.FRAMEWORK_STORAGE, def);
        setProperty(props, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT, def);
        setProperty(props, Constants.FRAMEWORK_SYSTEMPACKAGES, def);
        setProperty(props, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, def);
        setProperty(props, Constants.FRAMEWORK_VENDOR, def);
        setProperty(props, Constants.FRAMEWORK_VERSION, def);
        setProperty(props, Constants.FRAMEWORK_WINDOWSYSTEM, def);

        setProperty(props, Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, def);
        setProperty(props, Constants.SUPPORTS_FRAMEWORK_EXTENSION, def);
        setProperty(props, Constants.SUPPORTS_FRAMEWORK_FRAGMENT, def);
        setProperty(props, Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, def);

        if (dumpToFile) {
            PrintStream ps = new PrintStream(new File(bundleContext.getProperty("karaf.data"), "dump-properties-" + java.lang.System.currentTimeMillis() + ".properties"));
            ps.println("#Dump of the System and OSGi properties");
            ps.println("#Dump execute at " + new SimpleDateFormat().format(new Date()));
            printOrderedProperties(props, ps);
            ps.flush();
            ps.close();
        } else {
            printOrderedProperties(props, result);
        }

        return result;
    }

    private void printOrderedProperties(java.util.Properties props, PrintStream out) {
        Set<Object> keys = props.keySet();
        Vector<String> order = new Vector<String>(keys.size());
        for (Iterator<Object> i = keys.iterator(); i.hasNext(); ) {
            Object str = (Object) i.next();
            order.add((String) str);
        }
        Collections.sort(order);
        for (Iterator<String> i = order.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            out.println(key + "=" + props.getProperty(key));
        }
    }

    private void printOrderedProperties(java.util.Properties props, Map<String, String> result) {
        Set<Object> keys = props.keySet();
        Vector<String> order = new Vector<String>(keys.size());
        for (Iterator<Object> i = keys.iterator(); i.hasNext(); ) {
            Object str = (Object) i.next();
            order.add((String) str);
        }
        Collections.sort(order);
        for (Iterator<String> i = order.iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            result.put(key, props.getProperty(key));
        }
    }

    private void setProperty(java.util.Properties props, String key, String def) {
        String val = bundleContext.getProperty(key);
        if (val == null && def != null) {
            props.setProperty(key, def);
        } else if (val != null) {
            props.setProperty(key, val);
        }
    }

    public String getProperty(String key) {
        return System.getProperty(key);
    }

    public void setProperty(String key, String value, boolean persistent) throws Exception {
        if (persistent) {
            String etc = System.getProperty("karaf.etc");
            Properties props = new Properties(new File(etc, "system.properties"));
            props.put(key, value);
            props.save();
        }
        System.setProperty(key, value);
    }

}
