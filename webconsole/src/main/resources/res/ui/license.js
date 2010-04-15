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

var licenseButtons = false;
var licenseDetails = false;

/*
 * Fuction called after receiving the license data from the server to insert
 * it into the licenseDetails div
 * Because IE does not properly support the white-space:pre CSS setting when
 * DOM-loading data into a <pre> element, the licenseDetails element is a
 * <div> into which we insert the data surrounded by <pre>-</pre> tags as
 * innerHtml. This also works in IE.
 */
function insertLicenseData( /* String */ data )
{
    licenseDetails.html( "<pre>" + data + "</pre>" );
}

function displayBundle(/* String */ bundleIndex)
{
    var theBundleData = bundleData[bundleIndex];
    if (!theBundleData)
    {
        return;
    }

    var title = theBundleData.title;
    
    if (licenseButtons) {
        
        var innerHTML = "";
        for (var name in theBundleData.files)
        {
            var entry = theBundleData.files[name];
            var buttons = "";
            var firstPage = null;
            for (var idx in entry)
            {
                var descr = entry[idx];

                var link = pluginRoot + "/" + theBundleData.bid;
                if (descr.jar)
                {
                    link += descr.jar + "!/"; // inner jar attribute
                }
                link += descr.path;
				if (descr.path.indexOf('http:') == 0 || descr.path.indexOf('ftp:') == 0) link = descr.path;

				buttons += '<a href="' + link + '">' + descr.url + '</a> ';

				if (!firstPage)
				{
				    firstPage = link;
				}
            }
            if (buttons)
            {
				// apply i18n
				name =  '__res__' == name ? i18n.resources : i18n.resources_emb.msgFormat( name );
                innerHTML += name + ": " + buttons + "<br />";
            }
        }

        licenseButtons.html("<h1>" + title + "</h1>" + innerHTML);
    }
    
    if (firstPage) {
		openLicenseLink(firstPage);
    } else {
        licenseDetails.html("");
    }
    
	$("#licenseLeft a").removeClass('ui-state-default ui-corner-all');
	$("#licenseLeft #" +bundleIndex).addClass('ui-state-default ui-corner-all');

    $('#licenseButtons a').click(function() {
		openLicenseLink(this.href);
		return false;
    });
}

function openLicenseLink(uri) {
	if (uri.indexOf(window.location.href) == 0 || uri.indexOf(pluginRoot) == 0) { // local URI
        $.get(uri, insertLicenseData);
	} else {
		licenseDetails.html( '<iframe frameborder="0" src="' + uri+ '"></iframe>' );
	}
}


$(document).ready(function() {
	// init elements cache
	licenseButtons = $("#licenseButtons");
	licenseDetails = $("#licenseDetails")

	// render list of bundles
	var txt = "";
	for(id in bundleData) {
		txt += '<a id="' + id + '" href="javascript:displayBundle(\'' + id + '\')">' + bundleData[id].title + '</a>';
	}
	if (txt) {
		$("#licenseLeft").html(txt);
	} else {
		$(".statline").html(i18n.status_none);
	}

	// display first element
	displayBundle(0);
});