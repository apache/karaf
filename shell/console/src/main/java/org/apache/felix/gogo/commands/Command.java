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
package org.apache.felix.gogo.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote a class represents a command which is executable within a shell/scope or as a
 * command line process.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Deprecated
public @interface Command {

    /**
     * Return the scope or sub shell of the command.
     *
     * @return The command scope.
     */
    String scope();

    /**
     * Return the name of the command if used inside a shell.
     *
     * @return The command name.
     */
    String name();

    /**
     * Return the description of the command which is used to generate command line help.
     *
     * @return The command description.
     */
    String description() default "";

    /**
     * Return a detailed description of the command.
     *
     * @return The command detailed description.
     */
    String detailedDescription() default "";
}
