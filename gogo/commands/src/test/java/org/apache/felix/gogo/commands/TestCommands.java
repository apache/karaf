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

import java.util.List;
import java.util.Collections;
import java.util.Arrays;

import junit.framework.TestCase;
import org.osgi.service.command.CommandSession;
import org.apache.felix.gogo.runtime.shell.Context;
import org.apache.felix.gogo.commands.basic.SimpleCommand;

public class TestCommands extends TestCase {


    public void testCommand() throws Exception {
        Context c= new Context();
        c.addCommand("my-action", new SimpleCommand(MyAction.class));

        // Test help
        c.execute("my-action --help");

        // Test required argument
        try
        {
            c.execute("my-action");
            fail("Action should have thrown an exception because of a missing argument");
        }
        catch (IllegalArgumentException e)
        {
        }

        // Test required argument
        assertEquals(Arrays.asList(3), c.execute("my-action 3"));

        // Test required argument
        assertEquals(Arrays.asList(3), c.execute("my-action 3"));

        // Test required argument
        assertEquals(Arrays.asList(3, 5), c.execute("my-action 3 5"));

        // Test option
        assertEquals(Arrays.asList(4), c.execute("my-action -i 3"));

        // Test option alias
        assertEquals(Arrays.asList(4), c.execute("my-action --increment 3"));
    }

    @Command(scope = "test", name = "my-action", description = "My Action")
    public static class MyAction implements Action
    {

        @Option(name = "-i", aliases = { "--increment" }, description = "First option")
        private boolean increment;

        @Argument(name = "ids", description = "Bundle ids", required = true, multiValued = true)
        private List<Integer> ids;

        public Object execute(CommandSession session) throws Exception {
            if (increment)
            {
                for (int i = 0; i < ids.size(); i++)
                {
                    ids.set(i, ids.get(i) + 1);
                }
            }
            return ids;
        }
    }
}
