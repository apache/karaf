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
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The version of a snapshot, from the <code>maven-metadata.xml</code> file of its version directory in a repository.
 */
final class SnapshotMetadata {

    /**
     * The version of the file, a timestamped version (<code>1.0-20240101.123456-1</code>), or the base version
     * (<code>1.0-SNAPSHOT</code>) for a non unique snapshot.
     */
    final String version;

    /**
     * When the snapshot was deployed, in milliseconds, 0 if unknown.
     */
    final long updated;

    private SnapshotMetadata(String version, long updated) {
        this.version = version;
        this.updated = updated;
    }

    /**
     * @param metadata the content of the <code>maven-metadata.xml</code> file
     * @param baseVersion the version of the snapshot (<code>1.0-SNAPSHOT</code>)
     * @return the snapshot of the given type and classifier, <code>null</code> if the metadata doesn't have it
     */
    static SnapshotMetadata parse(byte[] metadata, String baseVersion, String extension, String classifier)
            throws IOException {
        Element root;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(metadata)).getDocumentElement();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Invalid Maven metadata: " + e.getMessage(), e);
        }
        Element versioning = child(root, "versioning");
        if (versioning == null) {
            return null;
        }
        long lastUpdated = parseTimestamp(text(versioning, "lastUpdated"));

        // Maven 3: the version of each file
        Element snapshotVersions = child(versioning, "snapshotVersions");
        if (snapshotVersions != null) {
            String expectedClassifier = classifier != null ? classifier : "";
            for (Node node = snapshotVersions.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && "snapshotVersion".equals(node.getLocalName())) {
                    Element snapshotVersion = (Element) node;
                    String fileClassifier = text(snapshotVersion, "classifier");
                    if (extension.equals(text(snapshotVersion, "extension"))
                            && expectedClassifier.equals(fileClassifier != null ? fileClassifier : "")
                            && text(snapshotVersion, "value") != null) {
                        long updated = parseTimestamp(text(snapshotVersion, "updated"));
                        return new SnapshotMetadata(text(snapshotVersion, "value"),
                                updated > 0 ? updated : lastUpdated);
                    }
                }
            }
        }

        // Maven 2: the timestamp and the build number of the last deployment
        Element snapshot = child(versioning, "snapshot");
        if (snapshot != null) {
            String timestamp = text(snapshot, "timestamp");
            String buildNumber = text(snapshot, "buildNumber");
            if (timestamp != null && buildNumber != null) {
                return new SnapshotMetadata(baseVersion.replace("SNAPSHOT", timestamp + "-" + buildNumber),
                        lastUpdated);
            }
            if ("true".equals(text(snapshot, "localCopy"))) {
                return new SnapshotMetadata(baseVersion, lastUpdated);
            }
        }
        return null;
    }

    /**
     * @param timestamp a Maven timestamp (<code>yyyyMMddHHmmss</code>, UTC)
     * @return the time in milliseconds, 0 if the timestamp is invalid
     */
    private static long parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return 0;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(timestamp).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private static Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String text(Element parent, String name) {
        Element element = child(parent, name);
        return element != null ? element.getTextContent().trim() : null;
    }

}
