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
package org.osgi.service.command;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * A command shell can create and maintain a number of command sessions.
 *
 * @author aqute
 */
public interface CommandProcessor
{
    /**
     * The scope of commands provided by this service. This name can be used to distinguish
     * between different command providers with the same function names.
     */
    final static String COMMAND_SCOPE = "osgi.command.scope";

    /**
     * A list of method names that may be called for this command provider. A
     * name may end with a *, this will then be calculated from all declared public
     * methods in this service.
     * <p/>
     * Help information for the command may be supplied with a space as
     * separation.
     */
    final static String COMMAND_FUNCTION = "osgi.command.function";

    /**
     * Create a new command session associated with IO streams.
     * <p/>
     * The session is bound to the life cycle of the bundle getting this
     * service. The session will be automatically closed when this bundle is
     * stopped or the service is returned.
     * <p/>
     * The shell will provide any available commands to this session and
     * can set additional variables.
     *
     * @param in  The value used for System.in
     * @param out The stream used for System.out
     * @param err The stream used for System.err
     * @return A new session.
     */
    CommandSession createSession(InputStream in, PrintStream out, PrintStream err);
}
