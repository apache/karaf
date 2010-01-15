package org.apache.felix.dm.samples.annotation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * This service creates a Configuration for the EnglishDictionary service, using OSGi ConfigAdmin.
 */
@Service
public class EnglishDictionaryConfiguration
{
    /**
     * OSGi Configuration Admin Service.
     */
    @ServiceDependency
    ConfigurationAdmin m_cm;

    /**
     * OSGi log Service (null object if the no log service available).
     */
    @ServiceDependency(required = false)
    LogService m_log;

    /**
     * All our dependencies are satisfied: configure the EnglishDictionary service.
     * We'll use the EnglishDictionary full java class name as the PID.
     */
    @Start
    protected void start()
    {
        try
        {
            String PID = EnglishDictionary.class.getName();
            Configuration config = m_cm.getConfiguration(PID, null);
            if (config.getBundleLocation() != null)
            {
                config.setBundleLocation(null);
            }

            config.update(new Hashtable<String, List<String>>()
            {
                {
                    put("words", Arrays.asList("hello", "world"));
                }
            });
            m_log.log(LogService.LOG_INFO, "registered configuration for PID " + PID);
        }
        catch (IOException e)
        {
            m_log.log(LogService.LOG_WARNING,
                "unexpected exception while initializing english dictionary configuration", e);
        }
    }
}
