/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.instance.command;

import java.util.List;

import org.apache.karaf.instance.command.completers.StartedInstanceCompleter;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;

@Command(scope = "instance", name = "stop", description = "Stop an existing container instance.")
@Service
public class StopCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true, multiValued = true)
    @Completion(StartedInstanceCompleter.class)
    private List<String> instances = null;

    @SuppressWarnings("deprecation")
    protected Object doExecute() throws Exception {
        final MultiException exception = new MultiException("Error stopping instance(s)");
        for (Instance instance : getMatchingInstances(instances)) {
            try {
                instance.stop();
            } catch (Exception e) {
                exception.addException(e);
            }
        }
        exception.throwIfExceptions();
        return null;
    }

}
