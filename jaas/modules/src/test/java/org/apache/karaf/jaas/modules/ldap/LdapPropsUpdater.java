/*
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.ldap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LdapPropsUpdater {

    private LdapPropsUpdater() {
    }

    public static void ldapProps(String propsPath, Function<String, String> mapFunction) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path inPath = new File(basedir + "/src/test/resources/" + propsPath).toPath();
        List<String> lines = Files.lines(inPath, StandardCharsets.UTF_8)
            .map(mapFunction)
            .collect(Collectors.toList());
        Path outPath = new File(basedir + "/target/test-classes/" + propsPath).toPath();
        Files.write(outPath, lines, StandardCharsets.UTF_8);
    }

}
