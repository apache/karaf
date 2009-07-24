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

package org.osgi.service.application;

import java.util.Map;

/**
 * It is allowed to schedule an application based on a specific event.
 * ScheduledApplication service keeps the schedule information. When the
 * specified event is fired a new instance must be launched. Note that launching
 * operation may fail because e.g. the application is locked.
 * <p>
 * Each <code>ScheduledApplication</code> instance has an identifier which is
 * unique within the scope of the application being scheduled.
 * <p>
 * <code>ScheduledApplication</code> instances are registered as services. The
 * {@link #APPLICATION_PID} service property contains the PID of the application
 * being scheduled, the {@link #SCHEDULE_ID} service property contains the
 * schedule identifier.
 * 
 * @version $Revision: 5673 $
 */
public interface ScheduledApplication {
    
    /**
     * The property key for the identifier of the application being scheduled.
     */
    public static final String APPLICATION_PID = ApplicationDescriptor.APPLICATION_PID;
    
    /**
     * The property key for the schedule identifier. The identifier is unique
     * within the scope of the application being scheduled.
     */
    public static final String SCHEDULE_ID = "schedule.id";
    
    /**
     * The key for the startup argument used to pass the event object that 
     * triggered the schedule to launch the application instance.
     * The event is passed in a {@link java.security.GuardedObject}
     * protected by the corresponding 
     * {@link org.osgi.service.event.TopicPermission}.
     */
    public static final String TRIGGERING_EVENT = "org.osgi.triggeringevent";
    
    /**
     * The topic name for the virtual timer topic. Time based schedules
     * should be created using this topic.
     */
    public static final String TIMER_TOPIC = "org/osgi/application/timer";
    
    /**
     * The name of the <i>year</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#YEAR}.
     */
    public static final String YEAR = "year";
    
    /**
     * The name of the <i>month</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#MONTH}.
     */
    public static final String MONTH = "month";
    
    /**
     * The name of the <i>day of month</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#DAY_OF_MONTH}.
     */
    public static final String DAY_OF_MONTH = "day_of_month";
    
    /**
     * The name of the <i>day of week</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#DAY_OF_WEEK}.
     */
    public static final String DAY_OF_WEEK = "day_of_week";
    
    /**
     * The name of the <i>hour of day</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#HOUR_OF_DAY}.
     */
    public static final String HOUR_OF_DAY = "hour_of_day";
    
    /**
     * The name of the <i>minute</i> attribute of a virtual timer event. The value is
     * defined by {@link java.util.Calendar#MINUTE}.
     */
    public static final String MINUTE = "minute";
    
    
    /**
     * Returns the identifier of this schedule. The identifier is unique within
     * the scope of the application that the schedule is related to. 
     * @return the identifier of this schedule
     * 
     */
    public String getScheduleId();

	/**
	 * Queries the topic of the triggering event. The topic may contain a
	 * trailing asterisk as wildcard.
	 * 
	 * @return the topic of the triggering event
	 * 
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public String getTopic();

	/**
	 * Queries the event filter for the triggering event.
	 * 
	 * @return the event filter for triggering event
	 * 
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public String getEventFilter();

	/**
	 * Queries if the schedule is recurring.
	 * 
	 * @return true if the schedule is recurring, otherwise returns false
	 * 
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public boolean isRecurring();

	/**
	 * Retrieves the ApplicationDescriptor which represents the application and
	 * necessary for launching.
	 * 
	 * @return the application descriptor that
	 *         represents the scheduled application
	 * 
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public ApplicationDescriptor getApplicationDescriptor();

	/**
	 * Queries the startup arguments specified when the application was
	 * scheduled. The method returns a copy of the arguments, it is not possible
	 * to modify the arguments after scheduling.
	 * 
	 * @return the startup arguments of the scheduled application. It may be
	 *         null if null argument was specified.
	 * 
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public Map getArguments();

	/**
	 * Cancels this schedule of the application.
	 * 
	 * @throws SecurityException
	 *             if the caller doesn't have "schedule"
	 *             ApplicationAdminPermission for the scheduled application.
	 * @throws IllegalStateException
	 *             if the scheduled application service is unregistered
	 */
	public void remove();
}
