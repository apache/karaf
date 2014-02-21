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

import org.apache.karaf.shell.commands.ansi.SimpleAnsi;

/**
 * Base class for exceptions thrown when executing commands.
 */
@Deprecated
public class CommandException extends Exception {

    private String help;

    public CommandException() {
    }

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(Throwable cause) {
        super(cause);
    }

    public CommandException(String help, String message) {
        super(message);
        this.help = help;
    }

    public CommandException(String help, String message, Throwable cause) {
        super(message, cause);
        this.help = help;
    }

    public String getNiceHelp() {
        return help != null ? help
                : SimpleAnsi.COLOR_RED + "Error executing command: " 
                 + getMessage() != null ? getMessage() : getClass().getName()
                 + SimpleAnsi.COLOR_DEFAULT;
    }

}
