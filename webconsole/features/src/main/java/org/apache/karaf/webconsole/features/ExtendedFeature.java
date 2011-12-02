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
package org.apache.karaf.webconsole.features;


import java.util.List;
import java.util.Map;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;


/**
 * 
 */
public class ExtendedFeature implements Feature
{

    public enum State
    {
        INSTALLED, UNINSTALLED;

        @Override
        public String toString()
        {
            //only capitalize the first letter
            String s = super.toString();
            return s.substring( 0, 1 ) + s.substring( 1 ).toLowerCase();
        }
    }

    protected final State state;
    protected final String repository;
    protected final Feature feature;


    //
    // Constructors
    //

    public ExtendedFeature( State state, String repository, Feature feature )
    {
        this.state = state;
        this.repository = repository;
        this.feature = feature;
    }


    //
    // Feature interface
    //


    public List<BundleInfo> getBundles()
    {
        return this.feature.getBundles();
    }


    public Map<String, Map<String, String>> getConfigurations()
    {
        return this.feature.getConfigurations();
    }

    public List<ConfigFileInfo> getConfigurationFiles() {
		return this.feature.getConfigurationFiles();
	}
    
    public List<Dependency> getDependencies()
    {
        return this.feature.getDependencies();
    }


    public String getId()
    {
        return this.feature.getId();
    }


    public String getName()
    {
        return this.feature.getName();
    }


    public String getVersion()
    {
        return this.feature.getVersion();
    }

    public String getResolver()
    {
        return this.feature.getResolver();
    }

    public String getDescription() {
        return this.feature.getDescription();
    }

    public String getDetails() {
        return this.feature.getDetails();
    }


    //
    // Additional methods
    //


    public String getRepository() {
        return this.repository;
    }

    public String getInstall() {
        return feature.getInstall();
    }

    public State getState() {
        return this.state;
    }
    
    public int getStartLevel() {
        return 0;
    }

    @Override
    public String getRegion() {
        return feature.getRegion();
    }
}
