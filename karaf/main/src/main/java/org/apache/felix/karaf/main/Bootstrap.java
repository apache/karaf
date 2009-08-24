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

    public static final String FRAMEWORK_PROPERTIES_FILE_NAME = "config.properties";

    public static final String KARAF_FRAMEWORK = "karaf.framework";

    public static void main(String[] args) {
        try {
            updateClassLoader();
            Main.main(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static Main launch(String[] args) throws Exception {
        updateClassLoader();
        Main main = new Main(args);
        main.launch();
        return main;
    }

    private static void updateClassLoader() throws Exception {
        File home = Utils.getKarafHome();
        File base = Utils.getKarafBase(home);
        File file = new File(new File(base, "etc"), FRAMEWORK_PROPERTIES_FILE_NAME);
        if (!file.exists()) {
            file = new File(new File(home, "etc"), FRAMEWORK_PROPERTIES_FILE_NAME);
        }
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        Properties props = new Properties();
        InputStream is = new FileInputStream(file);
        props.load(is);
        is.close();

        String framework = props.getProperty(KARAF_FRAMEWORK);
        if (framework == null) {
            throw new IllegalArgumentException("Property " + KARAF_FRAMEWORK + " must be set in the etc/" + FRAMEWORK_PROPERTIES_FILE_NAME + " configuration file");
        }
        String bundle = props.getProperty(KARAF_FRAMEWORK + "." + framework);
        if (bundle == null) {
            throw new IllegalArgumentException("Property " + KARAF_FRAMEWORK + "." + framework + " must be set in the etc/" + FRAMEWORK_PROPERTIES_FILE_NAME + " configuration file");
        }
        File bundleFile = new File(base, bundle);
        if (!bundleFile.exists()) {
            bundleFile = new File(home, bundle);
        }
        if (!bundleFile.exists()) {
            throw new FileNotFoundException(bundleFile.getAbsolutePath());
        }

        URLClassLoader classLoader = (URLClassLoader) Bootstrap.class.getClassLoader();
        Method mth = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        mth.setAccessible(true);
        mth.invoke(classLoader, bundleFile.toURL());
    }
}
