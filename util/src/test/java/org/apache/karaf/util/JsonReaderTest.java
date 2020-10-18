package org.apache.karaf.util;
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
import org.apache.karaf.util.json.JsonReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.Map;

public class JsonReaderTest {

    @Test
    public void testReader() throws Exception {
        String json = "{" +
                "\"foo\":\"bar\"," +
                "\"hello\":\"world\"" +
                "}";
        Map<String, Object> result = (Map<String, Object>) JsonReader.read(new StringReader(json));
        Assert.assertEquals("bar", result.get("foo"));
        Assert.assertEquals("world", result.get("hello"));
    }

}
