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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.subsystem.Subsystem;

@Command(scope = "subsystem", name = "uninstall", description = "Uninstall the specified subsystems")
@Service
public class UninstallAction extends SubsystemSupport implements Action {

    @Argument(description = "Subsystem names or ids")
    @Completion(SubsystemCompleter.class)
    String id;

    @Override
    public Object execute() throws Exception {
        for (Subsystem ss : getSubsystems(id)) {
            ss.uninstall();
        }
        return null;
    }

}
