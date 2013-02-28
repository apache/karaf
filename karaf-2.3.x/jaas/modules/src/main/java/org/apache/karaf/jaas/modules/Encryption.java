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

public interface Encryption {
    
    /**
     * <p>
     * Encrypt a password.
     * </p>
     * 
     * @param plain the password in plain format.
     * @return the encrypted password format.
     */
    public String encryptPassword(String plain);
    
    /**
     * <p>
     * Check password.
     * </p>
     * 
     * @param input password provided in plain format.
     * @param password the encrypted format to compare with.
     * @return true if the password match, false else.
     */
    public boolean checkPassword(String input, String password);

}
