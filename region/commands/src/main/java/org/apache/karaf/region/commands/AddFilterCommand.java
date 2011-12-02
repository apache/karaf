/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.region.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.util.VersionRange;
import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@Command(scope = "region", name = "addFilter", description = "Adds a Filter between two regions")
public class AddFilterCommand extends RegionCommandSupport {

    @Argument(index = 0, name = "fromregion", description = "Region 1", required = true, multiValued = false)
    String fromRegion;

    @Argument(index = 1, name = "toregion", description = "Region 2", required = true, multiValued = false)
    String toRegion;

    @Argument(index = 2, name = "filteritems", description = "bundles by id and packages with version to allow", required = false, multiValued = true)
    List<String> items;

    protected void doExecute(RegionDigraph regionDigraph) throws Exception {
        Region rFrom = getRegion(regionDigraph, fromRegion);
        Region rTo = getRegion(regionDigraph, toRegion);
        RegionFilterBuilder builder = regionDigraph.createRegionFilterBuilder();
        BundleContext framework = getBundleContext().getBundle(0).getBundleContext();
        if (items != null) {
            for (String item : items) {
                try {
                    long id = Long.parseLong(item);
                    Bundle b = framework.getBundle(id);
                    builder.allow("osgi.wiring.bundle", "(osgi.wiring.bundle=" + b.getSymbolicName() + ")");
                } catch (NumberFormatException e) {
                    for (Map.Entry<String, Map<String, String>> parsed: ManifestHeaderProcessor.parseImportString(item).entrySet()) {
                        String packageName = parsed.getKey();
                        Map<String, String> attributes = new HashMap<String, String>(parsed.getValue());
                        attributes.put("osgi.wiring.package", packageName);
                        String filter = generateFilter(attributes);
                        System.out.println("adding filter " + filter);
                        builder.allow("osgi.wiring.package", filter);
                    }
                }

            }
        }
        RegionFilter f = builder.build();
        regionDigraph.connect(rFrom, f, rTo);
    }

    //from aries util, with obr specific weirdness removed
    public static String generateFilter(Map<String, String> attribs) {
        StringBuilder filter = new StringBuilder("(&");
        boolean realAttrib = false;
        StringBuffer realAttribs = new StringBuffer();

        if (attribs == null) {
            attribs = new HashMap<String, String>();
        }

        for (Map.Entry<String, String> attrib : attribs.entrySet()) {
            String attribName = attrib.getKey();

            if (attribName.endsWith(":")) {
                // skip all directives. It is used to affect the attribs on the
                // filter xml.
            } else if ((Constants.VERSION_ATTRIBUTE.equals(attribName))
                    || (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(attribName))) {
                // version and bundle-version attrib requires special
                // conversion.
                realAttrib = true;

                VersionRange vr = ManifestHeaderProcessor
                        .parseVersionRange(attrib.getValue());

                filter.append("(" + attribName + ">=" + vr.getMinimumVersion());

                if (vr.getMaximumVersion() != null) {
                    filter.append(")(" + attribName + "<=");
                    filter.append(vr.getMaximumVersion());
                }

                if (vr.getMaximumVersion() != null && vr.isMinimumExclusive()) {
                    filter.append(")(!(" + attribName + "=");
                    filter.append(vr.getMinimumVersion());
                    filter.append(")");
                }

                if (vr.getMaximumVersion() != null && vr.isMaximumExclusive()) {
                    filter.append(")(!(" + attribName + "=");
                    filter.append(vr.getMaximumVersion());
                    filter.append(")");
                }
                filter.append(")");

            } else if (Constants.OBJECTCLASS.equals(attribName)) {
                realAttrib = true;
                // objectClass has a "," separated list of interfaces
                String[] values = attrib.getValue().split(",");
                for (String s : values)
                    filter.append("(" + Constants.OBJECTCLASS + "=" + s + ")");

            } else {
                // attribName was not version..
                realAttrib = true;

                filter.append("(" + attribName + "=" + attrib.getValue() + ")");
                // store all attributes in order to build up the mandatory
                // filter and separate them with ", "
                // skip bundle-symbolic-name in the mandatory directive query
                if (!!!Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE
                        .equals(attribName)) {
                    realAttribs.append(attribName);
                    realAttribs.append(", ");
                }
            }
        }

        // Prune (& off the front and ) off end
        String filterString = filter.toString();
        int openBraces = 0;
        for (int i = 0; openBraces < 3; i++) {
            i = filterString.indexOf('(', i);
            if (i == -1) {
                break;
            } else {
                openBraces++;
            }
        }
        if (openBraces < 3 && filterString.length() > 2) {
            filter.delete(0, 2);
        } else {
            filter.append(")");
        }

        String result = "";
        if (realAttrib != false) {
            result = filter.toString();
        }
        return result;
    }

}
