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
package org.apache.karaf.shell.commands.impl;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute a Java standard application.
 *
 * <p>By default looks for static main(String[]) to execute, but
 * you can specify a different static method that takes a String[]
 * to execute instead.
 */
@Command(scope = "shell", name = "java", description = "Executes a Java standard application.")
@Service
public class JavaAction implements Action {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "-m", aliases = {"--method"}, description = "Invoke a named method", required = false, multiValued = false)
    private String methodName = "main";

    @Argument(index = 0, name = "className", description="The name of the class to invoke", required = true, multiValued = false)
    private String className;

    @Argument(index = 1, name = "arguments", description="Arguments to pass to the method of the given class", required = false, multiValued = false)
    private List<String> args;

    @Override
    public Object execute() throws Exception {
        boolean info = log.isInfoEnabled();

        Class type = Thread.currentThread().getContextClassLoader().loadClass(className);
        if (info) {
            log.info("Using type: " + type);
        }

        Method method = type.getMethod(methodName, String[].class);
        if (info) {
            log.info("Using method: " + method);
        }

        if (info) {
            log.info("Invoking w/arguments: {}", args);
        }

        Object result = method.invoke(null, args);

        if (info) {
            log.info("Result: " + result);
        }

        return null;
    }

}
