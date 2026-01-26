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

import static java.util.stream.Collectors.toList;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.junit.Assert;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrincipalHelper {
    
    public static List<String> names(Collection<? extends Principal> principals) {
        return principals.stream().map(Principal::getName).collect(toList());
    }

    public static UserPrincipal getUser(AbstractPropertiesBackingEngine engine, String name) {
        List<UserPrincipal> matchingUsers = engine.listUsers().stream()
                .filter(user -> name.equals(user.getName())).collect(Collectors.toList());
        Assert.assertFalse("User with name " + name + " was not found", matchingUsers.isEmpty());
        return matchingUsers.iterator().next();
    }

    public static GroupPrincipal getGroup(AbstractPropertiesBackingEngine engine, String name) {
        List<GroupPrincipal> matchingGroups = engine.listGroups().keySet().stream()
                .filter(group -> name.equals(group.getName())).collect(Collectors.toList());
        Assert.assertFalse("Group with name " + name + " was not found", matchingGroups.isEmpty());
        return matchingGroups.iterator().next();
    }
    
}