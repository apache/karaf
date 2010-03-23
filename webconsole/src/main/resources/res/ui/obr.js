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
 
var repoTable = false;
var repoTableTemplate = false;
var addRepoUri = false;
var resTable = false;
var searchField = false;
var ifStatusOK = false;

//This prototype is provided by the Mozilla foundation and
//is distributed under the MIT license.
//http://www.ibiblio.org/pub/Linux/LICENSES/mit.license
if (!Array.prototype.map)
{
  Array.prototype.map = function(fun /*, thisp*/)
  {
    var len = this.length;
    if (typeof fun != "function")
      throw new TypeError();

    var res = new Array(len);
    var thisp = arguments[1];
    for (var i = 0; i < len; i++)
    {
      if (i in this)
        res[i] = fun.call(thisp, this[i], i, this);
    }

    return res;
  };
}

var uid = 0;
function guid() {
   uid = uid + 1;
   return (0x10000 + uid).toString(16).substring(1) + (((1+Math.random())*0x10000)|0).toString(16).substring(1);
}

/* displays a date in the user's local timezone */
function localTm(time) {
	return (time ? new Date(time) : new Date()).toLocaleString();
}

function doRepoAction(action, url) {
	if ( !url ) {
		Xalert('Invalid URI: ' + url, 'Error');
	} else {
		$.post(pluginRoot, {
			'action' : action,
			'url'    : url
		}, renderData, 'json');
	}
}

function showDetails( symbolicname, version ) {
    window.location.href = pluginRoot + '/' + symbolicname + '/' + version;
}

function showVersions( symbolicname ) {
	var _id = symbolicname.replace(/\./g, '_');
    $("#block" + _id).append("<div id='pluginInlineVersions" + _id + "' style='margin-left: 4em'><ul/></div>");
    $("#img" + _id).each(function() {
        $(this).
            removeClass('ui-icon-triangle-1-e').//right
            addClass('ui-icon-triangle-1-s');//down
    });
    $("#entry" + _id).each(function() {
        $(this).
            unbind('click').
            click(function() {hideVersions(symbolicname)}).
            attr("title", "Hide Versions");
    });
    var versions = [];
    for (var i in obrData.resources ) {
        if (obrData.resources[i].symbolicname == symbolicname) {
            versions.push(obrData.resources[i].version);
        }
    }
    versions.sort();
    for (var i in versions) {
        var txt = "<li><a href='javascript: showDetails(\"" + symbolicname + "\",\"" + versions[i] + "\")'>" + versions[i] + "</a></li>";
        $("#pluginInlineVersions" + _id + " > ul").append(txt);
    }
}

function hideVersions( symbolicname ) {
	var _id = symbolicname.replace(/\./g, '_');
    $("#img" + _id).each(function() {
        $(this).
            removeClass('ui-icon-triangle-1-s').//down
            addClass('ui-icon-triangle-1-e');//right
    });
    $("#pluginInlineVersions" + _id).each(function() {
        $(this).
            remove();
    });
    $("#entry" + _id).each(function() {
        $(this).
            unbind('click').
            click(function() {showVersions(symbolicname)}).
            attr("title", "Show Versions");
    });
}

function renderResource(res) {
	// proceed with resource
	var _id = res.symbolicname.replace(/\./g, '_');
	var _tr = resTable.find('#row' + _id);

	if (_tr.length == 0) { // not created yet, create it
	    var blockElement = createElement('span', '', {
            id: 'block' + _id
	    });
        var titleElement = createElement('span', '', {
            id: 'entry' + _id,
            title: "Show Versions"
        });
        var inputElement = createElement('span', 'ui-icon ui-icon-triangle-1-e', {
            id: 'img' + _id,
            style: {display: "inline-block"}
        });
        blockElement.appendChild(titleElement);
        titleElement.appendChild(inputElement);
        titleElement.appendChild(text(" "));
        titleElement.appendChild(text(res.presentationname ? res.presentationname : res.symbolicname));
        $(titleElement).click(function() {showVersions(res.symbolicname)});

		_tr = tr( null, { 'id' : 'row' + _id } , [
			td( null, null, [ blockElement ] ),
			td( null, null, [ text(res.installed ? res.version : '') ] )
		]);
		resTable.append( _tr );
	}
}

