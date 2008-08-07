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

function executeCommand(command) {
    var xmlhttp = getXmlHttp();
    if (!xmlhttp) {
        return;
    }
    
    if (xmlhttp.readyState < 4) {
        xmlhttp.abort();
    }
    
    var url = document.location;
    
    xmlhttp.open("POST", url);
    
    // set If-Modified-Since way back in the past to prevent
    // using any content from the cache
    xmlhttp.setRequestHeader("If-Modified-Since", new Date(0));
    xmlhttp.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
    
    xmlhttp.onreadystatechange = updateConsole;
    
    xmlhttp.send("command=" + encodeURIComponent(command));
}

function updateConsole() {
    var xmlhttp = getXmlHttp();
    if (!xmlhttp || xmlhttp.readyState != 4) {
        return;
    }
    
    var result = xmlhttp.responseText;
    if (!result) {
        return;
    }

    var console = document.getElementById("console");
    
    console.style.display = "";
    console.innerHTML = console.innerHTML + result;
    
    var consoleframe = document.getElementById("consoleframe");
    consoleframe.scrollTop = console.scrollHeight;

    document.forms["shellCommandForm"].elements["command"].value = "";
    
    shellCommandFocus();
}

function clearConsole() {
    var console = document.getElementById("console");

    console.style.display = "none";
    console.innerHTML = "";
    
    var consoleframe = document.getElementById("consoleframe");
    consoleframe.scrollTop = 0;
    
    shellCommandFocus();
}

function shellCommandFocus() {
    document.forms["shellCommandForm"].elements["command"].focus();
}

function runShellCommand() {
    var command = document.forms["shellCommandForm"].elements["command"].value;
    
    executeCommand(command);
}
