/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.action.command;

import java.lang.annotation.Annotation;

import org.apache.karaf.shell.api.action.Option;


public class HelpOption {

    public static final Option HELP = new Option() {
        public String name() {
            return "--help";
        }

        public String[] aliases() {
            return new String[]{};
        }

        public String description() {
            return "Display this help message";
        }

        public boolean required() {
            return false;
        }

        public boolean multiValued() {
            return false;
        }

        public String valueToShowInHelp() {
            return Option.DEFAULT_STRING;
        }

        public boolean censor() {
            return false;
        }

        public char mask() {
            return 0;
        }

        public Class<? extends Annotation> annotationType() {
            return Option.class;
        }
    };
}
