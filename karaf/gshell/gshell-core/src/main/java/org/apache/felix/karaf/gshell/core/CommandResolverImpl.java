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

package org.apache.felix.karaf.gshell.core;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.CommandException;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.registry.CommandResolver;
import org.apache.geronimo.gshell.registry.NoSuchCommandException;
import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.apache.geronimo.gshell.vfs.FileSystemAccess;
import org.apache.geronimo.gshell.wisdom.command.AliasCommand;
import org.apache.geronimo.gshell.wisdom.command.GroupCommand;
import org.apache.geronimo.gshell.wisdom.registry.GroupDirectoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link CommandResolver} component.
 *
 * @version $Rev: 741078 $ $Date: 2009-02-05 12:39:10 +0100 (Thu, 05 Feb 2009) $
 */
public class CommandResolverImpl
    implements CommandResolver, BeanContainerAware
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FileSystemAccess fileSystemAccess;

    private final GroupDirectoryResolver groupDirResolver;

    private FileObject commandsRoot;

    private FileObject aliasesRoot;

    private BeanContainer container;

    private String aliasCommandBeanId;
    private String groupCommandBeanId;

    public CommandResolverImpl(final FileSystemAccess fileSystemAccess, final GroupDirectoryResolver groupDirResolver) {
        assert fileSystemAccess != null;
        this.fileSystemAccess = fileSystemAccess;

        assert groupDirResolver != null;
        this.groupDirResolver = groupDirResolver;
    }

    public void setBeanContainer(final BeanContainer container) {
        assert container != null;

        this.container = container;
    }

    public void setAliasCommandBeanId(String aliasCommandBeanId) {
        this.aliasCommandBeanId = aliasCommandBeanId;
    }

    public void setGroupCommandBeanId(String groupCommandBeanId) {
        this.groupCommandBeanId = groupCommandBeanId;
    }

    //
    // TODO: Consider adding an undefined command handler to allow for even more customization of
    //       execution when no defined command is found?  So one can say directly execute a
    //       *.gsh script, which under the covers will translate into 'source *.gsh' (or really
    //       should be 'shell *.gsh' once we have a sub-shell command.
    //

    public Command resolveCommand(final String name, final Variables variables) throws CommandException {
        assert name != null;
        assert variables != null;

        log.debug("Resolving command name: {}", name);

        // Always try to resolve aliases before we resolve commands
        Command command = resolveAliasCommand(name, variables);

        if (command == null) {
            try {
                FileObject file = resolveCommandFile(name, variables);

                if (file != null) {
                    command = createCommand(file);
                }
            }
            catch (FileSystemException e) {
                log.warn("Unable to resolve command for name: " + name, e);
            }
        }

        if (command == null) {
            throw new NoSuchCommandException(name);
        }

        log.debug("Resolved command: {}", command);

        return command;
    }

    private FileObject getAliasesRoot() throws FileSystemException {
        if (aliasesRoot == null) {
            aliasesRoot = fileSystemAccess.createVirtualFileSystem(ALIASES_ROOT);
        }

        return aliasesRoot;
    }

    private AliasCommand resolveAliasCommand(final String name, final Variables variables) {
        assert name != null;
        assert variables != null;

        log.trace("Resolving alias for name: {}", name);

        AliasCommand command = null;

        try {
            FileObject root = getAliasesRoot();
            FileObject file = root.resolveFile(name);

            if (file != null && file.exists()) {
                log.trace("Resolved file: {}", file);

                command = createAliasCommand(file);
            }
        }
        catch (FileSystemException e) {
            log.debug("Failed to resolve alias command for name: " + name, e);
        }

        return command;
    }

    private FileObject getCommandsRoot() throws FileSystemException {
        if (commandsRoot == null) {
            commandsRoot = fileSystemAccess.createVirtualFileSystem(COMMANDS_ROOT);
        }

        return commandsRoot;
    }

    private FileObject resolveCommandFile(final String name, final Variables variables) throws FileSystemException {
        assert name != null;
        assert variables != null;

        log.trace("Resolving command file: {}", name);

        FileObject root = getCommandsRoot();

        // Special handling for root & group
        if (name.equals("/")) {
            return root;
        }
        else if (name.equals(".")) {
            return groupDirResolver.getGroupDirectory(variables);
        }

        Collection<String> searchPath = getSearchPath(variables);

        log.trace("Search path: {}", searchPath);

        FileObject groupDir = groupDirResolver.getGroupDirectory(variables);

        log.trace("Group dir: {}", groupDir);

        FileObject file = null;

        for (String pathElement : searchPath) {
            log.trace("Resolving file; name={}, pathElement={}", name, pathElement);

            FileObject dir;

            if (pathElement.equals("/")) {
                dir = root;
            }
            else if (pathElement.startsWith("/")) {
                dir = fileSystemAccess.resolveFile(root, pathElement.substring(1, pathElement.length()));
            }
            else {
                dir = fileSystemAccess.resolveFile(groupDir, pathElement);
            }

            log.trace("Dir: {}", dir);

            FileObject tmp = fileSystemAccess.resolveFile(dir, name);

            log.trace("File: {}", tmp);

            if (tmp.exists()) {
                file = tmp;
                break;
            }
        }

        if (file != null) {
            log.trace("Resolved file: {}", file);
        }

        return file;
    }

    private Collection<String> getSearchPath(final Variables vars) {
        assert vars != null;

        Object tmp = vars.get(PATH);

        if (tmp instanceof String) {
            return Arrays.asList(((String)tmp).split(PATH_SEPARATOR));
        }
        else if (tmp != null) {
            log.error("Invalid type for variable '" + PATH + "'; expected String; found: " + tmp.getClass());
        }

        // Return the default search path (group then root)
        return Arrays.asList(".", "/");
    }

    public Collection<Command> resolveCommands(String name, Variables variables) throws CommandException {
        // name may be null
        assert variables != null;

        if (name == null) {
            name = "";
        }

        log.debug("Resolving commands for name: {}", name);

        List<Command> commands = new ArrayList<Command>();

        try {
            FileObject file = resolveCommandFile(name, variables);

            log.trace("Resolved (for commands): {}", file);

            if (file != null && file.exists()) {
                if (file.getType().hasChildren()) {
                    for (FileObject child : file.getChildren()) {
                        Command command = createCommand(child);
                        commands.add(command);
                    }
                }
                else {
                    Command command = createCommand(file);
                    commands.add(command);
                }
            }
        }
        catch (FileSystemException e) {
            log.warn("Failed to resolve commands for name: " + name, e);
        }

        log.debug("Resolved {} commands", commands.size());
        if (log.isTraceEnabled()) {
            for (Command command : commands) {
                log.trace("    {}", command);
            }
        }

        return commands;
    }

    private Command createCommand(FileObject file) throws FileSystemException, CommandException {
        assert file != null;

        // HACK: Must dereference to avoid problems with the DelegateFileObject impl
        file = fileSystemAccess.dereference(file);

        log.trace("Creating command for file: {} ({})", file, file.getClass());

        Command command = null;

        if (file.exists()) {
            FileContent content = file.getContent();
            command = (Command)content.getAttribute("COMMAND");

            if (command == null) {
                if (file.getType().hasChildren()) {
                    command = createGroupCommand(file);
                    content.setAttribute("COMMAND", command);
                }

                // TODO: Try to construct AliasCommand?
            }
        }

        if (command == null) {
            throw new CommandException("Unable to create command for file: " + file.getName());
        }

        return command;
    }

    private AliasCommand createAliasCommand(final FileObject file) throws FileSystemException {
        assert file != null;

        String name = file.getName().getBaseName();

        log.trace("Creating command for alias: {}", name);

        AliasCommand command = container.getBean(aliasCommandBeanId, AliasCommand.class);

        String alias = (String) file.getContent().getAttribute("ALIAS");
        if (alias == null) {
            throw new IllegalStateException("Alias meta-file does not contain 'ALIAS' attribute: " + file);
        }

        command.setName(name);
        command.setAlias(alias);

        return command;
    }

    private GroupCommand createGroupCommand(final FileObject file) throws FileSystemException {
        assert file != null;

        log.trace("Creating command for group: {}", file);

        GroupCommand command = container.getBean(groupCommandBeanId, GroupCommand.class);
        String path = fileSystemAccess.dereference(commandsRoot).getName().getRelativeName(file.getName());
        if (".".equals(path)) {
            path = "/";
        }
        command.setPath(path);

        return command;
    }
}
