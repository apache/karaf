/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console;

import org.apache.felix.service.command.CommandSession;


/**
 * A helper class for name scoping
 */
public class NameScoping {

    public static final String MULTI_SCOPE_MODE_KEY = "MULTI_SCOPE_MODE";

    /**
     * Returns the name of the command which can omit the global scope prefix if the command starts with the
     * same prefix as the current application
     */
    public static String getCommandNameWithoutGlobalPrefix(CommandSession session, String key) {
        if (!isMultiScopeMode(session)) {
            String globalScope = (String) (session != null ? session.get("APPLICATION") : null);
            if (globalScope != null) {
                String prefix = globalScope + ":";
                if (key.startsWith(prefix)) {
                    // TODO we may only want to do this for single-scope mode when outside of OSGi?
                    // so we may want to also check for a isMultiScope mode == false
                    return key.substring(prefix.length());
                }
            }
        }
        return key;
    }

    /**
     * Returns true if the given scope is the global scope so that it can be hidden from help messages
     */
    public static boolean isGlobalScope(CommandSession session, String scope) {
        if (session == null)
            return false;

        if (!isMultiScopeMode(session)) {
            String globalScope = (String) session.get("APPLICATION");
            if (globalScope != null) {
                return scope.equals(globalScope);
            }
        }
        return false;
    }

    /**
     * Returns true if we are in multi-scope mode (the default) or if we are in single scope mode which means we
     * avoid prefixing commands with their scope
     */
    public static boolean isMultiScopeMode(CommandSession session) {
        if (session == null)
            return false;

        Object value = session.get(MULTI_SCOPE_MODE_KEY);
        if (value != null && value.equals("false")) {
            return false;
        }
        return true;
    }
}
