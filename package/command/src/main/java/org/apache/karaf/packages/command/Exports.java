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
package org.apache.karaf.packages.command;

import java.util.SortedMap;

import org.apache.karaf.packages.core.PackageService;
import org.apache.karaf.packages.core.PackageVersion;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.console.table.Col;
import org.apache.karaf.shell.console.table.ShellTable;
import org.osgi.framework.Bundle;

@Command(scope = "package", name = "exports", description = "Lists exported packages and the bundles that export them")
public class Exports extends OsgiCommandSupport {

    private PackageService packageService;

    public Exports(PackageService packageService) {
        super();
        this.packageService = packageService;
    }

    protected Object doExecute() throws Exception {
        SortedMap<String, PackageVersion> exports = packageService.getExports();
        ShellTable table = new ShellTable();
        table.column(new Col("Package Name"));
        table.column(new Col("Version"));
        table.column(new Col("ID"));
        table.column(new Col("Bundle Name"));
        
        for (String key : exports.keySet()) {
            PackageVersion pVer = exports.get(key);
            for (Bundle bundle : pVer.getBundles()) {
                table.addRow().addContent(pVer.getPackageName(),pVer.getVersion().toString(), bundle.getBundleId(), bundle.getSymbolicName());
            }
        }
        table.print(System.out);
        return null;
    }

}
