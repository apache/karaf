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

function header( /* int */ columns )
{
    document.write( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "<tr class='content'>" );
    document.write( "<th class='content'>Name</th>" );
    document.write( "<th class='content' width='100%'>Details</th>" );
    document.write( "<th class='content'>Version</th>" );
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
    if (dataArray.length == 1)
    {
        entry( dataArray[0], true );
    }
    else {
        for ( var idx in dataArray )
        {
            entry( dataArray[idx] );
        }
    }
}


function footer( /* int */ columns )
{
    document.write( "<tr class='content'>" );
    document.write( "<td colspan='" + columns + "' class='content'>&nbsp;</th>" );
    document.write( "</tr>" );

    document.write( "</table>" );
}


function entry( /* Object */ dataEntry, /* boolean */ singleEntry )
{
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry, singleEntry );
    document.write( serialize( trElement ) );

    // dataEntry detailed properties
    trElement = tr( null, { id: "entry" + dataEntry.id + "_details" } );
    if (dataEntry.props)
    {
        getDataEntryDetails( trElement, dataEntry.props );
    }
    document.write( serialize( trElement ) );
}


function entryInternal( /* Element */ parent, /* Object */ dataEntry, /* boolean */ singleEntry )
{

    var id = dataEntry.id;
    var name = dataEntry.name;
    var state = dataEntry.state;
    var icon = singleEntry ? "left" : (dataEntry.props ? "down" : "right");
    var event = singleEntry ? "history.back()" : "showDataEntryDetails(" + id + ")"; 

    parent.appendChild( td( "content right", null, [ text( id ) ] ) );
    
    parent.appendChild( td( "content", null, [
            createElement( "img", null, {
                src: appRoot + "/res/imgs/" + icon + ".gif",
                onClick: event,
                id: "entry" + id + "_inline"
            } ),
            text( "\u00a0" ),
            createElement( "a", null, {
                href: pluginRoot + "/" + id
            }, [ text( name ) ]
            )]
        )
    );

    parent.appendChild( td( "content center", null, [ text( state ) ] ) );

    for ( var aidx in dataEntry.actions )
    {
        var action = dataEntry.actions[aidx];
        parent.appendChild( actionButton( action.enabled, id, action.link, action.name ) );
    }
}


/* Element */ function actionButton( /* boolean */ enabled, /* long */ id, /* String */ op, /* String */ opLabel )
{
    var buttonTd = td( "content", { align: "right" } );
    if ( op )
    {
        var input = createElement( "input", "submit", {
                type: 'button',
                value: opLabel,
                onClick: 'changeDataEntryState("' + id + '", "' + op + '");'
            });
        if (!enabled)
        {
            input.setAttribute( "disabled", true );
        }
        buttonTd.appendChild( input );
    }
    else
    {
        addText( buttonTd, "\u00a0" );
    }
    
    return buttonTd;
}


function getDataEntryDetails( /* Element */ parent, /* Array of Object */ details )
{
    parent.appendChild( addText( td( "content"), "\u00a0" ) );
    
    var tdEl = td( "content", { colspan: 4 } );
    parent.appendChild( tdEl );
    
    var tableEl = createElement( "table", null, { border: 0 } );
    tdEl.appendChild( tableEl );
    
    var tbody = createElement( "tbody" );
    tableEl.appendChild( tbody );
    for (var idx in details)
    {
        var prop = details[idx];
        
        
        var trEl = tr();
        trEl.appendChild( addText( td( "aligntop", { noWrap: true } ), prop.key ) );

        var proptd = td( "aligntop" );
        trEl.appendChild( proptd );
        
        if (prop.value )
        {
            var values = new String( prop.value ).split( "<br />" );
            for (var i=0; i < values.length; i++)
            {
                if (i > 0) { proptd.appendChild( createElement( "br" ) ); }
                addText( proptd, values[i] );
            }
        }
        else
        {
            addText( proptd, "\u00a0" );
        }

        tbody.appendChild( trEl );
    }
 }

 
function showDetails(bundleId)
{
    var span = document.getElementById('bundle' + bundleId + '_details');
}


function showDataEntryDetails( id )
{
    var span = document.getElementById( 'entry' + id + '_details' );
    if (span)
    {
        if (span.firstChild)
        {
            clearChildren( span );
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
    if (span)
    {
        clearChildren( span );
        getDataEntryDetails( span, obj.props );
    }
    
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
                if (span && span.firstChild)
                {
                    clearChildren( span );
                    getDataEntryDetails( span, obj.props );
                }
                else
                {
                    obj.props = false;
                }
            }

            var span = document.getElementById('entry' + id);
            if (span)
            {
                clearChildren( span );
                entryInternal( span, obj );
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

function renderPackage( /* Array of Data Objects */ bundleData )
{

    // number of actions plus 3 -- id, name and state
    var columns = 4;
    
    header( columns );

    installForm(  );

    if (bundleData.error)
    {
        error( columns, bundleData.error );
    }
    else
    {
        data ( bundleData.data );
    }

    installForm(  );

    footer( columns );
}

function installForm( )
{
    document.write( "<form method='post' enctype='multipart/form-data'>" );
    document.write( "<tr class='content'>" );
    document.write( "<td class='content'>&nbsp;</td>" );
    document.write( "<td class='content'>" );
    document.write( "<input type='hidden' name='action' value='deploydp' />" );
    document.write( "<input class='input' type='file' name='pckfile' size='50'>" );
    document.write( "</td>" );
    document.write( "<td class='content' align='right' colspan='5' noWrap>" );
    document.write( "<input class='submit' style='width:auto' type='submit' value='Install or Update'>" );
    document.write( "</td>" );
    document.write( "</tr>" );
    document.write( "</form>" );
}