function getCapabilitiesByName(res, name) {
    var caps = [];
    for (var v in res.capabilities) {
        if (res.capabilities[v].name == name) {
            caps.push(res.capabilities[v]);
        }
    }
    return caps;
}

function getRequirementsByName(res, name) {
    var caps = [];
    for (var v in res.requirements) {
        if (res.requirements[v].name == name) {
            caps.push(res.requirements[v]);
        }
    }
    return caps;
}

function createDetailedTable(enclosing, name, headers, rows, callback) {
    if (rows && rows.length > 0) {
        var uuid = guid();
        var title = createElement('span', null, null, [
                                createElement('span', 'ui-icon ui-icon-triangle-1-e', { id: "img"+uuid, style: {display: "inline-block"} }),
                                text(" "),
                                text(name)
                             ]);
        enclosing.append(tr(null, null, [
            td(null, null, [ title ]),
            td(null, null, [ createElement('table', 'nicetable ui-widget ui-helper-hidden', { id: "alt1"+uuid }, [
                                createElement('thead', null, null, [
                                    tr(null, null, headers.map(function(x) {
                                        return th('ui-widget-header', null, [text(x)]);
                                    }))
                                ]),
                                createElement('tbody', null, null,
                                    rows.map(function(x) {
                                        var values = callback(x);
                                        var tds = values.map(function(x) {
                                            return td(null, null, [x]);
                                        });
                                        return tr(null, null, tds);
                                    })
                                )
                             ]),
                             createElement('span', null, { id: "alt2"+uuid }, [
                                text(rows.length)
                             ])
            ])
        ]));
        $(title).
                unbind('click').
                click(function(event) {
                    event.preventDefault();
                    $("#img"+uuid).toggleClass('ui-icon-triangle-1-s').//down
                                   toggleClass('ui-icon-triangle-1-e');//right
                    $("#alt1"+uuid).toggle();
                    $("#alt2"+uuid).toggle();
                });
    }
}

function trim(stringToTrim) {
	return stringToTrim.replace(/^\s+|\s+$/g,"");
}

function parseSimpleFilter(filter) {
    filter = filter.substring(1, filter.length-1);
    var start = 0;
    var pos = 0;
    var c = filter.charAt(pos);
    while (c != '~' && c != '<' && c != '>' && c != '=' && c != '(' && c != ')') {
        if (c == '<' && filterChars[pos+1] == '*') {
            break;
        }
        if (c == '*' && filterChars[pos+1] == '>') {
            break;
        }
        pos++;
        c = filter.charAt(pos);
    }
    if (pos == start) {
        throw ("Missing attr: " + filter.substring(pos));
    }

    var attr = trim(filter.substring(start, pos));
    var oper = filter.substring(pos, pos+2);
    var value;
    if (oper == '*>' || oper == '~=' || oper == '>=' || oper == '<=' || oper == '<*') {
        value = trim(filter.substring(pos+2));
        if (value == '') {
            throw ("Missing value: " + filter.substring(pos));
        }

        return { operator: oper, operands: [ attr, value ]};
    } else {
        if (c != '=') {
            throw ("Invalid operator: " + filter.substring(pos));
        }
        oper = '=';
        value = filter.substring(pos+1);
        if (value == '*' ) {
            return { operator: '=*', operands: [ attr ]};
        }
        return { operator: '=', operands: [ attr, value ]};
    }
}

function parseFilter(filter) {
    if (filter.charAt(0) != "(" || filter.charAt(filter.length-1) != ")") {
        throw "Wrong parenthesis: " + filter;
    }
    if (filter.charAt(1) == "!") {
        return { operator: filter.charAt(1), operands: [ parseFilter(filter.substring(2, filter.length-1)) ] };
    }
    if (filter.charAt(1) == "|" || filter.charAt(1) == "&") {
        var inner = filter.substring(2, filter.length-1);
        var flts = inner.match(/\([^\(\)]*(\([^\(\)]*(\([^\(\)]*(\([^\(\)]*\))*[^\(\)]*\))*[^\(\)]*\))*[^\(\)]*\)/g);
        return { operator: filter.charAt(1), operands: flts.map(function(x) { return parseFilter(x); }) };
    }
    return parseSimpleFilter(filter);
}

