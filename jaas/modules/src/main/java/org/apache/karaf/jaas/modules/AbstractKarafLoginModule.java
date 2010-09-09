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

import org.jasypt.util.password.ConfigurablePasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;


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
    
    /** 
     * Algorithm used to encrypt password such as:
     *  MD2
     *  MD5
     *  SHA-1
     *  SHA-256
     *  SHA-384
     *  SHA-512 
     */
    protected String encryptionAlgorithm = "MD5";
    
    /** Jasypt password encryptor */
    private ConfigurablePasswordEncryptor passwordEncryptor;

    public boolean commit() throws LoginException {
        RolePolicy policy = RolePolicy.getPolicy(rolePolicy);
        if(policy != null && roleDiscriminator != null) {
            policy.handleRoles(subject, principals, roleDiscriminator);
        } else subject.getPrincipals().addAll(principals);
        return true;
    }

    protected void clear() {
        user = null;
    }
    
    /**
     * <p>
     * Util method to encrypt a password given in plain format.
     * </p>
     * 
     * @param plain the plain password.
     * @return the encrypted password.
     */
    public String encryptPassword(String plain) {
        ConfigurablePasswordEncryptor encryptor = new ConfigurablePasswordEncryptor();
        encryptor.setAlgorithm(encryptionAlgorithm);
        return null;
    }
    
    /**
     * <p>
     * Check a password in plain with an encrypted password.
     * </p>
     * 
     * @param plainPassword a plain password in plain.
     * @param encryptedPassword an encrypted password.
     * @return true if the plain password match the encrypted one, false else.
     */
    public boolean checkPassword(String plainPassword, String encryptedPassword) {
        return passwordEncryptor.checkPassword(plainPassword, encryptedPassword);
    }

    public void initialize(Subject sub, CallbackHandler handler, Map options) {
        this.subject = sub;
        this.callbackHandler = handler;
        this.rolePolicy = (String) options.get("rolePolicy");
        this.roleDiscriminator = (String) options.get("roleDisciriminator");
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.encryptionAlgorithm = (String) options.get("encryptionAlgorithm");
        this.passwordEncryptor = new ConfigurablePasswordEncryptor();
        this.passwordEncryptor.setAlgorithm(encryptionAlgorithm);
    }
}
