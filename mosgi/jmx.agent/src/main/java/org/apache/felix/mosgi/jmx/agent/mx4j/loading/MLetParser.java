/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.loading;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.loading.MLet;

/**
 * The parser for MLet files, as specified in the JMX documentation.
 * This parser is case insensitive regards to the MLet tags: MLET is equal to mlet and to MLet.
 * This parser also supports XML-style comments in the file.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MLetParser
{
   public static final String OPEN_COMMENT = "<!--";
   public static final String CLOSE_COMMENT = "-->";

   public static final String OPEN_BRACKET = "<";
   public static final String CLOSE_BRACKET = ">";

   public static final String MLET_TAG = "MLET";
   public static final String CODE_ATTR = "CODE";
   public static final String OBJECT_ATTR = "OBJECT";
   public static final String ARCHIVE_ATTR = "ARCHIVE";
   public static final String CODEBASE_ATTR = "CODEBASE";
   public static final String NAME_ATTR = "NAME";
   public static final String VERSION_ATTR = "VERSION";

   public static final String ARG_TAG = "ARG";
   public static final String TYPE_ATTR = "TYPE";
   public static final String VALUE_ATTR = "VALUE";

   private MLet mlet;

   /**
    * Creates a new MLetParser
    */
   public MLetParser()
   {
   }

   /**
    * Creates a new MLetParser
    * @param mlet The MLet used to resolve classes specified in the ARG tags.
    */
   public MLetParser(MLet mlet)
   {
      this.mlet = mlet;
   }

   /**
    * Parses the given content, that must contains a valid MLet file.
    *
    * @param content The content to parse
    * @return A list of {@link MLetTag}s
    * @throws MLetParseException If the content is not a valid MLet file
    */
   public List parse(String content) throws MLetParseException
   {
      if (content == null) throw new MLetParseException("MLet file content cannot be null");

      // Strip comments
      content = stripComments(content.trim());
      content = convertToUpperCase(content);

      ArrayList mlets = parseMLets(content);
      if (mlets.size() < 1) throw new MLetParseException("MLet file is empty");

      ArrayList mletTags = new ArrayList();
      for (int i = 0; i < mlets.size(); ++i)
      {
         String mletTag = (String)mlets.get(i);

         MLetTag tag = parseMLet(mletTag);
         mletTags.add(tag);
      }

      return mletTags;
   }

   private MLetTag parseMLet(String content) throws MLetParseException
   {
      MLetTag tag = new MLetTag();
      parseMLetAttributes(tag, content);
      parseMLetArguments(tag, content);
      return tag;
   }

   private ArrayList parseMLets(String content) throws MLetParseException
   {
      ArrayList list = new ArrayList();
      int start = 0;
      int current = -1;
      while ((current = findOpenTag(content, start, MLET_TAG)) >= 0)
      {
         int end = findCloseTag(content, current + 1, MLET_TAG, true);
         if (end < 0) throw new MLetParseException("MLET tag not closed at index: " + current);

         String mlet = content.substring(current, end);
         list.add(mlet);

         start = end + 1;
      }
      return list;
   }

   private void parseMLetArguments(MLetTag tag, String content) throws MLetParseException
   {
      int start = 0;
      int current = -1;
      while ((current = findOpenTag(content, start, ARG_TAG)) >= 0)
      {
         int end = findCloseTag(content, current + 1, ARG_TAG, false);
         if (end < 0) throw new MLetParseException("ARG tag not closed");

         String arg = content.substring(current, end);

         int type = arg.indexOf(TYPE_ATTR);
         if (type < 0) throw new MLetParseException("Missing TYPE attribute");

         int value = arg.indexOf(VALUE_ATTR);
         if (value < 0) throw new MLetParseException("Missing VALUE attribute");

         String className = findAttributeValue(arg, type, TYPE_ATTR);
         tag.addArg(className, convertToObject(className, findAttributeValue(arg, value, VALUE_ATTR)));

         start = end + 1;
      }
   }

   private void parseMLetAttributes(MLetTag tag, String content) throws MLetParseException
   {
      int end = content.indexOf(CLOSE_BRACKET);
      String attributes = content.substring(0, end);

      // Find mandatory attributes
      int archive = -1;
      int object = -1;
      int code = -1;

      archive = attributes.indexOf(ARCHIVE_ATTR);
      if (archive < 0) throw new MLetParseException("Missing ARCHIVE attribute");

      code = attributes.indexOf(CODE_ATTR);
      object = attributes.indexOf(OBJECT_ATTR);
      if (code < 0 && object < 0) throw new MLetParseException("Missing CODE or OBJECT attribute");
      if (code > 0 && object > 0) throw new MLetParseException("CODE and OBJECT attributes cannot be both present");

      if (code >= 0)
         tag.setCode(findAttributeValue(attributes, code, CODE_ATTR));
      else
         tag.setObject(findAttributeValue(attributes, object, OBJECT_ATTR));

      tag.setArchive(findAttributeValue(attributes, archive, ARCHIVE_ATTR));

      // Look for optional attributes
      int codebase = attributes.indexOf(CODEBASE_ATTR);
      if (codebase >= 0) tag.setCodeBase(findAttributeValue(attributes, codebase, CODEBASE_ATTR));

      int name = attributes.indexOf(NAME_ATTR);
      if (name >= 0)
      {
         String objectName = findAttributeValue(attributes, name, NAME_ATTR);
         try
         {
            tag.setName(new ObjectName(objectName));
         }
         catch (MalformedObjectNameException x)
         {
            throw new MLetParseException("Invalid ObjectName: " + objectName);
         }
      }

      int version = attributes.indexOf(VERSION_ATTR);
      if (version >= 0) tag.setVersion(findAttributeValue(attributes, version, VERSION_ATTR));
   }

   private String findAttributeValue(String content, int start, String attribute) throws MLetParseException
   {
      int equal = content.indexOf('=', start);
      if (equal < 0) throw new MLetParseException("Missing '=' for attribute");

      // Ensure no garbage
      if (!attribute.equals(content.substring(start, equal).trim())) throw new MLetParseException("Invalid attribute");

      int begin = content.indexOf('"', equal + 1);
      if (begin < 0) throw new MLetParseException("Missing quotes for attribute value");

      // Ensure no garbage
      if (content.substring(equal + 1, begin).trim().length() != 0) throw new MLetParseException("Invalid attribute value");

      int end = content.indexOf('"', begin + 1);
      if (end < 0) throw new MLetParseException("Missing quote for attribute value");

      return content.substring(begin + 1, end).trim();
   }

   private int findOpenTag(String content, int start, String tag)
   {
      String opening = new StringBuffer(OPEN_BRACKET).append(tag).toString();
      return content.indexOf(opening, start);
   }

   private int findCloseTag(String content, int start, String tag, boolean strictSyntax)
   {
      int count = 1;

      do
      {
         int close = content.indexOf(CLOSE_BRACKET, start);
         if (close < 0)
         {
            return -1;
         }
         int open = content.indexOf(OPEN_BRACKET, start);
         if (open >= 0 && close > open)
         {
            ++count;
         }
         else
         {
            --count;
            if (count == 0)
            {
               // Either I found the closing bracket of the open tag,
               // or the closing tag
               if (!strictSyntax || (strictSyntax && content.charAt(close - 1) == '/'))
               {
                  // Found the closing tag
                  return close + 1;
               }
               else
               {
                  // Found the closing bracket of the open tag, go for the full closing tag
                  String closing = new StringBuffer(OPEN_BRACKET).append("/").append(tag).append(CLOSE_BRACKET).toString();
                  close = content.indexOf(closing, start);
                  if (close < 0)
                     return -1;
                  else
                     return close + closing.length();
               }
            }
         }

         start = close + 1;
      }
      while (true);
   }

   private String stripComments(String content) throws MLetParseException
   {
      StringBuffer buffer = new StringBuffer();
      int start = 0;
      int current = -1;
      while ((current = content.indexOf(OPEN_COMMENT, start)) >= 0)
      {
         int end = content.indexOf(CLOSE_COMMENT, current + 1);

         if (end < 0) throw new MLetParseException("Missing close comment tag at index: " + current);

         String stripped = content.substring(start, current);
         buffer.append(stripped);
         start = end + CLOSE_COMMENT.length();
      }
      String stripped = content.substring(start, content.length());
      buffer.append(stripped);
      return buffer.toString();
   }

   private String convertToUpperCase(String content) throws MLetParseException
   {
      StringBuffer buffer = new StringBuffer();
      int start = 0;
      int current = -1;
      while ((current = content.indexOf("\"", start)) >= 0)
      {
         int end = content.indexOf("\"", current + 1);

         if (end < 0) throw new MLetParseException("Missing closing quote at index: " + current);

         String converted = content.substring(start, current).toUpperCase();
         buffer.append(converted);
         String quoted = content.substring(current, end + 1);
         buffer.append(quoted);
         start = end + 1;
      }
      String converted = content.substring(start, content.length()).toUpperCase();
      buffer.append(converted);
      return buffer.toString();
   }

   private Object convertToObject(String clsName, String value) throws MLetParseException
   {
      try
      {
         if (clsName.equals("boolean") || clsName.equals("java.lang.Boolean"))
            return Boolean.valueOf(value);
         else if (clsName.equals("byte") || clsName.equals("java.lang.Byte"))
            return Byte.valueOf(value);
         else if (clsName.equals("char") || clsName.equals("java.lang.Character"))
         {
            char ch = 0;
            if (value.length() > 0) ch = value.charAt(0);
            return new Character(ch);
         }
         else if (clsName.equals("short") || clsName.equals("java.lang.Short"))
            return Short.valueOf(value);
         else if (clsName.equals("int") || clsName.equals("java.lang.Integer"))
            return Integer.valueOf(value);
         else if (clsName.equals("long") || clsName.equals("java.lang.Long"))
            return Long.valueOf(value);
         else if (clsName.equals("float") || clsName.equals("java.lang.Float"))
            return Float.valueOf(value);
         else if (clsName.equals("double") || clsName.equals("java.lang.Double"))
            return Double.valueOf(value);
         else if (clsName.equals("java.lang.String"))
            return value;
         else if (mlet != null)
         {
            try
            {
               Class cls = mlet.loadClass(clsName);
               Constructor ctor = cls.getConstructor(new Class[]{String.class});
               return ctor.newInstance(new Object[]{value});
            }
            catch (Exception ignored)
            {
            }
         }
      }
      catch (NumberFormatException x)
      {
         throw new MLetParseException("Invalid value: " + value);
      }
      return null;
   }
}
