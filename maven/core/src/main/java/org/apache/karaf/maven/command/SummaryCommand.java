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
package org.apache.karaf.maven.command;

import java.util.Dictionary;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.apache.maven.settings.Proxy;

@Command(scope = "maven", name = "summary", description = "Maven configuration summary.")
@Service
public class SummaryCommand extends MavenSecuritySupport {

    @Option(name = "-p", aliases = { "--property-ids" }, description = "Use PID property identifiers instead of their names", required = false, multiValued = false)
    boolean propertyIds;

    @Option(name = "-s", aliases = { "--source" }, description = "Adds information about where the value is configured", required = false, multiValued = false)
    boolean source;

    @Option(name = "-d", aliases = { "--description" }, description = "Adds description of Maven configuration options", required = false, multiValued = false)
    boolean description;

    @Override
    protected void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        ShellTable table = new ShellTable();
        table.column("Option");
        table.column("Value");
        if (source) {
            table.column("Source");
        }
        if (description) {
            table.column("Description");
        }

        addRow(table, propertyIds ? PROPERTY_LOCAL_REPOSITORY : "Local repository", localRepository,
                "Maven repository to store artifacts resolved in *remote repositories*");

        addRow(table, propertyIds ? PROPERTY_SETTINGS_FILE : "Settings file", settings,
                "Settings file that may contain configuration of additional repositories, http proxies and mirrors");

        addRow(table, propertyIds ? PROPERTY_SECURITY_FILE : "Security settings file", securitySettings,
                "Settings file that contain (or relocates to) master Maven password");

        if (showPasswords) {
            addRow(table, propertyIds ? "<master>" : "Master password", new SourceAnd<>(securitySettings.source, masterPassword),
                    "Master password used to decrypt proxy and server passwords");
        }

        // for default update/checksum policies specified at repository URI level, see
        // org.ops4j.pax.url.mvn.internal.AetherBasedResolver.addRepo()

        // see org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer#isUpdatedRequired()
        SourceAnd<String> updatePolicy = updatePolicy((String) config.get(prefix + PROPERTY_GLOBAL_UPDATE_POLICY));
        addRow(table, propertyIds ? PROPERTY_GLOBAL_UPDATE_POLICY : "Global update policy", updatePolicy,
                "Overrides update policy specified at repository level (if specified)");

        // see org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider#newChecksumPolicy()
        SourceAnd<String> checksumPolicy = checksumPolicy((String) config.get(prefix + PROPERTY_GLOBAL_CHECKSUM_POLICY));
        addRow(table, propertyIds ? PROPERTY_GLOBAL_CHECKSUM_POLICY : "Global checksum policy", checksumPolicy,
                "Checksum policy for all repositories");

        String updateReleasesProperty = (String) config.get(prefix + PROPERTY_UPDATE_RELEASES);
        boolean updateReleases = false;
        String sourceInfo = String.format(PATTERN_PID_PROPERTY, PID, prefix + PROPERTY_UPDATE_RELEASES);
        if (updateReleasesProperty == null) {
            sourceInfo = "Default \"false\"";
        } else {
            updateReleases = "true".equals(updateReleasesProperty);
        }
        addRow(table, propertyIds ? PROPERTY_UPDATE_RELEASES : "Update releases", new SourceAnd<>(sourceInfo, updateReleases),
                "Whether to download non-SNAPSHOT artifacts according to update policy");

        // see org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl.isValid()
        // ANY non null value (even "false"!) means that we require configadmin
        String requireConfigAdminProperty = context.getProperty(prefix + REQUIRE_CONFIG_ADMIN_CONFIG);
        boolean requireConfigAdmin = requireConfigAdminProperty != null;
        sourceInfo = "Default \"false\"";
        if (requireConfigAdmin) {
            sourceInfo = "BundleContext property (" + prefix + REQUIRE_CONFIG_ADMIN_CONFIG + ")";
        }
        addRow(table, propertyIds ? REQUIRE_CONFIG_ADMIN_CONFIG : "Require Config Admin", new SourceAnd<>(sourceInfo, requireConfigAdmin),
                "Whether MavenResolver service is registered ONLY with proper " + PID + " PID configuration");

