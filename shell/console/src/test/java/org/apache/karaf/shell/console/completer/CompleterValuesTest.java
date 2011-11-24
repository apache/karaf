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

import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.CompleterValues;
import org.apache.karaf.shell.commands.basic.SimpleCommand;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.Completer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CompleterValuesTest extends CompleterTestSupport {

    @Test
    public void testCompleteArgumnets() throws Exception {
        CommandSession session = new DummyCommandSession();
        Completer comp = new ArgumentCompleter(session, new SimpleCommand(MyAction.class), "my:action");

        // arg 0
        assertEquals(Arrays.asList("a1", "a2", "a3"), complete(comp, "action a"));
        assertEquals(Arrays.asList("b4", "b5"), complete(comp, "action b"));

        // arg 1
        assertEquals(Arrays.asList("c2", "c3"), complete(comp, "action a1 c"));
        assertEquals(Arrays.asList("d5", "d6", "d7"), complete(comp, "action b4 d"));

        // unknown args
        assertEquals(Arrays.asList(), complete(comp, "action c"));
        assertEquals(Arrays.asList(), complete(comp, "action a1 d5 a"));
    }

    public static class MyAction implements Action {
        @Argument(index = 0)
        String foo;
        @Argument(index = 1)
        String bar;

        public Object execute(CommandSession session) throws Exception {
            return null;
        }

        @CompleterValues(index = 0)
        public String[] getFooValues() {
            return new String[]{"a1", "a2", "a3", "b4", "b5"};
        }

        @CompleterValues(index = 1)
        public List<String> getBarValues() {
            return Arrays.asList("c2", "c3", "d5", "d6", "d7");
        }
    }

}
