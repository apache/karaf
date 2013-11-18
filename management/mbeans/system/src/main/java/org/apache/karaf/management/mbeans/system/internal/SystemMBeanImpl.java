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
package org.apache.karaf.management.mbeans.system.internal;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.management.mbeans.system.SystemMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * System MBean implementation.
 */
public class SystemMBeanImpl extends StandardMBean implements SystemMBean {

    private final static transient Logger LOGGER = LoggerFactory.getLogger(SystemMBeanImpl.class);

    private BundleContext bundleContext;

    public SystemMBeanImpl() throws NotCompliantMBeanException {
        super(SystemMBean.class);
    }

    public String getName() {
        return bundleContext.getProperty("karaf.name");
    }

    public void setName(String name) {
        try {
            String karafEtc = bundleContext.getProperty("karaf.etc");
            File syspropsFile = new File(karafEtc, "system.properties");
            FileInputStream fis = new FileInputStream(syspropsFile);
            Properties props = new Properties();
            props.load(fis);
            fis.close();
            props.setProperty("karaf.name", name);
            FileOutputStream fos = new FileOutputStream(syspropsFile);
            props.store(fos, "");
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getVersion() {
        return System.getProperty("karaf.version");
    }

    public void halt() throws Exception {
        halt(null);
    }

    public void halt(String time) throws Exception {
        shutdown(timeToSleep(time));
    }

    public void reboot() throws Exception {
        reboot(null, false);
    }

    public void reboot(String time, boolean cleanup) throws Exception {
        reboot(timeToSleep(time), cleanup);
    }

    public void setFrameworkDebug(boolean debug) throws Exception {
        Properties properties = new Properties(new File(System.getProperty("karaf.etc"), "config.properties"));
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

    public String getFramework() {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return "felix";
        } else {
            return "equinox";
        }
    }

    public void setFramework(String framework) throws Exception {
        Properties properties = new Properties(new File(System.getProperty("karaf.etc"), "config.properties"));
        if (!framework.equals("felix") || !framework.equals("equinox"))
            throw new IllegalArgumentException("Framework name is not supported. Only felix or equinox are supported.");
        properties.put("karaf.framework", framework);
        properties.save();
    }

    public void setStartLevel(int startLevel) {
        Bundle b = getBundleContext().getBundle(0);
        FrameworkStartLevel fsl = (FrameworkStartLevel) b.adapt(FrameworkStartLevel.class);
        fsl.setStartLevel(startLevel, null);
    }

    public int getStartLevel() {
        Bundle b = getBundleContext().getBundle(0);
        FrameworkStartLevel fsl = (FrameworkStartLevel) b.adapt(FrameworkStartLevel.class);
        return fsl.getStartLevel();
    }

    /* for backward compatibility */

    /**
     * @deprecated use halt() instead.
     */
    public void shutdown() throws Exception {
        halt();
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private void shutdown(final long sleep) {
        new Thread() {
            public void run() {
                try {
                    sleepWithMsg(sleep, "Shutdown in " + sleep / 1000 / 60 + " minute(s)");
                    getBundleContext().getBundle(0).stop();
                } catch (Exception e) {
                    LOGGER.error("Halt error", e);
                }
            }
        }.start();
    }

    private void reboot(final long sleep, final boolean clean) {
        new Thread() {
            public void run() {
                try {
                    sleepWithMsg(sleep, "Reboot in " + sleep / 1000 / 60 + " minute(s)");
                    System.setProperty("karaf.restart", "true");
                    System.setProperty("karaf.restart.clean", Boolean.toString(clean));
                    bundleContext.getBundle(0).stop();
                } catch (Exception e) {
                    LOGGER.error("Reboot error", e);
                }
            }
        }.start();
    }

    private void sleepWithMsg(final long sleep, String msg)
            throws InterruptedException {
        if (sleep > 0) {
            LOGGER.info(msg);
            System.err.println(msg);
        }
        Thread.sleep(sleep);
    }

    /**
     * Convert a time string to sleep period (in millisecond).
     *
     * @param time the time string.
     * @return the corresponding sleep period in millisecond.
     */
    private long timeToSleep(String time) throws Exception {
        long sleep = 0;
        if (time != null) {
            if (!time.equals("now")) {
                if (time.startsWith("+")) {
                    // delay in number of minutes provided
                    time = time.substring(1);
                    try {
                        sleep = Long.parseLong(time) * 60 * 1000;
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Time " + time + " is not valid");
                    }
                } else {
                    // try to parse the date in hh:mm
                    String[] strings = time.split(":");
                    if (strings.length != 2) {
                        throw new IllegalArgumentException("Time " + time + " is not valid");
                    }
                    GregorianCalendar currentDate = new GregorianCalendar();
                    GregorianCalendar shutdownDate = new GregorianCalendar(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE), Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                    if (shutdownDate.before(currentDate)) {
                        shutdownDate.set(Calendar.DATE, shutdownDate.get(Calendar.DATE) + 1);
                    }
                    sleep = shutdownDate.getTimeInMillis() - currentDate.getTimeInMillis();
                }
            }
        }
        return sleep;
    }

}
