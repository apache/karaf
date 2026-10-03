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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a plain jar into an OSGi bundle, generating the OSGi manifest headers with bnd.
 */
public final class BundleWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BundleWrapper.class);

    private BundleWrapper() {
    }

    /**
     * Generate the OSGi headers of a jar.
     *
     * @param jarStream     the jar to wrap
     * @param instructions  the bnd instructions
     * @param jarInfo       the origin of the jar (usually its URL), used as default symbolic name
     * @param overwriteMode what to do when the jar is already a bundle
     * @return the wrapped jar
     * @throws IOException if the jar cannot be read or wrapped
     */
    public static InputStream wrap(InputStream jarStream, Properties instructions, String jarInfo,
                                   OverwriteMode overwriteMode) throws IOException {
        LOG.debug("Wrapping {} (overwrite mode {}) using instructions {}", jarInfo, overwriteMode, instructions);
        try (Jar jar = new Jar("dot", jarStream)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null && jar.getResources().isEmpty()) {
                throw new IOException(jarInfo + " is not a jar or is empty");
            }
            if (manifest != null && overwriteMode == OverwriteMode.KEEP && isBundle(manifest)) {
                return write(jar);
            }
            // the analyzer owns the jar, so it has to stay open until the jar is written
            try (Analyzer analyzer = new Analyzer()) {
                analyzer.setJar(jar);
                analyzer.setProperties(instructions);
                if (manifest != null && overwriteMode == OverwriteMode.MERGE) {
                    analyzer.mergeManifest(manifest);
                }
                setDefaults(analyzer, jarInfo);
                jar.setManifest(analyzer.calcManifest());
                if (!analyzer.getErrors().isEmpty() || !analyzer.getWarnings().isEmpty()) {
                    LOG.debug("Wrapping {} reported errors {} and warnings {}",
                            jarInfo, analyzer.getErrors(), analyzer.getWarnings());
                }
                return write(jar);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to wrap " + jarInfo, e);
        }
    }

    private static boolean isBundle(Manifest manifest) {
        return manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE) != null
                || manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE) != null;
    }

    private static void setDefaults(Analyzer analyzer, String jarInfo) {
        if (isEmpty(analyzer.getProperty(Constants.IMPORT_PACKAGE))) {
            analyzer.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");
        }
        if (isEmpty(analyzer.getProperty(Constants.EXPORT_PACKAGE))) {
            analyzer.setProperty(Constants.EXPORT_PACKAGE, "*");
        }
        analyzer.setProperty(Constants.BUNDLE_SYMBOLICNAME,
                toSymbolicName(analyzer.getProperty(Constants.BUNDLE_SYMBOLICNAME, jarInfo)));
    }

    // replace the characters not allowed in a symbolic name, leaving the directives untouched
    static String toSymbolicName(String value) {
        int parameters = value.indexOf(';');
        String name = parameters < 0 ? value : value.substring(0, parameters);
        String sanitized = name.trim().replaceAll("[^a-zA-Z_0-9.-]", "_");
        return parameters < 0 ? sanitized : sanitized + value.substring(parameters);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static InputStream write(Jar jar) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jar.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

}
