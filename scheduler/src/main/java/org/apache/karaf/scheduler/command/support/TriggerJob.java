/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.karaf.scheduler.command.support;

import org.apache.karaf.scheduler.Scheduler;
import org.apache.karaf.scheduler.SchedulerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerJob implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerJob.class);

    private final Scheduler scheduler;
    private final String name;

    public TriggerJob(Scheduler scheduler, String name) {
        this.scheduler = scheduler;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            if (!scheduler.trigger(name)) {
                LOGGER.warn("Could not find a scheduled job with name " + name);
            }
        } catch (SchedulerError ex) {
            LOGGER.error("Failed to trigger job {}", name, ex);
        }
    }

}
