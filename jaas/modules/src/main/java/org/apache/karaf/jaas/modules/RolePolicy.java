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
import java.security.acl.Group;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public enum RolePolicy {

    PREFIXED_ROLES("prefix") {
        public void handleRoles(Subject subject,Set<Principal> principals,String discriminator) {
            for(Principal p:principals) {
                if(p instanceof RolePrincipal){
                    RolePrincipal rolePrincipal = new RolePrincipal(discriminator+p.getName());
                    subject.getPrincipals().add(rolePrincipal);
                } else {
                    subject.getPrincipals().add(p);
                }
            }
        }
    },
    GROUP_ROLES("group") {
        public void handleRoles(Subject subject,Set<Principal> principals,String discriminator) {
            Group group = new GroupPrincipal(discriminator);
            for(Principal p:principals) {
                if(p instanceof RolePrincipal) {
                    group.addMember(p);
                } else {
                    subject.getPrincipals().add(p);
                }
            }
            subject.getPrincipals().add(group);
        }
    };

    private String value;

    private static final Map<String, RolePolicy> policies = new HashMap<String, RolePolicy>();

    static {
        for (RolePolicy s : EnumSet.allOf(RolePolicy.class)) {
            policies.put(s.getValue(), s);
        }
    }

    private RolePolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public static RolePolicy getPolicy(String code) {
        return policies.get(code);
    }

    public abstract void handleRoles(Subject subject,Set<Principal> principals,String discriminator);
}
