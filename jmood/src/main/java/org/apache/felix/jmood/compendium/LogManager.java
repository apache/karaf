/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.jmood.compendium;

import java.util.*;
import javax.management.*;
import javax.management.openmbean.*;

import org.apache.felix.jmood.AgentConstants;
import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.utils.*;
import org.osgi.framework.*;
import org.osgi.service.log.*;


/**
 * 
 * This class enables remote management of
 *         org.osgi.service.log.LogReaderService It enables the operator to read
 *         the system log.
 */
// FUTURE WORK: limitations of Log manager: if the service appears or
// disappears, and/or there ismore than one service available, correct
// functioning is not guaranteed. A New manager should be branched for each
// available log service, and info should be merged to expose it afterwards.
// FUTURE WORK: Log Listener should be an anonymous class
// FUTURE WORK: Add persistence to the log manager
public class LogManager extends NotificationBroadcasterSupport implements
        LogListener, MBeanRegistration, LogManagerMBean {
    private LogReaderService logReader = null;

    private ServiceReference[] refs;

    private LogService log = null;

    private String[][] Log;

    private static final int DefaultLogLevel = 0;

    private int LogLevel;

    private Vector entryVector;

    private static int sequenceNumber = 0;

    private AgentContext ac;

    private ServiceRegistration registration;

    // FUTURE WORK: add persistence to sequence numbers
    public LogManager(AgentContext ac) {
        this.ac = ac;
    }

    public void setLogLevel(int level) {
        // FUTURE WORK This setting only affects to the agent. Extend it to the
        // service. This is implementation dependent.
        this.LogLevel = level;
    }

    public int getLogLevel() {
        return LogLevel;
    }

    public CompositeData[] getLog() {
        if (entryVector == null) {
            return null;
        } else {
            try {

                CompositeData[] value = new CompositeData[entryVector.size()];
                for (int i = 0; i < entryVector.size(); i++) {
                    value[i] = OSGi2JMXCodec
                            .encodeLogEntry((LogEntry) entryVector.elementAt(i));
                }
                return value;
            } catch (Exception e) {
                ac.error("Unexpected exception", e);
                return null;
            }
        }

    }

    /**
     * This method exposes the attribute LogFromReader for remote management.
     * The main difference with the log attribute is that the later uses the
     * level configuration specified by the log level attribute and as a
     * drawback does not include log entries registered before the log manager
     * was started.
     * 
     * @return
     */
    public String[] getLogMessages() {
        if (entryVector == null)
            return null;
        String[] msgs = new String[entryVector.size()];
        for (int i = 0; i < msgs.length; i++) {
            LogEntry entry = (LogEntry) entryVector.elementAt(i);
            msgs[i] = "BUNDLE " + entry.getBundle().getBundleId() + " ("
                    + entry.getBundle().getLocation() + "): LEVEL="
                    + this.getLevelAsString(entry.getLevel()) + "; MESSAGE="
                    + entry.getMessage();
        }
        return msgs;

    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     * 
     */
    public void postDeregister() {
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     * @param registrationDone
     */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     * @throws java.lang.Exception
     */
    public void preDeregister() throws Exception {
        registration.unregister();
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer,
     *      javax.management.ObjectName)
     * @param server
     * @param name
     * @return
     * @throws java.lang.Exception
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) {
        this.entryVector = new Vector();
        this.LogLevel = DefaultLogLevel;
        registration=ac.getBundleContext().registerService(LogListener.class.getName(), this, null);
        return name;
    }

    /**
     * @see org.osgi.service.log.LogListener#logged(org.osgi.service.log.LogEntry)
     * @param arg0
     */
    public void logged(LogEntry entry) {
        if (entry.getLevel() >= this.LogLevel) {
            this.notifyLogEntry(entry);
            entryVector.add(entry);
        }

    }

    private void notifyLogEntry(LogEntry entry) {
        // TEST: See if user data are correctly received. This we cannot do with
        // MC4J
        String Level = this.getLevelAsString(entry.getLevel());
        try {
            ObjectName source = new ObjectName(ObjectNames.LOG_SERVICE);
            String message = "Log entry added: Bundle "
                    + entry.getBundle().getLocation() + " with id "
                    + entry.getBundle().getBundleId()
                    + " has added a new log entry of level " + Level
                    + ". The message is: " + entry.getMessage();
            Notification notification = new Notification(
                    AgentConstants.LOG_NOTIFICATION_TYPE, source,
                    sequenceNumber++, message);
            // User data is CompositeData with the info of the log entry

            CompositeData userData = OSGi2JMXCodec.encodeLogEntry(entry);

            // Before using composite data, we used a simple string array:
            /*
             * String[] userData = new String[4]; userData[0] =
             * entry.getBundle().getLocation(); userData[1] = new
             * Long(entry.getBundle().getBundleId()).toString(); userData[2] =
             * Level; userData[3] = entry.getMessage();
             */

            notification.setUserData(userData);
            sendNotification(notification);
        } catch (Exception e) {
            ac.error("Unexpected exception", e);
        }
    }

    private String getLevelAsString(int level) {
        String Level;
        switch (level) {
        case LogService.LOG_DEBUG:
            Level = "DEBUG";
            break;
        case LogService.LOG_WARNING:
            Level = "WARNING";
            break;
        case LogService.LOG_INFO:
            Level = "INFO";
            break;
        case LogService.LOG_ERROR:
            Level = "ERROR";
            break;
        default:
            Level = "UserDefined: " + level;
            break;
        }
        return Level;

    }
}
