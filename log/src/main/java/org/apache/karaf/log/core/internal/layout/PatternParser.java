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
package org.apache.karaf.log.core.internal.layout;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.log.LogLevel;

/**
 * Copied from log4j
 */
/**
   Most of the work of the {@code org.apache.log4j.PatternLayout} class
   is delegated to the PatternParser class.

   @since 0.8.2
*/
public class PatternParser {

  private static final String LINE_SEP = System.getProperty("line.separator");

  private static final char ESCAPE_CHAR = '%';

  private static final int LITERAL_STATE = 0;
  private static final int CONVERTER_STATE = 1;
  private static final int MINUS_STATE = 2;
  private static final int DOT_STATE = 3;
  private static final int MIN_STATE = 4;
  private static final int MAX_STATE = 5;

  static final int FULL_LOCATION_CONVERTER = 1000;
  static final int METHOD_LOCATION_CONVERTER = 1001;
  static final int CLASS_LOCATION_CONVERTER = 1002;
  static final int LINE_LOCATION_CONVERTER = 1003;
  static final int FILE_LOCATION_CONVERTER = 1004;

  static final int RELATIVE_TIME_CONVERTER = 2000;
  static final int THREAD_CONVERTER = 2001;
  static final int LEVEL_CONVERTER = 2002;
  static final int NDC_CONVERTER = 2003;
  static final int MESSAGE_CONVERTER = 2004;

  int state;
  protected StringBuffer currentLiteral = new StringBuffer(32);
  protected int patternLength;
  protected int i;
  PatternConverter head;
  PatternConverter tail;
  protected FormattingInfo formattingInfo = new FormattingInfo();
  protected String pattern;

  public
  PatternParser(String pattern) {
    this.pattern = pattern;
    patternLength =  pattern.length();
    state = LITERAL_STATE;
  }

  private
  void  addToList(PatternConverter pc) {
    if(head == null) {
      head = tail = pc;
    } else {
      tail.next = pc;
      tail = pc;
    }
  }

  protected
  String extractOption() {
    if((i < patternLength) && (pattern.charAt(i) == '{')) {
      int end = i;
      int nb = 1;
      while (++end < patternLength) {
        switch (pattern.charAt(end)) {
          case '{':
            nb++;
            break;
          case '}':
            if (--nb == 0) {
              String r = pattern.substring(i + 1, end);
              i = end + 1;
              return r;
            }
            break;
        }
      }
    }
    return null;
  }


  /**
   * The option is expected to be in decimal and positive.
   * In case of error, zero is returned.
   *
   * @return The precision value, or zero in case of error.
   */
  protected int extractPrecisionOption() {
    String opt = extractOption();
    int r = 0;
    if(opt != null) {
      try {
	r = Integer.parseInt(opt);
	if(r <= 0) {
	    //LogLog.error("Precision option (" + opt + ") isn't a positive integer.");
	    r = 0;
	}
      }
      catch (NumberFormatException e) {
	//LogLog.error("Category option \""+opt+"\" not a decimal integer.", e);
      }
    }
    return r;
  }

