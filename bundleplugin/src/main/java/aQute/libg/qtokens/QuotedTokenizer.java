package aQute.libg.qtokens;

import java.util.*;

import aQute.libg.generics.*;

public class QuotedTokenizer {
	String	string;
	int		index				= 0;
	String	separators;
	boolean	returnTokens;
	boolean	ignoreWhiteSpace	= true;
	String	peek;
	char	separator;

	public QuotedTokenizer(String string, String separators, boolean returnTokens ) {
		if ( string == null )
			throw new IllegalArgumentException("string argument must be not null");
		this.string = string;
		this.separators = separators;
		this.returnTokens = returnTokens;
	}
	public QuotedTokenizer(String string, String separators) {
		this(string,separators,false);
	}

	public String nextToken(String separators) {
		separator = 0;
		if ( peek != null ) {
			String tmp = peek;
			peek = null;
			return tmp;
		}
		
		if ( index == string.length())
			return null;
		
		StringBuffer sb = new StringBuffer();

		while (index < string.length()) {
			char c = string.charAt(index++);

			if ( Character.isWhitespace(c)) {
				if ( index == string.length())
					break;
				else {
				    sb.append(c);
					continue;
				}
			}
			
			if (separators.indexOf(c) >= 0) {
				if (returnTokens)
					peek = Character.toString(c);
				else
					separator = c;
				break;
			}

			switch (c) {
				case '"' :
				case '\'' :
					quotedString(sb, c);
					break;

				default :
					sb.append(c);
			}
		}
		String result = sb.toString().trim();
		if ( result.length()==0 && index==string.length())
			return null;
		return result;
	}

	public String nextToken() {
		return nextToken(separators);
	}

	private void quotedString(StringBuffer sb, char c) {
		char quote = c;
		while (index < string.length()) {
			c = string.charAt(index++);
			if (c == quote)
				break;
			if (c == '\\' && index < string.length()
					&& string.charAt(index + 1) == quote)
				c = string.charAt(index++);
			sb.append(c);
		}
	}

	public String[] getTokens() {
		return getTokens(0);
	}

	private String [] getTokens(int cnt){
		String token = nextToken();
		if ( token == null ) 
			return new String[cnt];
		
		String result[] = getTokens(cnt+1);
		result[cnt]=token;
		return result;
	}

	public char getSeparator() { return separator; }
	
	public List<String> getTokenSet() {
		List<String> list = Create.list();
		String token = nextToken();
		while ( token != null ) {
			list.add(token);
			token = nextToken();
		}
		return list;
	}
}
