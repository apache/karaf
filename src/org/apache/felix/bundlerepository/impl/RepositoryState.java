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
package org.apache.felix.bundlerepository.impl;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.felix.bundlerepository.impl.kxmlsax.KXmlSAXParser;
import org.apache.felix.bundlerepository.impl.metadataparser.MultivalueMap;
import org.apache.felix.bundlerepository.impl.metadataparser.XmlCommonHandler;
import org.apache.felix.bundlerepository.BundleRecord;
import org.apache.felix.bundlerepository.ResolveException;
import org.osgi.framework.*;

public class RepositoryState
{
    private BundleContext m_context = null;
    private String[] m_urls = null;
    private Map m_recordMap = new HashMap();
    private BundleRecord[] m_recordArray = null;
    private boolean m_initialized = false;

    private int m_hopCount = 1;

    private static final String[] DEFAULT_REPOSITORY_URL = {
        "http://oscar-osgi.sf.net/alpha/repository.xml"
    };
    public static final String REPOSITORY_URL_PROP = "osgi.repository.url";
    public static final String EXTERN_REPOSITORY_TAG = "extern-repositories";

    public RepositoryState(BundleContext context)
    {
        m_context = context;

        String urlStr = m_context.getProperty(REPOSITORY_URL_PROP);
        if (urlStr != null)
        {
            StringTokenizer st = new StringTokenizer(urlStr);
            if (st.countTokens() > 0)
            {
                m_urls = new String[st.countTokens()];
                for (int i = 0; i < m_urls.length; i++)
                {
                    m_urls[i] = st.nextToken();
                }
            }
        }

        // Use the default URL if none were specified.
        if (m_urls == null)
        {
            m_urls = DEFAULT_REPOSITORY_URL;
        }
    }

    public String[] getURLs()
    {
        // Return a copy because the array is mutable.
        return (m_urls == null) ? null : (String[]) m_urls.clone();
    }

    public void setURLs(String[] urls)
    {
        if (urls != null)
        {
            m_urls = urls;
            initialize();
        }
    }
    
    public BundleRecord[] getRecords()
    {
        if (!m_initialized)
        {
            initialize();
        }

        // Returned cached array of bundle records.
        return m_recordArray;
    }

    public BundleRecord[] getRecords(String symName)
    {
        if (!m_initialized)
        {
            initialize();
        }

        // Return a copy of the array, since it would be mutable
        // otherwise.
        BundleRecord[] records = (BundleRecord[]) m_recordMap.get(symName);
        // Return a copy because the array is mutable.
        return (records == null) ? null : (BundleRecord[]) records.clone();
    }

    public BundleRecord getRecord(String symName, int[] version)
    {
        if (!m_initialized)
        {
            initialize();
        }

        BundleRecord[] records = (BundleRecord[]) m_recordMap.get(symName);
        if ((records != null) && (records.length > 0))
        {
            for (int i = 0; i < records.length; i++)
            {
                int[] targetVersion = Util.parseVersionString(
                    (String) records[i].getAttribute(BundleRecord.BUNDLE_VERSION));
            
                if (Util.compareVersion(targetVersion, version) == 0)
                {
                    return records[i];
                }
            }
        }

        return null;
    }

    public BundleRecord[] resolvePackages(LocalState localState, Filter[] reqFilters)
        throws ResolveException
    {
        // Create a list that will contain the transitive closure of
        // all import dependencies; use a list because this will keep
        // everything in order.
        List deployList = new ArrayList();
        // Add the target bundle
        resolvePackages(localState, reqFilters, deployList);
        
        // Convert list of symbolic names to an array of bundle
        // records and return it.
        BundleRecord[] records = new BundleRecord[deployList.size()];
        return (BundleRecord[]) deployList.toArray(records);
    }

