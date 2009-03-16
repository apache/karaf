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

package org.apache.servicemix.kernel.gshell.config.completers;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import jline.Completor;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.console.completer.StringsCompleter;
import org.apache.geronimo.gshell.shell.ShellContextHolder;
import org.apache.servicemix.kernel.gshell.config.ConfigCommandSupport;

/**
 * {@link jline.Completor} for Configuration Admin properties.
 *
 * Displays a list of existing properties based on the current configuration being edited.
 *
 */
public class ConfigurationPropertyCompleter implements Completor {

    public int complete(final String buffer, final int cursor, final List candidates) {
        Variables vars = ShellContextHolder.get().getVariables();
        if (vars.get(ConfigCommandSupport.PROPERTY_CONFIG_PID) == null) {
            return -1;
        }

        Dictionary props = (Dictionary) vars.get(ConfigCommandSupport.PROPERTY_CONFIG_PROPS);
        StringsCompleter delegate = new StringsCompleter();

        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            delegate.getStrings().add(key);
        }

        return delegate.complete(buffer, cursor, candidates);
    }
}