/**
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
package org.apache.karaf.shell.console;

import java.io.InputStream;
import java.io.PrintStream;

import jline.Terminal;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.karaf.shell.console.impl.Main;
import org.apache.karaf.shell.console.impl.jline.ConsoleImpl;

import java.util.Properties;

/**
 * This class is mostly here so that folks can see an example of how you can extend the Karaf Main shell.  Also
 * lets Karaf developers see how changes the Main class can affect the interface comparability
 * with sub classes.
 */
public class ExampleSubclassMain extends Main  {

    public static void main(String args[]) throws Exception {
        ExampleSubclassMain main = new ExampleSubclassMain();
        main.run(args);
    }

    public void ExampleSubclassMain() {
        // Sets the name of the shell and the current user.
        setApplication("example");
        setUser("unknown");
    }

    @Override
    protected ConsoleImpl createConsole(CommandProcessorImpl commandProcessor, InputStream in, PrintStream out, PrintStream err, Terminal terminal) throws Exception {
        return new ConsoleImpl(commandProcessor, in, out, err, terminal, null, null) {

            /**
             * If you don't overwrite, then karaf will use the welcome message found in the
             * following resource files:
             * <ul>
             *   <li>org/apache/karaf/shell/console/branding.properties</li>
             *   <li>org/apache/karaf/branding/branding.properties</li>
             * <ul>
             */
            @Override
            protected void welcome(Properties brandingProps) {
                session.getConsole().println("===============================================");
                session.getConsole().println(" Example Shell ");
                session.getConsole().println("===============================================");
            }

            /**
             * If you don't overwrite then Karaf builds a prompt based on the current app and user.
             * @return
             */
            @Override
            protected String getPrompt() {
                return "example>";
            }

            /**
             * If you don't overwrite, then karaf automatically adds session properties
             * found in the following resource files:
             * <ul>
             *   <li>org/apache/karaf/shell/console/branding.properties</li>
             *   <li>org/apache/karaf/branding/branding.properties</li>
             * <ul>
             */
            @Override
            protected void setSessionProperties(Properties brandingProps) {
                // we won't add any session properties.
            }

        };
    }

    /**
     * if you don't override, then Karaf will discover the commands listed in the
     * "META-INF/services/org/apache/karaf/shell/commands" resource file.
     *
     * @return
     */
    @Override
    public String getDiscoveryResource() {
        return "META-INF/services/org/example/commands.index";
    }


}
