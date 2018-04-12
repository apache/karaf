/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.subsystem.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.osgi.service.subsystem.Subsystem;

public abstract class SubsystemSupport {

    @Reference
    Subsystem subsystem;

    protected Subsystem getRoot() {
        Subsystem ss = subsystem;
        while (!ss.getParents().isEmpty()) {
            ss = ss.getParents().iterator().next();
        }
        return ss;
    }

    protected Subsystem getSubsystem(String id) {
        List<Subsystem> subsystems = getSubsystems(id);
        int nb = subsystems.size();
        if (nb == 0) {
            throw new IllegalArgumentException("No subsystem matching " + id);
        } else if (nb > 1) {
            throw new IllegalArgumentException("Multiple subsystems matching " + id);
        } else {
            return subsystems.get(0);
        }
    }

    protected List<Subsystem> getSubsystems(String id) {
        // Null id
        if (id == null || id.isEmpty()) {
            return getSubsystems();
        }
        List<Subsystem> subsystems = new ArrayList<>();
        // Try with the id
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(id);
        if (matcher.find()) {
            long lid = Long.parseLong(id);
            for (Subsystem ss : getSubsystems()) {
                if (ss.getSubsystemId() == lid) {
                    subsystems.add(ss);
                }
            }
            return subsystems;
        }
        // Try with an id range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(id);
        if (matcher.find()) {
            int index = id.indexOf('-');
            long startId = Long.parseLong(id.substring(0, index));
            long endId = Long.parseLong(id.substring(index + 1));
            for (Subsystem ss : getSubsystems()) {
                if (startId <= ss.getSubsystemId() && ss.getSubsystemId() <= endId) {
                    subsystems.add(ss);
                }
            }
            return subsystems;
        }
        int index = id.indexOf('/');
        Pattern p1, p2;
        if (index < 0) {
            p1 = Pattern.compile(id);
            p2 = null;
        } else {
            p1 = Pattern.compile(id.substring(0, index));
            p2 = Pattern.compile(id.substring(index + 1));
        }
        for (Subsystem ss : getSubsystems()) {
            if (p1.matcher(ss.getSymbolicName()).find() &&
                    (p2 == null || p2.matcher(ss.getVersion().toString()).find())) {
                subsystems.add(ss);
            }
        }
        return subsystems;
    }

    protected List<Long> getSubsytemIds(Collection<Subsystem> subsystems) {
        List<Long> ids = new ArrayList<>();
        for (Subsystem ss : subsystems) {
            long id = ss.getSubsystemId();
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        Collections.sort(ids);
        return ids;
    }

    protected List<Subsystem> getSubsystems() {
        Map<Long, Subsystem> subsystems = new TreeMap<>();
        doGetSubsystems(subsystems, getRoot());
        return new ArrayList<>(subsystems.values());
    }

    private void doGetSubsystems(Map<Long, Subsystem> subsystems, Subsystem subsystem) {
        if (subsystems.put(subsystem.getSubsystemId(), subsystem) == null) {
            for (Subsystem child : subsystem.getChildren()) {
                doGetSubsystems(subsystems, child);
            }
        }
    }

}
