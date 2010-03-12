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

// elements cache
var consoleframe = false;
var konsole = false;
var command = false;

function executeCommand(cmd) {
	$.post(document.location.href, { 'command' : encodeURIComponent(cmd) },
		function(result) {
			konsole.removeClass('ui-helper-hidden').append(result);
			consoleframe.attr('scrollTop', konsole.attr('scrollHeight'));
			command.val('');
			shellCommandFocus();
		}, 'html');
}

function shellCommandFocus() { command.focus() }

// automatically executed on load
$(document).ready(function(){
    
    // disable the shell form if the shell service is not available
    if (shellDisabled) {
    
        $('#shell_form').hide();
    
    } else {
    
    	// init cache
    	consoleframe = $('#consoleframe').click(shellCommandFocus);
    	konsole      = $('#console');
    	command      = $('#command').focus();
    
    	// attach action handlers
    	$('#clear').click(function() {
    		konsole.addClass('ui-helper-hidden').html('');
    		consoleframe.attr('scrollTop', 0);
    		shellCommandFocus();
    	});
    	$('#help').click(function() {
    		executeCommand('help');
    	});
    	$('form').submit(function() {
    		executeCommand(command.val());
    		return false;
    	});
    	
	}
});
