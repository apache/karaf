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
package org.apache.karaf.shell.impl.console.commands;

import java.io.PrintStream;
import java.util.List;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.CommandException;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public abstract class TopLevelCommand implements Command {

    @Override
    public String getScope() {
        return Session.SCOPE_GLOBAL;
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        return null;
//        return new StringsCompleter(new String[] { getName() });
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        if (arguments.contains("--help")) {
            printHelp(session, System.out);
            return null;
        }
        if (!arguments.isEmpty()) {
            String msg = COLOR_RED
                    + "Error executing command "
                    + INTENSITY_BOLD + getName() + INTENSITY_NORMAL
                    + COLOR_DEFAULT + ": " + "too many arguments specified";
            throw new CommandException(msg);
        }
        doExecute(session);
        return null;
    }

    protected void printHelp(Session session, PrintStream out) {
        out.println(INTENSITY_BOLD + "DESCRIPTION" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + getName() + INTENSITY_NORMAL);
        out.println();
        out.print("\t");
        out.println(getDescription());
        out.println();
        out.println(INTENSITY_BOLD + "SYNTAX" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(getName() + " [options]");
        out.println();
        out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "--help" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Display this help message");
        out.println();
    }

    protected abstract void doExecute(Session session) throws Exception;

}
