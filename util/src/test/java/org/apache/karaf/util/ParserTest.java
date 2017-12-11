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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.util.maven.Parser;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParserTest {

    private final static String PATH_WITH_CLASSIFIER = "org/apache/karaf/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT-feature.xml";
    private final static String PATH_WITHOUT_CLASSIFIER = "org/apache/karaf/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.xml";

    @Test
    public void parserTest() {
        Map parts = new HashMap();
        String uri = Parser.pathToMaven(PATH_WITH_CLASSIFIER, parts);
        assertEquals("mvn:org.apache.karaf/test/1.0-SNAPSHOT/xml/feature", uri);
        assertEquals("feature", parts.get("classifier"));
        uri = Parser.pathToMaven(PATH_WITHOUT_CLASSIFIER, parts);
        assertEquals("mvn:org.apache.karaf/test/1.0-SNAPSHOT/xml", uri);
        assertNull(parts.get("classifier"));
    }

    @Test
    public void unparserTest() throws MalformedURLException {
        Parser p1 = new Parser("org.apache/karaf/1/xml/features");
        assertThat(p1.toMvnURI(), equalTo("org.apache/karaf/1/xml/features"));
        Parser p2 = new Parser("org.apache/karaf/1/xml");
        assertThat(p2.toMvnURI(), equalTo("org.apache/karaf/1/xml"));
        Parser p3 = new Parser("org.apache/karaf/1/jar/uber");
        assertThat(p3.toMvnURI(), equalTo("org.apache/karaf/1/jar/uber"));
        Parser p4 = new Parser("org.apache/karaf/1//uber");
        assertThat(p4.toMvnURI(), equalTo("org.apache/karaf/1/jar/uber"));
        Parser p5 = new Parser("org.apache/karaf/1/jar");
        assertThat(p5.toMvnURI(), equalTo("org.apache/karaf/1"));
        Parser p6 = new Parser("org.apache/karaf/1");
        assertThat(p6.toMvnURI(), equalTo("org.apache/karaf/1"));
    }

}
