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
    var message = dataEntry.message;
    var level = dataEntry.level;
    var exception = dataEntry.exception;
    var service = dataEntry.service;

    parent.appendChild( td( null, null, [ text( printDate(dataEntry.received) ) ] ) );
    parent.appendChild( td( null, null, [ text( level ) ] ) );    
    parent.appendChild( td( null, null, [ text( message ) ] ) );
    parent.appendChild( td( null, null, [ text( service ) ] ) );
    parent.appendChild( td( null, null, [ text( exception ) ] ) );
}

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function loadData() {
	$.get(pluginRoot + "/data.json", { "minLevel":$("#minLevel").val()}, function(data) {
	    renderData(data);
	}, "json");	
}

function renderLogs() {
	$(document).ready(function(){
	    renderView( ["Received", "Level", "Message", "Service", "Exception"],
	    		 "<span>Severity at least: <select id='minLevel'><option value='1'>ERROR</option>" + 
	    		 "<option value='2'>WARN</option><option value='3'>INFO</option><option value='4' selected='selected'>DEBUG</option></select></span> "+
	    		 "<button class='reloadButton' type='button' name='reload'>Reload</button>");
	    loadData();
	    
	    $("#plugin_table").tablesorter();
	    $(".reloadButton").click(loadData);
	    $("#minLevel").change(function() {
	    	$.post(pluginRoot, { "minLevel":$("#minLevel").val()}, function(data) {
	    	    renderData(data);
	    	}, "json");
	    });
	});
}