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
package org.apache.karaf.features.internal.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

public class JacksonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonUtil.class);

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
    }

    private JacksonUtil() {
    }

    public static boolean isJson(String uri) {
        try {
            return isJson(new URL(uri).openStream());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJson(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("{")) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public static Features unmarshal(String uri) throws IOException {
        return unmarshal(new URL(uri).openStream());
    }

    public static Features unmarshal(InputStream inputStream) throws IOException {
        return mapper.readValue(inputStream, Features.class);
    }

    public static void marshal(Features features, OutputStream outputStream) throws IOException {
        mapper.writeValue(outputStream, features);
    }

    public static void marshal(Features features, Writer writer) throws IOException {
        mapper.writeValue(writer, features);
    }

}