  public
  PatternConverter parse() {
    char c;
    i = 0;
    while(i < patternLength) {
      c = pattern.charAt(i++);
      switch(state) {
      case LITERAL_STATE:
        // In literal state, the last char is always a literal.
        if(i == patternLength) {
          currentLiteral.append(c);
          continue;
        }
        if(c == ESCAPE_CHAR) {
          // peek at the next char.
          switch(pattern.charAt(i)) {
          case ESCAPE_CHAR:
            currentLiteral.append(c);
            i++; // move pointer
            break;
          case 'n':
            currentLiteral.append(LINE_SEP);
            i++; // move pointer
            break;
          default:
            if(currentLiteral.length() != 0) {
              addToList(new LiteralPatternConverter(
                                                  currentLiteral.toString()));
              //LogLog.debug("Parsed LITERAL converter: \""
              //           +currentLiteral+"\".");
            }
            currentLiteral.setLength(0);
            currentLiteral.append(c); // append %
            state = CONVERTER_STATE;
            formattingInfo.reset();
          }
        }
        else {
          currentLiteral.append(c);
        }
        break;
      case CONVERTER_STATE:
	currentLiteral.append(c);
	switch(c) {
	case '-':
	  formattingInfo.leftAlign = true;
	  break;
	case '.':
	  state = DOT_STATE;
	  break;
	default:
	  if(c >= '0' && c <= '9') {
	    formattingInfo.min = c - '0';
	    state = MIN_STATE;
	  }
	  else
	    finalizeConverter(c);
	} // switch
	break;
      case MIN_STATE:
	currentLiteral.append(c);
	if(c >= '0' && c <= '9')
	  formattingInfo.min = formattingInfo.min*10 + (c - '0');
	else if(c == '.')
	  state = DOT_STATE;
	else {
	  finalizeConverter(c);
	}
	break;
      case DOT_STATE:
	currentLiteral.append(c);
	if(c >= '0' && c <= '9') {
	  formattingInfo.max = c - '0';
	   state = MAX_STATE;
	}
	else {
	  //LogLog.error("Error occured in position "+i+".\n Was expecting digit, instead got char \""+c+"\".");
	  state = LITERAL_STATE;
	}
	break;
      case MAX_STATE:
	currentLiteral.append(c);
	if(c >= '0' && c <= '9')
	  formattingInfo.max = formattingInfo.max*10 + (c - '0');
	else {
	  finalizeConverter(c);
	  state = LITERAL_STATE;
	}
	break;
      } // switch
    } // while
    if(currentLiteral.length() != 0) {
      addToList(new LiteralPatternConverter(currentLiteral.toString()));
      //LogLog.debug("Parsed LITERAL converter: \""+currentLiteral+"\".");
    }
    return head;
  }

  protected
  void finalizeConverter(char c) {
    PatternConverter pc = null;
    switch(c) {
    case 'c':
      pc = new CategoryPatternConverter(formattingInfo,
					extractPrecisionOption());
      //LogLog.debug("CATEGORY converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'C':
      pc = new ClassNamePatternConverter(formattingInfo,
					 extractPrecisionOption());
      //LogLog.debug("CLASS_NAME converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'd':
      String dateFormatStr = AbsoluteTimeDateFormat.ISO8601_DATE_FORMAT;
      DateFormat df;
      String dOpt = extractOption();
      if(dOpt != null)
	dateFormatStr = dOpt;

      if(dateFormatStr.equalsIgnoreCase(
                                    AbsoluteTimeDateFormat.ISO8601_DATE_FORMAT))
	df = new  ISO8601DateFormat();
      else if(dateFormatStr.equalsIgnoreCase(
                                   AbsoluteTimeDateFormat.ABS_TIME_DATE_FORMAT))
	df = new AbsoluteTimeDateFormat();
      else if(dateFormatStr.equalsIgnoreCase(
                              AbsoluteTimeDateFormat.DATE_AND_TIME_DATE_FORMAT))
	df = new DateTimeDateFormat();
      else {
	try {
	  df = new SimpleDateFormat(dateFormatStr);
	}
	catch (IllegalArgumentException e) {
	  //LogLog.error("Could not instantiate SimpleDateFormat with " + dateFormatStr, e);
	  df = new ISO8601DateFormat();
	}
      }
      pc = new DatePatternConverter(formattingInfo, df);
      //LogLog.debug("DATE converter {"+dateFormatStr+"}.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'F':
      pc = new LocationPatternConverter(formattingInfo,
					FILE_LOCATION_CONVERTER);
      //LogLog.debug("File name converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'h':
      String pat = extractOption();
      String style = extractOption();
      pc = new HighlightPatternConverter(formattingInfo, pat, style);
      currentLiteral.setLength(0);
      break;
    /*case 'l':
      pc = new LocationPatternConverter(formattingInfo,
					FULL_LOCATION_CONVERTER);
      //LogLog.debug("Location converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;*/
    case 'L':
      pc = new LocationPatternConverter(formattingInfo,
					LINE_LOCATION_CONVERTER);
      //LogLog.debug("LINE NUMBER converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'm':
      pc = new BasicPatternConverter(formattingInfo, MESSAGE_CONVERTER);
      //LogLog.debug("MESSAGE converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'M':
      pc = new LocationPatternConverter(formattingInfo,
					METHOD_LOCATION_CONVERTER);
      //LogLog.debug("METHOD converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'p':
      pc = new BasicPatternConverter(formattingInfo, LEVEL_CONVERTER);
      //LogLog.debug("LEVEL converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 'r':
      pc = new BasicPatternConverter(formattingInfo,
					 RELATIVE_TIME_CONVERTER);
      //LogLog.debug("RELATIVE time converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
    case 't':
      pc = new BasicPatternConverter(formattingInfo, THREAD_CONVERTER);
      //LogLog.debug("THREAD converter.");
      //formattingInfo.dump();
      currentLiteral.setLength(0);
      break;
      /*case 'u':
      if(i < patternLength) {
	char cNext = pattern.charAt(i);
	if(cNext >= '0' && cNext <= '9') {
	  pc = new UserFieldPatternConverter(formattingInfo, cNext - '0');
	  LogLog.debug("USER converter ["+cNext+"].");
	  formattingInfo.dump();
	  currentLiteral.setLength(0);
	  i++;
	}
	else
	  LogLog.error("Unexpected char" +cNext+" at position "+i);
      }
      break;*/
    /*case 'x':
      pc = new BasicPatternConverter(formattingInfo, NDC_CONVERTER);
      //LogLog.debug("NDC converter.");
      currentLiteral.setLength(0);
      break;*/
    case 'X':
      String xOpt = extractOption();
      pc = new MDCPatternConverter(formattingInfo, xOpt);
      currentLiteral.setLength(0);
      break;
    default:
      //LogLog.error("Unexpected char [" +c+"] at position "+i+" in conversion patterrn.");
      pc = new LiteralPatternConverter(currentLiteral.toString());
      currentLiteral.setLength(0);
    }

    addConverter(pc);
  }

