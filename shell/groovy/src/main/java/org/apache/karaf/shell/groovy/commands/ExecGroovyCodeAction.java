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

package org.apache.karaf.shell.groovy.commands;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Command(scope = "groovy",
        name = "exec",
        description = "Executes Groovy code")
@Service
public class ExecGroovyCodeAction implements Action {

    @Argument(index = 0, name = "script", description = "Groovy code", required = true)
    private String groovyCode;


    @Argument(index = 1, name = "args", description = "Arguments in the format of key=value", multiValued = true)
    private List<String> args;

    @Override
    public Object execute() throws Exception {
        Binding binding = new Binding();

        if (args != null)
        {
            for (String arg : args)
            {
                int splitAt = arg.indexOf("=");
                if (splitAt <= 0)
                {
                    throw new IllegalArgumentException("Invalid argument " + arg);
                }
                else
                {
                    String key = arg.substring(0, splitAt);
                    String value = arg.substring(splitAt + 1);
                    binding.setVariable(key, value);
                }
            }
        }

        GroovyShell sh = new GroovyShell(binding);
        System.out.println(sh.evaluate(groovyCode));

        return null;
    }
}
