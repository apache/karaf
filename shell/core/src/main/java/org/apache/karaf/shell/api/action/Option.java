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
 * Used to mark an optional named command line option who's name typically starts with "--" or "-".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Option
{
    public static final String DEFAULT_STRING= "DEFAULT";

    /**
     * The name of this option.  Usually starting with a '-'.
     */
    String name();

    /**
     * Specify a list of aliases for this option.
     * Useful when using an option with short or long names.
     */
    String[] aliases() default {};

    /**
     * A textual description of the option.
     */
    String description() default "";

    /**
     * Whether this argument is mandatory or not.
     */
    boolean required() default false;

    /**
     * The last argument can be multi-valued in which case
     * the field type must be a List.  On the command line,
     * multi-valued options are used with specifying the option
     * multiple times with different values.
     */
    boolean multiValued() default false;

    /**
     * The generated help displays default values for arguments.
     * In case the value displayed in the help to the user should
     * be different that the default value of the field, one
     * can use this property to specify the value to display.
     */
    String valueToShowInHelp() default DEFAULT_STRING;
}
