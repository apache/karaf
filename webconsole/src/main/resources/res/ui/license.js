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

function displayBundle(/* String */ bundleId)
{
    var theBundleData = bundleData[bundleId];
    if (!theBundleData)
    {
        return;
    }

    var title = theBundleData.title;
    
    var licenseButtons = document.getElementById('licenseButtons');
    if (licenseButtons) {
        
        var innerHTML = "";
        for (var name in theBundleData.files)
        {
            var entry = theBundleData.files[name];
            var buttons = "";
            for (var idx in entry)
            {
                var descr = entry[idx];
                buttons += "<a href='javascript:displayFile(\"" + bundleId + "\", \"" + name + "\", " + idx + ");'"
                   + " >" + descr.url + "</a> ";
            }
            if (buttons)
            {
                innerHTML += name + ": " + buttons + "<br />";
            }
        }
        
        if (!innerHTML)
        {
            innerHTML = "<em>The Bundle contains neither LICENSE nor NOTICE files</em>";
        }
        
        licenseButtons.innerHTML = "<h1>" + title + "</h1>" + innerHTML;
    }
    
    var licenseDetails = document.getElementById('licenseDetails');
    if (licenseDetails)
    {
        licenseDetails.innerHTML = "";
    }
}

function displayFile ( /* String */ bundleId, /* String */ name, /* int */ idx )
{
    var theBundleData = bundleData[bundleId];
    if (!theBundleData)
    {
        return;
    }
    
    var file = theBundleData.files[name][idx];
    if (!file)
    {
        return;
    }
    
    var licenseDetails = document.getElementById('licenseDetails');
    if (licenseDetails)
    {
        licenseDetails.innerHTML = "<h3>" + name + ": " + file.url + "</h3><pre>" + file.data + "</pre>";
    }
}
