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

import org.apache.karaf.jaas.modules.encryption.EncryptionSupport;
import org.osgi.framework.BundleContext;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractKarafLoginModule implements LoginModule {

    protected Set<Principal> principals = new HashSet<Principal>();
    protected Subject subject;
    protected String user;
    protected CallbackHandler callbackHandler;
    protected boolean debug;
    protected Map<String, ?> options;
   
    protected String rolePolicy;
    protected String roleDiscriminator;
    protected boolean detailedLoginExcepion;

    /**
     * the bundle context is required to use the encryption service
     */
    protected BundleContext bundleContext;

    private EncryptionSupport encryptionSupport;

    public boolean commit() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        }
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
        this.detailedLoginExcepion = Boolean.parseBoolean((String) options.get("detailed.login.exception"));
        // the bundle context is set in the Config JaasRealm by default
        this.bundleContext = (BundleContext) options.get(BundleContext.class.getName());
        encryptionSupport = new EncryptionSupport(options);
    }


    public String getEncryptedPassword(String password) {
        Encryption encryption = encryptionSupport.getEncryption();
        String encryptionPrefix = encryptionSupport.getEncryptionPrefix();
        String encryptionSuffix = encryptionSupport.getEncryptionSuffix();

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
        Encryption encryption = encryptionSupport.getEncryption();
        String encryptionPrefix = encryptionSupport.getEncryptionPrefix();
        String encryptionSuffix = encryptionSupport.getEncryptionSuffix();

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
