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
package org.apache.karaf.bundle.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.util.bundles.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

@Command(scope = "bundle", name = "update", description = "Update bundle.")
@Service
public class Update extends BundleCommand {

    @Argument(index = 1, name = "location", description = "The bundles update location", required = false, multiValued = false)
    URI location;

    protected Object doExecute(Bundle bundle) throws Exception {
        if (location != null) {
            update(bundle, location.toURL());
        } else {
            String loc = bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
            if (loc != null && !loc.equals(bundle.getLocation())) {
                update(bundle, new URL(loc));
            } else {
                bundle.update();
            }
        }
        return null;
    }

    private void update(Bundle bundle, URL location) throws IOException, BundleException {
        try (InputStream is = location.openStream()) {
            File file = BundleUtils.fixBundleWithUpdateLocation(is, location.toString());
            try (FileInputStream fis = new FileInputStream(file)) {
                bundle.update(fis);
            }
            file.delete();
        }
    }

}
