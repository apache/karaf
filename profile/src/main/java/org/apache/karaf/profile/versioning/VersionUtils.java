/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.profile.versioning;

public class VersionUtils {

    private static final String SNAPSHOT = "SNAPSHOT";
    private static final char DELIM_DASH = '-';
    private static final char DELIM_DOT = '.';
    private static final String EMPTY_VERSION = "0.0.0";

    private VersionUtils() {
    }

    public static boolean versionEquals(String versionOne, String versionTwo) {
        if (isSnapshotVersion(versionOne) && isSnapshotVersion(versionTwo)) {
            // If both (and only if both) versions are snapshots, we compare without the snapshot classifier.
            // This is done to consider e.g. 1.2.0.SNAPSHOT and 1.2.SNAPSHOT equal.
            versionOne = versionWithoutSnapshot(versionOne);
            versionTwo = versionWithoutSnapshot(versionTwo);
        }

        // Create comparable version objects.
        final ComparableVersion cvOne = new ComparableVersion(versionOne);
        final ComparableVersion cvTwo = new ComparableVersion(versionTwo);

        // Use equals of comparable version class.
        return cvOne.equals(cvTwo);
    }

    /**
     * Check if a version is a snapshot version.
     *
     * @param version the version to check
     * @return true if {@code version} refers a snapshot version otherwise false
     */
    protected static boolean isSnapshotVersion(final String version) {
        return version.endsWith(SNAPSHOT);
    }

    /**
     * Remove the snapshot classifier at the end of a version.
     *
     * @param version the version
     * @return the given {@code version} without the snapshot classifier
     */
    protected static String versionWithoutSnapshot(final String version) {
        int idx;
        idx = version.lastIndexOf(SNAPSHOT);
        if (idx < 0) {
            return version;
        } else if (idx == 0) {
            return EMPTY_VERSION;
        } else {
            final char delim = version.charAt(idx - 1);
            if (delim == DELIM_DOT || delim == DELIM_DASH) {
                --idx;
            }
            return version.substring(0, idx);
        }
    }

}
