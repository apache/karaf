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
package org.apache.karaf.dev.core.internal;

import java.io.File;
import java.io.IOException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.dev.core.DevService;
import org.apache.karaf.dev.core.FrameworkType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class DevServiceImpl implements DevService {
    private BundleContext bundleContext;

    public DevServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FrameworkType getFramework() {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return FrameworkType.felix;
        } else {
            return FrameworkType.equinox;
        }
    }

    private Properties loadProps() throws IOException {
        return new Properties(new File(System.getProperty("karaf.base"), "etc/config.properties"));
    }

    public void setFramework(FrameworkType framework) {
        if (framework == null) {
            return;
        }
        try {
            Properties properties = loadProps();
            properties.put("karaf.framework", framework.name());
            properties.save();
        } catch (IOException e) {
            throw new RuntimeException("Error settting framework: " + e.getMessage(), e);
        }
    }

    public void setFrameworkDebug(boolean debug) {
        try {
            Properties properties = loadProps();
            if (debug) {
                properties.put("felix.log.level", "4");
                properties.put("osgi.debug", "etc/equinox-debug.properties");
            } else {
                properties.remove("felix.log.level");
                properties.remove("osgi.debug");
            }
            // TODO populate the equinox-debug.properties file with the one provided in shell/dev module
            properties.save();
        } catch (IOException e) {
            throw new RuntimeException("Error settting framework debugging: " + e.getMessage(), e);
        }
    }

    public void restart(boolean clean) {
        System.setProperty("karaf.restart", "true");
        System.setProperty("karaf.restart.clean", Boolean.toString(clean));
        try {
            bundleContext.getBundle(0).stop();
        } catch (BundleException e) {
            throw new RuntimeException("Error stopping framework bundle: " + e.getMessage(), e);
        }
    }

    @Override
    public String setSystemProperty(String key, String value, boolean persist) {
        if (persist) {
            try {
                String base = System.getProperty("karaf.base");
                Properties props = new Properties(new File(base, "etc/system.properties"));
                props.put(key, value);
                props.save();
            } catch (IOException e) {
                throw new RuntimeException("Error persisting system property", e);
            }
        }
        return System.setProperty(key, value);
    }

}
