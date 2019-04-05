/*
 *
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
package org.apache.karaf.jaas.modules.audit;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.Subject;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;

public class AbstractAuditLoginModuleTest {

    @Test
    public void getPrincipalInfo() {
        LogAuditLoginModule module = new LogAuditLoginModule();
        Map<String, String> options = new HashMap<>();
        options.put("logger", "test");
        Subject subject = new Subject();
        subject.getPrincipals().add(new ClientPrincipal("ssh", "/127.0.0.1"));
        subject.getPrincipals().add(new ClientPrincipal("ssh", "/127.0.0.2"));
        subject.getPrincipals().add((UserPrincipal) () -> "noexist");
        module.initialize(subject, new NamePasswordCallbackHandler("myuser", "mypassword"), null, options);
        Assert.assertEquals("ssh(/127.0.0.1), ssh(/127.0.0.2)", module.getPrincipalInfo());
    }

    @Test
    public void getPrincipalInfoEmpty() {
        LogAuditLoginModule module = new LogAuditLoginModule();
        Map<String, String> options = new HashMap<>();
        options.put("logger", "test");
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("myuser", "mypassword"), null, options);
        Assert.assertEquals("no client principals found", module.getPrincipalInfo());
    }
}
