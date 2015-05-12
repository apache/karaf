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
package org.apache.karaf.shell.ssh;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class ConfigHelper {

    public static final String BLUEPRINT_CONFIG = "OSGI-INF/blueprint/shell-ssh.xml";
    public static final String BLUEPRINT_VALUE_PATTERN =
            "^.*<cm:property\\s*name=\"<<KEY>>\"\\s*value=\"([^\"]+)\".*$";

    public static final String CONFIG_ALGORITHM = "algorithm";
    public static final String CONFIG_CIPHERS = "ciphers";
    public static final String CONFIG_KEXALGORITHMS = "kexAlgorithms";
    public static final String CONFIG_KEYSIZE = "keySize";
    public static final String CONFIG_MACS = "macs";

    /**
     * Retrieves a value from the specified blueprint configuration key.
     *
     * @param blueprintConfig the configuration file
     * @param key the configuration key
     *
     * @return the configured value or <code>null</code>
     *
     * @throws FileNotFoundException in case the blueprint config can not be found
     * @throws IOException in case an error occurs at blueprint config file reading
     */
    public static String getValue(String blueprintConfig, String key)
            throws IOException
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(blueprintConfig);
        if (is == null) {
            // retry with our class...
            cl = ConfigHelper.class.getClassLoader();
            is = cl.getResourceAsStream(blueprintConfig);
        }
        if (is == null) {
            throw new FileNotFoundException(
                    "Unable to find blueprint configuration file: "
                    + blueprintConfig);
        }
        String xml = IOUtils.toString(is, "UTF-8");
        String regex = BLUEPRINT_VALUE_PATTERN.replaceFirst("<<KEY>>", key);
        Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Retrieves a value from the specified blueprint configuration key.
     *
     * @param key the configuration key
     *
     * @return the configured value or <code>null</code>
     *
     * @throws FileNotFoundException in case the blueprint config can not be found
     * @throws IOException in case an error occurs at blueprint config file reading
     */
    public static String getValue(String key) throws IOException {
        return getValue(BLUEPRINT_CONFIG, key);
    }

    public static Integer getValueAsInt(String key) throws IOException {
        String value = getValue(key);
        if (value != null) {
            return Integer.valueOf(value);
        }
        return null;
    }
}