function simplify(filter) {
    if (filter.operator == '&' || filter.operator == '|') {
        filter.operands = filter.operands.map(function(x) { return simplify(x); });
    } else if (filter.operator == '!') {
        if (filter.operands[0].operator == '<=') {
            filter.operator = '>';
            filter.operands = filter.operands[0].operands;
        } else if (filter.operands[0].operator == '>=') {
            filter.operator = '<';
            filter.operands = filter.operands[0].operands;
        }
    }
    return filter;
}

function addRow(tbody, key, value) {
    if (value) {
        tbody.append( tr(null, null, [
            td(null, null, [ text(key) ]),
            td(null, null, [ text(value) ])
        ]));
    }
}

function renderDetailedResource(res) {
    var tbody = $('#detailsTableBody');

    tbody.append( tr(null, null, [
        th('ui-widget-header', null, [
            text("Resource")
        ]),
        th('ui-widget-header', null, [
            createElement('form', 'button-group', { method: "post"}, [
                createElement('input', null, { type: "hidden", name: "bundle", value: res.id}),
                createElement('input', 'ui-state-default ui-corner-all', { type: "submit", name: "deploy", value: "Deploy" }, [ text("dummy")]),
                createElement('input', 'ui-state-default ui-corner-all', { type: "submit", name: "deploystart", value: "Deploy and Start" }, [ text("dummy")]),
                text(" "),
                createElement('input', 'ui-state-default ui-corner-all', { id: "optional", type: "checkbox", name: "optional" }),
                text(" "),
                createElement('label', 'ui-widget', { 'for': "optional" }, [ text("deploy optional dependencies") ])
            ])
        ])
    ]));

    addRow(tbody, "Name", res.presentationname);
    addRow(tbody, "Description", res.description);
    addRow(tbody, "Symbolic name", res.symbolicname);
    addRow(tbody, "Version", res.version);
    addRow(tbody, "URI", res.uri);
    addRow(tbody, "Documentation", res.documentation);
    addRow(tbody, "Javadoc", res.javadoc);
    addRow(tbody, "Source", res.source);
    addRow(tbody, "License", res.license);
    addRow(tbody, "Copyright", res.copyright);
    addRow(tbody, "Size", res.size);

    // Exported packages
    createDetailedTable(tbody, "Exported packages", ["Package", "Version"],
                        getCapabilitiesByName(res, "package").sort(function(a,b) {
                            var pa = a.properties['package'], pb = b.properties['package']; return pa == pb ? 0 : pa < pb ? -1 : +1;
                        }),
                        function(p) {
                            return [ text(p.properties['package']), text(p.properties['version']) ];
                        });
    // Exported services
    createDetailedTable(tbody, "Exported services", ["Service"], getCapabilitiesByName(res, "service"), function(p) {
                            return [ text(p.properties['service']) ];
                        });
    // Imported packages
    createDetailedTable(tbody, "Imported packages", ["Package", "Version", "Optional"], getRequirementsByName(res, "package"), function(p) {
                            var f = parseFilter(p.filter);
                            simplify(f);
                            var n, vmin = "[0.0.0", vmax = "infinity)";
                            if (f.operator == '&') {
                                for (var i in f.operands) {
                                    var fi = f.operands[i];
                                    if (fi.operands[0] == 'package' && fi.operator == '=') {
                                        n = fi.operands[1];
                                    }
                                    if (fi.operands[0] == 'version') {
                                        if (fi.operator == '>=') {
                                            vmin = '[' + fi.operands[1];
                                        }
                                        if (fi.operator == '>') {
                                            vmin = '(' + fi.operands[1];
                                        }
                                        if (fi.operator == '<=') {
                                            vmax = fi.operands[1] + "]";
                                        }
                                        if (fi.operator == '<') {
                                            vmax = fi.operands[1] + ")";
                                        }
                                    }
                                }
                            }
                            return [ text(n ? n : p.filter), text(vmin + ", " + vmax), text(p.optional) ];
                        });
    // Imported bundles
    createDetailedTable(tbody, "Imported bundles", ["Bundle", "Version", "Optional"], getRequirementsByName(res, "bundle"), function(p) {
                            return [ text(p.filter), text(""), text(p.optional) ];
                        });
    // Imported services
    createDetailedTable(tbody, "Imported services", ["Service", "Optional"], getRequirementsByName(res, "service"), function(p) {
                            return [ text(p.filter), text(p.optional) ];
                        });
    // Required dependencies
    createDetailedTable(tbody, "Dependencies", ["Name", "Version"], res.required, function(p) {
                            var a = createElement('a', null, { href: (pluginRoot + '/' + p.symbolicname + '/' + p.version) });
                            a.appendChild(text(p.presentationname ? p.presentationname : p.symbolicname));
                            return [ a, text(p.version) ];
                        });
    // Optional dependencies
    createDetailedTable(tbody, "Optional Dependencies", ["Name", "Version"], res.optional, function(p) {
                            var a = createElement('a', null, { href: (pluginRoot + '/' + p.symbolicname + '/' + p.version) });
                            a.appendChild(text(p.presentationname ? p.presentationname : p.symbolicname));
                            return [ a, text(p.version) ];
                        });
    // Unsatisfied requirements
    createDetailedTable(tbody, "Unsatisfied Requirements", ["Requirement", "Optional"], res.unsatisfied, function(p) {
                            return [ text(p.filter), text(p.optional) ];
                        });

//    $('#detailsTableBody').append( tr(null, null, [ th('ui-widget-header', { colspan: 2 }, [ text("Resource") ]) ]) );
//    $('#detailsTableBody').append( tbody );
}

