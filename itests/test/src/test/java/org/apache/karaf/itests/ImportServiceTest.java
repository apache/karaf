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

import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ImportServiceTest extends BaseTest {

    private static final String BUNDLE2_NAME = "testbundle.require.service";
    private static final String BUNDLE1_NAME = "testbundle.import.service";

    @SuppressWarnings("deprecation")
    @Configuration
    public Option[] config() {
        List<Option> options = new ArrayList<>(Arrays.asList(super.config()));
        InputStream testBundleImportService = bundle()
            .set(Constants.IMPORT_SERVICE, "FooService")
            .set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE1_NAME)
            .set(Constants.BUNDLE_VERSION, "1.0.0")
            .set(Constants.BUNDLE_MANIFESTVERSION, "2")
            .build();
        options.add(CoreOptions.streamBundle(testBundleImportService));
        InputStream testBundleRequireService = bundle()
            .set(Constants.REQUIRE_CAPABILITY, "osgi.service;effective:=active;filter:=\"(objectClass=FooService)\"")
            .set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE2_NAME)
            .set(Constants.BUNDLE_VERSION, "1.0.0")
            .set(Constants.BUNDLE_MANIFESTVERSION, "2")
            .build();
        options.add(CoreOptions.streamBundle(testBundleRequireService));
        return options.toArray(new Option[] {});
    }
    
  
    /**
     * Checks that the resolver does not mandate specified required services to be present.
     * This is done for backwards compatibility as not all bundles define capabilities for services they start.
     */
    @Test
    public void checkBundleStarted() throws InterruptedException {
        waitBundleState(BUNDLE1_NAME, Bundle.ACTIVE);
        waitBundleState(BUNDLE2_NAME, Bundle.ACTIVE);
    }
}
