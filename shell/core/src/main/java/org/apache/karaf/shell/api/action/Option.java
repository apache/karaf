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
 * <p>Used to mark an optional named command line option who's name typically starts with "--" or "-".
 * This annotation can be applied to attributes of a class implementing an Action.
 * The value of the command line option will be automatically converted to the attribute type.</p>
 * @see org.apache.karaf.shell.support.converter.DefaultConverter
 *
 * <h2>Example 1 (boolean option):</h2>
 * <code>@Option(name="--force") boolean force;</code>
 *
 * <p>This will be represented as --force on the command line.</p>
 *
 * <h2>Example 2 (mandatory String option):</h2>
 * <code>@Option(name="-name",required=true) String name;</code>
 *
 * <p>This will be represented as -name=&lt;myname&gt; on the command line and the command will be rejected if the
 * option is not given.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Option
{
    String DEFAULT_STRING= "DEFAULT";

    /**
     * The name of this option.  Usually starting with a '-'.
     *
     * @return the option name.
     */
    String name();

    /**
     * Specify a list of aliases for this option.
     * Useful when using an option with short or long names.
     *
     * @return the option aliases (as a string array).
     */
    String[] aliases() default {};

    /**
     * A textual description of the option.
     *
     * @return the option description.
     */
    String description() default "";

    /**
     * Whether this argument is mandatory or not.
     *
     * @return true if the option is required, false else.
     */
    boolean required() default false;

    /**
     * The last argument can be multi-valued in which case
     * the field type must be a List.  On the command line,
     * multi-valued options are used with specifying the option
     * multiple times with different values.
     *
     * @return true if the option is multivalued, false else.
     */
    boolean multiValued() default false;

    /**
     * The generated help displays default values for arguments.
     * In case the value displayed in the help to the user should
     * be different that the default value of the field, one
     * can use this property to specify the value to display.
     *
     * @return the option description as shown in the help.
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
