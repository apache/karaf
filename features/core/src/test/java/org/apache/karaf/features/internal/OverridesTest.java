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
package org.apache.karaf.features.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.model.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OverridesTest {

    private String bsn = "bsn";
    private File b100;
    private File b101;
    private File b102;
    private File b110;

    @Before
    public void setUp() throws IOException {
        b100 = File.createTempFile("karaf", "-100.jar");
        copy(TinyBundles.bundle()
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.0")
                .build(),
                new FileOutputStream(b100));

        b101 = File.createTempFile("karaf", "-101.jar");
        copy(TinyBundles.bundle()
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.1")
                .build(),
                new FileOutputStream(b101));

        b102 = File.createTempFile("karaf", "-102.jar");
        copy(TinyBundles.bundle()
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.2")
                .build(),
                new FileOutputStream(b102));

        b110 = File.createTempFile("karaf", "-110.jar");
        copy(TinyBundles.bundle()
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.1.0")
                .build(),
                new FileOutputStream(b110));
    }

    @Test
    public void testMatching101() throws IOException {
        File props = File.createTempFile("karaf", "properties");
        Writer w = new FileWriter(props);
        w.write(b101.toURI().toString());
        w.write("\n");
        w.write(b110.toURI().toString());
        w.write("\n");
        w.close();

        List<BundleInfo> res = Overrides.override(
                Arrays.<BundleInfo>asList(new Bundle(b100.toURI().toString())),
                props.toURI().toString());
        assertNotNull(res);
        assertEquals(1, res.size());
        BundleInfo out = res.get(0);
        assertNotNull(out);
        assertEquals(b101.toURI().toString(), out.getLocation());
    }

    @Test
    public void testMatching102() throws IOException {
        File props = File.createTempFile("karaf", "properties");
        Writer w = new FileWriter(props);
        w.write(b101.toURI().toString());
        w.write("\n");
        w.write(b102.toURI().toString());
        w.write("\n");
        w.write(b110.toURI().toString());
        w.write("\n");
        w.close();

        List<BundleInfo> res = Overrides.override(
                Arrays.<BundleInfo>asList(new Bundle(b100.toURI().toString())),
                props.toURI().toString());
        assertNotNull(res);
        assertEquals(1, res.size());
        BundleInfo out = res.get(0);
        assertNotNull(out);
        assertEquals(b102.toURI().toString(), out.getLocation());
    }

    @Test
    public void testMatchingRange() throws IOException {
        File props = File.createTempFile("karaf", "properties");
        Writer w = new FileWriter(props);
        w.write(b101.toURI().toString());
        w.write("\n");
        w.write(b110.toURI().toString());
        w.write(";range=\"[1.0, 2.0)\"\n");
        w.close();

        List<BundleInfo> res = Overrides.override(
                Arrays.<BundleInfo>asList(new Bundle(b100.toURI().toString())),
                props.toURI().toString());
        assertNotNull(res);
        assertEquals(1, res.size());
        BundleInfo out = res.get(0);
        assertNotNull(out);
        assertEquals(b110.toURI().toString(), out.getLocation());
    }

    @Test
    public void testNotMatching() throws IOException {
        File props = File.createTempFile("karaf", "properties");
        Writer w = new FileWriter(props);
        w.write(b110.toURI().toString());
        w.write("\n");
        w.close();

        List<BundleInfo> res = Overrides.override(
                Arrays.<BundleInfo>asList(new Bundle(b100.toURI().toString())),
                props.toURI().toString());
        assertNotNull(res);
        assertEquals(1, res.size());
        BundleInfo out = res.get(0);
        assertNotNull(out);
        assertEquals(b100.toURI().toString(), out.getLocation());
    }

    /**
     * Copies the content of {@link java.io.InputStream} to {@link java.io.OutputStream}.
     *
     * @param input
     * @param output
     * @throws java.io.IOException
     */
    private void copy(final InputStream input, final OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 16];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            output.flush();
        }
        input.close();
        output.close();
    }

}
