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

function render(/* int */ startlevel, /* Array of Bundle Object */ bundles)
{

    header();

    installForm( startlevel );
    
    document.write( "<tr class='content'>" );
    document.write( "<td colspan='7' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    tableHeader();

    if ( !bundles )
    {
        document.write( "<tr class='content'>" );
        document.write( "<td class='content' colspan='6'>No Bundles installed currently</td>" );
        document.write( "</tr>" );
    }
    else
    {
        for ( var i = 0; i < bundles.length; i++ )
        {
            bundle( bundles[i] );
        }
    }

    document.write( "<tr class='content'>" );
    document.write( "<td colspan='7' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    installForm( startlevel );

    footer();
}


function header()
{
    document.write( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
}


function tableHeader()
{
    document.write( "<tr class='content'>" );
    document.write( "<th class='content'>ID</th>" );
    document.write( "<th class='content' width='100%'>Name</th>" );
    document.write( "<th class='content'>Status</th>" );
    document.write( "<th class='content' colspan='4'>Actions</th>" );
    document.write( "</tr>" );
}


function footer()
{
    document.write( "</table>" );
}


function bundle( /* Bundle */ bundle )
{  
    document.write( "<tr id='bundle" + bundle.bundleId + "'>" );
    document.write( bundleInternal( bundle ) );
    document.write( "</tr>" );
    document.write( "<tr id='bundle" + bundle.bundleId + "_details'>" );
    if (bundle.props)
    {
        document.write( bundleDetails( bundle.props ) );
    }
    document.write( "</tr>" );
}

   
/* String */ function bundleInternal( /* Bundle */ bundle )
{
    var icon = (bundle.props) ? "down" : "right";
    var theBundle = "<td class='content right'>" + bundle.bundleId + "</td>";
    theBundle += "<td class='content'><img src='" + appRoot + "/res/imgs/" + icon + ".gif' onClick='showDetails(" + bundle.bundleId + ")' id='bundle" + bundle.bundleId + "_inline' />";
    theBundle += " <a href='" + appRoot + "/bundles/" + bundle.bundleId + "'>" + bundle.name + "</a></td>";
    theBundle += "<td class='content center'>" + bundle.state + "</td>";

    // no buttons for system bundle
    if ( bundle.bundleId == 0 )
    {
        theBundle += "<td class='content' colspan='4'>&nbsp;</td>";
    }
    else
    {
        theBundle += actionForm( bundle.hasStart, bundle.bundleId, "start", "Start" );
        theBundle += actionForm( bundle.hasStop, bundle.bundleId, "stop", "Stop" );
        theBundle += actionForm( bundle.hasUpdate, bundle.bundleId, "update", "Update" );
        theBundle += actionForm( bundle.hasUninstall, bundle.bundleId, "uninstall", "Uninstall" );
    }

    return theBundle;
}


/* String */ function actionForm( /* boolean */ enabled, /* long */ bundleId, /* String */ action, /* String */ actionLabel )
{
    var theButton = "<td class='content' align='right'>";
    theButton += "<input class='submit' type='button' value='" + actionLabel + "'" + ( enabled ? "" : "disabled" ) + " onClick='changeBundle(" + bundleId + ", \"" + action + "\");' />";
    theButton += "</td>";
    return theButton;
}


function installForm( /* int */ startLevel )
{
    document.write( "<form method='post' enctype='multipart/form-data'>" );
    document.write( "<tr class='content'>" );
    document.write( "<td class='content'>&nbsp;</td>" );
    document.write( "<td class='content'>" );
    document.write( "<input type='hidden' name='action' value='install' />" );
    document.write( "<input class='input' type='file' name='bundlefile'>" );
    document.write( " - Start <input class='checkradio' type='checkbox' name='bundlestart' value='start'>" );
    document.write( " - Start Level <input class='input' type='input' name='bundlestartelevel' value='" + startLevel + "' width='4'>" );
    document.write( "</td>" );
    document.write( "<td class='content' align='right' colspan='5' noWrap>" );
    document.write( "<input class='submit' style='width:auto' type='submit' value='Install or Update'>" );
    document.write( "&nbsp;" );
    document.write( "<input class='submit' style='width:auto' type='button' value='Refresh Packages' onClick='changeBundle(0, \"refreshPackages\");'>" );
    document.write( "</td>" );
    document.write( "</tr>" );
    document.write( "</form>" );
}


function changeBundle(/* long */ bundleId, /* String */ action)
{
    var parm = "bundles/" + bundleId + "?action=" + action;
    sendRequest('POST', parm, bundleChanged);
}

    
function bundleChanged(obj)
{
    if (obj.reload)
    {
        document.location = document.location;
    }
    else
    {
        var bundleId = obj.bundleId;
        if (obj.state)
        {
            // has status, so draw the line
            var span = document.getElementById('bundle' + bundleId);
            if (span)
            {
                span.innerHTML = bundleInternal( obj );
            }
            
            if (obj.props)
            {
                var span = document.getElementById('bundle' + bundleId + '_details');
                if (span && span.innerHTML)
                {
                    span.innerHTML = bundleDetails( obj.props );
                }
            }
            
        }
        else
        {
            // no status, bundle has been uninstalled
            var span = document.getElementById('bundle' + bundleId);
            if (span)
            {
                span.parentNode.removeChild(span);
            }
            var span = document.getElementById('bundle' + bundleId + '_details');
            if (span)
            {
                span.parentNode.removeChild(span);
            }
        }
    }    
}

    
function showDetails(bundleId) {
    var span = document.getElementById('bundle' + bundleId + '_details');
    if (span)
    {
        if (span.innerHTML)
        {
            span.innerHTML = '';
            newLinkValue(bundleId, appRoot + "/res/imgs/right.gif");
        }
        else
        {
            sendRequest('GET', appRoot + "/bundles/" + bundleId + ".json", displayBundleDetails);
            newLinkValue(bundleId, appRoot + "/res/imgs/down.gif");
        }
    }
}


function displayBundleDetails(obj) {
    var span = document.getElementById('bundle' + obj.bundleId + '_details');
    if (span)
    {
        span.innerHTML = bundleDetails( obj.props );
    }
}


function newLinkValue(bundleId, newLinkValue)
{
    
    var link = document.getElementById("bundle" + bundleId + "_inline");
    if (link)
    {
        link.src = newLinkValue;
    }
}


/* String */ function bundleDetails( props )
{
        var innerHtml = '<td class=\"content\">&nbsp;</td><td class=\"content\" colspan=\"6\"><table broder=\"0\">';
        for (var i=0; i < props.length; i++)
        {
            innerHtml += '<tr><td valign=\"top\" noWrap>' + props[i].key + '</td><td valign=\"top\">' + props[i].value + '</td></tr>';
        }
        innerHtml += '</table></td>';
        
        return innerHtml;
}

