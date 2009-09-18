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
package org.apache.felix.karaf.shell.admin.internal.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;


/**
 * Creates a new Karaf instance 
 *
 * @version $Rev: 679826 $ $Date: 2008-07-25 17:00:12 +0200 (Fri, 25 Jul 2008) $
 */
@Command(scope = "admin", name = "create", description = "Create a new instance.")
public class CreateCommand extends AdminCommandSupport
{
    @Option(name = "-p", aliases = { "--port"}, description = "Port number for remote shell connection")
    private int port = 0;

    @Option(name = "-l", aliases = { "--location"}, description = "Location of the new instance on the file system")
    private String location;

    @Argument(index=0, required=true, description="Name of the new Karaf instance")
    private String instance = null;

    protected Object doExecute() throws Exception {
        getAdminService().createInstance(instance, port, location);
        return null;
    }

}
