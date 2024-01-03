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
package org.apache.karaf.config.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Command(scope = "config", name = "install", description = "Install a cfg file in the Karaf etc folder.")
@Service
public class InstallCommand implements Action {

    @Argument(index = 0, name = "url", description = "The URL of the cfg file.", required = true, multiValued = false)
    private String url;

    @Argument(index = 1, name = "finalname", description = "Final name of the cfg file", required = true, multiValued = false)
    private String finalname;

    @Option(name = "-o", aliases = { "--override" }, description = "Override the target cfg file", required = false, multiValued = false)
    private boolean override;

    @Override
    public Object execute() throws Exception {
        if (finalname.contains("..")) {
            throw new IllegalArgumentException("For security reason, relative path is not allowed in config file final name");
        }
        File etcFolder = new File(System.getProperty("karaf.etc"));
        File file = new File(etcFolder, finalname);
        if (file.exists()) {
            if (!override) {
                throw new IllegalArgumentException("Configuration file {} already exists " + finalname);
            } else {
                System.out.println("Overriding configuration file " + finalname);
            }
        } else {
            System.out.println("Creating configuration file " + finalname);
        }

        try (InputStream is = new BufferedInputStream(new URL(url).openStream())) {
            if (!file.exists()) {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                }
                file.createNewFile();
            }
            try (FileOutputStream fop = new FileOutputStream(file)) {
                StreamUtils.copy(is, fop);
            }
        } catch (RuntimeException | MalformedURLException e) {
            throw e;
        }
        return null;
    }

}
