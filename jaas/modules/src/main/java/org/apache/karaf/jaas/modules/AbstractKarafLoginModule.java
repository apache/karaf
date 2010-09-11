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
        if (this.encryption == null) {
            return password;
        }
        // TODO call the encryption service
        return null;
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
        if (this.encryption == null) {
            return input.equals(password);
        }
        // TODO call the encryption service
        return true;
    }
    
}