function renderRepository(repo) {
	var _tr = repoTableTemplate.clone();
	_tr.find('td:eq(0)').text( repo.name );
	_tr.find('td:eq(1)').text( repo.url );
	_tr.find('td:eq(2)').text( localTm(repo.lastModified) );
	_tr.find('li:eq(0)').click(function() {
		doRepoAction('refresh', repo.url);
	});
	_tr.find('li:eq(1)').click(function() {
		doRepoAction('delete', repo.url);
	});
	repoTable.append(_tr);
}

function renderData() {
	repoTable.empty();
	resTable.empty();
	if ( obrData.status ) {
		$('.statline').html(i18n.status_ok);
		ifStatusOK.removeClass('ui-helper-hidden');
		for (var i in obrData.repositories ) {
			renderRepository( obrData.repositories[i] );
		}
		if (obrData.details) {
		    $('#resTable').addClass('ui-helper-hidden');
		    $('#detailsTable').removeClass('ui-helper-hidden');
		    for (var i in obrData.resources ) {
			    renderDetailedResource( obrData.resources[i] );
            }
		} else {
		    for (var i in obrData.resources ) {
			    renderResource( obrData.resources[i] );
            }
		}
	} else {
		$('.statline').html(i18n.status_no);
		ifStatusOK.addClass('ui-helper-hidden');
	}
}


$.extend({
  getUrlVars: function(){
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
      var j = hashes[i].indexOf('=');
      if (j > 0) {
        var k = hashes[i].slice(0, j);
        var v = hashes[i].slice(j + 1);
        vars.push(k);
        vars[k] = v;
      } else {
        vars.push(hashes[i]);
        vars[hashes[i]] = true;
      }
    }
    return vars;
  },
  getUrlVar: function(name){
    return $.getUrlVars()[name];
  }
});

$(document).ready( function() {
	repoTable = $('#repoTable tbody');
	repoTableTemplate = repoTable.find('tr').clone();
	addRepoUri = $('#addRepoUri');
	resTable = $('#resTable tbody').empty();
	searchField = $('#searchField');
	ifStatusOK = $('#ifStatusOK');
	
	var query = $.getUrlVar('query');
	if (query) {
        searchField.val(decodeURIComponent(query));
    }

	$('#addRepoBtn').click(function(event) {
        event.preventDefault();
		doRepoAction('add', addRepoUri.val());
	});
	$('#searchBtn').click(function(event) {
        event.preventDefault();
        window.location.href = pluginRoot + '?query=' + encodeURIComponent(searchField.val());
	});
	searchField.keypress(function(event) {
        if (event.keyCode == 13) {
            event.preventDefault();
            $('#searchBtn').click();
        }
	});

	renderData();
    initStaticWidgets();
});

