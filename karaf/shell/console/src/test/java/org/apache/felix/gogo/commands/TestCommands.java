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
import java.util.Arrays;
import java.io.IOException;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;
import org.osgi.service.command.CommandSession;
import org.apache.felix.gogo.commands.basic.SimpleCommand;

public class TestCommands extends TestCase {


    public void testPrompt() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.set("USER", "gnodet");
        c.set("APPLICATION", "karaf");
        //c.set("SCOPE", "");
        Object p = c.execute("echo \"@|bold ${USER}|@${APPLICATION}:@|bold ${SCOPE}|> \"");
        System.out.println("Prompt: " + p);
    }

    public void testCommand() throws Exception {
        Context c= new Context();
        c.addCommand("capture", this);
        c.addCommand("my-action", new SimpleCommand(MyAction.class));

        // Test help
        Object help = c.execute("my-action --help | capture");
        assertTrue(help instanceof String);
        assertTrue(((String) help).indexOf("My Action") >= 0);
        assertTrue(((String) help).indexOf("First option") >= 0);
        assertTrue(((String) help).indexOf("Bundle ids") >= 0);


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

    public String capture() throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            sw.write(s);
            s = rdr.readLine();
        }
        return sw.toString();
    }

    public CharSequence echo(Object args[])
    {
        if (args == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String del = "";
        for (Object arg : args)
        {
            sb.append(del);
            if (arg != null)
            {
                sb.append(arg);
                del = " ";
            }
        }
        return sb;
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