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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JaasTest extends BaseTest {

    @Test
    public void realmListCommand() throws Exception {
        String listRealmsOutput = executeCommand("jaas:realm-list");
        assertContains("PropertiesLoginModule", listRealmsOutput);
        assertContains("PublickeyLoginModule", listRealmsOutput);
    }

    @Ignore
    //ignore it as this is too time consuming
    public void testLoginNoLeak() throws Exception {
        for (int i = 0; i<200000; i++) {
            doLogin();
        }
    }

    @Inject
    protected BundleContext bundleContext;

    @Test  // shows the leak afaics
    public void testLoginSingleReg() throws Exception {
        for (int i=0; i<10; i++) {
            doLogin();
        }
        assertEquals(3, bundleContext.getServiceReferences(ArtifactInstaller.class.getName(), null).length);
    }

    private void doLogin() throws Exception {
        final String userPassRealm = "karaf";
        LoginContext lc = new LoginContext(userPassRealm, callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof PasswordCallback) {
                    PasswordCallback passwordCallback = (PasswordCallback) callback;
                    passwordCallback.setPassword(userPassRealm.toCharArray());
                } else if (callback instanceof NameCallback) {
                    NameCallback nameCallback = (NameCallback) callback;
                    nameCallback.setName(userPassRealm);
                }
            }
        });
        lc.login();
        assertNotNull(lc.getSubject());
    }


}
