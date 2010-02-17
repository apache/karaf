/* shuts down server after [num] seconds */
function shutdown(num, formname, elemid) {
	var elem = $('#' + elemid);
	var secs=" second";
	var ellipsis="...";
	if (num > 0) {
		if (num != 1) {
			secs+="s";
		}
	    elem.html(num+secs+ellipsis);
		setTimeout('shutdown('+(--num)+', "'+formname+'", "'+elemid+'")',1000);
	} else {
	    $('#' + formname).submit();
	}
}

/* aborts server shutdown and redirects to [target] */
function abort(target) {
    top.location.href=target;
}

/* displays a date in the user's local timezone */
function localTm(time) {
	return (time ? new Date(time) : new Date()).toLocaleString();
}
/* fill in the data */
$(document).ready(function() {
	if(typeof statData == 'undefined') return;
	for(i in statData) {
		var target = $('#' + i);
		if (target.val()) {
			target.val(statData[i]);
		} else {
			target.text(statData[i]);
		}
	}
	$('#lastStarted').text(localTm(statData.lastStarted));
	var st = statData.shutdownTimer;
	$('#shutdownform').css('display', st ? 'none' : 'block');
	$('#shutdownform2').css('display', st ? 'block' : 'none');
	$('#shutdown_type').val(statData.shutdownType);
});