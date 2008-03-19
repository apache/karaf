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
package org.apache.geronimo.gshell.commands.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Concatenate and print files and/or URLs.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@CommandComponent(id="utils:cat", description="Concatenate and print files and/or URLs")
public class CatCommand extends OsgiCommandSupport
{
    @Option(name="-n", description="Number the output lines, starting at 1")
    private boolean displayLineNumbers;

    @Argument(description="File or URL", required=true)
    private List<String> args;

    protected Object doExecute() throws Exception {
        //
        // Support "-" if length is one, and read from io.in
        // This will help test command pipelines.
        //
        if (args.size() == 1 && "-".equals(args.get(0))) {
            log.info("Printing STDIN");
            cat(new BufferedReader(io.in), io);
        }
        else {
            for (String filename : args) {
                BufferedReader reader;

                // First try a URL
                try {
                    URL url = new URL(filename);
                    log.info("Printing URL: " + url);
                    reader = new BufferedReader(new InputStreamReader(url.openStream()));
                }
                catch (MalformedURLException ignore) {
                    // They try a file
                    File file = new File(filename);
                    log.info("Printing file: " + file);
                    reader = new BufferedReader(new FileReader(file));
                }

                try {
                    cat(reader, io);
                }
                finally {
                    IOUtil.close(reader);
                }
            }
        }

        return SUCCESS;
    }

    private void cat(final BufferedReader reader, final IO io) throws IOException {
        String line;
        int lineno = 1;

        while ((line = reader.readLine()) != null) {
            if (displayLineNumbers) {
                String gutter = StringUtils.leftPad(String.valueOf(lineno++), 6);
                io.out.print(gutter);
                io.out.print("  ");
            }
            io.out.println(line);
        }
    }
}
