/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
function renderData( eventData )  {
	switch(eventData.status) {
		case -1: // no event admin
			$(".statline").html(i18n.stat_no_service);
			$("#scr").addClass('ui-helper-hidden');
			break;
		case  0: // no components
			$(".statline").html(i18n.stat_no_components);
			$('#scr').addClass('ui-helper-hidden');
			break;
		default:
			$(".statline").html(i18n.stat_ok.msgFormat(eventData.status));
			$('#scr').removeClass('ui-helper-hidden');
			
			tableBody.empty();
			for ( var idx in eventData.data ) {
				entry( eventData.data[idx] );
			}
			$("#plugin_table").trigger("update");
			if ( drawDetails ) renderDetails(eventData);
			initStaticWidgets();
	}
}

function entry( /* Object */ dataEntry ) {
	var id = dataEntry.id;
	var name = dataEntry.name;

	var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'entry' + dataEntry.id);

	_.find('.bIcon').attr('id', 'img' + id).click(function() {
		showDetails(id);
	}).after(drawDetails ? name : ('<a href="' + window.location.pathname + '/' + id + '">' + name + '</a>'));

	_.find('td:eq(0)').text( id );
	_.find('td:eq(2)').text( dataEntry.state );

	// setup buttons
	if ( dataEntry.stateRaw == 1 || dataEntry.stateRaw == 1024 ) { // disabled or disabling
		_.find('li:eq(0)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(id, 'enable') });
	} else {
		_.find('li:eq(1)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(id, 'disable') });
	}
	if ( dataEntry.configurable ) _.find('li:eq(2)').removeClass('ui-helper-hidden').click(function() { // configure
		changeDataEntryState(dataEntry.pid, 'configure');
	});	
}

function changeDataEntryState(/* long */ id, /* String */ action) {
	if ( action == "configure") {
		window.location = appRoot + "/configMgr/" + id;
		return;
	}
	$.post(pluginRoot + "/" + id, {"action":action}, function(data) {
		renderData(data);
	}, "json");	
}

function showDetails( id ) {
	$.get(pluginRoot + "/" + id + ".json", null, function(data) {
		renderDetails(data);
	}, "json");
}

function loadData() {
	$.get(pluginRoot + "/.json", null, function(data) {
		renderData(data);
	}, "json");	
}

function hideDetails( id ) {
	$("#img" + id).each(function() {
		$("#pluginInlineDetails").remove();
		$(this).
			removeClass('ui-icon-triangle-1-w').//left
			removeClass('ui-icon-triangle-1-s').//down
			addClass('ui-icon-triangle-1-e').//right
		    attr("title", "Details").
			unbind('click').click(function() {showDetails(id)});
	});
}

function renderDetails( data ) {
	data = data.data[0];
	$("#pluginInlineDetails").remove();
	$("#entry" + data.id + " > td").eq(1).append("<div id='pluginInlineDetails'/>");
	$("#img" + data.id).each(function() {
		if ( drawDetails ) {
			var ref = window.location.pathname;
			ref = ref.substring(0, ref.lastIndexOf('/'));
			$(this).
				removeClass('ui-icon-triangle-1-e').//right
				removeClass('ui-icon-triangle-1-s').//down
				addClass('ui-icon-triangle-1-w').//left
				attr("title", "Back").
				unbind('click').click(function() {window.location = ref});
		} else {
			$(this).
				removeClass('ui-icon-triangle-1-w').//left
				removeClass('ui-icon-triangle-1-e').//right
				addClass('ui-icon-triangle-1-s').//down
				attr("title", "Hide Details").
				unbind('click').click(function() {hideDetails(data.id)});
		}
	});
	$("#pluginInlineDetails").append("<table border='0'><tbody></tbody></table>");
	var details = data.props;
	for (var idx in details) {
		var prop = details[idx];
		var key = i18n[prop.key] ? i18n[prop.key] : prop.key; // i18n

		var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + key + "</td><td class='aligntop' style='border:0px none'>";	
		if (prop.value) {
			if ( $.isArray(prop.value) ) {
				var i = 0;
				for(var pi in prop.value) {
					var value = prop.value[pi];
					if (i > 0) { txt = txt + "<br/>"; }
					var span;
					if (value.substring(0, 2) == "!!") {
						txt = txt + "<span style='color: red;'>" + value + "</span>";
					} else {
						txt = txt + value;
					}
					i++;
				}
			} else {
				txt = txt + prop.value;
			}
		} else {
			txt = txt + "\u00a0";
		}
		txt = txt + "</td></tr>";
		$("#pluginInlineDetails > table > tbody").append(txt);
	}
}

var tableBody = false;
var tableEntryTemplate = false;

$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
	tableEntryTemplate = tableBody.find('tr').clone();

	renderData(scrData);

	$(".reloadButton").click(loadData);

	var extractMethod = function(node) {
		var link = node.getElementsByTagName("a");
		if ( link && link.length == 1 ) {
			return link[0].innerHTML;
		}
		return node.innerHTML;
	};
	$("#plugin_table").tablesorter({
		headers: {
			0: { sorter:"digit"},
			3: { sorter: false }
		},
		sortList: [[1,0]],
		textExtraction:extractMethod
	});
});

