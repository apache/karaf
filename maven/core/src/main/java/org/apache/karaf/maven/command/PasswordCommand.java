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
package org.apache.karaf.maven.command;

import java.io.File;
import java.io.FileWriter;
import java.util.Dictionary;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.cm.Configuration;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;
import org.sonatype.plexus.components.sec.dispatcher.model.io.xpp3.SecurityConfigurationXpp3Writer;

@Command(scope = "maven", name = "password", description = "Manage passwords for remote repositories and proxies")
@Service
public class PasswordCommand extends MavenConfigurationSupport {

    @Option(name = "-ep", aliases = { "--encrypt-password" }, description = "Encrypts passwords to use for remote repositories and proxies, see \"mvn -ep\"", required = false, multiValued = false)
    boolean ep;

    @Option(name = "-emp", aliases = { "--encrypt-master-password" }, description = "Encrypts master password used to encrypt/decrypt other passwords, see \"mvn -emp\"", required = false, multiValued = false)
    boolean emp;

    @Option(name = "-p", aliases = { "--persist" }, description = "", required = false, multiValued = false)
    boolean persist;

    @Override
    public void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        if (ep && emp) {
            System.err.println("Please specify only one of --encrypt-password and --encrypt-master-password");
            return;
        }

        if (ep && persist) {
            System.err.println("Ordinary passwords are not persisted - use the encrypted password in either <proxy> or <server>");
            return;
        }

        if (ep) {
            // encrypt password using master password
            if (masterPassword == null) {
                System.err.println("Master password is not available");
                return;
            }
            String password = session.readLine("Password to encrypt: ", '*');
            System.out.println("Encrypted password: " + cipher.encryptAndDecorate(password, masterPassword));
            System.out.println("You can use this encrypted password when defining repositories and proxies");
            return;
        }

        if (emp) {
            if (persist && !confirm("Maven security settings will be stored in new file. This file will be used in org.ops4j.pax.url.mvn.security property. Continue? (y/N) ")) {
                return;
            }

            // encrypt master password using DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION
            String password = session.readLine("Master password to encrypt: ", '*');
            String encryptedPassword = cipher.encryptAndDecorate(password, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
            System.out.println("Encrypted master password: " + encryptedPassword);
            if (persist) {
                SettingsSecurity settingsSecurity = new SettingsSecurity();
                settingsSecurity.setMaster(encryptedPassword);
                File dataDir = context.getDataFile(".");
                if (!dataDir.isDirectory()) {
                    System.err.println("Can't access data directory for " + context.getBundle().getSymbolicName() + " bundle");
                    return;
                }
                File newSecuritySettingsFile = nextSequenceFile(dataDir, RE_SECURITY_SETTINGS, PATTERN_SECURITY_SETTINGS);
                try (FileWriter fw = new FileWriter(newSecuritySettingsFile)) {
                    new SecurityConfigurationXpp3Writer().write(fw, settingsSecurity);
                }

                System.out.println("New security settings stored in \"" + newSecuritySettingsFile.getCanonicalPath() + "\"");

                Configuration cmConfig = cm.getConfiguration(PID);
                config.put(prefix + PROPERTY_SECURITY_FILE, newSecuritySettingsFile.getCanonicalPath());
                cmConfig.update(config);
            }
        }
    }

    @Override
    protected boolean showPasswords() {
        return true;
    }

}
