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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;

public class JaasHelper {

    private static final String ROLE_WILDCARD = "*";

    public static boolean currentUserHasRole(String requestedRole) {
        if (ROLE_WILDCARD.equals(requestedRole)) {
            return true;
        }

        AccessControlContext acc = AccessController.getContext();
        if (acc == null) {
            return false;
        }
        Subject subject = Subject.getSubject(acc);
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
        } else {
            clazz = RolePrincipal.class.getName();
            role = requestedRole;
        }
        for (Principal p : principals) {
            if (clazz.equals(p.getClass().getName()) && role.equals(p.getName())) {
                return true;
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
        // set up the new Subject-based AccessControlContext for doPrivileged
        final AccessControlContext currentAcc = AccessController.getContext();
        final AccessControlContext newAcc = AccessController.doPrivileged
                (new PrivilegedAction<AccessControlContext>() {
                    public AccessControlContext run() {
                        if (subject == null)
                            return new AccessControlContext(currentAcc, null);
                        else
                            return new AccessControlContext(currentAcc, new OsgiSubjectDomainCombiner(subject));
                    }
                });
        // call doPrivileged and push this new context on the stack
        return AccessController.doPrivileged(action, newAcc);
    }

    public static <T> T doAs(final Subject subject,
                             final PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        if (action == null) {
            throw new NullPointerException();
        }
        // set up the new Subject-based AccessControlContext for doPrivileged
        final AccessControlContext currentAcc = AccessController.getContext();
        final AccessControlContext newAcc = AccessController.doPrivileged
                (new PrivilegedAction<AccessControlContext>() {
                    public AccessControlContext run() {
                        if (subject == null)
                            return new AccessControlContext(currentAcc, null);
                        else
                            return new AccessControlContext(currentAcc, new OsgiSubjectDomainCombiner(subject));
                    }
                });
        // call doPrivileged and push this new context on the stack
        return AccessController.doPrivileged(action, newAcc);
    }

    public static class OsgiSubjectDomainCombiner extends SubjectDomainCombiner {

        private final Subject subject;

        public OsgiSubjectDomainCombiner(Subject subject) {
            super(subject);
            this.subject = subject;
        }

        public ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
                                          ProtectionDomain[] assignedDomains) {
            int cLen = (currentDomains == null ? 0 : currentDomains.length);
            int aLen = (assignedDomains == null ? 0 : assignedDomains.length);
            ProtectionDomain[] newDomains = new ProtectionDomain[cLen + aLen];
            Principal[] principals = subject.getPrincipals().toArray(new Principal[0]);
            for (int i = 0; i < cLen; i++) {
                newDomains[i] = new DelegatingProtectionDomain(currentDomains[i], principals);
            }
            for (int i = 0; i < aLen; i++) {
                newDomains[cLen + i] = assignedDomains[i];
            }
            newDomains = optimize(newDomains);
            return newDomains;
        }

        private ProtectionDomain[] optimize(ProtectionDomain[] domains) {
            if (domains == null || domains.length == 0) {
                return null;
            }
            ProtectionDomain[] optimized = new ProtectionDomain[domains.length];
            ProtectionDomain pd;
            int num = 0;
            for (int i = 0; i < domains.length; i++) {
                if ((pd = domains[i]) != null) {
                    boolean found = false;
                    for (int j = 0; j < num && !found; j++) {
                        found = (optimized[j] == pd);
                    }
                    if (!found) {
                        optimized[num++] = pd;
                    }
                }
            }
            if (num > 0 && num < domains.length) {
                ProtectionDomain[] downSize = new ProtectionDomain[num];
                System.arraycopy(optimized, 0, downSize, 0, downSize.length);
                optimized = downSize;
            }
            return ((num == 0 || optimized.length == 0) ? null : optimized);
        }
    }

    public static class DelegatingProtectionDomain extends ProtectionDomain {

        private final ProtectionDomain delegate;

        DelegatingProtectionDomain(ProtectionDomain delegate, Principal[] principals) {
            super(delegate.getCodeSource(), delegate.getPermissions(), delegate.getClassLoader(), principals);
            this.delegate = delegate;
        }

        @Override
        public boolean implies(Permission permission) {
            return delegate.implies(permission);
        }

    }
}
