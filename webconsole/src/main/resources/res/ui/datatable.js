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

function renderDataTable( /* Array of Data Objects */ components )
{
    // number of actions plus 3 -- id, name and state
    var columns = components.numActions + 3;
    
    header( columns );

    if (components.error)
    {
        error( columns, components.error );
    }
    else
    {
        data ( components.data );
    }

    footer( columns );
}


function header( /* int */ columns )
{
    document.write( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "<tr class='content'>" );
    document.write( "<th class='content'>ID</th>" );
    document.write( "<th class='content' width='100%'>Name</th>" );
    document.write( "<th class='content'>Status</th>" );
    document.write( "<th class='content' colspan='" + (columns - 3) + "'>Actions</th>" );
    document.write( "</tr>" );

}


function error( /* int */ columns, /* String */ message )
{
    document.write( "<tr class='content'>" );
    document.write( "<td class='content'>&nbsp;</td>" );
    document.write( "<td class='content' colspan='" + (columns - 1) + "'>" + message + "</td>" );
    document.write( "</tr>" );
}


function data( /* Array of Object */ dataArray )
{
    // render components
    for ( var idx in dataArray )
    {
        entry( dataArray[idx] );
    }
}


function footer( /* int */ columns )
{
    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "</table>" );
}


function entry( /* Object */ dataEntry )
{
    document.write( "<tr id='entry" + dataEntry.id + "'>" );
    document.write( entryInternal( dataEntry ) );
    document.write( "</tr>" );

    // dataEntry detailed properties
    document.write( "<tr id='entry" + dataEntry.id + "_details'>" );
    if (dataEntry.props)
    {
        document.write( getDataEntryDetails( dataEntry.props ) );
    }
    document.write( "</tr>" );
}


/* String */ function entryInternal( /* Object */ dataEntry )
{
    var id = dataEntry.id;
    var name = dataEntry.name;
    var state = dataEntry.state;
    var icon = (dataEntry.props) ? "down" : "right";

    var html = "<td class='content right'>" + id + "</td>";
    html += "<td class='content'>";
    html += "<img src='" + appRoot + "/res/imgs/" + icon + ".gif' onClick='showDataEntryDetails(" + id + ")' id='entry" + id + "_inline' />";
    html += "<a href='" + pluginRoot + "/" + id + "'>" + name + "</a></td>";

    html += "<td class='content center'>" + state + "</td>";

    for ( var aidx in dataEntry.actions )
    {
        var action = dataEntry.actions[aidx];
        html += actionButton( action.enabled, id, action.link, action.name );
    }
    
    return html;
}


/* String */ function actionButton( /* boolean */ enabled, /* long */ id, /* String */ op, /* String */ opLabel )
{
    var theButton = "<td class='content' align='right'>";
    if ( op )
    {
        theButton += "<input class='submit' type='button' value='" + opLabel + "'" + ( enabled ? "" : "disabled" ) + " onClick='changeDataEntryState(" + id + ", \"" + op + "\");' />";
    }
    else
    {
        theButton += "&nbsp;";
    }
    theButton += "</td>";
    return theButton;
}


/* String */ function getDataEntryDetails( /* Array of Object */ details )
{
    var innerHtml = '<td class=\"content\">&nbsp;</td><td class=\"content\" colspan=\"4\"><table broder=\"0\">';
    for (var idx in details)
    {
        var prop = details[idx];
        innerHtml += '<tr><td valign=\"top\" noWrap>' + prop.key + '</td><td valign=\"top\">' + prop.value + '</td></tr>';
    }
    innerHtml += '</table></td>';
    return innerHtml;
 }

 
function showDetails(bundleId) {
    var span = document.getElementById('bundle' + bundleId + '_details');
}


function showDataEntryDetails( id )
{
    var span = document.getElementById( 'entry' + id + '_details' );
    if (span)
    {
        if (span.innerHTML)
        {
            span.innerHTML = '';
            newLinkValue( id, appRoot + "/res/imgs/right.gif" );
        }
        else
        {
            sendRequest( 'POST', pluginRoot + '/' + id, displayDataEntryDetails );
            newLinkValue( id, appRoot + "/res/imgs/down.gif" );
        }
    }
}


function newLinkValue( /* long */ id, /* String */ newLinkValue )
{
    
    var link = document.getElementById( "entry" + id + "_inline" );
    if (link)
    {
        link.src = newLinkValue;
    }
}


function displayDataEntryDetails( obj )
{
    var span = document.getElementById('entry' + obj.id + '_details');
    if (!span)
    {
        return;
    }
    
    span.innerHTML = getDataEntryDetails( obj.props );
}


function changeDataEntryState(/* long */ id, /* String */ action)
{
    var parm = pluginRoot + "/" + id + "?action=" + action;
    sendRequest('POST', parm, dataEntryStateChanged);
}

    
function dataEntryStateChanged(obj)
{
    if (obj.reload)
    {
        document.location = document.location;
    }
    else
    {
        var id = obj.id;
        if (obj.state)
        {
            // has status, so draw the line
            if (obj.props)
            {
                var span = document.getElementById('entry' + id + '_details');
                if (span && span.innerHTML)
                {
                    span.innerHTML = getDataEntryDetails( obj.props );
                }
                else
                {
                    obj.props = false;
                }
            }

            var span = document.getElementById('entry' + id);
            if (span)
            {
                span.innerHTML = entryInternal( obj );
            }
            
            
        }
        else
        {
            // no status, dataEntry has been removed/uninstalled 
            var span = document.getElementById('entry' + id);
            if (span)
            {
                span.parentNode.removeChild(span);
            }
            var span = document.getElementById('entry' + id + '_details');
            if (span)
            {
                span.parentNode.removeChild(span);
            }
        }
    }    
}

    
