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
package org.apache.felix.bundleplugin;


import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * Provide access to the archive configuration from the jar plugin
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class JarPluginConfiguration
{
    public static MavenArchiveConfiguration getArchiveConfiguration( MavenProject project )
    {
        MavenArchiveConfiguration archiveConfig = new MavenArchiveConfiguration();

        try
        {
            ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
            ClassLoader loader = JarPluginConfiguration.class.getClassLoader();
            ExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
            ConverterLookup converters = new DefaultConverterLookup();

            PlexusConfiguration settings = null;

            try
            {
                // first look for bundle specific archive settings
                settings = getPluginConfiguration( project, "org.apache.felix", "maven-bundle-plugin" );
                settings = settings.getChild( "archive" );
            }
            catch ( Exception e )
            {
            }

            // if it's empty fall back to the jar archive settings
            if ( null == settings || settings.getChildCount() == 0 )
            {
                settings = getCorePluginConfiguration( project, "jar" );
                settings = settings.getChild( "archive" );
            }

            converter.processConfiguration( converters, archiveConfig, loader, settings, evaluator, null );
        }
        catch ( Exception e )
        {
            // ignore and return empty configuration...
        }

        return archiveConfig;
    }


    private static PlexusConfiguration getCorePluginConfiguration( MavenProject project, String pluginName )
    {
        return getPluginConfiguration( project, "org.apache.maven.plugins", "maven-" + pluginName + "-plugin" );
    }


    private static PlexusConfiguration getPluginConfiguration( MavenProject project, String groupId, String artifactId )
    {
        Xpp3Dom pluginConfig = project.getGoalConfiguration( groupId, artifactId, null, null );

        return new XmlPlexusConfiguration( pluginConfig );
    }
}
