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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote a class represents a command which is executable
 * within a shell/scope or as a command line process.
 *
 * All classes annotated with @Command should implement the
 * {@link org.apache.karaf.shell.api.action.Action} interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Command
{
    /**
     * Returns the scope or sub shell of the command.
     *
     * @return the command scope.
     */
    String scope();

    /**
     * Returns the name of the command if used inside a shell.
     *
     * @return the command name.
     */
    String name();

    /**
     * Returns the description of the command which is used to generate command line help.
     *
     * @return the command description.
     */
    String description() default "";

    /**
     * Returns a detailed description of the command.
     * This description will be shown in the help for the command.
     * Longer descriptions can be externalized using a
     * <code>classpath:[location]</code> url, in which case the
     * description will be loaded from the bundle at the given location,
     * relatively to the implementation of the command.
     *
     * @return the command long description.
     */
    String detailedDescription() default "";
}
