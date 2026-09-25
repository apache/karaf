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
package org.apache.karaf.util.jaas;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.security.auth.Subject;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;

public class JaasHelper {

    private static final String ROLE_WILDCARD = "*";

    public static boolean currentUserHasRole(String requestedRole) {
        if (ROLE_WILDCARD.equals(requestedRole)) {
            return true;
        }

        Subject subject = Subject.current();
        if (subject == null) {
            return false;
        }

        return currentUserHasRole(subject.getPrincipals(), requestedRole);
    }

    public static boolean currentUserHasRole(Set<Principal> principals, String requestedRole) {
        if (ROLE_WILDCARD.equals(requestedRole)) {
            return true;
        }

        String clazz;
        String role;
        int index = requestedRole.indexOf(':');
        if (index > 0) {
            clazz = requestedRole.substring(0, index);
            role = requestedRole.substring(index + 1);
            
            for (Principal p : principals) {
                if (clazz.equals(p.getClass().getName()) && role.equals(p.getName())) {
                    return true;
                }
            }
        } else {
            role = requestedRole;
            
            for (Principal p : principals) {
                if (RolePrincipal.class.isAssignableFrom(p.getClass()) && role.equals(p.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void runAs(final Subject subject,
                             final Runnable action) {
        if (action == null) {
            throw new NullPointerException();
        }
        doAs(subject, (PrivilegedAction<Object>)(() -> { action.run(); return null; } ));
    }

    public static <T> T doAs(final Subject subject,
                             final PrivilegedAction<T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        try {
            return Subject.callAs(subject, action::run);
        } catch (CompletionException e) {
            throw unwrapUnchecked(e);
        }
    }

    public static <T> T doAs(final Subject subject,
                             final PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        if (action == null) {
            throw new NullPointerException();
        }
        try {
            return Subject.callAs(subject, action::run);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception && !(cause instanceof RuntimeException)) {
                throw new PrivilegedActionException((Exception) cause);
            }
            throw unwrapUnchecked(e);
        }
    }

    private static RuntimeException unwrapUnchecked(CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return e;
    }
}
