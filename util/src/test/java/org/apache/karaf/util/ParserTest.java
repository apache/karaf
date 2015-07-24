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

import junit.framework.Assert;
import org.apache.karaf.util.maven.Parser;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ParserTest {

    private final static String PATH_WITH_CLASSIFIER = "org/apache/karaf/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT-feature.xml";
    private final static String PATH_WITHOUT_CLASSIFIER = "org/apache/karaf/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.xml";

    @Test
    public void parserTest() throws Exception {
        Map parts = new HashMap();
        String uri = Parser.pathToMaven(PATH_WITH_CLASSIFIER, parts);
        Assert.assertEquals("mvn:org.apache.karaf/test/1.0-SNAPSHOT/xml/feature", uri);
        Assert.assertEquals("feature", parts.get("classifier"));
        uri = Parser.pathToMaven(PATH_WITHOUT_CLASSIFIER, parts);
        Assert.assertEquals("mvn:org.apache.karaf/test/1.0-SNAPSHOT/xml", uri);
        Assert.assertNull(parts.get("classifier"));
    }

}
