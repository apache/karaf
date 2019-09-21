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
package org.apache.karaf.jaas.modules.audit;

import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

public class LogAuditLoginModule extends AbstractAuditLoginModule {

    public static final String LOG_LEVEL_OPTION = "level";
    public static final String LOG_LOGGER_OPTION = "logger";

    private String level = "INFO";
    private Logger logger;

    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        level = JAASUtils.getString(options, LOG_LEVEL_OPTION);
        logger = LoggerFactory.getLogger(JAASUtils.getString(options, LOG_LOGGER_OPTION));
    }

    protected synchronized void audit(Action action, String username) {
        String actionStr;
        switch (action) {
            case ATTEMPT: actionStr = "Authentication attempt"; break;
            case SUCCESS: actionStr = "Authentication succeeded"; break;
            case FAILURE: actionStr = "Authentication failed"; break;
            case LOGOUT: actionStr = "Explicit logout"; break;
            default: actionStr = action.toString(); break;
        }
        if (level.equalsIgnoreCase("debug")) {
            logger.debug("{} - {} - {}", actionStr, username, getPrincipalInfo());
        } else if (level.equalsIgnoreCase("trace")) {
            logger.trace("{} - {} - {}", actionStr, username, getPrincipalInfo());
        } else if (level.equalsIgnoreCase("warn")) {
            logger.warn("{} - {} - {}", actionStr, username, getPrincipalInfo());
        } else if (level.equalsIgnoreCase("error")) {
            logger.error("{} - {} - {}", actionStr, username, getPrincipalInfo());
        } else {
            logger.info("{} - {} - {}", actionStr, username, getPrincipalInfo());
        }
    }

}
