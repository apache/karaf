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
package org.apache.felix.metatype.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.metatype.DefaultMetaTypeProvider;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.DesignateObject;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.OCD;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * The <code>MetaTypeInformationImpl</code> class implements the
 * <code>MetaTypeInformation</code> interface returned from the
 * <code>MetaTypeService</code>.
 *
 * @author fmeschbe
 */
public class MetaTypeInformationImpl implements MetaTypeInformation {

    // also defined in org.osgi.service.cm.ConfigurationAdmin, but copied
    // here to not create a synthetic dependency
    public static final String SERVICE_FACTORYPID = "service.factoryPid";

    private final Bundle bundle;

    private Set pids;

    private Set factoryPids;

    private Set locales;

    private Map metaTypeProviders;

    protected MetaTypeInformationImpl(Bundle bundle) {
        this.bundle = bundle;
        this.pids = new TreeSet();
        this.factoryPids = new TreeSet();
        this.metaTypeProviders = new HashMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getBundle()
     */
    public Bundle getBundle() {
        return this.bundle;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getFactoryPids()
     */
    public String[] getFactoryPids() {
        return (String[]) this.factoryPids.toArray(new String[this.factoryPids.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeInformation#getPids()
     */
    public String[] getPids() {
        return (String[]) this.pids.toArray(new String[this.pids.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    public String[] getLocales() {
        if (this.locales == null) {
            synchronized (this) {
                Set newLocales = new TreeSet();
                for (Iterator mi = this.metaTypeProviders.values().iterator(); mi.hasNext();) {
                    MetaTypeProvider mtp = (MetaTypeProvider) mi.next();
                    this.addValues(newLocales, mtp.getLocales());
                }
                this.locales = newLocales;
            }
        }

        return (String[]) this.locales.toArray(new String[this.locales.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String,
     *      java.lang.String)
     */
    public ObjectClassDefinition getObjectClassDefinition(String id,
            String locale) {
        MetaTypeProvider mtp = (MetaTypeProvider) this.metaTypeProviders.get(id);
        return (mtp != null) ? mtp.getObjectClassDefinition(id, locale) : null;
    }

    // ---------- internal support for metadata -------------------------------

    Designate getDesignate( String pid )
    {
        Object mto = this.metaTypeProviders.get( pid );
        if ( mto instanceof DefaultMetaTypeProvider )
        {
            return ( ( DefaultMetaTypeProvider ) mto ).getDesignate( pid );
        }

        return null;
    }

    // ---------- setters to fill the values -----------------------------------

    protected void addMetaData(MetaData md) {
        if (md.getDesignates() != null) {
            // meta type provide to register by PID
            DefaultMetaTypeProvider dmtp = new DefaultMetaTypeProvider(this.bundle, md);

            Iterator designates = md.getDesignates().values().iterator();
            while (designates.hasNext()) {
                Designate designate = (Designate) designates.next();

                // get the OCD reference, ignore the designate if none
                DesignateObject object = designate.getObject();
                String ocdRef = (object == null) ? null : object.getOcdRef();
                if (ocdRef == null) {
                    continue;
                }

                // get ocd for the reference, ignore designate if none
                OCD ocd = (OCD) md.getObjectClassDefinitions().get(ocdRef);
                if (ocd == null) {
                    continue;
                }

                // gather pids and factory pids
                this.pids.add(designate.getPid());
                if (designate.getFactoryPid() != null) {
                    this.factoryPids.add( designate.getFactoryPid() );
                }

                // register a metatype provider for the pid
                this.addMetaTypeProvider(designate.getPid(), dmtp);
            }
        }
    }

    protected void addPids(String[] pids) {
        this.addValues(this.pids, pids);
    }

    protected void removePid(String pid) {
        this.pids.remove(pid);
    }

    protected void addFactoryPids(String[] factoryPids) {
        this.addValues(this.factoryPids, factoryPids);
    }

    protected void removeFactoryPid(String factoryPid) {
        this.factoryPids.remove(factoryPid);
    }

    protected void addMetaTypeProvider(String key, MetaTypeProvider mtp) {
        if (key != null && mtp != null) {
            this.metaTypeProviders.put(key, mtp);
            this.locales = null;
        }
    }

    protected MetaTypeProvider removeMetaTypeProvider(String key) {
        if (key != null) {
            this.locales = null;
            return (MetaTypeProvider) this.metaTypeProviders.remove(key);
        }

        return null;
    }

    private void addValues(Collection dest, Object[] values) {
        if (values != null && values.length > 0) {
            dest.addAll(Arrays.asList(values));
        }
    }
}
