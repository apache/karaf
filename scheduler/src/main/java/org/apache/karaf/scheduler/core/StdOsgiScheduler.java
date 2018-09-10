package org.apache.karaf.scheduler.core;

import org.quartz.*;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.StdScheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StdOsgiScheduler extends StdScheduler {

    private final QuartzSchedulerStorage storage;


    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given
     * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     *
     * @param sched
     */
    public StdOsgiScheduler(final QuartzScheduler sched) {
        super(sched);
        this.storage = new QuartzSchedulerStorage();
    }

    public QuartzSchedulerStorage getStorage() {
        return storage;
    }

    @Override
    public Date scheduleJob(final JobDetail jobDetail, final Trigger trigger)
            throws SchedulerException {

        JobDataMap context = (JobDataMap) jobDetail.getJobDataMap().get(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT);

        String contextKey = UUID.randomUUID().toString();

        context.put(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY, contextKey);
        jobDetail.getJobDataMap().put(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY, contextKey);
        storage.put(contextKey, context);

        jobDetail.getJobDataMap().remove(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT);

        final Date result = super.scheduleJob(jobDetail, trigger);

        return result;
    }

    @Override
    public boolean deleteJob(JobKey jobKey) throws SchedulerException {
        final JobDetail jobDetail = getJobDetail(jobKey);
        final JobDataMap jobDataMap = jobDetail.getJobDataMap();

        final String contextKey = (String) jobDataMap.get(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY);
        if (null != contextKey) {
            storage.release(contextKey);
        }

        return super.deleteJob(jobKey);
    }

    @Override
    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        if (null != jobKeys) {
            final List<String> contextKeyList = new ArrayList<>();
            for(JobKey jobKey : jobKeys) {
                final JobDetail jobDetail = getJobDetail(jobKey);
                final JobDataMap jobDataMap = jobDetail.getJobDataMap();

                final String contextKey = (String) jobDataMap.get(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY);
                if (null != contextKey) {
                    contextKeyList.add(contextKey);
                }
            }

            for(String contextKey : contextKeyList) {
                storage.release(contextKey);
            }
        }

        return super.deleteJobs(jobKeys);
    }

    @Override
    public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
        final Trigger trigger = getTrigger(triggerKey);
        final JobDataMap jobDataMap = trigger.getJobDataMap();

        final String contextKey = (String) jobDataMap.get(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY);
        if (null != contextKey) {
            storage.release(contextKey);
        }

        return super.unscheduleJob(triggerKey);
    }

    @Override
    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
        if (null != triggerKeys) {
            final List<String> contextKeyList = new ArrayList<>();
            for(TriggerKey triggerKey : triggerKeys) {
                final Trigger trigger = getTrigger(triggerKey);
                final JobDataMap jobDataMap = trigger.getJobDataMap();

                final String contextKey = (String) jobDataMap.get(org.apache.karaf.scheduler.core.QuartzScheduler.DATA_MAP_CONTEXT_KEY);
                if (null != contextKey) {
                    contextKeyList.add(contextKey);
                }
            }

            for(String contextKey : contextKeyList) {
                storage.release(contextKey);
            }
        }

        return super.unscheduleJobs(triggerKeys);
    }

}
