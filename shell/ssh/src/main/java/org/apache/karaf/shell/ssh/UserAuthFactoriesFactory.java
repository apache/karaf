/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.auth.UserAuthPublicKey;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.ReifiedType;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>A factory for user authentication factories to set on
 * {@link SshServer#setUserAuthFactories(java.util.List)} based on a
 * comma-separated list of authentication methods.</p>
 *
 * <p>Currently, the following methods are supported:</p>
 * <ul>
 *     <li><code>password</code>
 *          Password authentication against a given JAAS domain.</p></li>
 *     <li><code>publickey</code>
 *          Public key authentication against an OpenSSH <code>authorized_keys</code> file.</p></li>
 * </ul>
 * </p>
 */
public class UserAuthFactoriesFactory {

    public static final String PASSWORD_METHOD = "password";
    public static final String PUBLICKEY_METHOD = "publickey";

    private Set<String> methodSet;
    private List<NamedFactory<UserAuth>> factories;

    public static Converter getConverter() {
        return new Converter();
    }

    /**
     * This blueprint type converter silently converts instances of
     * <code>Class X implements NameFactory</code>
     * to the reified type <code>cNameFactory</code>
     * and therefore helps blueprint to set the returned factories on
     * {@link SshServerAction#setUserAuthFactories(List)} without complaining
     * about type conversion errors.
     */
    public static class Converter implements org.osgi.service.blueprint.container.Converter {

        public boolean canConvert(Object sourceObject, ReifiedType targetType) {
            return NamedFactory.class.isAssignableFrom(sourceObject.getClass())
                    && UserAuth.class.equals(((ParameterizedType) sourceObject.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0])
                    && NamedFactory.class.equals(targetType.getRawClass())
                    && UserAuth.class.equals(targetType.getActualTypeArgument(0).getRawClass());
        }

        public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
            return sourceObject;
        }

    }

    public void setAuthMethods(String methods) {
        this.methodSet = new HashSet<String>();
        this.factories = new ArrayList<NamedFactory<UserAuth>>();
        String[] ams = methods.split(",");
        for (String am : ams) {
            if (PASSWORD_METHOD.equals(am)) {
                this.factories.add(new UserAuthPassword.Factory());
            } else if (PUBLICKEY_METHOD.equals(am)) {
                this.factories.add(new UserAuthPublicKey.Factory());
            } else {
                throw new ComponentDefinitionException("Invalid authentication method " + am + " specified");
            }
            this.methodSet.add(am);
        }
    }

    public List<NamedFactory<UserAuth>> getFactories() {
        return factories;
    }

    public boolean isPublickeyEnabled() {
        return this.methodSet.contains(PUBLICKEY_METHOD);
    }

    public boolean isPasswordEnabled() {
        return this.methodSet.contains(PASSWORD_METHOD);
    }

}
