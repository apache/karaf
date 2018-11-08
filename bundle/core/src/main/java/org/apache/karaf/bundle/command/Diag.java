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

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;

@Command(
        scope = "bundle",
        name = "diag",
        description = "Displays diagnostic information why a bundle is not Active")
@Service
public class Diag extends BundlesCommand {

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
        BundleInfo info = bundleService.getInfo(bundle);
        if (info.getState() == BundleState.Failure
                || info.getState() == BundleState.Waiting
                || info.getState() == BundleState.GracePeriod
                || info.getState() == BundleState.Installed) {
            String title = ShellUtil.getBundleName(bundle);
            System.out.println(title);
            System.out.println(ShellUtil.getUnderlineString(title));
            System.out.println("Status: " + info.getState().toString());
            System.out.println(this.bundleService.getDiag(bundle));
            System.out.println();
        }
    }
}
