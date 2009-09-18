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

package org.apache.felix.karaf.shell.config.completers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.karaf.shell.console.completer.StringsCompleter;
import org.apache.felix.karaf.shell.console.Completer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * {@link jline.Completor} for Configuration Admin configurations.
 *
 * Displays a list of existing config admin configurations for completion.
 *
 */
public class ConfigurationCompleter implements Completer, ConfigurationListener {

    private final StringsCompleter delegate = new StringsCompleter();

    private ConfigurationAdmin admin;

    public void setAdmin(ConfigurationAdmin admin) {
        this.admin = admin;
    }

    public void init() {
        Configuration[] configs;
        try {
            configs = admin.listConfigurations(null);
            if (configs == null) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        Collection<String> pids = new ArrayList<String>();

        for (Configuration config : configs) {
            if (config.getFactoryPid() != null) {
                pids.add(config.getFactoryPid());
            } else {
                pids.add(config.getPid());
            }
        }

        delegate.getStrings().addAll(pids);

    }

    public int complete(final String buffer, final int cursor, final List candidates) {
        return delegate.complete(buffer, cursor, candidates);
    }

    public void configurationEvent(ConfigurationEvent configurationEvent) {
        String pid = configurationEvent.getFactoryPid()!=null ? configurationEvent.getFactoryPid() : configurationEvent.getPid();
        if (configurationEvent.getType() == ConfigurationEvent.CM_DELETED) {
            delegate.getStrings().remove(pid);
        } else if (configurationEvent.getType() == ConfigurationEvent.CM_UPDATED) {
            delegate.getStrings().add(pid);
        }
    }
}
