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

@Command(scope = "subsystem", name = "install", description = "Install a new subsystem")
@Service
public class InstallAction extends SubsystemSupport implements Action {

    @Argument(name = "Subsystem to install the new subsystem into")
    @Completion(SubsystemCompleter.class)
    String id;

    @Argument(name = "New subsystem url", index = 1)
    String location;

    @Override
    public Object execute() throws Exception {
        getSubsystem(id).install(location);
        return null;
    }

}
