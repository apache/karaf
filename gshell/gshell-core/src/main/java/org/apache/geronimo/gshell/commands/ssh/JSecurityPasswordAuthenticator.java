/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.gshell.commands.ssh;

import org.apache.sshd.server.PasswordAuthenticator;
import org.jsecurity.SecurityUtils;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.mgt.SecurityManager;
import org.jsecurity.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="http://jsecurity.org">JSecurity</a> {@link PasswordAuthenticator}.
 *
 * @version $Rev: 722797 $ $Date: 2008-12-03 08:18:16 +0100 (Wed, 03 Dec 2008) $
 */
public class JSecurityPasswordAuthenticator
    implements PasswordAuthenticator
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SecurityManager securityManager;

    public JSecurityPasswordAuthenticator(final SecurityManager securityManager) {
        // securityManager can be null
        this.securityManager = securityManager;
    }

    public JSecurityPasswordAuthenticator() {
        this(null);
    }

    public Object authenticate(final String username, final String password) {
        assert username != null;
        assert password != null;

        log.debug("Authenticating: {}/{}", username, password);

        Subject currentUser;

        if (securityManager != null) {
            currentUser = securityManager.getSubject();
        }
        else {
            currentUser = SecurityUtils.getSubject();
        }

        if (currentUser.isAuthenticated()) {
            log.debug("Logging out current user: {}", currentUser.getPrincipal());
            currentUser.logout();
        }

        try {
            UsernamePasswordToken token = new UsernamePasswordToken(username, password);
            currentUser.login(token);
            Object principal = currentUser.getPrincipal();
            log.info("User [{}] logged in successfully", principal);
            return principal;
        }
        catch (AuthenticationException e) {
            log.error("Authentication failed: " + e, e);
            return null;
        }
    }
}
