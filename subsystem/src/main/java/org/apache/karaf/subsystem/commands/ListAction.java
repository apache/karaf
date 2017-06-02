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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.service.subsystem.Subsystem;

@Command(scope = "subsystem", name = "list", description = "List all subsystems")
@Service
public class ListAction extends SubsystemSupport implements Action {

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("ID").alignRight();
        table.column("SymbolicName");
        table.column("Version");
        table.column("State");
        table.column("Type");
        table.column("Parents");
        table.column("Children");

        for (Subsystem ss : getSubsystems()) {
            table.addRow().addContent(
                    ss.getSubsystemId(),
                    ss.getSymbolicName(),
                    ss.getVersion(),
                    ss.getState().toString(),
                    getType(ss),
                    getSubsytemIds(ss.getParents()),
                    getSubsytemIds(ss.getChildren())
            );
        }
        table.print(System.out);
        return null;
    }

    private String getType(Subsystem subsystem) {
        String type = subsystem.getType();
        if (type.startsWith("osgi.subsystem.")) {
            type = type.substring("osgi.subsystem.".length());
        }
        return type;
    }

}
