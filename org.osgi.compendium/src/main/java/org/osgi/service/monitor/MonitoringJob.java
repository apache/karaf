/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
 * 
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

package org.osgi.service.monitor;

/**
 * A Monitoring Job is a request for scheduled or event based notifications on
 * update of a set of <code>StatusVariable</code>s. The job is a data structure
 * that holds a non-empty list of <code>StatusVariable</code> names, an
 * identification of the initiator of the job, and the sampling parameters.
 * There are two kinds of monitoring jobs: time based and change based. Time
 * based jobs take samples of all <code>StatusVariable</code>s with a specified
 * frequency. The number of samples to be taken before the job finishes may be
 * specified. Change based jobs are only interested in the changes of the
 * monitored <code>StatusVariable</code>s. In this case, the number of changes
 * that must take place between two notifications can be specified.
 * <p>
 * The job can be started on the <code>MonitorAdmin</code> interface. Running
 * the job (querying the <code>StatusVariable</code>s, listening to changes, and
 * sending out notifications on updates) is the task of the
 * <code>MonitorAdmin</code> implementation.
 * <p>
 * Whether a monitoring job keeps track dynamically of the
 * <code>StatusVariable</code>s it monitors is not specified. This means that if
 * we monitor a <code>StatusVariable</code> of a <code>Monitorable</code>
 * service which disappears and later reappears then it is implementation
 * specific whether we still receive updates of the <code>StatusVariable</code>
 * changes or not.
 * 
 * @version $Revision: 5673 $
 */
public interface MonitoringJob {
    /**
     * Stops a Monitoring Job. Note that a time based job can also stop
     * automatically if the specified number of samples have been taken.
     */
    public void stop();

    /**
     * Returns the identitifier of the principal who initiated the job. This is
     * set at the time when
     * {@link MonitorAdmin#startJob MonitorAdmin.startJob()} method is called.
     * This string holds the ServerID if the operation was initiated from a
     * remote manager, or an arbitrary ID of the initiator entity in the local
     * case (used for addressing notification events).
     * 
     * @return the ID of the initiator, cannot be <code>null</code>
     */
    public String getInitiator();

    /**
     * Returns the list of <code>StatusVariable</code> names that are the
     * targets of this measurement job. For time based jobs, the
     * <code>MonitorAdmin</code> will iterate through this list and query all
     * <code>StatusVariable</code>s when its timer set by the job's frequency
     * rate expires.
     * 
     * @return the target list of the measurement job in
     *         [Monitorable_ID]/[StatusVariable_ID] format, cannot be
     *         <code>null</code>
     */
    public String[] getStatusVariableNames();

    /**
     * Returns the delay (in seconds) between two samples. If this call returns
     * N (greater than 0) then the <code>MonitorAdmin</code> queries each
     * <code>StatusVariable</code> that belongs to this job every N seconds.
     * The value 0 means that the job is not scheduled but event based: in this
     * case instant notification on changes is requested (at every nth change of
     * the value, as specified by the report count parameter).
     * 
     * @return the delay (in seconds) between samples, or 0 for change based
     *         jobs
     */
    public int getSchedule();

    /**
     * Returns the number of times <code>MonitorAdmin</code> will query the
     * <code>StatusVariable</code>s (for time based jobs), or the number of
     * changes of a <code>StatusVariable</code> between notifications (for
     * change based jobs). Time based jobs with non-zero report count will take
     * <code>getReportCount()</code>*<code>getSchedule()</code> time to
     * finish. Time based jobs with 0 report count and change based jobs do not
     * stop automatically, but all jobs can be stopped with the {@link #stop}
     * method.
     * 
     * @return the number of measurements to be taken, or the number of changes
     *         between notifications
     */
    public int getReportCount();

    /**
     * Returns whether the job was started locally or remotely.  Jobs started by
     * the clients of this API are always local, remote jobs can only be started
     * using the Device Management Tree.
     * 
     * @return <code>true</code> if the job was started from the local device,
     *         <code>false</code> if the job was initiated from a management 
     *         server through the device management tree
     */
    public boolean isLocal();
    
    /**
     * Returns whether the job is running.   A job is running until it is
     * explicitely stopped, or, in case of time based jobs with a finite report
     * count, until the given number of measurements have been made.
     *   
     * @return <code>true</code> if the job is still running, <code>false</code>
     *         if it has finished
     */
    public boolean isRunning();
}
