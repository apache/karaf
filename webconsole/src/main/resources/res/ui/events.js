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
	document.write( "<div class='fullwidth tablelayout'>");
    renderButtons();
	document.write( "<div class='table'>");
    document.write( "<table id='events' class='tablelayout'>" );

    document.write( "<thead><tr>" );
    document.write( "<th>Received</th>" );
    document.write( "<th>Topic</th>" );
    document.write( "<th>Properties</th>" );
    document.write( "</tr></thead><tbody>" );

}

function renderData( eventData ) 
{
	$(".statusline").empty().append(eventData.status);
    data ( eventData.data );
}

function data( /* Array of Object */ dataArray )
{
	$("#events > tbody > tr").remove();	
    for ( var idx in dataArray )
    {
        entry( dataArray[idx] );
    }
}


function footer( /* int */ columns )
{
    document.write( "</tbody></table>" );
    document.write( "</div>");
    renderButtons();
    document.write( "</div>");
}


function entry( /* Object */ dataEntry )
{
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry );
	$("#events > tbody").append(trElement);	
}


function entryInternal( /* Element */ parent, /* Object */ dataEntry )
{

    var id = dataEntry.id;
    var topic = dataEntry.topic;
    var properties = dataEntry.properties;

    parent.appendChild( td( null, null, [ text( new Date(dataEntry.received) ) ] ) );
    parent.appendChild( td( null, null, [ text( topic ) ] ) );

    var tableE = createElement("table");
    var bodyE = createElement("tbody");
    tableE.appendChild(bodyE);

    for( var p in dataEntry.properties ) {
    	bodyE.appendChild(tr(null, null, [td(null, null, [text(p)] ),
    	                                  td(null, null, [text(dataEntry.properties[p])])]));
    }
    
    parent.appendChild( td( null, null, [tableE] ) );
}

function renderStatusLine() {
	document.write( "<div class='fullwidth'>");
    document.write( "<div class='statusline'>" );
    document.write( "</div>" );
    document.write( "</div>" );
}

function renderButtons( )
{
	document.write( "<div class='fullwidth'>");
    document.write( "<div class='buttons'>" );
    document.write( "<div class='button'><button id='reloadButton' type='button' name='reload'>Reload</button></div>" );
    document.write( "<div class='button'><button id='clearButton' type='button' name='clear'>Clear List</button></div>" );
    document.write( "</div>" );
    document.write( "</div>" );
}

function loadData() 
{
	$.get(pluginRoot + "/data.json", null, function(data) {
	    renderData(data);
	}, "json");	
}

function renderEvents( )
{

    // date, topic and properties
    var columns = 3;
    
    renderStatusLine();

    header( columns );

    footer( columns );

    renderStatusLine();

    loadData();
    
    $("#events").tablesorter();
    $("#reloadButton").click(loadData);
    $("#clearButton").click(function () {
    	$("#events > tbody > tr").remove();
    	$.post(pluginRoot, { "action":"clear" }, function(data) {
    	    renderData(data);
    	}, "json");
    });
}
