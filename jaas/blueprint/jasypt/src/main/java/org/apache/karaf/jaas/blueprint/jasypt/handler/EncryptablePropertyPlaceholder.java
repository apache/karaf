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
package org.apache.karaf.jaas.blueprint.jasypt.handler;

import org.apache.aries.blueprint.ext.AbstractPropertyPlaceholderExt;
import org.jasypt.encryption.StringEncryptor;

import java.util.Map;

public class EncryptablePropertyPlaceholder extends AbstractPropertyPlaceholderExt {

    private StringEncryptor encryptor;

    public StringEncryptor getEncryptor() {
        return encryptor;
    }

    public void setEncryptor(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public void init() {

    }

    @Override
    protected String getProperty(String val) {
        return encryptor.decrypt(val);
    }

    @Override
    public Map<String, Object> getDefaultProperties() {
        return null;
    }


}
