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
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.service.subsystem.Subsystem;

@Service
public class SubsystemCompleter extends SubsystemSupport implements Completer {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        List<String> strings = new ArrayList<>();
        for (Subsystem ss : getSubsystems()) {
            strings.add(Long.toString(ss.getSubsystemId()));
            strings.add(ss.getSymbolicName() + "/" + ss.getVersion());
        }
        return new StringsCompleter(strings).complete(session, commandLine, candidates);
    }
}