  protected
  void addConverter(PatternConverter pc) {
    currentLiteral.setLength(0);
    // Add the pattern converter to the list.
    addToList(pc);
    // Next pattern is assumed to be a literal.
    state = LITERAL_STATE;
    // Reset formatting info
    formattingInfo.reset();
  }

  // ---------------------------------------------------------------------
  //                      PatternConverters
  // ---------------------------------------------------------------------

  private static class BasicPatternConverter extends PatternConverter {
    int type;

    BasicPatternConverter(FormattingInfo formattingInfo, int type) {
      super(formattingInfo);
      this.type = type;
    }

    public
    String convert(PaxLoggingEvent event) {
      switch(type) {
      case RELATIVE_TIME_CONVERTER:
	return (Long.toString(event.getTimeStamp() - getStartTime()));
      case THREAD_CONVERTER:
	return event.getThreadName();
      case LEVEL_CONVERTER:
	return event.getLevel().toString();
    //  case NDC_CONVERTER:
	//return event.getNDC();
      case MESSAGE_CONVERTER: {
	return event.getRenderedMessage();
      }
      default: return null;
      }
    }
  }

  private static class LiteralPatternConverter extends PatternConverter {
    private String literal;

    LiteralPatternConverter(String value) {
      literal = value;
    }

    public
    final
    void format(StringBuffer sbuf, PaxLoggingEvent event) {
      sbuf.append(literal);
    }

    public
    String convert(PaxLoggingEvent event) {
      return literal;
    }
  }

  private static class DatePatternConverter extends PatternConverter {
    private DateFormat df;
    private Date date;

    DatePatternConverter(FormattingInfo formattingInfo, DateFormat df) {
      super(formattingInfo);
      date = new Date();
      this.df = df;
    }

    public
    String convert(PaxLoggingEvent event) {
      date.setTime(event.getTimeStamp());
      String converted = null;
      try {
        converted = df.format(date);
      }
      catch (Exception ex) {
        //LogLog.error("Error occured while converting date.", ex);
      }
      return converted;
    }
  }

