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
package org.apache.karaf.instance.command.completers;

import java.util.List;

import org.apache.karaf.instance.InstanceService;
import org.apache.karaf.instance.Instance;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.console.Completer;

/**
 * Displays a list of configured server instances for the instance commands.
 *
 */
public class InstanceCompleter implements Completer {

    private InstanceService instanceService;

    public void setInstanceService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    public int complete(String buffer, int cursor, List candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Instance instance : instanceService.getInstances()) {
            delegate.getStrings().add(instance.getName());
        }
        return delegate.complete(buffer, cursor, candidates);
    }

}
