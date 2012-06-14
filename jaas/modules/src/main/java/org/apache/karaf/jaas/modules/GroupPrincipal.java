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
import java.util.Enumeration;
import java.util.Hashtable;

public class GroupPrincipal implements Group {

    private String name;
    private Hashtable<String,Principal> members = new Hashtable<String, Principal>();

    public GroupPrincipal(String name) {
        this.name = name;
    }
    
    public boolean addMember(Principal user) {
        members.put(user.getName(), user);
        return true;
    }

    public boolean removeMember(Principal user) {
        members.remove(user.getName());
        return true;
    }

    public boolean isMember(Principal member) {
        return members.contains(member.getName());
    }

    public Enumeration<? extends Principal> members() {
        return members.elements();
    }

    public String getName() {
        return name;
    }
}
