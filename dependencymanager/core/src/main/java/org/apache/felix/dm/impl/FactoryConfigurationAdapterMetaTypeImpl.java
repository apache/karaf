package org.apache.felix.dm.impl;

import java.util.Dictionary;

import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.apache.felix.dm.impl.metatype.MetaTypeProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

public class FactoryConfigurationAdapterMetaTypeImpl extends FactoryConfigurationAdapterImpl implements MetaTypeProvider
{
    // Our MetaType Provider for describing our properties metadata
    private MetaTypeProviderImpl m_metaType;
    
   /**
     * Creates a new CM factory configuration adapter.
     * 
     * @param factoryPid
     * @param updateMethod
     * @param adapterInterface
     * @param adapterImplementation
     * @param adapterProperties
     * @param propagate
     */
    public FactoryConfigurationAdapterMetaTypeImpl(String factoryPid, String updateMethod, Object adapterImplementation, Object adapterInterface, Dictionary adapterProperties, boolean propagate,
        BundleContext bctx, Logger logger, String heading, String description, String localization, PropertyMetaData[] properyMetaData)
    {
        super(factoryPid, updateMethod, adapterImplementation, adapterInterface, adapterProperties, propagate);
        m_metaType = new MetaTypeProviderImpl(m_factoryPid, bctx, logger, null, this);
        m_metaType.setName(heading);
        m_metaType.setDescription(description);
        if (localization != null) 
        {
            m_metaType.setLocalization(localization);
        }
        for (int i = 0; i < properyMetaData.length; i ++) 
        {
            m_metaType.add(properyMetaData[i]);
        }
    }

    public String[] getLocales()
    {
        return m_metaType.getLocales();
    }

    public ObjectClassDefinition getObjectClassDefinition(String id, String locale)
    {
        return m_metaType.getObjectClassDefinition(id, locale);
    }
}
