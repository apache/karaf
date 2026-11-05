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
package org.apache.karaf.services.url.internal;

import java.net.MalformedURLException;

import org.junit.Test;

import static org.junit.Assert.*;

public class WrapUrlParserTest {

    @Test
    public void testJarOnly() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar");

        assertEquals("file:/repo/test.jar", parser.getWrappedJarUrl().toExternalForm());
        assertNull(parser.getInstructionsUrl());
        assertTrue(parser.getInstructions().isEmpty());
    }

    @Test
    public void testJarWithInstructions() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar$Bundle-SymbolicName=test&Bundle-Version=1.2.3");

        assertEquals("file:/repo/test.jar", parser.getWrappedJarUrl().toExternalForm());
        assertNull(parser.getInstructionsUrl());
        assertEquals(2, parser.getInstructions().size());
        assertEquals("test", parser.getInstructions().getProperty("Bundle-SymbolicName"));
        assertEquals("1.2.3", parser.getInstructions().getProperty("Bundle-Version"));
    }

    @Test
    public void testJarWithInstructionsFile() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar,file:/repo/test.bnd");

        assertEquals("file:/repo/test.jar", parser.getWrappedJarUrl().toExternalForm());
        assertEquals("file:/repo/test.bnd", parser.getInstructionsUrl().toExternalForm());
        assertTrue(parser.getInstructions().isEmpty());
    }

    @Test
    public void testJarWithInstructionsFileAndInstructions() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar,file:/repo/test.bnd$overwrite=merge");

        assertEquals("file:/repo/test.jar", parser.getWrappedJarUrl().toExternalForm());
        assertEquals("file:/repo/test.bnd", parser.getInstructionsUrl().toExternalForm());
        assertEquals("merge", parser.getInstructions().getProperty("overwrite"));
    }

    @Test
    public void testInstructionWithPackageList() throws Exception {
        WrapUrlParser parser = new WrapUrlParser(
                "file:/repo/test.jar$Export-Package=org.example.*;version=\"1.0\",org.other&Import-Package=*;resolution:=optional");

        assertEquals("file:/repo/test.jar", parser.getWrappedJarUrl().toExternalForm());
        assertNull(parser.getInstructionsUrl());
        assertEquals("org.example.*;version=\"1.0\",org.other", parser.getInstructions().getProperty("Export-Package"));
        assertEquals("*;resolution:=optional", parser.getInstructions().getProperty("Import-Package"));
    }

    @Test
    public void testInstructionValueIsDecoded() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar$Bundle-Name=My%20Bundle%26Co");

        assertEquals("My Bundle&Co", parser.getInstructions().getProperty("Bundle-Name"));
    }

    @Test
    public void testInstructionHeaderIsNormalized() throws Exception {
        WrapUrlParser parser = new WrapUrlParser(
                "file:/repo/test.jar$bundle-symbolicname=test&web-contextpath=/test&Custom-Header=value");

        assertEquals("test", parser.getInstructions().getProperty("Bundle-SymbolicName"));
        assertEquals("/test", parser.getInstructions().getProperty("Web-ContextPath"));
        assertEquals("value", parser.getInstructions().getProperty("Custom-Header"));
    }

    @Test
    public void testInstructionWithoutValue() throws Exception {
        WrapUrlParser parser = new WrapUrlParser("file:/repo/test.jar$Import-Package=");

        assertEquals("", parser.getInstructions().getProperty("Import-Package"));
    }

    @Test(expected = MalformedURLException.class)
    public void testNullPath() throws Exception {
        new WrapUrlParser(null);
    }

    @Test(expected = MalformedURLException.class)
    public void testEmptyPath() throws Exception {
        new WrapUrlParser(" ");
    }

    @Test(expected = MalformedURLException.class)
    public void testPathStartingWithInstructionsSeparator() throws Exception {
        new WrapUrlParser("$Bundle-SymbolicName=test");
    }

    @Test(expected = MalformedURLException.class)
    public void testPathEndingWithInstructionsSeparator() throws Exception {
        new WrapUrlParser("file:/repo/test.jar$");
    }

    @Test(expected = MalformedURLException.class)
    public void testInvalidWrappedJarUrl() throws Exception {
        new WrapUrlParser("not-a-url");
    }

    @Test(expected = MalformedURLException.class)
    public void testInvalidInstruction() throws Exception {
        new WrapUrlParser("file:/repo/test.jar$Bundle-SymbolicName");
    }

    @Test(expected = MalformedURLException.class)
    public void testInvalidInstructionEncoding() throws Exception {
        new WrapUrlParser("file:/repo/test.jar$Bundle-Name=100%");
    }

}
