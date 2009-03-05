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
function renderStatusLine() {
	$("#plugin_content").append( "<div class='fullwidth'><div class='statusline'/></div>" );
}

function renderView( /* Array of String */ columns, /* String */ buttons ) {
    renderStatusLine();
    renderButtons(buttons);
    var txt = "<div class='table'><table id='plugin_table' class='tablelayout'><thead><tr>";
    for ( var name in columns ) {
    	txt = txt + "<th class='col_" + columns[name] + "'>" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table></div>";
    $("#plugin_content").append( txt );
    renderButtons(buttons);
    renderStatusLine();	
}

function renderButtons( buttons ) {
	$("#plugin_content").append( "<form method='post' enctype='multipart/form-data'><div class='fullwidth'><div class='buttons'>" +
	                             buttons + "</div></div></form>" );
}

function renderData( eventData )  {
	$(".statusline").empty().append(eventData.status);
	$("#plugin_table > tbody > tr").remove();	
    for ( var idx in eventData.data ) {
        entry( eventData.data[idx] );
    }
    $("#plugin_table").trigger("update");
}

function entry( /* Object */ dataEntry ) {
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry );
	$("#plugin_table > tbody").append(trElement);	
}


function entryInternal( /* Element */ parent, /* Object */ dataEntry ) {
    var id = dataEntry.id;
    var topic = dataEntry.topic;
    var properties = dataEntry.properties;

    parent.appendChild( td( null, null, [ text( printDate(dataEntry.received) ) ] ) );
    parent.appendChild( td( null, null, [ text( topic ) ] ) );

    var propE;
    if ( dataEntry.info ) {
    	propE = text(dataEntry.info);
    } else {
	    var tableE = createElement("table");
	    var bodyE = createElement("tbody");
	    tableE.appendChild(bodyE);
	
	    for( var p in dataEntry.properties ) {
	    	var c1 = td(null, null, [text(p)]);
	    	$(c1).css("border", "0px none");
	    	var c2 = td(null, null, [text(dataEntry.properties[p])]);
	    	$(c2).css("border", "0px none");
	    	bodyE.appendChild(tr(null, null, [ c1, c2 ]));
	    }
	    propE = tableE;
    }
    
    parent.appendChild( td( null, null, [propE] ) );
}

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function loadData() {
	$.get(pluginRoot + "/data.json", null, function(data) {
	    renderData(data);
	}, "json");	
}

function renderEvents() {
	$(document).ready(function(){
	    renderView( ["Received", "Topic", "Properties"],
	    		 "<button class='clearButton' type='button' name='clear'>Clear List</button>" +
	    		 "<button class='reloadButton' type='button' name='reload'>Reload</button>");
	    loadData();
	    
	    $("#plugin_table").tablesorter();
	    $(".reloadButton").click(loadData);
	    $(".clearButton").click(function () {
	    	$("#plugin_table > tbody > tr").remove();
	    	$.post(pluginRoot, { "action":"clear" }, function(data) {
	    	    renderData(data);
	    	}, "json");
	    });
	});
}
