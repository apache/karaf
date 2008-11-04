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


/* shuts down server after [num] seconds */
function shutdown(num, formname, elemid) {
	var elem;
	var canCount = document.getElementById;
	if (canCount) {
	    elem = document.getElementById(elemid);
	    canCount = (typeof(elem) != "undefined" && typeof(elem.innerHTML) != "undefined");
	}
	var secs=" second";
	var ellipsis="...";
	if (num > 0) {
		if (num != 1) {
			secs+="s";
		}
		if (canCount) {
		    elem.innerHTML=num+secs+ellipsis;
		}
		setTimeout('shutdown('+(--num)+', "'+formname+'", "'+elemid+'")',1000);
	} else {
	    document[formname].submit();
	}
}

/* aborts server shutdown and redirects to [target] */
function abort(target) {
    top.location.href=target;
}

/* checks if values of [pass1] and [pass2] match */
function checkPasswd(form, pass0, pass1, pass2) {
    var check = false;
    check = (form[pass0].value != form[pass1].value);
    if (!check) {
        alert("Old and new password must be different.");
        form[pass1].value="";
        form[pass2].value="";
        form[pass1].focus();
    }
    check = (form[pass1].value == form[pass2].value);
    if (!check) {
        alert("Passwords did not match. Please type again.");
        form[pass1].value="";
        form[pass2].value="";
        form[pass1].focus();
    }
    return check;
}

/* displays a date in the user's local timezone */
function localDate(time) {
    var date = time ? new Date(time) : new Date();
    document.write(date.toLocaleString());
}

//-----------------------------------------------------------------------------
// Ajax Support

// request object, do not access directly, use getXmlHttp instead
var xmlhttp = null;
function getXmlHttp() {
	if (xmlhttp) {
		return xmlhttp;
	}
	
	if (window.XMLHttpRequest) {
		xmlhttp = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		try {
			xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (ex) {
			try {
				xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
			} catch (ex) {
			}
		}
	}
	
	return xmlhttp;
}

function sendRequest(/* String */ method, /* url */ url, /* function */ callback) {
    var xmlhttp = getXmlHttp();
    if (!xmlhttp) {
        return;
    }
    
    if (xmlhttp.readyState < 4) {
    	xmlhttp.abort();
  	}
  	
  	if (!method) {
  		method = 'GET';
  	}
  	
  	if (!url) {
  		url = document.location;
  	} else if (url.charAt(0) == '?') {
  		url = document.location + url;
    }
  	
    priv_callback = callback;

    xmlhttp.open(method, url);
    
    // set If-Modified-Since way back in the past to prevent
    // using any content from the cache
    xmlhttp.setRequestHeader("If-Modified-Since", new Date(0));
    
    xmlhttp.onreadystatechange = handleResult;
    xmlhttp.send(null);
  	
}

var priv_callback = null;

function handleResult() {
    var xmlhttp = getXmlHttp();
    if (!xmlhttp || xmlhttp.readyState != 4) {
        return;
    }
    
    var result = xmlhttp.responseText;
    if (!result) {
        return;
    }

    var theCallBack = priv_callback;
    priv_callback = null;
    
	if (theCallBack) {
	    try
        {
            var obj = eval('(' + result + ')');
	       theCallBack(obj);
        }
        catch (e)
        {
            // error evaluating response, don't care ...
        }
	}
}
