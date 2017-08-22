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
package org.apache.karaf.jaas.modules.encryption;

import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

public class EncryptionSupport {
    private final Logger logger = LoggerFactory.getLogger(EncryptionSupport.class);

    private BundleContext bundleContext;
    private Encryption encryption;
    private String encryptionPrefix;
    private String encryptionSuffix;
    private boolean debug;
    private boolean enabled;
    private String name;

    private Map<String, String> encOpts;


    public EncryptionSupport(Map<String, ?> options) {
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        encOpts = new HashMap<>();
        for (String key : options.keySet()) {
            if (key.startsWith("encryption.")) {
                encOpts.put(key.substring("encryption.".length()), options.get(key).toString());
            }
        }
        encryptionPrefix = encOpts.remove("prefix");
        encryptionSuffix = encOpts.remove("suffix");
        enabled = Boolean.parseBoolean(encOpts.remove("enabled"));
        if (!enabled) {
            if (debug) {
                logger.debug("Encryption is disabled.");
            }
        }
        name = encOpts.remove("name");
        if (debug) {
            if (name != null && name.length() > 0) {
                logger.debug("Encryption is enabled. Using service " + name + " with options " + encOpts);
            } else {
                logger.debug("Encryption is enabled. Using options " + encOpts);
            }
        }
    }

    public Encryption getEncryption() {
        if (encryption != null) {
            return encryption;
        }
        if (!enabled) {
            return null;
        }

        ServiceTracker<EncryptionService, EncryptionService> tracker = new ServiceTracker<>(bundleContext, getFilter(), null);
        tracker.open();
        try {
            return getEncryptionInternal(tracker);
        } finally {
            tracker.close();
        }
    }

    private Encryption getEncryptionInternal(ServiceTracker<EncryptionService, EncryptionService> tracker) {
        try {
            tracker.waitForService(20000);
        } catch (InterruptedException e1) {
            return null;
        }
        SortedMap<ServiceReference<EncryptionService>, EncryptionService> tracked = tracker.getTracked();
        if (tracked.isEmpty()) {
            throw new IllegalStateException(noEncryptionServiceMsg());
        }
        for (EncryptionService encryptionService : tracked.values()) {
            try {
                Encryption encr = encryptionService.createEncryption(encOpts);
                if (encr != null) {
                    this.encryption = encr;
                    return encryption;
                }
            } catch (IllegalStateException e) {
                // continue
            }
        }
        throw new IllegalStateException("No EncryptionService supporting the required options could be found.");
    }

    private String noEncryptionServiceMsg() {
        if (name != null && name.length() > 0) {
            return "Encryption service " + name + " not found. Please check that the encryption service is correctly set up.";
        } else {
            return "No encryption service found. Please install the Karaf encryption feature and check that the encryption algorithm is supported.";
        }
    }

    private org.osgi.framework.Filter getFilter() {
        String nameFilter = name != null && name.length() > 0 ? "(name=" + name + ")" : null;
        String objFilter = "(objectClass=" + EncryptionService.class.getName() + ")";
        String filter = nameFilter == null ? objFilter : "&(" + nameFilter + objFilter + ")"; 
        try {
            return FrameworkUtil.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getEncryptionSuffix() {
        return encryptionSuffix;
    }

    public void setEncryptionSuffix(String encryptionSuffix) {
        this.encryptionSuffix = encryptionSuffix;
    }

    public String getEncryptionPrefix() {
        return encryptionPrefix;
    }

    public void setEncryptionPrefix(String encryptionPrefix) {
        this.encryptionPrefix = encryptionPrefix;
    }

}
