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

function renderView( /* Array of String */ columns, /* Array of String */ buttons ) {
    renderStatusLine();
    renderButtons(buttons);
    var txt = "<div class='table'><table id='bundles' class='tablelayout'><thead><tr>";
    for ( var name in columns ) {
    	txt = txt + "<th class='col_" + columns[name] + "'>" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table></div>";
    $("#plugin_content").append( txt );
    renderButtons(buttons);
    renderStatusLine();	
}

function renderButtons( buttons ) {
	$("#plugin_content").append( "<div class='fullwidth'><div class='buttons'>" +
	                             "<form method='post' enctype='multipart/form-data'><div style='padding-top:5px;'>" +
	                             buttons + "</div></form></div></div>" );
}

function renderData( eventData )  {
	$(".statusline").empty().append(eventData.status);
	$("#bundles > tbody > tr").remove();
    for ( var idx in eventData.data ) {
        entry( eventData.data[idx] );
    }
    $("#bundles").trigger("update");
    if ( drawDetails ) {
	    renderDetails(eventData);
    }
}

function entry( /* Object */ dataEntry ) {
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry );
	$("#bundles > tbody").append(trElement);	
}

function actionButton( /* Element */ parent, /* string */ id, /* Obj */ action ) {
	var enabled = action.enabled;
	var op = action.link;
	var opLabel = action.name;
	var img = action.image;
	
	var input = createElement( "input", null, {
            type: 'image',
            title: opLabel,
            alt: opLabel,
            src: imgRoot + '/bundle_' + img + '.png',
            onClick: 'changeDataEntryState(' + id + ', "' + op + '");'
        });
		
    if (!enabled) {
        input.setAttribute( "disabled", true );
    }
    var div = createElement("div");
    div.setAttribute("style", "float:left; margin-left:10px;");
    div.appendChild(input);
    parent.appendChild( div );
}

function entryInternal( /* Element */ parent, /* Object */ dataEntry ) {
    var id = dataEntry.id;
    var name = dataEntry.name;
    var state = dataEntry.state;
    
    var inputElement = createElement("img", "rightButton", {
    	src: appRoot + "/res/imgs/arrow_right.png",
    	border: "none",
    	id: 'img' + id,
    	title: "Back",
    	alt: "Back",
    	width: 14,
    	height: 14,
        onClick: 'showDetails(' + id + ');'
    });
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
    
    for ( var a in dataEntry.actions ) {
    	actionButton( actionsTd, id, dataEntry.actions[a] );
    }
    parent.appendChild( actionsTd );
}

function loadData() {
	$.get(pluginRoot + "/.json", null, function(data) {
	    renderData(data);
	}, "json");	
}

function changeDataEntryState(/* long */ id, /* String */ action) {
	$.post(pluginRoot + "/" + id, {"action":action}, function(data) {
	    renderData(data);
	}, "json");	
}

function refreshPackages() {
	$.post(window.location.pathname, {"action": "refreshPackages"}, function(data) {
	    renderData(data);
	}, "json");	
}

function showDetails( id ) {
    $.get(pluginRoot + "/" + id + ".json", null, function(data) {
    	renderDetails(data);
    }, "json");
}

function hideDetails( id ) {
	$("#img" + id).each(function() {
		$("#bundleInlineDetails").remove();
        this.setAttribute("src", appRoot + "/res/imgs/arrow_right.png");
        this.setAttribute("onClick", "showDetails('" + id + "')");
        this.setAttribute("title", "Details");
        this.setAttribute("alt", "Details");
	});
}

function renderDetails( data ) {
	data = data.data[0];
	$("#entry" + data.id + " > td").eq(1).append("<div id='bundleInlineDetails'/>");
	$("#img" + data.id).each(function() {
		if ( drawDetails ) {
            this.setAttribute("src", appRoot + "/res/imgs/arrow_left.png");
    	    var ref = window.location.pathname;
    	    ref = ref.substring(0, ref.lastIndexOf('/'));
            this.setAttribute("onClick", "window.location = '" + ref + "';");
            this.setAttribute("title", "Back");
            this.setAttribute("alt", "Back");
		} else {
            this.setAttribute("src", appRoot + "/res/imgs/arrow_down.png");
            this.setAttribute("onClick", "hideDetails('" + data.id + "')");
            this.setAttribute("title", "Hide Details");
            this.setAttribute("alt", "Hide Details");
		}
	});
	$("#bundleInlineDetails").append("<table border='0'><tbody></tbody></table>");
    var details = data.props;
    for (var idx in details) {
        var prop = details[idx];
        
        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + prop.key + "</td><td class='aligntop' style='border:0px none'>";	        
        if (prop.value) {
        	if ( prop.key == 'Bundle Documentation' )  {
        		txt = txt + "<a href='" + prop.value + "' target='_blank'>" + prop.value + "</a>";
        	} else  {
        		if ( $.isArray(prop.value) ) {
	        		var i = 0;
	        		for(var pi in prop.value) {
	        			var value = prop.value[pi];
		                if (i > 0) { txt = txt + "<br/>"; }
		                var span;
		                if (value.substring(0, 2) == "!!") {
		                	txt = txt + "<span style='{color: \"red\"}'>" + value + "</span>";
		                } else {
		                	txt = txt + value;
		                }
		                i++;
	        		}
        		} else {
        			txt = txt + prop.value;
        		}
        	}
        } else {
        	txt = txt + "\u00a0";
        }
        txt = txt + "</td></tr>";
        $("#bundleInlineDetails > table > tbody").append(txt);
	}
}

function renderBundles(data) {
	$(document).ready(function(){
    	renderView( ["Id", "Name", "Status", "Actions"],
        		"<input type='hidden' name='action' value='install' style='margin-left:20px;'/>" +
                "<input class='input' type='file' name='bundlefile' size='50' style='margin-left:10px;'/>" +
         		" - Start <input class='checkradio' type='checkbox' name='bundlestart' value='start' style='vertical-align:middle;'/>" +
        		" - Start Level <input class='input' type='input' name='bundlestartlevel' value='" + startLevel + "' size='4'/>" +
         		"<input type='submit' value='Install or Update' style='margin-left:60px'/>" +
        		"<button id='refreshPackages' type='button' name='refresh' style='margin-left:10px'>Refresh Packages</button>"
        		 );
        $("#refreshPackages").click(refreshPackages);
        renderData(data);
        
        var extractMethod = function(node) {
        	var link = node.getElementsByTagName("a");
            if ( link && link.length == 1 ) {
            	return link[0].innerHTML;
            }
            return node.innerHTML;
        };
        $("#bundles").tablesorter({
            headers: {
        	    0: { sorter:"digit"},
                3: { sorter: false }
            },
            sortList: [[1,0]],
            textExtraction:extractMethod 
        });
    });
}
