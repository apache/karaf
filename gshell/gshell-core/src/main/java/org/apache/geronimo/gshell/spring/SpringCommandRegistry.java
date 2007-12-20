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
package org.apache.geronimo.gshell.spring;

import java.util.Map;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.common.Arguments;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.layout.NotFoundException;
import org.apache.geronimo.gshell.layout.model.AliasNode;
import org.apache.geronimo.gshell.layout.model.CommandNode;
import org.apache.geronimo.gshell.layout.model.GroupNode;
import org.apache.geronimo.gshell.layout.model.Layout;
import org.apache.geronimo.gshell.layout.model.Node;
import org.apache.geronimo.gshell.registry.DefaultCommandRegistry;
import org.apache.geronimo.gshell.registry.DuplicateRegistrationException;
import org.apache.geronimo.gshell.registry.NotRegisteredException;
import org.apache.geronimo.gshell.shell.Environment;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 11, 2007
 * Time: 3:47:24 PM
 * To change this template use File | Settings | File Templates.
 */
    public class SpringCommandRegistry extends DefaultCommandRegistry implements LayoutManager {

    public static final String SEPARATOR = ":";

    public static final String ALIAS_PREFIX = "alias:";

    private Environment env;

    private MutableLayout layout = new MutableLayout();

    public SpringCommandRegistry(Environment env) {
        this.env = env;
    }

    public void register(final Command command, Map<String, ?> properties) throws DuplicateRegistrationException {
        // Find command name
        String name = command.getId();
        if (name.lastIndexOf(':') >= 0) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }
        if (properties.containsKey("name")) {
            name = (String) properties.get("name");
        }

        // Find rank
        int rank = 0;
        if (properties.containsKey("rank")) {
            rank = Integer.parseInt((String) properties.get("rank"));
        }

        // Find or create the subshell group
        GroupNode gn = layout;
        String shell = (String) properties.get("shell");
        String[] aliases = properties.get("alias") != null ? properties.get("alias").toString().split(",") : new String[0];

        if (name.equals(shell))
        {
            Node n = gn.find(shell);
            MutableGroupNode g = new MutableGroupNode(shell);
            gn.add(g);
            register(command);
        }
        else
        {
            if (shell != null && shell.length() > 0) {
                Node n = gn.find(shell);
                if (n == null) {
                    MutableGroupNode g = new MutableGroupNode(shell);
                    gn.add(g);
                    register(new GroupCommand(shell, g));
                    gn = g;
                } else if (n instanceof GroupNode) {
                    gn = (GroupNode) n;
                } else {
                    throw new IllegalStateException("A command conflicts has been detected when registering " + command.getId());
                }
            }

            CommandNode cn = new CommandNode(name, command.getId());
            gn.add(cn);

            for (int i = 0; i < aliases.length; i++) {
                if (!name.equals(aliases[i])) {
                    AliasNode an = new AliasNode(aliases[i], ALIAS_PREFIX + command.getId());
                    gn.add(an);
                }
            }

            register(command);
        }
    }

    public void unregister(final Command command, Map<String, ?> properties) throws NotRegisteredException {
        // Find command name
        String name = command.getId();
        if (name.lastIndexOf(':') >= 0) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }
        if (properties.containsKey("name")) {
            name = (String) properties.get("name");
        }
        String shell = (String) properties.get("shell");

        if (name.equals(shell) || shell == null || shell.length() == 0) {
            Node n = layout.find(name);
            layout.removeNode(n);
        } else {
            MutableGroupNode gn = (MutableGroupNode) layout.find(shell);
            Node n = gn.find(name);
            gn.removeNode(n);
            if (gn.size() == 0) {
                layout.removeNode(gn);
            }
        }

        unregister(command);
    }

    public Layout getLayout() {
        return layout;
    }

    public Node findNode(String s) throws NotFoundException {
        Node start = (Node) env.getVariables().get(CURRENT_NODE);
        if (start != null) {
            try {
                return findNode(start, s);
            } catch (NotFoundException e) {
                // Ignore, we need to try at root level
            }
        }
        return findNode(layout, s);
    }

    public Node findNode(Node node, String s) throws NotFoundException {
        if (node instanceof GroupNode) {
            if (s.startsWith(ALIAS_PREFIX)) {
                s = s.substring(ALIAS_PREFIX.length());
                for (Node n : ((GroupNode) node).nodes()) {
                    if (n instanceof CommandNode && ((CommandNode) n).getId().equals(s)) {
                        return n;
                    }
                }
                throw new NotFoundException(s);
            }
            Node n = ((GroupNode) node).find(s);
            if (n == null) {
                throw new NotFoundException(s);
            }
            if (n instanceof GroupNode) {
                return new CommandNode(n.getName(), n.getName());
            }
            return n;
        } else {
            throw new NotFoundException(s);
        }
    }

    public class GroupCommand implements Command {

        private String id;
        private GroupNode gn;

        public GroupCommand(String id, GroupNode gn) {
            this.id = id;
            this.gn = gn;
        }

        @Deprecated
        public String getId() {
            return id;
        }

        @Deprecated
        public String getDescription() {
            return "Group command";
        }

        public Object execute(CommandContext commandContext, Object... objects) throws Exception {
            env.getVariables().set(CURRENT_NODE, gn);
            if (objects.length > 0) {
                try {
                    String cmdId = String.valueOf(objects[0]);
                    Node n = gn.find(cmdId);
                    if (n == null) {
                        n = layout.find(cmdId);
                    }
                    CommandContext ctx = commandContext;
                    Command cmd;
                    if (n instanceof CommandNode) {
                        cmd = lookup(((CommandNode) n).getId());
                    } else if (n instanceof GroupNode) {
                        cmd = new GroupCommand(cmdId, (GroupNode) n);
                    } else if (n instanceof AliasNode) {
                        cmd = lookup(((AliasNode) n).getCommand().substring(ALIAS_PREFIX.length()));
                    } else {
                        throw new IllegalStateException("Unrecognized node type: " + n.getClass().getName());
                    }
                    return cmd.execute(ctx, Arguments.shift(objects));
                } finally {
                    env.getVariables().unset(CURRENT_NODE);
                }
            }
            return SUCCESS;
        }
    }

    public static class MutableLayout extends Layout {

        public void removeNode(Node n) {
            nodes.remove(n);
        }

    }

    public static class MutableGroupNode extends GroupNode {

        public MutableGroupNode(String name) {
            super(name);
        }

        public void removeNode(Node n) {
            nodes.remove(n);
        }

    }
}