    private void resolvePackages(
        LocalState localState, Filter[] reqFilters, List deployList)
        throws ResolveException
    {
        for (int reqIdx = 0;
            (reqFilters != null) && (reqIdx < reqFilters.length);
            reqIdx++)
        {
            // If the package can be locally resolved, then
            // it can be completely ignored; otherwise, try
            // to find a resolving bundle.
            if (!localState.isResolvable(reqFilters[reqIdx]))
            {
                // Select resolving bundle for current package.
                BundleRecord source = selectResolvingBundle(
                    deployList, localState, reqFilters[reqIdx]);
                // If there is no resolving bundle, then throw a
                // resolve exception.
                if (source == null)
                {
throw new IllegalArgumentException("HACK: SHOULD THROW RESOLVE EXCEPTION: " + reqFilters[reqIdx]);
//                    throw new ResolveException(reqFilters[reqIdx]);
                }
                // If the resolving bundle is already in the deploy list,
                // then just ignore it; otherwise, add it to the deploy
                // list and resolve its packages.
                if (!deployList.contains(source))
                {
                    deployList.add(source);
                    Filter[] filters = (Filter[])
                        source.getAttribute("requirements");
                    resolvePackages(localState, filters, deployList);
                }
            }
        }
    }

    /**
     * Selects a single source bundle record for the target package from
     * the repository. The algorithm tries to select a source bundle record
     * if it is already installed locally in the framework; this approach
     * favors updating already installed bundles rather than installing
     * new ones. If no matching bundles are installed locally, then the
     * first bundle record providing the target package is returned.
     * @param targetPkg the target package for which to select a source
     *        bundle record.
     * @return the selected bundle record or <tt>null</tt> if no sources
     *         could be found.
    **/
    private BundleRecord selectResolvingBundle(
        List deployList, LocalState localState, Filter targetFilter)
    {
        BundleRecord[] exporters = findExporters(targetFilter);
        if (exporters == null)
        {
            return null;
        }

        // Try to select a source bundle record that is already
        // in the deployed list to minimize the number of bundles
        // that need to be deployed. If this is not possible, then
        // try to select a bundle that is already installed locally,
        // since it might be possible to update this bundle to
        // minimize the number of bundles installed in the framework.
        for (int i = 0; i < exporters.length; i++)
        {
            if (deployList.contains(exporters[i]))
            {
                return exporters[i];
            }
            else
            {
                String symName = (String)
                    exporters[i].getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME);
                if (symName != null)
                {
                    BundleRecord[] records = localState.findBundles(symName);
                    if (records != null)
                    {
                        return exporters[i];
                    }
                }
            }
        }
            
