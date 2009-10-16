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

function renderAdmin( data ) {
    $(document).ready( function() {
        renderView();
        renderData( data );
    } );
}

function renderView() {
    renderStatusLine();
    var txt = "<form method='post'><div class='table'><table id='create_instance_table' class='tablelayout'><tbody>" +
    "<tr><input type='hidden' name='action' value='create'/>" +
    "<td>Name: <input id='name' type='text' name='name' style='width:70%' colspan='2'/></td>" +
    "<td>Port: <input id='port' type='text' name='port' style='width:70%' colspan='2'/></td>" +
    "<td>Location: <input id='location' type='text' name='location' style='width:70%' colspan='2'/></td>" +
    "<td />" +
    "</tr><tr><td>Features: <input id='features' type='text' name='features' style='width:70%' colspan='2'" + 
    " title='Specify initial features separated by commas.'/></td>" + 
    "<td colspan='2'>Feature URLs: <input id='featureURLs' type='text' name='featureURLs' style='width:80%' colspan='2'" + 
    " title='Specify additional feature URLs separate by commas.'/></td>" +
    "<td class='col_Actions'><input type='button' value='Create' onclick='createInstance()'/></td>" +
    "</tr></tbody></table></div></form><br/>";
    $("#plugin_content").append( txt );
    renderTable( "Karaf Instances", "instances_table", ["Pid", "Name", "Port", "State", "Location", "Actions"] );
    renderStatusLine();
}

function createInstance() {
    var name = document.getElementById( "name" ).value;
    var port = document.getElementById( "port" ).value;
    var location = document.getElementById( "location" ).value;
    var features = document.getElementById( "features" ).value;
    var featureURLs = document.getElementById( "featureURLs" ).value;
    postCreateInstance( name, port, location, features, featureURLs );
}

function postCreateInstance( /* String */ name, /* String */ port, /* String */ location, 
		/* String */ features, /* String */ featureURLs ) {
    $.post( pluginRoot, {"action": "create", "name": name, "port": port, "location": location, 
                             "features": features, "featureURLs": featureURLs }, function( data ) {
        renderData( data );
    }, "json" );
}

function renderStatusLine() {
    $("#plugin_content").append( "<div class='fullwidth'><div class='statusline'/></div>" );
}

function renderTable( /* String */ title, /* String */ id, /* array of Strings */ columns ) {
    var txt = "<div class='table'><table class='tablelayout'><tbody><tr>" +
    "<td style='color:#6181A9;background-color:#e6eeee'>" +
    title + "</td></tr></tbody></table>" +
    "<table id='" + id + "' class='tablelayout'><thead><tr>";
    for ( var name in columns ) {
        txt = txt + "<th class='col_" + columns[name] + "' style='border-top:#e6eeee'>" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table></div>";
    $("#plugin_content").append( txt );
}

function renderData( /* Object */ data ) {
    renderStatusData( data.status );
    renderInstancesTableData( data.instances );
    $("#instances_table").tablesorter( {
        headers: {
            5: { 
                sorter: false
            }
        },
        sortList: [[0,0]],
    } );
}

function renderStatusData( /* String */ status )  {
    $(".statusline").empty().append( status );
}

function renderInstancesTableData( /* array of Objects */ instances ) {
    $("#instances_table > tbody > tr").remove();
    for ( var idx in instances ) {
        var trElement = tr( null, { 
            id: instances[idx].pid
        } );
        renderInstanceData( trElement, instances[idx] );
        $("#instances_table > tbody").append( trElement );
    }
    $("#instances_table").trigger( "update" );
}

function renderInstanceData( /* Element */ parent, /* Object */ instance ) {
    parent.appendChild( td( null, null, [ text( instance.pid ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.name ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.port ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.state ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.location ) ] ) );
    var actionsTd = td( null, null );
    var div = createElement( "div", null, {
        style: { 
            "text-align": "left"
        }
    } );
    actionsTd.appendChild( div );

    for ( var a in instance.actions ) {
        instanceButton( div, instance.name, instance.actions[a] );
    }
    parent.appendChild( actionsTd );
}

function instanceButton( /* Element */ parent, /* String */ name, /* Obj */ action ) {
    var input = createElement( "input", null, {
        type: 'image',
        style: {
            "margin-left": "10px"
        },
        title: action.title,
        alt: action.title,
        src: imgRoot + '/bundle_' + action.image + '.png'
    } );
    $(input).click( function() {
        changeInstanceState( action.op, name )
    } );
    parent.appendChild( input );
}

function changeInstanceState( /* String */ action, /* String */ name) {
    $.post( pluginRoot, {
        "action": action,
        "name": name
    }, function( data ) {
        renderData( data );
    }, "json" );
}


