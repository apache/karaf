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
var view = 0;

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
    if ( view == 1 ) {
		$("#timeline").remove();
		$("div.table").append( "<div id='timeline' width='100%'></div>" );
        for ( var idx in eventData.data ) {
            entryTimeline( eventData.data[idx] );
        }
    }
}

function entry( /* Object */ dataEntry ) {
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry );
	$("#plugin_table > tbody").append(trElement);	
}

function entryTimeline( /* Object */ dataEntry ) {
	var txt = "<div class='event" + dataEntry.category + "' style='overflow:visible;white-space:nowrap;width:" + dataEntry.width + "%;'>";
	txt = txt + "<b>" + dataEntry.offset + "</b>&nbsp;<b>" + dataEntry.topic + "</b>";
	if ( dataEntry.info ) {
	    txt = txt + "&nbsp;:&nbsp;" + dataEntry.info;
	}
    txt = txt + "</div>";
	$("#timeline").prepend(txt);	
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
            $(c1).css("padding", "0 4px 0 0");
	    	var c2 = td(null, null, [text(dataEntry.properties[p])]);
	    	$(c2).css("border", "0px none");
            $(c2).css("padding", "0 0 0 4px");
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

function switchView() {
	if ( view == 0 ) {
		view = 1;
		$("#plugin_table").hide();
		$(".switchButton").empty();
		$(".switchButton").append("List");
		loadData();
	} else {
		view = 0;
		$("#timeline").remove();
		$("#plugin_table").show();
		$(".switchButton").empty();
		$(".switchButton").append("Timeline");
	}
}
function renderEvents() {
	$(document).ready(function(){
	    renderView( ["Received", "Topic", "Properties"],
	    		 "<button class='switchButton' type='button' name='switch'>Timeline</button>" +
	    		 "<button class='clearButton' type='button' name='clear'>Clear List</button>" +
	    		 "<button class='reloadButton' type='button' name='reload'>Reload</button>");
	    loadData();
	    
	    $("#plugin_table").tablesorter();
	    $(".reloadButton").click(loadData);
	    $(".switchButton").click(switchView);
	    $(".clearButton").click(function () {
	    	$("#plugin_table > tbody > tr").remove();
	    	$.post(pluginRoot, { "action":"clear" }, function(data) {
	    	    renderData(data);
	    	}, "json");
	    });
	});
}