  private static class HighlightPatternConverter extends PatternConverter {
    static Map<String, String> SEQUENCES;
    static {
      SEQUENCES = new HashMap<>();
      SEQUENCES.put("csi", "\u001b[");
      SEQUENCES.put("suffix", "m");
      SEQUENCES.put("separator", ";");
      SEQUENCES.put("normal", "0");
      SEQUENCES.put("bold", "1");
      SEQUENCES.put("bright", "1");
      SEQUENCES.put("dim", "2");
      SEQUENCES.put("underline", "3");
      SEQUENCES.put("blink", "5");
      SEQUENCES.put("reverse", "7");
      SEQUENCES.put("hidden", "8");
      SEQUENCES.put("black", "30");
      SEQUENCES.put("fg_black", "30");
      SEQUENCES.put("red", "31");
      SEQUENCES.put("fg_red", "31");
      SEQUENCES.put("green", "32");
      SEQUENCES.put("fg_green", "32");
      SEQUENCES.put("yellow", "33");
      SEQUENCES.put("fg_yellow", "33");
      SEQUENCES.put("blue", "34");
      SEQUENCES.put("fg_blue", "34");
      SEQUENCES.put("magenta", "35");
      SEQUENCES.put("fg_magenta", "35");
      SEQUENCES.put("cyan", "36");
      SEQUENCES.put("fg_cyan", "36");
      SEQUENCES.put("white", "37");
      SEQUENCES.put("fg_white", "37");
      SEQUENCES.put("default", "39");
      SEQUENCES.put("fg_default", "39");
      SEQUENCES.put("bg_black", "40");
      SEQUENCES.put("bg_red", "41");
      SEQUENCES.put("bg_green", "42");
      SEQUENCES.put("bg_yellow", "43");
      SEQUENCES.put("bg_blue", "44");
      SEQUENCES.put("bg_magenta", "45");
      SEQUENCES.put("bg_cyan", "46");
      SEQUENCES.put("bg_white", "47");
      SEQUENCES.put("bg_default", "49");
    }
    private PatternConverter pattern;
    private Map<String, String> style;

    HighlightPatternConverter(FormattingInfo formattingInfo, String pattern, String style) {
      super(formattingInfo);
      this.pattern = new PatternParser(pattern).parse();
      Map<String, String> unparsed = new HashMap<>();
      unparsed.put("trace", "cyan");
      unparsed.put("debug", "cyan");
      unparsed.put("info", "bright green");
      unparsed.put("warn", "bright yellow");
      unparsed.put("error", "bright red");
      unparsed.put("fatal", "bright red");
      if (style != null) {
        style = style.toLowerCase(Locale.ENGLISH);
        if (style.indexOf(',') < 0 && style.indexOf('=') < 0) {
          unparsed.put("trace", style.trim());
          unparsed.put("debug", style.trim());
          unparsed.put("info", style.trim());
          unparsed.put("warn", style.trim());
          unparsed.put("error", style.trim());
          unparsed.put("fatal", style.trim());
        } else {
          String[] keys = style.split("\\s*,\\s*");
          for (String key : keys) {
            String[] val = key.split("\\s*=\\s*");
            if (val.length > 1) {
              unparsed.put(val[0].trim(), val[1].trim());
            }
          }
        }
      }
      this.style = new HashMap<>();
      for (Map.Entry<String, String> e : unparsed.entrySet()) {
        this.style.put(e.getKey(), createSequence(e.getValue().split("\\s")));
      }
    }

    private String createSequence(String... names) {
      StringBuilder sb = new StringBuilder(SEQUENCES.get("csi"));
      boolean first = true;
      for (String name : names) {
        name = name.trim();
        if (!first) {
          sb.append(SEQUENCES.get("separator"));
        }
        first = false;
        sb.append(SEQUENCES.getOrDefault(name, name));
      }
      sb.append(SEQUENCES.get("suffix"));
      return sb.toString();
    }

