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
package org.apache.karaf.system.management.internal;

import org.apache.karaf.system.FrameworkType;
import org.apache.karaf.system.SystemService;
import org.apache.karaf.system.management.SystemMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * System MBean implementation.
 */
public class SystemMBeanImpl extends StandardMBean implements SystemMBean {

    private SystemService systemService;
    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public SystemMBeanImpl() throws NotCompliantMBeanException {
        super(SystemMBean.class);
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public SystemService getSystemService() {
        return this.systemService;
    }

    public void halt() throws MBeanException {
        try {
            systemService.halt();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void halt(String time) throws MBeanException {
        try {
            systemService.halt(time);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void reboot() throws MBeanException {
        try {
            systemService.reboot();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void reboot(String time) throws MBeanException {
        try {
            systemService.reboot(time, SystemService.Swipe.NONE);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void rebootCleanCache(String time) throws MBeanException {
        try {
            systemService.reboot(time, SystemService.Swipe.CACHE);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void rebootCleanAll(String time) throws MBeanException {
        try {
            systemService.reboot(time, SystemService.Swipe.ALL);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public void setStartLevel(int startLevel) throws MBeanException {
        try {
            systemService.setStartLevel(startLevel);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public int getStartLevel() throws MBeanException {
        try {
            return systemService.getStartLevel();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public String getFramework() {
        return this.systemService.getFramework().toString();
    }

    @Override
    public void setFramework(String framework) {
        this.systemService.setFramework(FrameworkType.valueOf(framework.toLowerCase()));
    }

    @Override
    public void setFrameworkDebug(boolean debug) {
        this.systemService.setFrameworkDebug(debug);
    }

    @Override
    public String getName() {
        return this.systemService.getName();
    }

    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Instance name can't be null or empty");
        }
        this.systemService.setName(name);
    }

    @Override
    public String getVersion() {
        return this.systemService.getVersion();
    }

    @Override
    public Map<String, String> getProperties(boolean unset, boolean dumpToFile) throws MBeanException {
        try {
            Map<String, String> result = new HashMap<>();

            Properties props = (Properties) java.lang.System.getProperties().clone();

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
                ps.println("#Dump executed at " + new SimpleDateFormat().format(new Date()));
                printOrderedProperties(props, ps);
                ps.flush();
                ps.close();
            } else {
                printOrderedProperties(props, result);
            }

            return result;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    private void printOrderedProperties(Properties props, PrintStream out) {
        Set<Object> keys = props.keySet();
        List<String> order = new ArrayList<>(keys.size());
        for (Object str : keys) {
            order.add((String) str);
        }
        Collections.sort(order);
        for (String key : order) {
            out.println(key + "=" + props.getProperty(key));
        }
    }

    private void printOrderedProperties(Properties props, Map<String, String> result) {
        Set<Object> keys = props.keySet();
        List<String> order = new ArrayList<>(keys.size());
        for (Object str : keys) {
            order.add((String) str);
        }
        Collections.sort(order);
        for (String key : order) {
            result.put(key, props.getProperty(key));
        }
    }

    private void setProperty(Properties props, String key, String def) {
        String val = bundleContext.getProperty(key);
        if (val == null && def != null) {
            props.setProperty(key, def);
        } else if (val != null) {
            props.setProperty(key, val);
        }
    }

    @Override
    public String getProperty(String key) {
        return java.lang.System.getProperty(key);
    }

    @Override
    public void setProperty(String key, String value, boolean persistent) {
        systemService.setSystemProperty(key, value, persistent);
    }

}
