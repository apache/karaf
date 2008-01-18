/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.kernel.filemonitor;

import java.io.File;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

/**
 * @version $Revision: 1.1 $
 */
public class BundlePackerTest extends TestCase {
    public void testFiles() throws Exception {
        File bundleDir = new File("target/deploy");
        File unpackDir = new File("target/data");
        FileUtils.deleteDirectory(unpackDir);

        bundleDir.mkdirs();
        unpackDir.mkdirs();

        FileUtils.copyDirectory(new File("src/test/data"), unpackDir);

/*
        FileMonitor packer = new FileMonitor(bundleDir, unpackDir);
        packer.scan();
*/
    }

}
