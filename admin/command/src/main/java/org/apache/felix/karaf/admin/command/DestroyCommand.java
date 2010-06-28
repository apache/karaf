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
package org.apache.felix.karaf.admin.command;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * Destroy an existing Karaf instance
 *
 * @version $Rev: 679826 $ $Date: 2008-07-25 17:00:12 +0200 (Fri, 25 Jul 2008) $
 */
@Command(scope = "admin", name = "destroy", description = "Destroys an existing container instance.")
public class DestroyCommand extends AdminCommandSupport
{
    @Argument(index = 0, name = "name", description="The name of the container instance to destroy", required = true, multiValued = false)
    private String instance = null;

    protected Object doExecute() throws Exception {
        getExistingInstance(instance).destroy();
        return null;
    }

}