        // If none of the sources are installed locally, then
        // just pick the first one.
        return exporters[0];
    }

    /**
     * Returns an array of bundle records that resolve the supplied
     * package declaration.
     * @param target the package declaration to resolve.
     * @return an array of bundle records that resolve the package
     *         declaration or <tt>null</tt> if none are found.
    **/
    private BundleRecord[] findExporters(Filter targetFilter)
    {
        MapToDictionary mapDict = new MapToDictionary(null);

        // Create a list for storing bundles that can resolve package.
        List resolveList = new ArrayList();
        for (int recIdx = 0; recIdx < m_recordArray.length; recIdx++)
        {
            Map[] capMaps = (Map[]) m_recordArray[recIdx].getAttribute("capability");
            for (int capIdx = 0; capIdx < capMaps.length; capIdx++)
            {
                mapDict.setSourceMap(capMaps[capIdx]);
                if (targetFilter.match(mapDict))
                {
                    resolveList.add(m_recordArray[recIdx]);
                }
            }
        }

        // If no resolving bundles were found, return null.
        if (resolveList.size() == 0)
        {
            return null;
        }

        // Otherwise, return an array containing resolving bundles.
        return (BundleRecord[]) resolveList.toArray(new BundleRecord[resolveList.size()]);
    }

    private boolean isUpdateAvailable(
        PrintStream out, PrintStream err, Bundle bundle)
    {
        // Get the bundle's update location.
        String symname =
            (String) bundle.getHeaders().get(BundleRecord.BUNDLE_SYMBOLICNAME);

        // Get associated repository bundle recorded for the
        // local bundle and see if an update is necessary.
        BundleRecord[] records = getRecords(symname);
        if (records == null)
        {
            err.println(Util.getBundleName(bundle) + " not in repository.");
            return false;
        }
        
        // Check bundle version againts bundle record version.
        for (int i = 0; i < records.length; i++)
        {
            int[] bundleVersion = Util.parseVersionString(
                (String) bundle.getHeaders().get(BundleRecord.BUNDLE_VERSION));
            int[] recordVersion = Util.parseVersionString(
                (String) records[i].getAttribute(BundleRecord.BUNDLE_VERSION));
            if (Util.compareVersion(recordVersion, bundleVersion) > 0)
            {
                return true;
            }
        }
        return false;
    }

    private void initialize()
    {
        m_initialized = true;
        m_recordMap.clear();

        for (int urlIdx = 0; (m_urls != null) && (urlIdx < m_urls.length); urlIdx++)
        {
            parseRepositoryFile(m_hopCount, m_urls[urlIdx]);
        }
        
        // Cache a sorted array of all bundle records.
        List list = new ArrayList();
        for (Iterator i = m_recordMap.entrySet().iterator(); i.hasNext(); )
        {
            BundleRecord[] records = (BundleRecord[]) ((Map.Entry) i.next()).getValue();
            for (int recIdx = 0; recIdx < records.length; recIdx++)
            {
                list.add(records[recIdx]);
            }
        }
        m_recordArray = (BundleRecord[]) list.toArray(new BundleRecord[list.size()]);
        Arrays.sort(m_recordArray, new Comparator() {
            public int compare(Object o1, Object o2)
            {
                BundleRecord r1 = (BundleRecord) o1;
                BundleRecord r2 = (BundleRecord) o2;
                String name1 = (String) r1.getAttribute(BundleRecord.BUNDLE_NAME);
                String name2 = (String) r2.getAttribute(BundleRecord.BUNDLE_NAME);
                return name1.compareToIgnoreCase(name2);
            }
        });
    }

    private void parseRepositoryFile(int hopCount, String urlStr)
    {
        InputStream is = null;
        BufferedReader br = null;

        try
        {
            // Do it the manual way to have a chance to 
            // set request properties as proxy auth (EW).
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection(); 

            // Support for http proxy authentication
            String auth = System.getProperty("http.proxyAuth");
            if ((auth != null) && (auth.length() > 0))
            {
                if ("http".equals(url.getProtocol()) ||
                    "https".equals(url.getProtocol()))
                {
                    String base64 = Util.base64Encode(auth);
                    conn.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
                }
            }
            is = conn.getInputStream();

            // Create the parser Kxml
            XmlCommonHandler handler = new XmlCommonHandler();
            handler.addType("bundles", ArrayList.class);
            handler.addType("repository", HashMap.class);
            handler.addType("extern-repositories", ArrayList.class);
            handler.addType("bundle", MultivalueMap.class);
            handler.addType("requirement", String.class);
            handler.addType("capability", ArrayList.class);
            handler.addType("property", HashMap.class);
            handler.setDefaultType(String.class);

            br = new BufferedReader(new InputStreamReader(is));
            KXmlSAXParser parser;
            parser = new KXmlSAXParser(br);
            try
            {
                parser.parseXML(handler);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return;
            }

            List root = (List) handler.getRoot();
            for (int bundleIdx = 0; bundleIdx < root.size(); bundleIdx++)
            {
                Object obj = root.get(bundleIdx);
                
                // The elements of the root will either be a HashMap for
                // the repository tag or a MultivalueMap for the bundle
                // tag, as indicated above when we parsed the file.
                
                // If HashMap, then read repository information.
                if (obj instanceof HashMap)
                {
                    // Create a case-insensitive map.
                    Map repoMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2)
                        {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    repoMap.putAll((Map) obj);

                    // Process external repositories if hop count is
                    // greater than zero.
                    if (hopCount > 0)
                    {
                        // Get the external repository list.
                        List externList = (List) repoMap.get(EXTERN_REPOSITORY_TAG);
                        for (int i = 0; (externList != null) && (i < externList.size()); i++)
                        {
                            parseRepositoryFile(hopCount - 1, (String) externList.get(i));
                        }
                    }
                }
                // Else if mulitvalue map, then create a bundle record
                // for the associated bundle meta-data.
                else if (obj instanceof MultivalueMap)
                {
                    // Create a case-insensitive map.
                    Map bundleMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2)
                        {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    bundleMap.putAll((Map) obj);

                    // Convert capabilities into case-insensitive maps.
                    List list = (List) bundleMap.get("capability");
                    Map[] capabilityMaps = convertCapabilities(list);
                    bundleMap.put("capability", capabilityMaps);

                    // Convert requirements info filters.
                    list = (List) bundleMap.get("requirement");
                    Filter[] filters = convertRequirements(list);
                    bundleMap.put("requirement", filters);

                    // Convert any remaining single-element lists into
                    // the element itself.
                    for (Iterator i = bundleMap.keySet().iterator(); i.hasNext(); )
                    {
                        Object key = i.next();
                        Object value = bundleMap.get(key);
                        if ((value instanceof List) &&
                            (((List) value).size() == 1))
                        {
                            bundleMap.put(key, ((List) value).get(0));
                        }
                    }

                    // Create a bundle record using the map.
                    BundleRecord record = new BundleRecord(bundleMap);
                    // TODO: Filter duplicates.
                    BundleRecord[] records =
                        (BundleRecord[]) m_recordMap.get(
                            record.getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME));
                    if (records == null)
                    {
                        records = new BundleRecord[] { record };
                    }
                    else
                    {
                        BundleRecord[] newRecords = new BundleRecord[records.length + 1];
                        System.arraycopy(records, 0, newRecords, 0, records.length);
                        newRecords[records.length] = record;
                        records = newRecords;
                    }
                    m_recordMap.put(
                        record.getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME), records);
                }
            }
        }
        catch (MalformedURLException ex)
        {
            ex.printStackTrace(System.err);
//            System.err.println("Error: " + ex);
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
//            System.err.println("Error: " + ex);
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }
    }

    private Map[] convertCapabilities(List capLists)
    {
        Map[] capabilityMaps = new Map[(capLists == null) ? 0 : capLists.size()];
        for (int capIdx = 0; (capLists != null) && (capIdx < capLists.size()); capIdx++)
        {
            // Create a case-insensitive map.
            capabilityMaps[capIdx] = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                }
            });

            List capList = (List) capLists.get(capIdx);
            
            for (int propIdx = 0; propIdx < capList.size(); propIdx++)
            {
                Map propMap = (Map) capList.get(propIdx);
                String name = (String) propMap.get("name");
                String type = (String) propMap.get("type");
                String value = (String) propMap.get("value");
                try
                {
                    Class clazz = this.getClass().getClassLoader().loadClass(type);
                    Object o = clazz
                        .getConstructor(new Class[] { String.class })
                            .newInstance(new Object[] { value });
                    capabilityMaps[capIdx].put(name, o);
                }
                catch (Exception ex)
                {
// TODO: DETERMINE WHAT TO DO HERE.
                    // Two options here, we can either ignore the
                    // entire capability or we can just ignore the
                    // property. For now, just ignore the property.
                    continue;
                }
            }
        }
        return capabilityMaps;
    }

    private Filter[] convertRequirements(List reqsList)
    {
        Filter[] filters = new Filter[(reqsList == null) ? 0 : reqsList.size()];
        for (int i = 0; (reqsList != null) && (i < reqsList.size()); i++)
        {
            try
            {
                filters[i] = m_context.createFilter((String) reqsList.get(i));
            }
            catch (InvalidSyntaxException ex)
            {
            }
        }
        return filters;
    }
}