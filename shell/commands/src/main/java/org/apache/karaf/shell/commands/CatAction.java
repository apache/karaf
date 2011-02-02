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
package org.apache.karaf.shell.commands;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.karaf.shell.console.AbstractAction;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;

/**
 * Concatenate and print files and/or URLs.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@Command(scope = "shell", name = "cat", description = "Displays the content of a file or URL.")
public class CatAction extends AbstractAction {

    @Option(name = "-n", aliases = {}, description = "The number the output lines, starting at 1.", required = false, multiValued = false)
    private boolean displayLineNumbers;

    @Argument(index = 0, name = "paths or urls", description = "A list of file paths or urls to display separated by whitespaces (use - for STDIN)", required = true, multiValued = true)
    private List<String> paths;

    protected Object doExecute() throws Exception {
        //
        // Support "-" if length is one, and read from io.in
        // This will help test command pipelines.
        //
        if (paths.size() == 1 && "-".equals(paths.get(0))) {
            log.info("Printing STDIN");
            cat(new BufferedReader(new InputStreamReader(System.in)));
        }
        else {
            for (String filename : paths) {
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
                    cat(reader);
                }
                finally {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        return null;
    }

    private void cat(final BufferedReader reader) throws IOException
    {
        String line;
        int lineno = 1;

        while ((line = reader.readLine()) != null) {
            if (displayLineNumbers) {
                System.out.print(String.format("%6d  ", lineno++));
            }
            System.out.println(line);
        }
    }
}
