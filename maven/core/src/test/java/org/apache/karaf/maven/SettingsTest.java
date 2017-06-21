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
package org.apache.karaf.maven;

import java.io.IOException;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.junit.Test;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PBECipher;

public class SettingsTest {

    @Test
    public void readAndWriteSettings() throws IOException {
        Settings settings = new Settings();
        Server s = new Server();
        s.setId("id");
        s.setUsername("admin");
        s.setPassword("admin");
        settings.getServers().add(s);

        new DefaultSettingsWriter().write(System.out, null, settings);
    }

    @Test
    public void encryptMavenPassword() throws Exception {
        // non-master password ('mvn -ep admin')
        DefaultPlexusCipher plexusCipher = new DefaultPlexusCipher();
        System.out.println(plexusCipher.encrypt("admin", "admin"));

        // master password (`mvn -emp admin`)
        PBECipher cipher = new PBECipher();
        System.out.println(cipher.encrypt64("admin","settings.security"));
    }

    @Test
    public void decryptMavenPassword() throws Exception {
        // non-master password ('mvn -ep admin')
        DefaultPlexusCipher plexusCipher = new DefaultPlexusCipher();
        System.out.println(plexusCipher.decrypt("{EhjazkVpkMoHjAgaUKX+UxeXn9lsJGHst2uFKmhNZ8U=}", "admin"));
        System.out.println(plexusCipher.decrypt("{oWE12FbirwYHNit93TAMA+OC/GJge2r9FuzI8kOuHlA=}", "settings.security"));

        // master password (`mvn -emp admin`)
        PBECipher cipher = new PBECipher();
        System.out.println(cipher.decrypt64("EhjazkVpkMoHjAgaUKX+UxeXn9lsJGHst2uFKmhNZ8U=","admin"));
        System.out.println(cipher.decrypt64("oWE12FbirwYHNit93TAMA+OC/GJge2r9FuzI8kOuHlA=","settings.security"));
    }

}
