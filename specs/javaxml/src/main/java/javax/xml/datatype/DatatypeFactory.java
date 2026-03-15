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
package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DatatypeFactory {

    public static final String DATATYPEFACTORY_PROPERTY = "javax.xml.datatype.DatatypeFactory";

    private static final String DATATYPEFACTORY_IMPLEMENTATION_CLASS = "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl";

    private static final Pattern XDTSCHEMA_YMD = Pattern.compile("[^DT]*");

    private static final Pattern XDTSCHEMA_DTD = Pattern.compile("[^YM]*[DT].*");


    protected DatatypeFactory() {
    }


    public static DatatypeFactory newDefaultInstance() {
        try {
            return $FactoryFinder.newInstance(DatatypeFactory.class, DATATYPEFACTORY_IMPLEMENTATION_CLASS, null, false, true);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Unable to create default instance", e);
        }
    }

    public static DatatypeFactory newInstance() throws DatatypeConfigurationException {
        return $FactoryFinder.find(DatatypeFactory.class, DATATYPEFACTORY_IMPLEMENTATION_CLASS);
    }


    public static DatatypeFactory newInstance(String factoryClassName, ClassLoader classLoader) throws DatatypeConfigurationException {
        return $FactoryFinder.newInstance(DatatypeFactory.class, factoryClassName, classLoader, false);
    }

    public abstract Duration newDuration(final String lexicalRepresentation);

    public abstract Duration newDuration(final long durationInMilliSeconds);

    public abstract Duration newDuration(final boolean isPositive, final BigInteger years, final BigInteger months,
                                         final BigInteger days, final BigInteger hours, final BigInteger minutes, final BigDecimal seconds);

    public Duration newDuration(final boolean isPositive, final int years, final int months,
                                final int days, final int hours, final int minutes, final int seconds) {
        BigInteger realYears = (years != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) years) : null;
        BigInteger realMonths = (months != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) months) : null;
        BigInteger realDays = (days != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) days) : null;
        BigInteger realHours = (hours != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) hours) : null;
        BigInteger realMinutes = (minutes != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) minutes) : null;
        BigDecimal realSeconds = (seconds != DatatypeConstants.FIELD_UNDEFINED) ? BigDecimal.valueOf((long) seconds) : null;
        return newDuration(isPositive, realYears, realMonths, realDays, realHours, realMinutes, realSeconds);
    }


    public Duration newDurationDayTime(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException("Trying to create an xdt:dayTimeDuration with an invalid lexical representation of \"null\"");
        }
        Matcher matcher = XDTSCHEMA_DTD.matcher(lexicalRepresentation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Trying to create an xdt:dayTimeDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires years and months only.");
        }
        return newDuration(lexicalRepresentation);
    }

    public Duration newDurationDayTime(final long durationInMilliseconds) {
        return newDuration(durationInMilliseconds);
    }

    public Duration newDurationDayTime(final boolean isPositive, final BigInteger day,
                                       final BigInteger hour, final BigInteger minute, final BigInteger second) {
        return newDuration(isPositive, null, null,
                           day, hour, minute, (second != null) ? new BigDecimal(second) : null);
    }

    public Duration newDurationDayTime(final boolean isPositive, final int day,
                                       final int hour, final int minute, final int second) {
        return newDurationDayTime(isPositive, BigInteger.valueOf((long) day), BigInteger.valueOf((long) hour),
                                  BigInteger.valueOf((long) minute), BigInteger.valueOf((long) second));
    }

    public Duration newDurationYearMonth(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"null\"");
        }
        Matcher matcher = XDTSCHEMA_YMD.matcher(lexicalRepresentation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires days and times only.");
        }
        return newDuration(lexicalRepresentation);
    }


    public Duration newDurationYearMonth(final long durationInMilliseconds) {
        Duration fullDuration = newDuration(durationInMilliseconds);
        boolean isPositive = fullDuration.getSign() != -1;
        BigInteger years = (BigInteger) fullDuration.getField(DatatypeConstants.YEARS);
        if (years == null) {
            years = BigInteger.ZERO;
        }
        BigInteger months = (BigInteger) fullDuration.getField(DatatypeConstants.MONTHS);
        if (months == null) {
            months = BigInteger.ZERO;
        }
        return newDurationYearMonth(isPositive, years, months);
    }


    public Duration newDurationYearMonth(final boolean isPositive, final BigInteger year, final BigInteger month) {
        return newDuration(isPositive, year, month,
                           null, null, null, null);
    }


    public Duration newDurationYearMonth(final boolean isPositive, final int year, final int month) {
        return newDurationYearMonth(isPositive, BigInteger.valueOf((long) year), BigInteger.valueOf((long) month));
    }

    public abstract XMLGregorianCalendar newXMLGregorianCalendar();

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(final String lexicalRepresentation);

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(final GregorianCalendar cal);

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(
            final BigInteger year,
            final int month,
            final int day,
            final int hour,
            final int minute,
            final int second,
            final BigDecimal fractionalSecond,
            final int timezone);

    public XMLGregorianCalendar newXMLGregorianCalendar(
            final int year,
            final int month,
            final int day,
            final int hour,
            final int minute,
            final int second,
            final int millisecond,
            final int timezone) {
        BigInteger realYear = (year != DatatypeConstants.FIELD_UNDEFINED) ? BigInteger.valueOf((long) year) : null;
        BigDecimal realMillisecond = null;
        if (millisecond != DatatypeConstants.FIELD_UNDEFINED) {
            if (millisecond < 0 || millisecond > 1000) {
                throw new IllegalArgumentException(
                        "javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendar("
                                + "int year, int month, int day, int hour, int minute, int second, int millisecond, int timezone)"
                                + "with invalid millisecond: " + millisecond
                );
            }
            realMillisecond = BigDecimal.valueOf((long) millisecond).movePointLeft(3);
        }
        return newXMLGregorianCalendar(realYear, month,
                day,
                hour,
                minute,
                second,
                realMillisecond,
                timezone
        );
    }


    public XMLGregorianCalendar newXMLGregorianCalendarDate(
            final int year,
            final int month,
            final int day,
            final int timezone) {

        return newXMLGregorianCalendar(
                year,
                month,
                day,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }


    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final int timezone) {

        return newXMLGregorianCalendar(
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }


    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final BigDecimal fractionalSecond,
            final int timezone) {

        return newXMLGregorianCalendar(
                null,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                fractionalSecond,
                timezone);
    }


    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final int milliseconds,
            final int timezone) {


        BigDecimal realMilliseconds = null;
        if (milliseconds != DatatypeConstants.FIELD_UNDEFINED) {
            if (milliseconds < 0 || milliseconds > 1000) {
                throw new IllegalArgumentException(
                        "javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendarTime("
                                + "int hours, int minutes, int seconds, int milliseconds, int timezone)"
                                + "with invalid milliseconds: " + milliseconds
                );
            }

            realMilliseconds = BigDecimal.valueOf((long) milliseconds).movePointLeft(3);
        }

        return newXMLGregorianCalendarTime(
                hours,
                minutes,
                seconds,
                realMilliseconds,
                timezone
        );
    }
}
