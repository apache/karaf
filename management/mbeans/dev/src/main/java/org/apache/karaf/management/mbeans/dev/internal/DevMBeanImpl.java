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

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

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
        Properties properties = new Properties(new File(System.getProperty("karaf.base"), "etc/config.properties"));
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

}
