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

package org.apache.geronimo.gshell.commands.builtins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.annotation.Requirement;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.codehaus.plexus.util.IOUtil;

/**
 * Read and execute commands from a file/url in the current shell environment.
 *
 * @version $Rev: 600779 $ $Date: 2007-12-04 04:55:33 +0100 (Tue, 04 Dec 2007) $
 */
@CommandComponent(id="gshell-builtins:source", description="Load a file/url into the current shell")
public class SourceCommand
    extends OsgiCommandSupport
{
    @Requirement
    private CommandExecutor executor;

    @Argument(required=true, description="Source file")
    private String source;

    protected Object doExecute() throws Exception {
        URL url;

        try {
            url = new URL(source);
        }
        catch (MalformedURLException e) {
            url = new File(source).toURI().toURL();
        }

        BufferedReader reader = openReader(url);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String tmp = line.trim();

                // Ignore empty lines and comments
                if (tmp.length() == 0 || tmp.startsWith("#")) {
                    continue;
                }

                executor.execute(line);
            }
        }
        finally {
            IOUtil.close(reader);
        }

        return SUCCESS;
    }

    private BufferedReader openReader(final Object source) throws IOException {
        BufferedReader reader;

        if (source instanceof File) {
            File file = (File)source;
            log.info("Using source file: {}", file);

            reader = new BufferedReader(new FileReader(file));
        }
        else if (source instanceof URL) {
            URL url = (URL)source;
            log.info("Using source URL: {}", url);

            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        }
        else {
            String tmp = String.valueOf(source);

            // First try a URL
            try {
                URL url = new URL(tmp);
                log.info("Using source URL: {}", url);

                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            catch (MalformedURLException ignore) {
                // They try a file
                File file = new File(tmp);
                log.info("Using source file: {}", file);

                reader = new BufferedReader(new FileReader(tmp));
            }
        }

        return reader;
    }
}
