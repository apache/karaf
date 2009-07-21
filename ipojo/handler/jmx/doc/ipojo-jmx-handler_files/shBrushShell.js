dp.sh.Brushes.Shell = function()
{
	var keywords =	'alias array autor base basename break ' +
                    'cat catv cd chdir cmpv continue conv copy ' +
                    'crc ctime cut dirname echo env eval exec else if elif then ' +
                    'export expr extern false fmode fork fprint ' +
                    'fsize fstat fullname global goend goto grep ifdef ' +
                    'ifset ifenv inv kill line link list ' +
                    'local localset mkdirs mktemp move mtime nop print ' +
                    'prints pwd read readc readl readonly rel ' +
                    'remove return seek set shift sleep sortl ' +
                    'static stime sum system systime tee test times ' +
                    'tr trap true type typeset tz umask unalias ' +
                    'unexport unset unsetenv ver wait wc whence ' +
                    'sane exit prompt let';


	this.regexList = [
		{ regex: new RegExp('#.*$', 'gm'),							css: 'comment' },  			// one line
		{ regex: new RegExp(this.GetKeywords(keywords), 'gm'),		css: 'keyword' }			// keyword
		];

	this.CssClass = 'dp-shell';
}

dp.sh.Brushes.Shell.prototype	= new dp.sh.Highlighter();
dp.sh.Brushes.Shell.Aliases	= ['shell'];
