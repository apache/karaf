/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.url.internal;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Maven update policy: when to check the remote repositories for a newer version of a snapshot already in the
 * local repository (<code>always</code>, <code>daily</code>, <code>interval:minutes</code> or <code>never</code>).
 */
final class UpdatePolicy {

    private static final Logger LOG = LoggerFactory.getLogger(UpdatePolicy.class);

    static final UpdatePolicy ALWAYS = new UpdatePolicy("always", 0);
    static final UpdatePolicy DAILY = new UpdatePolicy("daily", -1);
    static final UpdatePolicy NEVER = new UpdatePolicy("never", Long.MAX_VALUE);

    private static final String INTERVAL = "interval:";

    private final String name;

    /**
     * In milliseconds, -1 for daily.
     */
    private final long interval;

    private UpdatePolicy(String name, long interval) {
        this.name = name;
        this.interval = interval;
    }

    /**
     * @param policy the policy, <code>daily</code> (as Maven) when <code>null</code> or invalid
     */
    static UpdatePolicy parse(String policy) {
        if (policy == null || policy.trim().isEmpty()) {
            return DAILY;
        }
        String value = policy.trim();
        if (ALWAYS.name.equalsIgnoreCase(value)) {
            return ALWAYS;
        }
        if (DAILY.name.equalsIgnoreCase(value)) {
            return DAILY;
        }
        if (NEVER.name.equalsIgnoreCase(value)) {
            return NEVER;
        }
        if (value.regionMatches(true, 0, INTERVAL, 0, INTERVAL.length())) {
            try {
                long minutes = Long.parseLong(value.substring(INTERVAL.length()).trim());
                if (minutes >= 0) {
                    return new UpdatePolicy(value, minutes * 60 * 1000);
                }
            } catch (NumberFormatException e) {
                // invalid
            }
        }
        LOG.warn("Invalid update policy {}, using daily", policy);
        return DAILY;
    }

    /**
     * @param lastCheck the time of the last check, 0 if there was none
     * @param now the current time
     */
    boolean isUpdateRequired(long lastCheck, long now) {
        if (this == NEVER) {
            return false;
        }
        if (lastCheck <= 0) {
            return true;
        }
        if (interval == -1) {
            // as Maven, once per day: when the last check was before today midnight
            Calendar midnight = Calendar.getInstance();
            midnight.setTimeInMillis(now);
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            return lastCheck < midnight.getTimeInMillis();
        }
        return now - lastCheck >= interval;
    }

    @Override
    public String toString() {
        return name;
    }

}
