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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.apache.karaf.jaas.modules.JAASUtils;

public class FileAuditLoginModule extends AbstractAuditLoginModule {

    public static final String LOG_FILE_OPTION = "file";

    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private String logFile;

    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        logFile = JAASUtils.getString(options, LOG_FILE_OPTION);
    }

    protected synchronized void audit(Action action, String username) {
        Date date = new Date();
        try {
            File file = new File(logFile);
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file, true);
            FileChannel channel = out.getChannel();
            FileLock lock = channel.lock(0, Long.MAX_VALUE, false);
            PrintWriter writer = new PrintWriter(out, false);
            String actionStr;
            switch (action) {
            case ATTEMPT: actionStr = "Authentication attempt"; break;
            case SUCCESS: actionStr = "Authentication succeeded"; break;
            case FAILURE: actionStr = "Authentication failed"; break;
            case LOGOUT: actionStr = "Explicit logout"; break;
            default: actionStr = action.toString(); break;
            }
            writer.println(DATE_FORMAT.format(date) + " - " + actionStr + " - " + username + " - " + getPrincipalInfo());
            writer.flush();
            writer.close();
            if (lock.isValid()) {
                lock.release();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to authentication log file", e);
        }
    }

}
