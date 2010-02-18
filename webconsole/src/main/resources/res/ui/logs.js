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
 
var logsElem  = false;
var logs2Elem = false;
var tableElem = false;
var statElem  = false;
 
function renderData( eventData )  {
	statElem.empty().append(eventData.status ? i18n.status_ok : i18n.status_missing);
	logsElem.css("display", eventData.status ? "block" : "none"  );
	if (eventData.status) {
		$("#plugin_table > tbody > tr").remove();	
		var hasEntries = false;
		for ( var idx in eventData.data ) {
			entry( eventData.data[idx] );
			hasEntries = true;
		}
		logs2Elem.css("display", hasEntries ? "block" : "none"  );
	
		if (hasEntries) tableElem.trigger("update").trigger("applyWidgets");
	}
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
	switch (dataEntry.raw_level) { // i18n
		case 1: level = i18n.error; break;
		case 2: level = i18n.warn; break;
		case 3: level = i18n.info; break;
		case 4: level = i18n.debug; break;
	}
    parent.appendChild( td( null, null, [ text( printDate(dataEntry.received) ) ] ) );
    parent.appendChild( td( null, null, [ text( level ) ] ) );    
    parent.appendChild( td( null, null, [ text( wordWrap(message) ) ] ) );
    parent.appendChild( td( null, null, [ text( wordWrap(service) ) ] ) );
    parent.appendChild( td( null, null, [ text( exception ) ] ) );
}

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function loadData() {
	$.get(pluginRoot + "/data.json", { "minLevel":$(".minLevel").val()}, renderData, "json");
	return false; // for button
}

$(document).ready(function() {
	// install user interaction handlers
    $(".reloadButton").click(loadData);
    $(".minLevel").change(function() {
		var value = $(this).val();
		$(".minLevel").val(value); // same values for both select boxes
    	$.post(pluginRoot, {"minLevel":value}, function(data) {
    	    renderData(data);
    	}, "json");
    });
	logsElem  = $("#logs");
    logs2Elem = $("#logs2");
	tableElem = $("#plugin_table");
	statElem  = $(".statline");
	// load logs
	loadData();
});