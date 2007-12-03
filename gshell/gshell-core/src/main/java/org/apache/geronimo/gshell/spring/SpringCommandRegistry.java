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
package org.apache.geronimo.gshell.spring;

import java.util.Map;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.DefaultCommandRegistry;
import org.apache.geronimo.gshell.registry.NotRegisteredException;
import org.apache.geronimo.gshell.registry.DuplicateRegistrationException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 11, 2007
 * Time: 3:47:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpringCommandRegistry extends DefaultCommandRegistry {

    public void register(final Command command, Map<String, ?> properties) throws DuplicateRegistrationException {
        register(command);
    }

    public void unregister(final Command command, Map<String, ?> properties) throws NotRegisteredException {
        unregister(command);
    }
}