    public
    String convert(PaxLoggingEvent event) {
      String s;
      if (event.getLevel().toLevel().equals(LogLevel.TRACE)) {
        s = "trace";
      } else if (event.getLevel().toLevel().equals(LogLevel.DEBUG)) {
        s = "debug";
      } else if (event.getLevel().toLevel().equals(LogLevel.INFO)) {
        s = "info";
      } else if (event.getLevel().toLevel().equals(LogLevel.WARN)) {
        s = "warn";
      } else if (event.getLevel().toLevel().equals(LogLevel.ERROR)) {
        s = "error";
      } else if (event.getLevel().toLevel().equals(LogLevel.AUDIT)) {
        s = "audit";
      } else {
        s = "error";
      }
      String str = style.get(s);
      if (str != null) {
        return str + pattern.convert(event) + SEQUENCES.get("csi") + SEQUENCES.get("suffix");
      } else {
        return pattern.convert(event);
      }
    }
  }

  private class LocationPatternConverter extends PatternConverter {
    int type;

    LocationPatternConverter(FormattingInfo formattingInfo, int type) {
      super(formattingInfo);
      this.type = type;
    }

    public
    String convert(PaxLoggingEvent event) {
      if(!event.locationInformationExists()){
        return "?";
      }
      PaxLocationInfo locationInfo = event.getLocationInformation();
      switch(type) {
      /*case FULL_LOCATION_CONVERTER:
	return locationInfo.fullInfo;*/
      case METHOD_LOCATION_CONVERTER:
	return locationInfo.getMethodName();
      case LINE_LOCATION_CONVERTER:
	return locationInfo.getLineNumber();
      case FILE_LOCATION_CONVERTER:
	return locationInfo.getFileName();
      default: return null;
      }
    }
  }

  private static abstract class NamedPatternConverter extends PatternConverter {
    int precision;

    NamedPatternConverter(FormattingInfo formattingInfo, int precision) {
      super(formattingInfo);
      this.precision =  precision;
    }

    abstract
    String getFullyQualifiedName(PaxLoggingEvent event);

    public
    String convert(PaxLoggingEvent event) {
      String n = getFullyQualifiedName(event);
      if (n == null)
	    return null;
      if(precision <= 0)
	return n;
      else {
	int len = n.length();

	// We substract 1 from 'len' when assigning to 'end' to avoid out of
	// bounds exception in return r.substring(end+1, len). This can happen if
	// precision is 1 and the category name ends with a dot.
	int end = len -1 ;
	for(int i = precision; i > 0; i--) {
	  end = n.lastIndexOf('.', end-1);
	  if(end == -1)
	    return n;
	}
	return n.substring(end+1, len);
      }
    }
  }

  private class ClassNamePatternConverter extends NamedPatternConverter {

    ClassNamePatternConverter(FormattingInfo formattingInfo, int precision) {
      super(formattingInfo, precision);
    }

    String getFullyQualifiedName(PaxLoggingEvent event) {
      if(!event.locationInformationExists()){
        return "?";
      }
      return event.getLocationInformation().getClassName();
    }
  }

  private class CategoryPatternConverter extends NamedPatternConverter {

    CategoryPatternConverter(FormattingInfo formattingInfo, int precision) {
      super(formattingInfo, precision);
    }

    String getFullyQualifiedName(PaxLoggingEvent event) {
      return event.getLoggerName();
    }
  }

  private class MDCPatternConverter extends PatternConverter {
    String key;

    MDCPatternConverter(FormattingInfo formattingInfo, String key) {
      super(formattingInfo);
      this.key = key;
    }

    public
    String convert(PaxLoggingEvent event) {
        Map properties = event.getProperties();
        if (properties == null) {
          return null;
        }
        else if (key == null) {
            StringBuilder buf = new StringBuilder("{");
            if (properties.size() > 0) {
              Object[] keys = properties.keySet().toArray();
              Arrays.sort(keys);
              for (Object key : keys) {
                  buf.append('{');
                  buf.append(key);
                  buf.append(',');
                  buf.append(properties.get(key));
                  buf.append('}');
              }
            }
            buf.append('}');
            return buf.toString();
        } else {
          Object val = properties.get(key);
          if(val == null) {
              return null;
          } else {
              return val.toString();
          }
        }
    }

  }

  private static long startTime = 0;
  private static long getStartTime() {
      if (startTime == 0) {
          synchronized (PatternParser.class) {
              try {
                  startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
              } catch (Throwable t) {
                  startTime = System.currentTimeMillis();
              }
          }
      }
      return startTime;
  }
}

