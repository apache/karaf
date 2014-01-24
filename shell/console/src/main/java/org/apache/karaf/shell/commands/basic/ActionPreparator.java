/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.commands.basic;

import java.util.List;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;

public interface ActionPreparator {

    /**
     * Check if the arguments are valid for the action and inject the arguments into the fields
     * of the action
     * 
     * Using deprecated Action for compatiblity
     * 
     * @param action
     * @param session
     * @param arguments
     * @return
     * @throws Exception
     */
    boolean prepare(@SuppressWarnings("deprecation") Action action, CommandSession session, List<Object> arguments) throws Exception;

}
