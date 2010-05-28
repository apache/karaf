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
package org.apache.felix.service.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Parameter
{
    static final String UNSPECIFIED = "org.apache.felix.service.command.unspecified.parameter";

    /**
     * Parameter name and aliases which must start with the hyphen character.
     * @return parameter names.
    **/
    String[] names();

    /**
     * The default value of the parameter if its name is present on the
     * command line. If this value is specified, then the command parsing
     * will not expect a value on the command line for this parameter.
     * If this value is UNSPECIFIED, then an argument must be specified on the
     * command line for the parameter.
     * @return default value of the parameter if its name is present on the
     *         command line.
    **/
    String presentValue() default UNSPECIFIED;

    /**
     * The default value of the parameter if its name is not present on the
     * command line. This value is effectively the default value for the
     * parameter.
     * @return default value of the parameter if its name is not present
     *         on the command line.
    **/
    String absentValue();
}