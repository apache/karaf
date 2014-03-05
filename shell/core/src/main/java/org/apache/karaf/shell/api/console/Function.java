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
package org.apache.karaf.shell.api.console;

import java.util.List;

/**
 * This interface represents some code that can be executed in the
 * {@link Session}.
 */
public interface Function {

    /**
     * Execute this function within the given Session and with the given
     * arguments.
     *
     * @param session the current session
     * @param arguments the arguments of this function
     * @return the result
     * @throws Exception if any exception occurs
     */
    Object execute(Session session, List<Object> arguments) throws Exception;

}
