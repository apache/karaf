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
package org.apache.karaf.shell.api.action;

/**
 * <p>An action is the default implementation of the commands in karaf.
 * In OSGi, Actions are discovered using an extender and a new instance
 * of the class is created when the command is invoked, so that the
 * implementation does not need to be thread safe.</p>
 *
 * <p>Before the call to the execute method the action is checked for
 * fields annotated with @Reference and injected with services coming
 * from the SessionFactory's Registry or from the OSGi registry.
 * Methods annotated with @Init are then called.  The next step is to
 * inject command line parameters into fields annotated with @Option
 * and @Argument and then call the execute method.</p>
 * 
 * <p>Any class implementing Action must have a no argument constructor. This
 * is necessary so the help generator can instantiate the class and get the 
 * default values.</p>
 *
 * <p>In order to make commands available from the non-OSGi shell,
 * the commands must be listed in a file available at
 * META-INF/services/org/apache/karaf/shell/commmands.</p>
 *
 * @see org.apache.karaf.shell.api.action.Command
 * @see org.apache.karaf.shell.api.action.lifecycle.Service
 */
public interface Action {

    /**
     * Execute the action which has been injected with services from the
     * registry, options and arguments from the command line.
     *
     * @return <code>null</code> or the result of the action execution
     * @throws Exception in case of execution failure.
     */
    Object execute() throws Exception;

}
