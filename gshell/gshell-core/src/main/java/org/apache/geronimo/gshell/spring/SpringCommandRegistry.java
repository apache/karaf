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

    private Environment env;

    private Map groupAliases;
    private Layout layout = new Layout();

    public SpringCommandRegistry(Environment env) {
        this.env = env;
    }

    public Map getGroupAliases() {
        return groupAliases;
    }

    public void setGroupAliases(Map groupAliases) {
        this.groupAliases = groupAliases;
    }

    public void register(final Command command, Map<String, ?> properties) throws DuplicateRegistrationException {
        String id = command.getId();
        String[] s = id.split(SEPARATOR);
        GroupNode gn = layout;
        for (int i = 0; i < s.length - 1; i++) {
            if (groupAliases != null && groupAliases.containsKey(s[i])) {
                s[i] = (String) groupAliases.get(s[i]);
                if (s[i].length() == 0) {
                    continue;
                }
            }
            Node n = gn.find(s[i]);
            if (n == null) {
                GroupNode g = new GroupNode(s[i]);
                gn.add(g);
                register(new GroupCommand(s[i], g));
                gn = g;
            } else if (n instanceof GroupNode) {
                gn = (GroupNode) n;
            } else {
                throw new IllegalStateException("A command conflicts has been detected when registering " + id);
            }
        }
        CommandNode cn = new CommandNode(s[s.length - 1], id);
        gn.add(cn);
        register(command);
    }

    public void unregister(final Command command, Map<String, ?> properties) throws NotRegisteredException {
        unregister(command);
    }

    public Layout getLayout() {
        return layout;
    }

    public Node findNode(String s) throws NotFoundException {
        Node start = (Node) env.getVariables().get(CURRENT_NODE);
        if (start != null) {
            Node n = findNode(start, s);
            if (n != null) {
                return n;
            }
        }
        return findNode(layout, s);
    }

    public Node findNode(Node node, String s) throws NotFoundException {
        if (node instanceof GroupNode) {
            Node n = ((GroupNode) node).find(s);
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
            if (objects.length > 0) {
                String cmdId = String.valueOf(objects[0]);
                Node n = gn.find(cmdId);
                CommandContext ctx = commandContext;
                Command cmd;
                if (n instanceof CommandNode) {
                    cmd = lookup(((CommandNode) n).getId());
                } else if (n instanceof GroupNode) {
                    cmd = new GroupCommand(cmdId, (GroupNode) n);
                } else {
                    throw new IllegalStateException("Unrecognized node type: " + n.getClass().getName());
                }
                return cmd.execute(ctx, Arguments.shift(objects));
            } else {
                env.getVariables().set(CURRENT_NODE, gn);
                return SUCCESS;
            }
        }
    }
}
