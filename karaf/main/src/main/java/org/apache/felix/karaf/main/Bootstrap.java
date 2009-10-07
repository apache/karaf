/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.karaf.main;

import java.util.Properties;
import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class Bootstrap {

    public static void main(String[] args) {
        try {
            Main.main(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static Main launch(String[] args) throws Exception {
        Main main = new Main(args);
        main.launch();
        return main;
    }

}
