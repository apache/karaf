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
package org.apache.karaf.shell.commands.impl;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileOrUriCompleter;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
@Command(scope = "shell", name = "source", description = "Run a script")
@Service
public class SourceAction implements Action {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Argument(index = 0, name = "script", description = "A URI pointing to the script", required = true, multiValued = false)
    @Completion(FileOrUriCompleter.class)
    private String script;

    @Argument(index = 1, name = "args", description = "Arguments for the script", required = false, multiValued = true)
    private List<Object> args;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        BufferedReader reader = null;
        Object arg0 = session.get("0");
        try {
            try {
                URL url = new URI(script).toURL();
                log.info("Printing URL: " + url);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            catch (MalformedURLException | IllegalArgumentException ignore) {
                // fallback to a file
                Path file = session.currentDir().resolve(script);
                log.info("Printing file: " + file);
                reader = new BufferedReader(Files.newBufferedReader(file));
            }

            CharArrayWriter w = new CharArrayWriter();
            int n;
            char[] buf = new char[8192];
            while ((n = reader.read(buf)) > 0) {
                w.write(buf, 0, n);
            }

            for (int i = 0; args != null && i < args.size(); i++) {
                session.put( Integer.toString(i+1), args.get(i) );
            }

            session.execute(w.toString());
        } finally {
            for (int i = 0; args != null && i < args.size(); i++) {
                session.put( Integer.toString(i+1), null );
            }
            session.put("0", arg0);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return null;
    }
}
