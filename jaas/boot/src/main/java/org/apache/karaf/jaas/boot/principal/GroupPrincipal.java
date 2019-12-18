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
package org.apache.karaf.jaas.boot.principal;

import java.io.Serializable;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;

public class GroupPrincipal implements Group, Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private Hashtable<String,Principal> members = new Hashtable<>();

    public GroupPrincipal(String name) {
        assert name != null;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupPrincipal that = (GroupPrincipal) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "GroupPrincipal[" + name + "]";
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
        return members.containsKey(member.getName());
    }

    public Enumeration<? extends Principal> members() {
        return members.elements();
    }
}
