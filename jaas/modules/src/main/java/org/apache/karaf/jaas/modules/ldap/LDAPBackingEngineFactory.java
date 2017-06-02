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
package org.apache.karaf.jaas.modules.ldap;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.ldap.LDAPLoginModule;
import java.util.Map;

/**
 * Karaf JAAS backing engine factory to support basic list funcitonality
 * for the LDAP login module.
 */
public class LDAPBackingEngineFactory implements BackingEngineFactory {

    @Override
    public String getModuleClass() {
        return LDAPLoginModule.class.getName();
    }

    @Override
    public BackingEngine build(Map<String, ?> options) {
        return new LDAPBackingEngine(options);
    }

}