        // see org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl.buildSettings()
        String useFallbackRepositoriesProperty = (String) config.get(prefix + PROPERTY_USE_FALLBACK_REPOSITORIES);
        boolean useFallbackRepositories = Boolean.parseBoolean(useFallbackRepositoriesProperty);
        sourceInfo = "Default \"false\"";
        if (useFallbackRepositoriesProperty != null) {
            sourceInfo = String.format(PATTERN_PID_PROPERTY, PID, prefix + PROPERTY_USE_FALLBACK_REPOSITORIES);
        }
        addRow(table, propertyIds ? PROPERTY_USE_FALLBACK_REPOSITORIES : "Use fallback repository", new SourceAnd<>(sourceInfo, useFallbackRepositories),
                "Whether Maven Central is used as implicit, additional remote repository");

        // see org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl.enableProxy()
        // "proxySupport" and "proxies" are not used in "new" MavenResolver

        // see org.eclipse.aether.internal.impl.DefaultOfflineController#checkOffline()
        String offlineProperty = (String) config.get(prefix + PROPERTY_OFFLINE);
        boolean offline = Boolean.parseBoolean(offlineProperty);
        sourceInfo = "Default \"false\"";
        if (offlineProperty != null) {
            sourceInfo = String.format(PATTERN_PID_PROPERTY, PID, prefix + PROPERTY_OFFLINE);
        }
        addRow(table, propertyIds ? PROPERTY_OFFLINE : "Offline mode", new SourceAnd<>(sourceInfo, offline),
                "Disables access to external remote repositories (file:// based ones are still used)");

        // see org.ops4j.pax.url.mvn.internal.HttpClients.createConnManager()
        String certificateCheckProperty = (String) config.get(prefix + PROPERTY_CERTIFICATE_CHECK);
        boolean certificateCheck = Boolean.parseBoolean(certificateCheckProperty);
        sourceInfo = "Default \"false\"";
        if (certificateCheckProperty != null) {
            sourceInfo = String.format(PATTERN_PID_PROPERTY, PID, prefix + PROPERTY_CERTIFICATE_CHECK);
        }
        addRow(table, propertyIds ? PROPERTY_CERTIFICATE_CHECK : "SSL/TLS certificate check", new SourceAnd<>(sourceInfo, certificateCheck),
                "Turns on server certificate validation for HTTPS remote repositories");

        // repositories (short list)
        MavenRepositoryURL[] remoteRepositories = repositories(config, true);
        boolean first = true;
        for (MavenRepositoryURL url : remoteRepositories) {
            addRow(table, first ? (propertyIds ? PROPERTY_REPOSITORIES : "Remote repositories") : "", new SourceAnd<>(url.getFrom().getSource(), url.getURL().toString()),
                first ? "Remote repositories where artifacts are being resolved if not found locally" : "");
            first = false;
        }

        // default repositories (short list)
        MavenRepositoryURL[] defaultRepositories = repositories(config, false);
        first = true;
        for (MavenRepositoryURL url : defaultRepositories) {
            addRow(table, first ? (propertyIds ? PROPERTY_DEFAULT_REPOSITORIES : "Default repositories") : "", new SourceAnd<>(url.getFrom().getSource(), url.getURL().toString()),
                    first ? "Repositories where artifacts are looked up before trying remote resolution" : "");
            first = false;
        }

        // proxies (short list)
        if (mavenSettings != null && mavenSettings.getProxies() != null) {
            first = true;
            for (Proxy proxy : mavenSettings.getProxies()) {
                String value = String.format("%s:%s", proxy.getHost(), proxy.getPort());
                addRow(table, first ? (propertyIds ? "<proxies>" : "HTTP proxies") : "", new SourceAnd<>(MavenRepositoryURL.FROM.SETTINGS.getSource(), value),
                        first ? "Maven HTTP proxies" : "");
                first = false;
            }
        }

        System.out.println();
        table.print(System.out);
        System.out.println();
    }

    /**
     * Helper to add row to {@link ShellTable}
     * @param table
     * @param label
     * @param value
     * @param descriptionText
     */
    private <T> void addRow(ShellTable table, String label, SourceAnd<T> value, String descriptionText) {
        Row row = table.addRow();
        row.addContent(label, value.val());
        if (source) {
            row.addContent(value.source);
        }
        if (description) {
            row.addContent(descriptionText);
        }
    }

}
