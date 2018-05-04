/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafTestWatcher extends TestWatcher {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    private static final Logger LOG = LoggerFactory.getLogger(KarafTestWatcher.class);

    @Override
    protected void starting(Description description) {
        System.out.println();
        System.out.println(ANSI_GREEN + description.getTestClass().getSimpleName() + ": " + description.getMethodName() + ANSI_RESET);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        LOG.error(">>>>>> FAILED: {} , cause: {}", description.getDisplayName(), e.getMessage());
        e.printStackTrace();
    }

    @Override
    protected void succeeded(Description description) {
    }


}