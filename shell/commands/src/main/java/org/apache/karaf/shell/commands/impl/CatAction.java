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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

/**
 * Concatenate and print files and/or URLs.
 */
@Command(scope = "shell", name = "cat", description = "Displays the content of a file or URL.")
public class CatAction extends AbstractAction {

    @Option(name = "-n", aliases = {}, description = "Number the output lines, starting at 1.", required = false, multiValued = false)
    private boolean displayLineNumbers;

    @Option(name = "-", description = "Use stdin")
    private boolean stdin;

    @Argument(index = 0, name = "paths or urls", description = "A list of file paths or urls to display separated by whitespace (use - for STDIN)", required = false, multiValued = true)
    private List<String> paths;

    protected Object doExecute() throws Exception {
        if (stdin) {
            paths = Collections.singletonList("-");
        }

        if (paths == null) {
            throw new RuntimeException("Need to supply a path");
        }

        for (String filename : paths) {
            BufferedReader reader = new BufferedReader(createReader(filename));
            try {
                cat(reader);
            } finally {
                closeReader(reader);
            }
        }

        return null;
    }

    private void closeReader(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Create a reader for a url orfor a file
     * 
     * @param urlOrfileName
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private Reader createReader(String urlOrfileName) throws IOException, FileNotFoundException {
        if ("-".equals(urlOrfileName)) {
            log.debug("Printing STDIN");
            return new InputStreamReader(System.in);
        }
        try {
            URL url = new URL(urlOrfileName);
            log.debug("Printing URL: " + url);
            return new InputStreamReader(url.openStream());
        } catch (MalformedURLException ignore) {
            File file = new File(urlOrfileName);
            log.debug("Printing file: " + file);
            return new FileReader(file);
        }
    }

    private void cat(final BufferedReader reader) throws IOException {
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
