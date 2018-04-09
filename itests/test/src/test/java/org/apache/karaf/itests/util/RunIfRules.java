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
package org.apache.karaf.itests.util;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.karaf.itests.util.RunIfRule.RunIf;
import org.apache.karaf.itests.util.RunIfRule.RunIfCondition;

public class RunIfRules {

    @RunIf(condition = RunIfNotOnJdk8Condition.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface RunIfNotOnJdk8 {

    }

    public static class RunIfNotOnJdk8Condition implements RunIfCondition {
        @Override
        public boolean isSatisfied() {
            String jdk = System.getProperty("java.specification.version");
            return jdk.equals("1.5") || jdk.equals("1.6") || jdk.equals("1.7");
        }
    }

}
