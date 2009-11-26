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
	$("#plugin_content").append(
			"<div class='fullwidth'><div class='statusline'/></div>");
}

function renderView( /* Array of String */columns) {
	renderStatusLine();
	var txt = "<div class='table'><table id='plugin_table' class='tablelayout'><thead><tr>";
	for ( var name in columns) {
		txt = txt + "<th class='col_" + columns[name] + "'>" + columns[name]
				+ "</th>";
	}
	txt = txt + "</tr></thead><tbody></tbody></table></div>";
	$("#plugin_content").append(txt);
	renderStatusLine();
}

function renderData(eventData) {
	$(".statusline").empty().append(eventData.status);
	$("#plugin_table > tbody > tr").remove();
	for ( var idx in eventData.data) {
		entry(eventData.data[idx]);
	}
	$("#plugin_table").trigger("update");
	if (drawDetails) {
		renderDetails(eventData);
	}
}

function entry( /* Object */dataEntry) {
	var trElement = tr(null, {
		id : "entry" + dataEntry.id
	});
	entryInternal(trElement, dataEntry);
	$("#plugin_table > tbody").append(trElement);
}

function entryInternal( /* Element */parent, /* Object */dataEntry) {
	var id = dataEntry.id;
	var name = dataEntry.id;

	var inputElement = createElement("img", "rightButton", {
		src : appRoot + "/res/imgs/arrow_right.png",
		style : {
			border : "none"
		},
		id : 'img' + id,
		title : "Details",
		alt : "Details",
		width : 14,
		height : 14
	});
	$(inputElement).click(function() {
		showDetails(id)
	});
	var titleElement;
	if (drawDetails) {
		titleElement = text(name);
	} else {
		titleElement = createElement("a", null, {
			href : window.location.pathname + "/" + id
		});
		titleElement.appendChild(text(name));
	}
	var bundleElement = createElement("a", null, {
		href : bundlePath + dataEntry.bundleId
	});
	bundleElement.appendChild(text(dataEntry.bundleSymbolicName + " ("
			+ dataEntry.bundleId + ")"));

	parent
			.appendChild(td(null, null,
					[ inputElement, text(" "), titleElement ]));
	parent.appendChild(td(null, null, [ text(dataEntry.types) ]));
	parent.appendChild(td(null, null, [ bundleElement ]));
}

function showDetails(id) {
	$.get(pluginRoot + "/" + id + ".json", null, function(data) {
		renderDetails(data);
	}, "json");
}

function hideDetails(id) {
	$("#img" + id).each(function() {
		$("#pluginInlineDetails").remove();
		$(this).attr("src", appRoot + "/res/imgs/arrow_right.png");
		$(this).attr("title", "Details");
		$(this).attr("alt", "Details");
		$(this).unbind('click').click(function() {
			showDetails(id)
		});
	});
}

function renderDetails(data) {
	data = data.data[0];
	$("#pluginInlineDetails").remove();
	$("#entry" + data.id + " > td").eq(1).append(
			"<div id='pluginInlineDetails'/>");
	$("#img" + data.id).each(function() {
		if (drawDetails) {
			$(this).attr("src", appRoot + "/res/imgs/arrow_left.png");
			$(this).attr("title", "Back");
			$(this).attr("alt", "Back");
			var ref = window.location.pathname;
			ref = ref.substring(0, ref.lastIndexOf('/'));
			$(this).unbind('click').click(function() {
				window.location = ref;
			});
		} else {
			$(this).attr("src", appRoot + "/res/imgs/arrow_down.png");
			$(this).attr("title", "Hide Details");
			$(this).attr("alt", "Hide Details");
			$(this).unbind('click').click(function() {
				hideDetails(data.id)
			});
		}
	});
	$("#pluginInlineDetails").append(
			"<table border='0'><tbody></tbody></table>");
	var details = data.props;
	for ( var idx in details) {
		var prop = details[idx];

		var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>"
				+ prop.key
				+ "</td><td class='aligntop' style='border:0px none'>";
		if (prop.value) {
			if ($.isArray(prop.value)) {
				var i = 0;
				for ( var pi in prop.value) {
					var value = prop.value[pi];
					if (i > 0) {
						txt = txt + "<br/>";
					}
					var span;
					if (value.substring(0, 6) == "INFO: ") {
						txt = txt + "<span style='color: grey;'>!!"
								+ value.substring(5) + "</span>";
					} else if (value.substring(0, 7) == "ERROR: ") {
						txt = txt + "<span style='color: red;'>!!"
								+ value.substring(6) + "</span>";
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

function renderServices(data) {
	$(document).ready(function() {
		renderView( [ "Id", "Type(s)", "Bundle" ]);
		renderData(data);

		var extractMethod = function(node) {
			var link = node.getElementsByTagName("a");
			if (link && link.length == 1) {
				return link[0].innerHTML;
			}
			return node.innerHTML;
		};
		$("#plugin_table").tablesorter( {
			headers : {
				0 : {
					sorter : "digit"
				}
			},
			sortList : [ [ 1, 0 ] ],
			textExtraction : extractMethod
		});
	});
}