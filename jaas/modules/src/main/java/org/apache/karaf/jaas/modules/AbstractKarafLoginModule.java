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
package org.apache.karaf.jaas.modules;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


/**
 * <p>
 * Abstract JAAS login module extended by all Karaf Login Modules.
 * </p>
 * 
 * @author iocanel, jbonofre
 */
public abstract class AbstractKarafLoginModule implements LoginModule {

    protected Set<Principal> principals = new HashSet<Principal>();
    protected Subject subject;
    protected String user;
    protected CallbackHandler callbackHandler;
    protected boolean debug;
    protected Map<String, ?> options;

    protected String rolePolicy;
    protected String roleDiscriminator;
    
    /** the bundle context is required to use the encryption service */
    protected BundleContext bundleContext;

    private Encryption encryption;
    private String encryptionPrefix;
    private String encryptionSuffix;

    private static final Log LOG = LogFactory.getLog(AbstractKarafLoginModule.class);

    public boolean commit() throws LoginException {
        RolePolicy policy = RolePolicy.getPolicy(rolePolicy);
        if (policy != null && roleDiscriminator != null) {
            policy.handleRoles(subject, principals, roleDiscriminator);
        } else {
            subject.getPrincipals().addAll(principals);
        }
        return true;
    }

    protected void clear() {
        user = null;
    }

    public void initialize(Subject sub, CallbackHandler handler, Map options) {
        this.subject = sub;
        this.callbackHandler = handler;
        this.options = options;
        this.rolePolicy = (String) options.get("role.policy");
        this.roleDiscriminator = (String) options.get("role.discriminator");
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
    }

    public Encryption getEncryption() {
        if (encryption == null) {
            Map<String,String> encOpts = new HashMap<String,String>();
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
                    LOG.debug("Encryption is disabled.");
                }
            } else {
                String name = encOpts.remove("name");
                if (debug) {
                    if (name != null && name.length() > 0) {
                        LOG.debug("Encryption is enabled. Using service " + name + " with options " + encOpts);
                    } else {
                        LOG.debug("Encryption is enabled. Using options " + encOpts);
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
                if (encryptionServiceReferences.length == 0) {
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

    public String getEncryptedPassword(String password) {
        Encryption encryption = getEncryption();
        if (encryption == null) {
            return password;
        } else {
            boolean prefix = encryptionPrefix == null || password.startsWith(encryptionPrefix);
            boolean suffix = encryptionSuffix == null || password.endsWith(encryptionSuffix);
            if (prefix && suffix) {
                return password;
            } else {
                String p = encryption.encryptPassword(password);
                if (encryptionPrefix != null) {
                    p = encryptionPrefix + p;
                }
                if (encryptionSuffix != null) {
                    p = p + encryptionSuffix;
                }
                return p;
            }
        }

    }

    public boolean checkPassword(String plain, String encrypted) {
        Encryption encryption = getEncryption();
        if (encryption == null) {
            return plain.equals(encrypted);
        } else {
            boolean prefix = encryptionPrefix == null || encrypted.startsWith(encryptionPrefix);
            boolean suffix = encryptionSuffix == null || encrypted.endsWith(encryptionSuffix);
            if (prefix && suffix) {
                encrypted = encrypted.substring(encryptionPrefix != null ? encryptionPrefix.length() : 0,
                        encrypted.length() - (encryptionSuffix != null ? encryptionSuffix.length() : 0));
                return encryption.checkPassword(plain, encrypted);
            } else {
                return plain.equals(encrypted);
            }
        }
    }

}
