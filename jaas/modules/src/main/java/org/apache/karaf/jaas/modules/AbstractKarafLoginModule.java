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
    
    /** define the encryption algorithm to use to encrypt password */
    protected String encryption;
    
    /** the bundle context is required to use the encryption service */
    protected BundleContext bundleContext;
    
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
        this.rolePolicy = (String) options.get("rolePolicy");
        this.roleDiscriminator = (String) options.get("roleDiscriminator");
        this.debug = Boolean.parseBoolean((String) options.get("debug"));
        this.encryption = (String) options.get("encryption");
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
    }
    
    /**
     * <p>
     * Encrypt password.
     * </p>
     * 
     * @param password the password in plain format.
     * @return the encrypted password format.
     */
    public String encryptPassword(String password) {
        if (this.encryption == null || this.encryption.trim().isEmpty()) {
            if (debug) {
                LOG.debug("Encryption is disabled.");
            }
            return password;
        }
        if (debug) {
            LOG.debug("Encryption is enabled and use " + encryption + " encryption algorithm.");
        }
        // lookup the encryption service reference
        ServiceReference[] encryptionServiceReferences;
        try {
            encryptionServiceReferences = bundleContext.getServiceReferences(Encryption.class.getName(), "(algorithm=" + encryption + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("The encryption service filter is not well formed.", e);
        }
        if (encryptionServiceReferences.length == 0) {
            throw new IllegalStateException("Encryption service not found for encryption algorithm " + encryption + ". Please install the Karaf encryption feature and check that the encryption algorithm is supported..");
        }
        // get the encryption service implementation
        Encryption encryptionService = (Encryption) bundleContext.getService(encryptionServiceReferences[0]);
        if (encryptionService == null) {
            throw new IllegalStateException("Encryption service not found. Please install the Karaf encryption feature.");
        }
        // encrypt the password
        String encryptedPassword = encryptionService.encryptPassword(password);
        // release the encryption service reference
        bundleContext.ungetService(encryptionServiceReferences[0]);
        return encryptedPassword;
    }
    
    /**
     * <p>
     * Check if the provided password match the reference one.
     * </p>
     * 
     * @param input the provided password (plain format).
     * @param password the reference one (encrypted format).
     * @return true if the passwords match, false else.
     */
    public boolean checkPassword(String input, String password) {
        if (this.encryption == null || this.encryption.trim().isEmpty()) {
            if (debug) {
                LOG.debug("Encryption is disabled.");
            }
            return input.equals(password);
        }        
        if (debug) {
            LOG.debug("Encryption is enabled and use " + encryption + " encryption algorithm.");
        }
        // lookup the encryption service reference
        ServiceReference[] encryptionServiceReferences = new ServiceReference[0];
        try {
            encryptionServiceReferences = bundleContext.getServiceReferences(Encryption.class.getName(), "(algorithm=" + encryption + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("The encryption service filter is not well formed.", e);
        }
        if (encryptionServiceReferences.length == 0) {
            throw new IllegalStateException("Encryption service not found for encryption algorithm " + encryption + ". Please install the Karaf encryption feature and check that the encryption algorithm is supported..");
        }
        // get the encryption service implementation
        Encryption encryptionService = (Encryption) bundleContext.getService(encryptionServiceReferences[0]);
        if (encryptionService == null) {
            throw new IllegalStateException("Encryption service not found. Please install the Karaf encryption feature.");
        }
        // check password
        boolean equals = encryptionService.checkPassword(input, password);
        String encryptedPassword = encryptionService.encryptPassword(password);
        // release the encryption service reference
        bundleContext.ungetService(encryptionServiceReferences[0]);
        return equals;
    }
    
}
