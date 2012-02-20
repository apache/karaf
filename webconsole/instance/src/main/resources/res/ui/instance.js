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

function renderInstance( data ) {
    $(document).ready( function() {
        renderView();
        renderData( data );
    } );
}

function renderView() {
    renderStatusLine();
    var txt = "<form method='post'><div class='ui-widget-header ui-corner-top buttonGroup'><table id='create_instance_table' class='nicetable ui-widget'><thead>" +
    "<tr><input type='hidden' name='action' value='create'/>" +
    "<th>Name: <input id='name' type='text' name='name' style='width:70%' colspan='2'/></th>" +
    "<th>SSH Port: <input id='sshPort' type='text' name='sshPort' style='width:70%' colspan='2'/></th>" +
    "<th>RMI Registry Port: <input id='rmiRegistryPort' type='text' name='rmiRegistryPort' style='width:70%' colspan='2'/></th>" +
    "<th>RMI Server Port: <input id='rmiServerPort' type='text' name='rmiServerPort' style='width:70%' colspan='2'/></th>" +
    "<th>Location: <input id='location' type='text' name='location' style='width:70%' colspan='2'/></th>" +
    "<th>JavaOpts: <input id='javaOpts' type='text' name='javaOpts' style='width:70%' colspan='2'/></th>" +
    "<th />" +
    "</tr><tr><th>Features: <input id='features' type='text' name='features' style='width:70%' colspan='2'" + 
    " title='Specify initial features separated by commas.'/></th>" + 
    "<th colspan='2'>Feature URLs: <input id='featureURLs' type='text' name='featureURLs' style='width:80%' colspan='2'" + 
    " title='Specify additional feature URLs separate by commas.'/></th>" +
    "<th class='col_Actions'><input type='button' value='Create' onclick='createInstance()'/></th>" +
    "</tr></thead></table></div></form>";
    $("#plugin_content").append( txt );
    renderTable( "Karaf Instances", "instances_table", ["Pid", "Name", "SSH Port", "RMI Registry Port", "RMI Server Port", "State", "JavaOpts", "Location", "Actions"] );
    renderStatusLine();
}

function createInstance() {
    var name = document.getElementById( "name" ).value;
    var sshPort = document.getElementById( "sshPort" ).value;
    var rmiRegistryPort = document.getElementById("rmiRegistryPort").value;
    var rmiServerPort = document.getElementById("rmiServerPort").value;
    var location = document.getElementById( "location" ).value;
    var javaOpts = document.getElementById( "javaOpts" ).value;
    var features = document.getElementById( "features" ).value;
    var featureURLs = document.getElementById( "featureURLs" ).value;
    postCreateInstance( name, sshPort, rmiRegistryPort, rmiServerPort, location, javaOpts, features, featureURLs );
}

function postCreateInstance( /* String */ name, /* String */ sshPort, /* String */ rmiRegistryPort, /* String */ rmiServerPort, /* String */ location,
		/* String */ javaOpts, /* String */ features, /* String */ featureURLs ) {
    $.post( pluginRoot, {"action": "create", "name": name, "sshPort": sshPort, "rmiRegistryPort": rmiRegistryPort, "rmiServerPort": rmiServerPort, "location": location,
                             "javaOpts": javaOpts, "features": features, "featureURLs": featureURLs }, function( data ) {
        renderData( data );
    }, "json" );
}

function renderStatusLine() {
    $("#plugin_content").append( "<div class='fullwidth'><div class='statusline'/></div>" );
}

function renderTable( /* String */ title, /* String */ id, /* array of Strings */ columns ) {
    var txt = "<table class='ui-widget-header nicetable noauto ui-widget'><thead><tr>" +
    "<th>" +
    title + "</th></tr></thead></table>" +
    "<table id='" + id + "' class='tablesorter nicetable noauto ui-widget'><thead><tr>";
    for ( var name in columns ) {
        txt = txt + "<th class='col_" + columns[name] + " ui-widget-header header' >" + columns[name] + "</th>";
    }
    txt = txt + "</tr></thead><tbody></tbody></table>";
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
    parent.appendChild( td( null, null, [ text( instance.sshPort ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.rmiRegistryPort ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.rmiServerPort ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.state ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.location ) ] ) );
    parent.appendChild( td( null, null, [ text( instance.javaOpts ) ] ) );
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


