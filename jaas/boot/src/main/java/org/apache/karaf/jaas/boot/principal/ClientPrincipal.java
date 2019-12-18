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
package org.apache.karaf.jaas.boot.principal;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

public class ClientPrincipal implements Principal, Serializable {

    private final String method;
    private final String address;

    public ClientPrincipal(String method, String address) {
        this.method = method;
        this.address = address;
    }

    @Override
    public String getName() {
        return method + "(" + address + ")";
    }

    public String getMethod() {
        return method;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientPrincipal that = (ClientPrincipal) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return "ClientPrincipal[" + getName() + "]";
    }

}
