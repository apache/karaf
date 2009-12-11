/**
 * Copyright (c) 2005 - 2009, James Auldridge
 * All rights reserved.
 *
 * Licensed under the BSD, MIT, and GPL (your choice!) Licenses:
 *  http://code.google.com/p/cookies/wiki/License
 *
 */
var jaaulde=window.jaaulde||{};jaaulde.utils=jaaulde.utils||{};jaaulde.utils.cookies=(function()
{var cookies=[];var defaultOptions={hoursToLive:null,path:'/',domain:null,secure:false};var resolveOptions=function(options)
{var returnValue;if(typeof options!=='object'||options===null)
{returnValue=defaultOptions;}
else
{returnValue={hoursToLive:(typeof options.hoursToLive==='number'&&options.hoursToLive!==0?options.hoursToLive:defaultOptions.hoursToLive),path:(typeof options.path==='string'&&options.path!==''?options.path:defaultOptions.path),domain:(typeof options.domain==='string'&&options.domain!==''?options.domain:defaultOptions.domain),secure:(typeof options.secure==='boolean'&&options.secure?options.secure:defaultOptions.secure)};}
return returnValue;};var expiresGMTString=function(hoursToLive)
{var dateObject=new Date();dateObject.setTime(dateObject.getTime()+(hoursToLive*60*60*1000));return dateObject.toGMTString();};var assembleOptionsString=function(options)
{options=resolveOptions(options);return((typeof options.hoursToLive==='number'?'; expires='+expiresGMTString(options.hoursToLive):'')+'; path='+options.path+
(typeof options.domain==='string'?'; domain='+options.domain:'')+
(options.secure===true?'; secure':''));};var splitCookies=function()
{cookies={};var pair,name,value,separated=document.cookie.split(';');for(var i=0;i<separated.length;i=i+1)
{pair=separated[i].split('=');name=pair[0].replace(/^\s*/,'').replace(/\s*$/,'');value=decodeURIComponent(pair[1]);cookies[name]=value;}
return cookies;};var constructor=function(){};constructor.prototype.get=function(cookieName)
{var returnValue;splitCookies();if(typeof cookieName==='string')
{returnValue=(typeof cookies[cookieName]!=='undefined')?cookies[cookieName]:null;}
else if(typeof cookieName==='object'&&cookieName!==null)
{returnValue={};for(var item in cookieName)
{if(typeof cookies[cookieName[item]]!=='undefined')
{returnValue[cookieName[item]]=cookies[cookieName[item]];}
else
{returnValue[cookieName[item]]=null;}}}
else
{returnValue=cookies;}
return returnValue;};constructor.prototype.filter=function(cookieNameRegExp)
{var returnValue={};splitCookies();if(typeof cookieNameRegExp==='string')
{cookieNameRegExp=new RegExp(cookieNameRegExp);}
for(var cookieName in cookies)
{if(cookieName.match(cookieNameRegExp))
{returnValue[cookieName]=cookies[cookieName];}}
return returnValue;};constructor.prototype.set=function(cookieName,value,options)
{if(typeof value==='undefined'||value===null)
{if(typeof options!=='object'||options===null)
{options={};}
value='';options.hoursToLive=-8760;}
var optionsString=assembleOptionsString(options);document.cookie=cookieName+'='+encodeURIComponent(value)+optionsString;};constructor.prototype.del=function(cookieName,options)
{var allCookies={};if(typeof options!=='object'||options===null)
{options={};}
if(typeof cookieName==='boolean'&&cookieName===true)
{allCookies=this.get();}
else if(typeof cookieName==='string')
{allCookies[cookieName]=true;}
for(var name in allCookies)
{if(typeof name==='string'&&name!=='')
{this.set(name,null,options);}}};constructor.prototype.test=function()
{var returnValue=false,testName='cT',testValue='data';this.set(testName,testValue);if(this.get(testName)===testValue)
{this.del(testName);returnValue=true;}
return returnValue;};constructor.prototype.setOptions=function(options)
{if(typeof options!=='object')
{options=null;}
defaultOptions=resolveOptions(options);};return new constructor();})();(function()
{if(window.jQuery)
{(function($)
{$.cookies=jaaulde.utils.cookies;var extensions={cookify:function(options)
{return this.each(function()
{var i,resolvedName=false,resolvedValue=false,name='',value='',nameAttrs=['name','id'],nodeName,inputType;for(i in nameAttrs)
{if(!isNaN(i))
{name=$(this).attr(nameAttrs[i]);if(typeof name==='string'&&name!=='')
{resolvedName=true;break;}}}
if(resolvedName)
{nodeName=this.nodeName.toLowerCase();if(nodeName!=='input'&&nodeName!=='textarea'&&nodeName!=='select'&&nodeName!=='img')
{value=$(this).html();resolvedValue=true;}
else
{inputType=$(this).attr('type');if(typeof inputType==='string'&&inputType!=='')
{inputType=inputType.toLowerCase();}
if(inputType!=='radio'&&inputType!=='checkbox')
{value=$(this).val();resolvedValue=true;}}
if(resolvedValue)
{if(typeof value!=='string'||value==='')
{value=null;}
$.cookies.set(name,value,options);}}});},cookieFill:function()
{return this.each(function()
{var i,resolvedName=false,name='',value,nameAttrs=['name','id'],iteration=0,nodeName;for(i in nameAttrs)
{if(!isNaN(i))
{name=$(this).attr(nameAttrs[i]);if(typeof name==='string'&&name!=='')
{resolvedName=true;break;}}}
if(resolvedName)
{value=$.cookies.get(name);if(value!==null)
{nodeName=this.nodeName.toLowerCase();if(nodeName==='input'||nodeName==='textarea'||nodeName==='select')
{$(this).val(value);}
else
{$(this).html(value);}}}
iteration=0;});},cookieBind:function(options)
{return this.each(function()
{$(this).cookieFill().change(function()
{$(this).cookify(options);});});}};$.each(extensions,function(i)
{$.fn[i]=this;});})(window.jQuery);}})();