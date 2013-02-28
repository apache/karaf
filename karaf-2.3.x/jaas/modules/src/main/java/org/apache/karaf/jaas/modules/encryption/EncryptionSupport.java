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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EncryptionSupport {

    private final Logger logger = LoggerFactory.getLogger(EncryptionSupport.class);

    private BundleContext bundleContext;

    private Encryption encryption;
    private String encryptionPrefix;
    private String encryptionSuffix;
    protected Map<String, ?> options;

    protected boolean debug;


    public EncryptionSupport(Map<String, ?> options) {
        this.options = options;
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
    }

    public Encryption getEncryption() {
        if (encryption == null) {
            Map<String, String> encOpts = new HashMap<String, String>();
            for (String key : options.keySet()) {
                if (key.startsWith("encryption.")) {
                    encOpts.put(key.substring("encryption.".length()), options.get(key).toString());
                }
            }
            encryptionPrefix = encOpts.remove("prefix");
            encryptionSuffix = encOpts.remove("suffix");
            boolean enabled = Boolean.parseBoolean(encOpts.remove("enabled"));

            if (!enabled) {
                if (debug) {
                    logger.debug("Encryption is disabled.");
                }
            } else {
                String name = encOpts.remove("name");
                if (debug) {
                    if (name != null && name.length() > 0) {
                        logger.debug("Encryption is enabled. Using service " + name + " with options " + encOpts);
                    } else {
                        logger.debug("Encryption is enabled. Using options " + encOpts);
                    }
                }
                // lookup the encryption service reference
                ServiceReference[] encryptionServiceReferences;
                try {
                    encryptionServiceReferences = bundleContext.getServiceReferences(
                            EncryptionService.class.getName(),
                            name != null && name.length() > 0 ? "(name=" + name + ")" : null);
                } catch (InvalidSyntaxException e) {
                    throw new IllegalStateException("The encryption service filter is not well formed.", e);
                }
                if (encryptionServiceReferences == null || encryptionServiceReferences.length == 0) {
                    if (name != null && name.length() > 0) {
                        throw new IllegalStateException("Encryption service " + name + " not found. Please check that the encryption service is correctly set up.");
                    } else {
                        throw new IllegalStateException("No encryption service found. Please install the Karaf encryption feature and check that the encryption algorithm is supported..");
                    }
                }
                Arrays.sort(encryptionServiceReferences);
                for (ServiceReference ref : encryptionServiceReferences) {
                    try {
                        EncryptionService encryptionService = (EncryptionService) bundleContext.getService(ref);
                        if (encryptionService != null) {
                            try {
                                encryption = encryptionService.createEncryption(encOpts);
                                if (encryption != null) {
                                    break;
                                }
                            } finally {
                                bundleContext.ungetService(ref);
                            }
                        }
                    } catch (IllegalStateException e) {
                        // continue
                    }
                }
                if (encryption == null) {
                    throw new IllegalStateException("No EncryptionService supporting the required options could be found.");
                }
            }
        }
        return encryption;
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
