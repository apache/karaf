/*
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
package org.apache.karaf.instance.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.instance.command.ChangeOptsCommand;
import org.apache.karaf.instance.command.ChangeRmiRegistryPortCommand;
import org.apache.karaf.instance.command.ChangeRmiServerPortCommand;
import org.apache.karaf.instance.command.ChangeSshPortCommand;
import org.apache.karaf.instance.command.CloneCommand;
import org.apache.karaf.instance.command.CreateCommand;
import org.apache.karaf.instance.command.DestroyCommand;
import org.apache.karaf.instance.command.InstanceCommandSupport;
import org.apache.karaf.instance.command.ListCommand;
import org.apache.karaf.instance.command.RenameCommand;
import org.apache.karaf.instance.command.RestartCommand;
import org.apache.karaf.instance.command.StartCommand;
import org.apache.karaf.instance.command.StatusCommand;
import org.apache.karaf.instance.command.StopCommand;
import org.apache.karaf.instance.core.internal.InstanceServiceImpl;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.impl.action.command.DefaultActionPreparator;

public class Execute {
    static Class<? extends Action> x = CreateCommand.class;
    private static final Class<?>[] COMMAND_CLASSES = new Class[]{
            ChangeOptsCommand.class,
            ChangeRmiRegistryPortCommand.class,
            ChangeRmiServerPortCommand.class,
            ChangeSshPortCommand.class,
            CloneCommand.class,
            CreateCommand.class,
            DestroyCommand.class,
            ListCommand.class,
            RenameCommand.class,
            RestartCommand.class,
            StartCommand.class,
            StatusCommand.class,
            StopCommand.class};
    private static final Map<String, Class<?>> COMMANDS = new TreeMap<>();

    static {
        for (Class<?> c : COMMAND_CLASSES) {
            Command ann = c.getAnnotation(Command.class);
            if (ann == null) {
                continue;
            }
            COMMANDS.put(ann.name(), c);
        }
    }

    // For testing
    static boolean exitAllowed = true;

    /**
     * Environment variable for specifying extra options to the Karaf instance
     * process kicked off from this Java process.
     */
    private static final String ENV_KARAF_OPTS = "KARAF_OPTS";

    /**
     * System property for specifying extra options to the Karaf instance
     * process kicked off from this Java process.
     */
    private static final String PROP_KARAF_OPTS = "karaf.opts";

    public static void main(String[] args) throws Exception {
        Package p = Package.getPackage("org.apache.karaf.instance.main");
        if (p != null && p.getImplementationVersion() != null) {
            System.setProperty("karaf.version", p.getImplementationVersion());
        }

        if (args.length == 0) {
            listCommands();
            exit(0);
        }
        String commandName = args[0];
        Class<?> cls = COMMANDS.get(commandName);
        if (cls == null) {
            System.err.println("Command not found: " + commandName);
            exit(-1);
        }

        String storage = System.getProperty("karaf.instances");
        if (storage == null) {
            System.err.println("System property 'karaf.instances' is not set. \n" +
                    "This property needs to be set to the full path of the instance.properties file.");
            exit(-2);
        }
        File storageFile = new File(storage);
        System.setProperty("user.dir", storageFile.getParentFile().getParentFile().getCanonicalPath());

        try {
            String karafOpts = System.getenv(ENV_KARAF_OPTS);
            if (karafOpts != null) {
                System.setProperty(PROP_KARAF_OPTS, karafOpts);
            }
        } catch (Exception e) {
            System.err.println("Could not read KARAF_OPTS environment variable: " + e.getMessage());
            if (System.getProperty("karaf.showStackTrace") != null) {
                throw e;
            }
        }

        Object command = cls.newInstance();
        if (command instanceof InstanceCommandSupport) {
            try {
                execute((InstanceCommandSupport) command, storageFile, args);
            } catch (Exception e) {
                System.err.println("Error execution command '" + commandName + "': " + e.getMessage());
                if (System.getProperty("karaf.showStackTrace") != null) {
                    throw e;
                }
            }
        } else {
            System.err.println("Not an instance command: " + commandName);
            exit(-3);
        }
    }

    static void execute(InstanceCommandSupport command, File storageFile, String[] args) throws Exception {
        DefaultActionPreparator dap = new DefaultActionPreparator();
        List<Object> params = new ArrayList<>(Arrays.asList(args));
        params.remove(0); // this is the actual command name

        if (!dap.prepare(command, null, params)) {
            return;
        }

        InstanceServiceImpl instanceService = new InstanceServiceImpl();
        instanceService.setStorageLocation(storageFile);
        command.setInstanceService(instanceService);
        command.execute();
    }

    private static void listCommands() {
        System.out.println("Available commands:");
        for (Map.Entry<String, Class<?>> entry : COMMANDS.entrySet()) {
            Command ann = entry.getValue().getAnnotation(Command.class);
            System.out.printf("  %s - %s\n", entry.getKey(), ann.description());
        }

        System.out.println("Type 'command --help' for more help on the specified command.");
    }

    private static void exit(int rc) {
        if (exitAllowed) {
            System.exit(rc);
        } else {
            throw new RuntimeException(Integer.toString(rc));
        }
    }

}
