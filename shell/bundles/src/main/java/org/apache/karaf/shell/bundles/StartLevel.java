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
package org.apache.karaf.shell.bundles;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

@Command(scope = "bundle", name = "start-level", description = "Gets or sets the start level of a bundle.")
public class StartLevel extends BundleCommand {

    @Argument(index = 1, name = "startLevel", description = "The bundle's new start level", required = false, multiValued = false)
    Integer level;

    protected void doExecute(Bundle bundle) throws Exception {
        // Get package admin service.
        BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
        if (bsl == null) {
            System.out.println("StartLevel service is unavailable.");
            return;
        }
        if (level == null) {
            System.out.println("Level " + bsl.getStartLevel());
        }
        else if ((level < 50) && bsl.getStartLevel() > 50){
            for (;;) {
                StringBuffer sb = new StringBuffer();
                System.err.println("You are about to designate bundle as a system bundle.  Do you wish to continue (yes/no): ");
                System.err.flush();
                for (;;) {
                    int c = System.in.read();
                    if (c < 0) {
                        return;
                    }
                    System.err.println((char) c);
                    if (c == '\r' || c == '\n') {
                        break;
                    }
                    sb.append((char) c);
                }
                String str = sb.toString();
                if ("yes".equals(str)) {
                    bsl.setStartLevel(level);
                    break;
                } else if ("no".equals(str)) {
                    break;
                }
            }
        } else {
            bsl.setStartLevel(level);
        }
    }

}
