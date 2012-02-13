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
package org.apache.karaf.kar.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

@Command(scope = "kar", name = "uninstall", description = "Uninstall a KAR file.")
public class UninstallKarCommand extends KarCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the KAR file to uninstall.", required = true, multiValued = false)
    private String name;

    @Option(name = "-c", aliases = { "--clean" }, description = "Cleanup the system repository from the KAR content", required = false, multiValued = false)
    private boolean clean = false;
    
    public Object doExecute() throws Exception {
        this.getKarService().uninstall(name, clean);
        return null;
    }
    
}
