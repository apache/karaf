$(document).ready(function() {
	var dlg = $('#waitDlg').dialog({
		modal    : true,
		autoOpen : false,
		draggable: false,
		resizable: false,
		closeOnEscape: false
	});

	$('#tabs').tabs({ajaxOptions: {
		beforeSend : function() { dlg.dialog('open') },
		complete   : function() { dlg.dialog('close')}
	}}).tabs('paging');
});