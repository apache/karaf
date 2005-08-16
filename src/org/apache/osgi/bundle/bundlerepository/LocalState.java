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
package org.apache.osgi.bundle.bundlerepository;

import java.util.*;

import org.apache.osgi.service.bundlerepository.BundleRecord;
import org.apache.osgi.service.bundlerepository.IPackage;
import org.osgi.framework.*;

public class LocalState
{
    private BundleContext m_context = null;
    private List m_localRecordList = new ArrayList();

    public LocalState(BundleContext context)
    {
        m_context = context;
        initialize();
    }

    public BundleRecord findBundle(String symName, int[] version)
    {
        for (int i = 0; i < m_localRecordList.size(); i++)
        {
            BundleRecord brLocal = (BundleRecord) m_localRecordList.get(i);
            String localSymName = (String)
                brLocal.getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME);
            int[] localVersion = Util.parseVersionString((String)
                brLocal.getAttribute(BundleRecord.BUNDLE_VERSION));
            if ((localSymName != null) &&
                localSymName.equals(symName) &&
                (Util.compareVersion(localVersion, version) == 0))
            {
                return brLocal;
            }
        }
        return null;
    }

    public BundleRecord[] findBundles(String symName)
    {
        List matchList = new ArrayList();
        for (int i = 0; i < m_localRecordList.size(); i++)
        {
            BundleRecord brLocal = (BundleRecord) m_localRecordList.get(i);
            String localSymName = (String)
                brLocal.getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME);
            if ((localSymName != null) && localSymName.equals(symName))
            {
                matchList.add(brLocal);
            }
        }
        return (BundleRecord[]) matchList.toArray(new BundleRecord[matchList.size()]);
    }

    public void update(BundleRecord oldRecord, BundleRecord newRecord)
    {
        // To update the old record we need to replace it with
        // a new one, since BundleRecords are immutable. Make
        // a new record that contains the attributes of the new
        // record, but is associated with the local bundle of
        // the old record.
        if (oldRecord instanceof LocalBundleRecord)
        {
            String[] keys = newRecord.getAttributes();
            Map map = new HashMap();
            for (int i = 0; i < keys.length; i++)
            {
                map.put(keys, newRecord.getAttribute(keys[i]));
            }
            BundleRecord updatedRecord =
                new LocalBundleRecord(
                    map, ((LocalBundleRecord) oldRecord).getBundle());
            int idx = m_localRecordList.indexOf(oldRecord);
            if (idx >= 0)
            {
                m_localRecordList.set(idx, updatedRecord);
            }
        }
    }

    public LocalBundleRecord findUpdatableBundle(BundleRecord record)
    {
        // Determine if any bundles with the specified symbolic
        // name are already installed locally.
        BundleRecord[] localRecords = findBundles(
            (String)record.getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME));
        if (localRecords != null)
        {
            // Since there are local bundles with the same symbolic
            // name installed, then we must determine if we can
            // update an existing bundle or if we must install
            // another one. Loop through all local bundles with same
            // symbolic name and find the first one that can be updated
            // without breaking constraints of existing bundles.
            for (int i = 0; i < localRecords.length; i++)
            {
                // Check to make sure that the version of the target
                // record is greater than the local bundle version,
                // since we do not want to perform a downgrade.
//                int[] vLocal = Util.parseVersionString((String)
//                    localRecords[i].getAttribute(BundleRecord.BUNDLE_VERSION));
//                int[] vTarget = Util.parseVersionString((String)
//                    record.getAttribute(BundleRecord.BUNDLE_VERSION));
// TODO: VERIFY WHAT IS GOING ON HERE.
                // If the target bundle is a newer version and it is
                // export compatible with the local bundle, then return it.
                if (isUpdatable(localRecords[i], record))
                {
                    return (LocalBundleRecord) localRecords[i];
                }
            }
        }
        return null;
    }

    public boolean isUpdatable(BundleRecord oldVersion, BundleRecord newVersion)
    {
        // First get all of the potentially resolvable package declarations
        // from the local bundles for the old version of the bundle.
        Filter[] reqFilters = getResolvableImportDeclarations(oldVersion);
        if (reqFilters == null)
        {
            return true;
        }
        // Now make sure that all of the resolvable import declarations
        // for the old version of the bundle  can also be satisfied by
        // the new version of the bundle.
        Map[] capMaps = (Map[])
            newVersion.getAttribute("capability");
        if (capMaps == null)
        {
            return false;
        }
        MapToDictionary mapDict = new MapToDictionary(null);
        for (int reqIdx = 0; reqIdx < reqFilters.length; reqIdx++)
        {
            boolean satisfied = false;
            for (int capIdx = 0; !satisfied && (capIdx < capMaps.length); capIdx++)
            {
                mapDict.setSourceMap(capMaps[capIdx]);
                if (reqFilters[reqIdx].match(mapDict))
                {
                    satisfied = true;
                }
            }

            // If any of the previously resolvable package declarations
            // cannot be resolved, then the bundle is not updatable.
            if (!satisfied)
            {
                return false;
            }
        }
        return true;
    }
    
    public Filter[] getResolvableImportDeclarations(BundleRecord record)
    {
        Map[] capMaps = (Map[])
            record.getAttribute("capability");
        if ((capMaps != null) && (capMaps.length > 0))
        {
            List filterList = new ArrayList();
            // Use brute force to determine if any of the exports
            // could possibly resolve any of the imports.
            MapToDictionary mapDict = new MapToDictionary(null);
            for (int capIdx = 0; capIdx < capMaps.length; capIdx++)
            {
                boolean added = false;
                for (int recIdx = 0; !added && (recIdx < m_localRecordList.size()); recIdx++)
                {
                    BundleRecord brLocal = (BundleRecord) m_localRecordList.get(recIdx);
                    Filter[] reqFilters = (Filter[])
                        brLocal.getAttribute("requirement");
                    for (int reqIdx = 0;
                        (reqFilters != null) && (reqIdx < reqFilters.length);
                        reqIdx++)
                    {
                        mapDict.setSourceMap(capMaps[capIdx]);
                        if (reqFilters[reqIdx].match(mapDict))
                        {
                            added = true;
                            filterList.add(reqFilters[reqIdx]);
                        }
                    }
                }
            }
            return (Filter[])
                filterList.toArray(new Filter[filterList.size()]);
        }
        return null;
    }

    public boolean isResolvable(Filter reqFilter)
    {
        MapToDictionary mapDict = new MapToDictionary(null);
        for (int brIdx = 0; brIdx < m_localRecordList.size(); brIdx++)
        {
            BundleRecord brLocal = (BundleRecord) m_localRecordList.get(brIdx);
            Map[] capMaps = (Map[]) brLocal.getAttribute("capability");
            for (int capIdx = 0; (capMaps != null) && (capIdx < capMaps.length); capIdx++)
            {
                mapDict.setSourceMap(capMaps[capIdx]);
                if (reqFilter.match(mapDict))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void initialize()
    {
        Bundle[] bundles = m_context.getBundles();
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            Dictionary dict = bundles[i].getHeaders();
            // Create a case-insensitive map.
            Map bundleMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });

            for (Enumeration keys = dict.keys(); keys.hasMoreElements(); )
            {
                Object key = keys.nextElement();
                bundleMap.put(key, dict.get(key));
            }
            
            // Remove and convert any import package declarations
            // into requirement filters.
            String target = (String) bundleMap.remove(BundleRecord.IMPORT_PACKAGE);
            if (target != null)
            {
                IPackage[] pkgs = R4Package.parseImportOrExportHeader(target);
                Filter[] filters = new Filter[(pkgs == null) ? 0 : pkgs.length];
                for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
                {
                    try
                    {
                        String low = pkgs[pkgIdx].getVersionLow().isInclusive()
                            ? "(version>=" + pkgs[pkgIdx].getVersionLow() + ")"
                            : "(!(version<=" + pkgs[pkgIdx].getVersionLow() + ")";

                        if (pkgs[pkgIdx].getVersionHigh() != null)
                        {
                            String high = pkgs[pkgIdx].getVersionHigh().isInclusive()
                                ? "(version<=" + pkgs[pkgIdx].getVersionHigh() + ")"
                                : "(!(version>=" + pkgs[pkgIdx].getVersionHigh() + ")";
                            filters[pkgIdx] = m_context.createFilter(
                                "(&(type=Export-Package)(name="
                                + pkgs[pkgIdx].getId() + ")"
                                + low + high + ")");
                        }
                        else
                        {
                            filters[pkgIdx] = m_context.createFilter(
                                "(&(type=Export-Package)(name="
                                + pkgs[pkgIdx].getId() + ")"
                                + low + ")");
                        }
                    }
                    catch (InvalidSyntaxException ex)
                    {
                        // Ignore, since it should not happen.
                    }
                }
                bundleMap.put("requirement", filters);
            }

            // Remove and convert any export package declarations
            // into capability maps.
            target = (String) bundleMap.remove(BundleRecord.EXPORT_PACKAGE);
            if (target != null)
            {
                IPackage[] pkgs = R4Package.parseImportOrExportHeader(target);
                Map[] capMaps = new Map[(pkgs == null) ? 0 : pkgs.length];
                for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
                {
                    // Create a case-insensitive map.
                    capMaps[pkgIdx] = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2)
                        {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    capMaps[pkgIdx].put("type", "Export-Package");
                    capMaps[pkgIdx].put("name", pkgs[pkgIdx].getId());
                    capMaps[pkgIdx].put("version", pkgs[pkgIdx].getVersionLow());
                }
                bundleMap.put("capability", capMaps);
            }

            // For the system bundle, add a special platform capability.
            if (bundles[i].getBundleId() == 0)
            {
                // Create a case-insensitive map.
                Map map = new TreeMap(new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        return o1.toString().compareToIgnoreCase(o2.toString());
                    }
                });
                map.put(
                    Constants.FRAMEWORK_VERSION,
                    m_context.getProperty(Constants.FRAMEWORK_VERSION));
                map.put(
                    Constants.FRAMEWORK_VENDOR,
                    m_context.getProperty(Constants.FRAMEWORK_VENDOR));
                map.put(
                    Constants.FRAMEWORK_LANGUAGE,
                    m_context.getProperty(Constants.FRAMEWORK_LANGUAGE));
                map.put(
                    Constants.FRAMEWORK_OS_NAME,
                    m_context.getProperty(Constants.FRAMEWORK_OS_NAME));
                map.put(
                    Constants.FRAMEWORK_OS_VERSION,
                    m_context.getProperty(Constants.FRAMEWORK_OS_VERSION));
                map.put(
                    Constants.FRAMEWORK_PROCESSOR,
                    m_context.getProperty(Constants.FRAMEWORK_PROCESSOR));
//                map.put(
//                    FelixConstants.FELIX_VERSION_PROPERTY,
//                    m_context.getProperty(FelixConstants.FELIX_VERSION_PROPERTY));
                Map[] capMaps = (Map[]) bundleMap.get("capability");
                if (capMaps == null)
                {
                    capMaps = new Map[] { map };
                }
                else
                {
                    Map[] newCaps = new Map[capMaps.length + 1];
                    newCaps[0] = map;
                    System.arraycopy(capMaps, 0, newCaps, 1, capMaps.length);
                    capMaps = newCaps;
                }
                bundleMap.put("capability", capMaps);
            }
            m_localRecordList.add(new LocalBundleRecord(bundleMap, bundles[i]));
        }
    }

    public static class LocalBundleRecord extends BundleRecord
    {
        private Bundle m_bundle = null;

        LocalBundleRecord(Map attrMap, Bundle bundle)
        {
            super(attrMap);
            m_bundle = bundle;
        }

        public Bundle getBundle()
        {
            return m_bundle;
        }
    }
}