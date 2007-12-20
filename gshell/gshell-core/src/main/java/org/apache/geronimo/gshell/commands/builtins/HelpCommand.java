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
package org.apache.geronimo.gshell.commands.builtins;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.geronimo.gshell.ansi.Code;
import org.apache.geronimo.gshell.ansi.Renderer;
import org.apache.geronimo.gshell.branding.Branding;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.annotation.Requirement;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.layout.model.AliasNode;
import org.apache.geronimo.gshell.layout.model.CommandNode;
import org.apache.geronimo.gshell.layout.model.GroupNode;
import org.apache.geronimo.gshell.layout.model.Node;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.registry.NotRegisteredException;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.codehaus.plexus.util.StringUtils;

/**
 * Display help
 *
 * @version $Rev: 596570 $ $Date: 2007-11-20 09:47:27 +0100 (Tue, 20 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:help", description="Show command help")
public class HelpCommand
    extends OsgiCommandSupport
{
    @Requirement
    private CommandRegistry commandRegistry;

    @Requirement
    private LayoutManager layoutManager;

    @Requirement
    private Branding branding;

    @Argument(required = false, multiValued = true)
    private List<String> path;

    private Renderer renderer = new Renderer();

    public HelpCommand(CommandRegistry commandRegistry, LayoutManager layoutManager, Branding branding) {
        this.commandRegistry = commandRegistry;
        this.layoutManager = layoutManager;
        this.branding = branding;
    }

    protected OsgiCommandSupport createCommand() throws Exception {
        return new HelpCommand(commandRegistry, layoutManager, branding);
    }

    protected Object doExecute() throws Exception {
        io.out.println();

        GroupNode gn = layoutManager.getLayout();
        if (context.getVariables().get(LayoutManager.CURRENT_NODE) != null) {
            gn = (GroupNode) context.getVariables().get(LayoutManager.CURRENT_NODE);
        }
        CommandNode cn = null;
        if (path != null) {
            for (String p : path) {
                if (cn != null) {
                    io.err.println("Unexpected path '" + p + "'");
                    return FAILURE;
                }
                Node n = gn.find(p);
                if (n == null) {
                    io.err.println("Path '" + p + "' not found!");
                    return FAILURE;
                } else if (n instanceof GroupNode) {
                    gn = (GroupNode) n;
                } else if (n instanceof CommandNode) {
                    cn = (CommandNode) n;
                } else if (n instanceof AliasNode) {
                    cn = (CommandNode) layoutManager.findNode(gn, ((AliasNode) n).getCommand());
                } else {
                    throw new IllegalStateException("Unsupported node type " + n.getParent().getName());
                }
            }
        }

        if (cn == null) {
            if (gn == layoutManager.getLayout()) {
                io.out.print(branding.getAbout());
                io.out.println();
            }
            displayGroupCommands(gn);
        }
        else {
            displayCommandHelp(cn.getId());
        }

        return SUCCESS;
    }

    private void displayGroupCommands(final GroupNode group) throws Exception {
        int maxNameLen = 20; // FIXME: Figure this out dynamically

        boolean hasShells = false;

        if (group == layoutManager.getLayout()) {
            io.out.println("Available commands:");
        } else {
            io.out.println("Available commands in " + Renderer.encode(group.getName(), Code.BOLD) + ":");
        }

        SortedSet<Node> nodes = new TreeSet<Node>(new Comparator<Node>() {
            public int compare(Node o1, Node o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        nodes.addAll(group.nodes());

        // First display command/aliases nodes
        for (Node child : nodes) {
            if (child instanceof CommandNode) {
                try {
                    CommandNode node = (CommandNode) child;
                    String name = StringUtils.rightPad(node.getName(), maxNameLen);

                    Command command = commandRegistry.lookup(node.getId());
                    String desc = command.getDescription();

                    io.out.print("  ");
                    io.out.print(renderer.render(Renderer.encode(name, Code.BOLD)));

                    if (desc != null) {
                        io.out.print("  ");
                        io.out.println(desc);
                    }
                    else {
                        io.out.println();
                    }
                } catch (NotRegisteredException e) {
                    // Ignore those exceptions (command will not be displayed)
                }
            }
            else if (child instanceof AliasNode) {
                AliasNode node = (AliasNode) child;
                String name = StringUtils.rightPad(node.getName(), maxNameLen);
                String cmd = layoutManager.findNode(group, node.getCommand()).getName();

                io.out.print("  ");
                io.out.print(renderer.render(Renderer.encode(name, Code.BOLD)));
                io.out.print("  ");

                io.out.print("Alias to: ");

                io.out.println(renderer.render(Renderer.encode(cmd, Code.BOLD)));
            } else if (child instanceof GroupNode) {
                hasShells = true;
            }
        }

        io.out.println();

        if (hasShells) {
            io.out.println("Available shells:");
            // Then groups
            for (Node child : nodes) {
                if (child instanceof GroupNode) {
                    GroupNode node = (GroupNode) child;
                    io.out.print("  ");
                    io.out.println(renderer.render(Renderer.encode(node.getName(), Code.BOLD)));
                }
            }
            io.out.println();
        }
    }

    private String extractCommandName(String command) {
        return command.substring(command.lastIndexOf(':') + 1);
    }

    private void displayCommandHelp(final String path) throws Exception {
        assert path != null;

        Command cmd = commandRegistry.lookup(path);

        if (cmd == null) {
            io.out.println("Command " + Renderer.encode(path, Code.BOLD) + " not found.");
            io.out.println("Try " + Renderer.encode("help", Code.BOLD) + " for a list of available commands.");
        }
        else {
            io.out.println("Command " + Renderer.encode(extractCommandName(path), Code.BOLD));
            io.out.println("   " + cmd.getDescription());

            cmd.execute(context, "--help");
        }

        io.out.println();
    }
}

