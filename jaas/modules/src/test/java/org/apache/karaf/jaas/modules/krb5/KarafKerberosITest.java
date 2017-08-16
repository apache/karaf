/*
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
package org.apache.karaf.jaas.modules.krb5;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.kerberos.kdc.AbstractKerberosITest;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;

public class KarafKerberosITest extends AbstractKerberosITest {

    protected String createKrb5Conf(ChecksumType checksumType, EncryptionType encryptionType, boolean isTcp) throws IOException {
        File file = folder.newFile("krb5.conf");
        PrintStream out = new PrintStream(file);
        out.println("[libdefaults]");
        out.println("default_realm = " + REALM);
        out.println("default_tkt_enctypes = " + encryptionType.getName());
        out.println("default_tgs_enctypes = " + encryptionType.getName());
        out.println("permitted_enctypes = " + encryptionType.getName());
        out.println("default-checksum_type = " + checksumType.getName());
        if (isTcp) {
            out.println("udp_preference_limit = 1");
        }
        out.println("[realms]");
        out.println(REALM + " = {");
        out.println("kdc = " + HOSTNAME + ":" + kdcServer.getTransports()[0].getPort());
        out.println("}");
        out.println("[domain_realm]");
        out.println("." + Strings.lowerCaseAscii(REALM) + " = " + REALM);
        out.println(Strings.lowerCaseAscii(REALM) + " = " + REALM);
        out.close();
        return file.getAbsolutePath();
    }

}
