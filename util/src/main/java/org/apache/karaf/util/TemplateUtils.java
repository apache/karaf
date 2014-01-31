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
package org.apache.karaf.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TemplateUtils {
    private TemplateUtils() {
    }

    public static void createFromTemplate(File outFile, InputStream templateIs, HashMap<String, String> properties) {
        if (outFile.exists()) {
            throw new IllegalArgumentException("File " + outFile.getPath()
                                               + " already exists. Remove it if you wish to recreate it.");
        }
        PrintStream out = null;
        Scanner scanner = null;
        try {
            // read it line at a time so that we can use the platform line ending when we write it out
            out = new PrintStream(new FileOutputStream(outFile));
            scanner = new Scanner(templateIs);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = filter(line, properties);
                out.println(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Can not create " + outFile, e);
        } finally {
            safeClose(out);
            safeClose(templateIs);
        }
    }

    private static String filter(String line, HashMap<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            int p1 = line.indexOf(key);
            if (p1 >= 0) {
                String l1 = line.substring(0, p1);
                String l2 = line.substring(p1 + key.length());
                line = l1 + entry.getValue() + l2;
            }
        }
        return line;
    }

    private static void safeClose(Closeable cl) {
        if (cl == null) {
            return;
        }
        try {
            cl.close();
        } catch (Throwable ignore) {
            // nothing to do
        }
    }

}
