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
package org.apache.karaf.shell.commands;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

/**
 * TODO
 */
@Command(scope = "shell", name = "source", description = "Run a script")
public class SourceAction extends AbstractAction {

    @Argument(index = 0, name = "script", description = "A URI pointing to the script", required = true, multiValued = false)
    private String script;

    @Argument(index = 1, name = "args", description = "Arguments for the script", required = false, multiValued = true)
    private List<Object> args;

    @Override
    protected Object doExecute() throws Exception {
        BufferedReader reader = null;
        Object arg0 = session.get("0");
        try {
            // First try a URL
            try {
                URL url = new URL(script);
                log.info("Printing URL: " + url);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            catch (MalformedURLException ignore) {
                // They try a file
                File file = new File(script);
                log.info("Printing file: " + file);
                reader = new BufferedReader(new FileReader(file));
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

            return session.execute(w.toString());
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
    }
}
