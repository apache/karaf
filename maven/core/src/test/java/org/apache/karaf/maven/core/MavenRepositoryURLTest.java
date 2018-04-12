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
package org.apache.karaf.maven.core;

import java.net.MalformedURLException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MavenRepositoryURLTest {

    @Test
    public void uris() throws MalformedURLException {
        String uri1, uri2;
        MavenRepositoryURL mavenURI;

        uri1 = "http://localhost/@id=id1@snapshots@update=interval:42@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@snapshots@update=interval:42";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));

        uri1 = "http://localhost/@id=id1@snapshots@checksum=fail@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@snapshots@checksum=fail";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));

        uri1 = "http://localhost/@id=id1@snapshots@noreleases@update=interval:42@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@noreleases@snapshots@snapshotsUpdate=interval:42";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));

        uri1 = "http://localhost/@id=id1@update=interval:42@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@releasesUpdate=interval:42";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));

        uri1 = "http://localhost/@id=id1@snapshots@noreleases@checksum=fail@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@noreleases@snapshots@snapshotsChecksum=fail";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));

        uri1 = "http://localhost/@id=id1@checksum=fail@_from=" + MavenRepositoryURL.FROM.SETTINGS;
        uri2 = "http://localhost/@id=id1@releasesChecksum=fail";
        mavenURI = new MavenRepositoryURL(uri1);
        assertThat(mavenURI.asRepositorySpec(), equalTo(uri2));
    }

}
