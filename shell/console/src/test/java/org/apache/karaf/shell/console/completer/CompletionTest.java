/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console.completer;

import java.util.Arrays;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.commands.basic.Context;
import org.apache.karaf.shell.commands.basic.SimpleCommand;
import org.apache.karaf.shell.commands.basic.SimpleSubShell;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.ExitAction;
import org.apache.karaf.shell.console.SessionProperties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompletionTest extends CompleterTestSupport {

    @Test
    public void testSubShellCompletion() throws Exception {
        Context context = new Context();
        context.set("SCOPE", "*");
        context.set(SessionProperties.COMPLETION_MODE, "subshell");
        CommandSessionHolder.setSession(context.getSession());

        context.addCommand("*", new SimpleSubShell("foo"), "foo");
        context.addCommand("*", new SimpleCommand(ExitAction.class), "exit");
        context.addCommand("foo", new SimpleCommand(MyAction.class), "my-action");
        context.addCommand("foo", new SimpleCommand(MyActionTwoArguments.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyAction.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyActionTwoArguments.class), "another");

        Completer comp = new CommandsCompleter(context.getSession());

        context.execute("foo");
        assertEquals(Arrays.asList("my-action "), complete(comp, "my"));
        assertEquals(Arrays.asList("exit ", "my-action ", "one-action "), complete(comp, ""));
        assertEquals(Arrays.asList(), complete(comp, "an"));
        assertEquals(Arrays.asList("--check", "--foo", "--help", "--integer", "--string"),
                     complete(comp, "my-action --"));
        assertEquals(Arrays.asList("--dummy", "--help"), complete(comp, "one-action --"));

        context.execute("exit");
        assertEquals(Arrays.asList(), complete(comp, "my"));
        assertEquals(Arrays.asList("exit ", "foo "), complete(comp, ""));
        assertEquals(Arrays.asList(), complete(comp, "an"));
    }

    @Test
    public void testFirstCompletion() throws Exception {
        Context context = new Context();
        context.set("SCOPE", "*");
        context.set(SessionProperties.COMPLETION_MODE, "first");
        CommandSessionHolder.setSession(context.getSession());

        context.addCommand("*", new SimpleSubShell("foo"), "foo");
        context.addCommand("*", new SimpleCommand(ExitAction.class), "exit");
        context.addCommand("foo", new SimpleCommand(MyAction.class), "my-action");
        context.addCommand("foo", new SimpleCommand(MyActionTwoArguments.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyAction.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyActionTwoArguments.class), "another");

        Completer comp = new CommandsCompleter(context.getSession());

        context.execute("foo");
        assertEquals(Arrays.asList("my-action "), complete(comp, "my"));
        assertEquals(Arrays.asList("my-action ", "one-action "), complete(comp, ""));
        assertEquals(Arrays.asList("another "), complete(comp, "an"));
        assertEquals(Arrays.asList("--check", "--foo", "--help", "--integer", "--string"),
                     complete(comp, "my-action --"));
        assertEquals(Arrays.asList("--dummy", "--help"), complete(comp, "one-action --"));

        context.execute("exit");
        assertEquals(Arrays.asList("my-action "), complete(comp, "my"));
        assertEquals(Arrays.asList("*:exit", "*:foo", "another", "bar:another",
                                   "bar:one-action", "exit", "foo",
                                   "foo:my-action", "foo:one-action", "my-action",
                                   "one-action", "one-action"), complete(comp, ""));
        assertEquals(Arrays.asList("another "), complete(comp, "an"));
    }

    @Test
    public void testGlobalCompletion() throws Exception {
        Context context = new Context();
        context.set("SCOPE", "*");
        context.set(SessionProperties.COMPLETION_MODE, "global");
        CommandSessionHolder.setSession(context.getSession());

        context.addCommand("*", new SimpleSubShell("foo"), "foo");
        context.addCommand("*", new SimpleCommand(ExitAction.class), "exit");
        context.addCommand("foo", new SimpleCommand(MyAction.class), "my-action");
        context.addCommand("foo", new SimpleCommand(MyActionTwoArguments.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyAction.class), "one-action");
        context.addCommand("bar", new SimpleCommand(MyActionTwoArguments.class), "another");

        Completer comp = new CommandsCompleter(context.getSession());

        context.execute("foo");
        assertEquals(Arrays.asList("my-action "), complete(comp, "my"));
        assertEquals(Arrays.asList("*:exit", "*:foo", "another", "bar:another",
                                    "bar:one-action", "exit", "foo",
                                   "foo:my-action", "foo:one-action", "my-action",
                                   "one-action", "one-action"), complete(comp, ""));
        assertEquals(Arrays.asList("another "), complete(comp, "an"));
        assertEquals(Arrays.asList("--check", "--foo", "--help", "--integer", "--string"),
                     complete(comp, "my-action --"));
        assertEquals(Arrays.asList("--dummy", "--help"), complete(comp, "one-action --"));

        context.execute("exit");
        assertEquals(Arrays.asList("my-action "), complete(comp, "my"));
        assertEquals(Arrays.asList("*:exit", "*:foo", "another", "bar:another",
                                   "bar:one-action", "exit", "foo",
                                   "foo:my-action", "foo:one-action", "my-action",
                                   "one-action", "one-action"), complete(comp, ""));
        assertEquals(Arrays.asList("another "), complete(comp, "an"));
    }

    public static class MyAction implements Action {
        @Option(name = "-f", aliases = { "--foo" })
        int f;

        @Option(name = "-c", aliases = "--check")
        boolean check;

        @Option(name = "-s", aliases = "--string")
        String string;

        @Option(name = "-i", aliases = "--integer")
        String integer;

        public Object execute(CommandSession session) throws Exception {
            return null;
        }
    }

    public static class MyActionTwoArguments implements Action {

        @Option(name = "--dummy")
        boolean dummy;

        @Argument(index = 0, name = "one", description = "one description", required = true, multiValued = false)
        private String one;

        @Argument(index = 1, name = "two", description = "two description", required = true, multiValued = false)
        private String two;

        public Object execute(CommandSession session) throws Exception {
            return null;
        }

    }
}
