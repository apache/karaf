dp.sh.Brushes.Java = function()
{
	var keywords =	'abstract assert boolean break byte case catch char class const' +
                    'continue default do double else enum extends false final finally float' +
                    'for goto if implements import instanceof inst interface log native' +
                    'new null package private protected public return short static strictfp super' +
                    'switch synchronized this throw throws transient true try void volatile while';

    this.regexList = [
		{ regex: new RegExp('//.*$', 'gm'),							css: 'comment' },			// one line comments
		{ regex: new RegExp('/\\*[\\s\\S]*?\\*/', 'g'),				css: 'comment' },			// multiline comments
		{ regex: new RegExp('"(?:[^"\n]|[\"])*?"', 'g'),			css: 'string' },			// double quoted strings
		{ regex: new RegExp("'(?:[^'\n]|[\'])*?'", 'g'),			css: 'string' },			// single quoted strings
		{ regex: new RegExp('^\\s*@.*', 'gm'),						css: 'preprocessor' },		// preprocessor tags like @see
		{ regex: new RegExp(this.GetKeywords(keywords), 'gm'),		css: 'keyword' }			// keywords
		];

	this.CssClass = 'dp-c';
}

dp.sh.Brushes.Java.prototype	= new dp.sh.Highlighter();
dp.sh.Brushes.Java.Aliases	    = ['java'];
