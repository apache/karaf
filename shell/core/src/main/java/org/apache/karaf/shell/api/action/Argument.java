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
 * Represents a positional argument on a command line (as opposed to an optional named {@link Option}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Argument
{
    String DEFAULT_STRING= "DEFAULT";

    String DEFAULT = "##default";

    /**
     * Name of the argument.
     * By default, the field name will be used.
     *
     * @return the argument name.
     */
    String name() default DEFAULT;

    /**
     * A textual description of the argument.
     *
     * @return the argument description.
     */
    String description() default "";

    /**
     * Whether this argument is mandatory or not.
     *
     * @return true if the argument is required, false else.
     */
    boolean required() default false;

    /**
     * Position of the argument in the command line.
     * When using multiple arguments, the indices must be
     * starting from 0 and incrementing without any holes.
     *
     * @return the argument index.
     */
    int index() default 0;

    /**
     * The last argument can be multi-valued in which case
     * the field type must be a List.
     *
     * @return true if the argument has multiple values, false else.
     */
    boolean multiValued() default false;

    /**
     * The generated help displays default values for arguments.
     * In case the value displayed in the help to the user should
     * be different that the default value of the field, one
     * can use this property to specify the value to display.
     *
     * @return the argument help string representation.
     */
    String valueToShowInHelp() default DEFAULT_STRING;

    /**
     * Censor the argument in the console. Characters will be replaced with {@link Argument#mask()}.
     * This is useful for hiding sensitive data like passwords.
     *
     * @return true if the argument should be censored in the console.
     */
    boolean censor() default false;

    /**
     * Character to use when censoring the argument in the console.
     *
     * @return the Character to use when censoring the argument in the console.
     */
    char mask() default '*';
}
