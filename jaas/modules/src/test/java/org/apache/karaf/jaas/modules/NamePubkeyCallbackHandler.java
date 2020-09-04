/*
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
package org.apache.karaf.jaas.modules;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Iterator;
import java.util.Objects;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.karaf.jaas.modules.publickey.PublickeyCallback;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;

/**
 * {@link CallbackHandler} implementation handling a name and public key.
 */
public class NamePubkeyCallbackHandler implements CallbackHandler {

    private final String name;
    private final PublicKey publicKey;

    public NamePubkeyCallbackHandler(String name, PublicKey publicKey) {
        this.name = Objects.requireNonNull(name);
        this.publicKey = Objects.requireNonNull(publicKey);
    }

    public NamePubkeyCallbackHandler(String name, Path publicKeyFile) throws IOException {
        this.name = Objects.requireNonNull(name);

        FileKeyPairProvider provider = new FileKeyPairProvider(publicKeyFile);
        Iterator<KeyPair> keys = provider.loadKeys(null).iterator();
        if (!keys.hasNext()) {
            throw new IOException("no public keys loaded");
        }
        this.publicKey = keys.next().getPublic();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback cb : callbacks) {
            if (cb instanceof NameCallback) {
                ((NameCallback) cb).setName(name);
            } else if (cb instanceof PublickeyCallback) {
                ((PublickeyCallback) cb).setPublicKey(publicKey);
            }
        }
    }
}
