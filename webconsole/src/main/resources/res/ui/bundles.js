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

function renderData( eventData )  {
	var s = eventData.s;
    $(".statline").html(i18n.statline.msgFormat(s[0], s[1], s[2], s[3], s[4]));
    $("#plugin_table > tbody > tr").remove();
    for ( var idx in eventData.data ) {
        if ( currentBundle == null || !drawDetails || currentBundle == eventData.data[idx].id) {
            entry( eventData.data[idx] );
        }
    }
    if ( drawDetails && eventData.data.length == 1 ) {
        renderDetails(eventData.data[0]);    
    } else if ( currentBundle != null ) {
        var id = currentBundle;
        hideDetails(id);
        showDetails(id);
    }
    initStaticWidgets();
}

function entry( /* Object */ dataEntry ) {
    var trElement = tr( null, { id: "entry" + dataEntry.id } );
    entryInternal( trElement,  dataEntry );
    $("#plugin_table > tbody").append(trElement);   
}

function actionButton( /* Element */ parent, /* string */ id, /* Obj */ action ) {
    if ( !action.enabled ) {
        return;
    }
    var enabled = action.enabled;
    var op = action.link;
    var opLabel = action.name;
    var img = action.image;
    // fixup JQuery UI icons
    if(img == "start" ) img = "play";
    if(img == "update") img = "transferthick-e-w";
    if(img == "delete") img = "trash";

	// apply i18n
	opLabel = i18n[opLabel] ? i18n[opLabel] : opLabel;
    
    var input = createElement('li', 'dynhover', {
        title: opLabel
    });
    $(input)
        .html('<span class="ui-icon ui-icon-'+img+'"></span>')
        .click(function() {changeDataEntryState(id, op)});

    if (!enabled) {
        $(input).attr("disabled", true).addClass("ui-state-disabled");
    }
    parent.appendChild( input );
}

function entryInternal( /* Element */ parent, /* Object */ dataEntry ) {
    var id = dataEntry.id;
    var name = dataEntry.name;
    var state = dataEntry.state;

    // right arrow
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
    parent.appendChild( td( null, null, [ text( dataEntry.version ) ] ) );
    parent.appendChild( td( null, null, [ text( dataEntry.symbolicName ) ] ) );
    parent.appendChild( td( null, null, [ text( state ) ] ) );
    var actionsTd = td( null, null );
    var div = createElement('ul', 'icons ui-widget');
    actionsTd.appendChild(div);
    
    for ( var a in dataEntry.actions ) {
        actionButton( div, id, dataEntry.actions[a] );
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
    currentBundle = id;
    $.get(pluginRoot + "/" + id + ".json", null, function(data) {
        renderDetails(data.data[0]);
    }, "json");
}

function hideDetails( id ) {
    currentBundle = null;
    $("#img" + id).each(function() {
        $("#pluginInlineDetails" + id).remove();
        $(this).
            removeClass('ui-icon-triangle-1-w').//left
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e').//right
            attr("title", "Details").
            unbind('click').click(function() {showDetails(id)});
    });
}

function renderDetails( data ) {
    $("#entry" + data.id + " > td").eq(1).append("<div id='pluginInlineDetails"  + data.id + "'/>");
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
    $("#pluginInlineDetails" + data.id).append("<table border='0'><tbody></tbody></table>");
    var details = data.props;
    for (var idx in details) {
        var prop = details[idx];
		var key = i18n[prop.key] ? i18n[prop.key] : prop.key;

        var txt = "<tr><td class='aligntop' noWrap='true' style='border:0px none'>" + key + "</td><td class='aligntop' style='border:0px none'>";          
        if (prop.value) {
            if ( prop.key == 'Bundle Documentation' )  {
                txt = txt + "<a href='" + prop.value + "' target='_blank'>" + prop.value + "</a>";
            } else  {
                if ( $.isArray(prop.value) ) {
                    var i = 0;
                    for(var pi in prop.value) {
                        var value = prop.value[pi];
                        if (i > 0) { txt = txt + "<br/>"; }
                        txt = txt + value;
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
        $("#pluginInlineDetails" + data.id + " > table > tbody").append(txt);
    }
}

$(document).ready(function(){
	$(".refreshPackages").click(refreshPackages);
	$(".reloadButton").click(loadData);
	$(".installButton").click(function() {
		document.location = pluginRoot + "/upload";
	});
	renderData(__bundles__);

	// check for cookie
	var cv = $.cookies.get("webconsolebundlelist");
	var lo = (cv ? cv.split(",") : [1,0]);
	$("#plugin_table").tablesorter({
		headers: {
			0: { sorter:"digit" },
			5: { sorter: false }
		},
		textExtraction:mixedLinksExtraction,
		sortList: cv ? [lo] : false
	}).bind("sortEnd", function() {
		var table = $("#plugin_table").eq(0).attr("config");
		$.cookies.set("webconsolebundlelist", table.sortList.toString());
	});
});

