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
			
			$("#plugin_table > tbody > tr").remove();
			for ( var idx in eventData.data ) {
				entry( eventData.data[idx] );
			}
			$("#plugin_table").trigger("update");
			if ( drawDetails ) renderDetails(eventData);
			initStaticWidgets();
	}
}

function entry( /* Object */ dataEntry ) {
	var trElement = tr( null, { id: "entry" + dataEntry.id } );
	entryInternal( trElement,  dataEntry );
	$("#plugin_table > tbody").append(trElement);	
}

function actionButton( /* Element */ parent, /* string */ id, /* Obj */ action, /* string */ pid ) {
	var enabled = action.enabled;
	var op = action.link;
	var opLabel = action.name;
	var img = action.image;
	// fixup JQuery UI icons
	if (img == 'configure')	  { img = 'wrench'
	} else if (img == 'disable') { img = 'stop' //locked
	} else if (img == 'enable')  { img = 'play' //unlocked
	}
	
	// apply i18n
	opLabel = i18n[opLabel] ? i18n[opLabel] : opLabel;
	
	var arg = id;
	if ( op == "configure" ) {
		arg = pid
	}
	var input = createElement('li', 'dynhover', {
		title: opLabel
	});
	$(input)
		.html('<span class="ui-icon ui-icon-'+img+'"></span>')
		.click(function() {changeDataEntryState(arg, op)});

	if (!enabled) {
		$(input).attr('disabled', true).addClass('ui-state-disabled');
		
	}
	parent.appendChild( input );
}

function entryInternal( /* Element */ parent, /* Object */ dataEntry ) {
	var id = dataEntry.id;
	var name = dataEntry.name;
	var state = dataEntry.state;

	var inputElement = createElement('span', 'ui-icon ui-icon-triangle-1-e', {
		title: "Details",
		id: 'img' + id,
		style: {display: "inline-block"}
	});
	$(inputElement).click(function() {showDetails(id)});
	var titleElement;
	if ( drawDetails ) {
		titleElement = text(name);
	} else {
		titleElement = createElement ("a", null, {
			href: window.location.pathname + "/" + id
		});
		titleElement.appendChild(text(name));
	}

	parent.appendChild( td( null, null, [ text( id ) ] ) );
	parent.appendChild( td( null, null, [ inputElement, text(" "), titleElement ] ) );
	parent.appendChild( td( null, null, [ text( state ) ] ) );
	var actionsTd = td( null, null );
	var div = createElement('ul', 'icons ui-widget');
	actionsTd.appendChild(div);

	for ( var a in dataEntry.actions ) {
		actionButton( div, id, dataEntry.actions[a], dataEntry.pid );
	}
	parent.appendChild( actionsTd );
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


$(document).ready(function(){
	renderData (scrData);

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

