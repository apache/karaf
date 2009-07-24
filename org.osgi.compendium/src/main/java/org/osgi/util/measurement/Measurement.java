/*
 * Copyright (c) OSGi Alliance (2002, 2008). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.util.measurement;

/**
 * Represents a value with an error, a unit and a time-stamp.
 * 
 * <p>
 * A <code>Measurement</code> object is used for maintaining the tuple of value,
 * error, unit and time-stamp. The value and error are represented as doubles
 * and the time is measured in milliseconds since midnight, January 1, 1970 UTC.
 * 
 * <p>
 * Mathematic methods are provided that correctly calculate taking the error
 * into account. A runtime error will occur when two measurements are used in an
 * incompatible way. E.g., when a speed (m/s) is added to a distance (m). The
 * measurement class will correctly track changes in unit during multiplication
 * and division, always coercing the result to the most simple form. See
 * {@link Unit} for more information on the supported units.
 * 
 * <p>
 * Errors in the measurement class are absolute errors. Measurement errors
 * should use the P95 rule. Actual values must fall in the range value +/- error
 * 95% or more of the time.
 * 
 * <p>
 * A <code>Measurement</code> object is immutable in order to be easily shared.
 * 
 * <p>
 * Note: This class has a natural ordering that is inconsistent with equals. See
 * {@link #compareTo}.
 * 
 * @Immutable
 * @version $Revision: 5715 $
 */
public class Measurement implements Comparable {
	private final double				value;
	private final double				error;
	private final long					time;
	private final Unit					unit;
	private transient volatile String	name;
	private transient volatile int		hashCode;

	/**
	 * Create a new <code>Measurement</code> object.
	 * 
	 * @param value The value of the <code>Measurement</code>.
	 * @param error The error of the <code>Measurement</code>.
	 * @param unit The <code>Unit</code> object in which the value is measured. If
	 *        this argument is <code>null</code>, then the unit will be set to
	 *        {@link Unit#unity}.
	 * @param time The time measured in milliseconds since midnight, January 1,
	 *        1970 UTC.
	 */
	public Measurement(double value, double error, Unit unit, long time) {
		this.value = value;
		this.error = Math.abs(error);
		this.unit = (unit != null) ? unit : Unit.unity;
		this.time = time;
	}

	/**
	 * Create a new <code>Measurement</code> object with a time of zero.
	 * 
	 * @param value The value of the <code>Measurement</code>.
	 * @param error The error of the <code>Measurement</code>.
	 * @param unit The <code>Unit</code> object in which the value is measured. If
	 *        this argument is <code>null</code>, then the unit will be set to
	 *        {@link Unit#unity}.
	 */
	public Measurement(double value, double error, Unit unit) {
		this(value, error, unit, 0l);
	}

	/**
	 * Create a new <code>Measurement</code> object with an error of 0.0 and a
	 * time of zero.
	 * 
	 * @param value The value of the <code>Measurement</code>.
	 * @param unit The <code>Unit</code> in which the value is measured. If this
	 *        argument is <code>null</code>, then the unit will be set to
	 *        {@link Unit#unity}.
	 */
	public Measurement(double value, Unit unit) {
		this(value, 0.0d, unit, 0l);
	}

	/**
	 * Create a new <code>Measurement</code> object with an error of 0.0, a unit
	 * of {@link Unit#unity} and a time of zero.
	 * 
	 * @param value The value of the <code>Measurement</code>.
	 */
	public Measurement(double value) {
		this(value, 0.0d, null, 0l);
	}

	/**
	 * Returns the value of this <code>Measurement</code> object.
	 * 
	 * @return The value of this <code>Measurement</code> object as a double.
	 */
	public final double getValue() {
		return value;
	}

	/**
	 * Returns the error of this <code>Measurement</code> object. The error is
	 * always a positive value.
	 * 
	 * @return The error of this <code>Measurement</code> as a double.
	 */
	public final double getError() {
		return error;
	}

	/**
	 * Returns the <code>Unit</code> object of this <code>Measurement</code> object.
	 * 
	 * @return The <code>Unit</code> object of this <code>Measurement</code> object.
	 * 
	 * @see Unit
	 */
	public final Unit getUnit() {
		return unit;
	}

	/**
	 * Returns the time at which this <code>Measurement</code> object was taken.
	 * The time is measured in milliseconds since midnight, January 1, 1970 UTC,
	 * or zero when not defined.
	 * 
	 * @return The time at which this <code>Measurement</code> object was taken or
	 *         zero.
	 */
	public final long getTime() {
		return time;
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the product of this
	 * object multiplied by the specified object.
	 * 
	 * @param m The <code>Measurement</code> object that will be multiplied with
	 *        this object.
	 * @return A new <code>Measurement</code> that is the product of this object
	 *         multiplied by the specified object. The error and unit of the new
	 *         object are computed. The time of the new object is set to the
	 *         time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be multiplied.
	 * @see Unit
	 */
	public Measurement mul(Measurement m) {
		double mvalue = m.value;
		return new Measurement(value * mvalue, Math.abs(value) * m.error
				+ error * Math.abs(mvalue), unit.mul(m.unit), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the product of this
	 * object multiplied by the specified value.
	 * 
	 * @param d The value that will be multiplied with this object.
	 * @param u The <code>Unit</code> of the specified value.
	 * @return A new <code>Measurement</code> object that is the product of this
	 *         object multiplied by the specified value. The error and unit of
	 *         the new object are computed. The time of the new object is set to
	 *         the time of this object.
	 * @throws ArithmeticException If the units of this object and the specified
	 *         value cannot be multiplied.
	 * @see Unit
	 */
	public Measurement mul(double d, Unit u) {
		return new Measurement(value * d, error * Math.abs(d), unit.mul(u),
				time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the product of this
	 * object multiplied by the specified value.
	 * 
	 * @param d The value that will be multiplied with this object.
	 * @return A new <code>Measurement</code> object that is the product of this
	 *         object multiplied by the specified value. The error of the new
	 *         object is computed. The unit and time of the new object is set to
	 *         the unit and time of this object.
	 */
	public Measurement mul(double d) {
		return new Measurement(value * d, error * Math.abs(d), unit, time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the quotient of this
	 * object divided by the specified object.
	 * 
	 * @param m The <code>Measurement</code> object that will be the divisor of
	 *        this object.
	 * @return A new <code>Measurement</code> object that is the quotient of this
	 *         object divided by the specified object. The error and unit of the
	 *         new object are computed. The time of the new object is set to the
	 *         time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be divided.
	 * @see Unit
	 */
	public Measurement div(Measurement m) {
		double mvalue = m.value;
		return new Measurement(value / mvalue,
				(Math.abs(value) * m.error + error * Math.abs(mvalue))
						/ (mvalue * mvalue), unit.div(m.unit), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the quotient of this
	 * object divided by the specified value.
	 * 
	 * @param d The value that will be the divisor of this object.
	 * @param u The <code>Unit</code> object of the specified value.
	 * @return A new <code>Measurement</code> that is the quotient of this object
	 *         divided by the specified value. The error and unit of the new
	 *         object are computed. The time of the new object is set to the
	 *         time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be divided.
	 * @see Unit
	 */
	public Measurement div(double d, Unit u) {
		return new Measurement(value / d, error / Math.abs(d), unit.div(u),
				time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the quotient of this
	 * object divided by the specified value.
	 * 
	 * @param d The value that will be the divisor of this object.
	 * @return A new <code>Measurement</code> object that is the quotient of this
	 *         object divided by the specified value. The error of the new
	 *         object is computed. The unit and time of the new object is set to
	 *         the <code>Unit</code> and time of this object.
	 */
	public Measurement div(double d) {
		return new Measurement(value / d, error / Math.abs(d), unit, time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the sum of this
	 * object added to the specified object.
	 * 
	 * The error and unit of the new object are computed. The time of the new
	 * object is set to the time of this object.
	 * 
	 * @param m The <code>Measurement</code> object that will be added with this
	 *        object.
	 * @return A new <code>Measurement</code> object that is the sum of this and
	 *         m.
	 * @see Unit
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be added.
	 */
	public Measurement add(Measurement m) {
		return new Measurement(value + m.value, error + m.error, unit
				.add(m.unit), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the sum of this
	 * object added to the specified value.
	 * 
	 * @param d The value that will be added with this object.
	 * @param u The <code>Unit</code> object of the specified value.
	 * @return A new <code>Measurement</code> object that is the sum of this
	 *         object added to the specified value. The unit of the new object
	 *         is computed. The error and time of the new object is set to the
	 *         error and time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified value cannot be added.
	 * @see Unit
	 */
	public Measurement add(double d, Unit u) {
		return new Measurement(value + d, error, unit.add(u), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the sum of this
	 * object added to the specified value.
	 * 
	 * @param d The value that will be added with this object.
	 * @return A new <code>Measurement</code> object that is the sum of this
	 *         object added to the specified value. The error, unit, and time of
	 *         the new object is set to the error, <code>Unit</code> and time of
	 *         this object.
	 */
	public Measurement add(double d) {
		return new Measurement(value + d, error, unit, time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the subtraction of
	 * the specified object from this object.
	 * 
	 * @param m The <code>Measurement</code> object that will be subtracted from
	 *        this object.
	 * @return A new <code>Measurement</code> object that is the subtraction of
	 *         the specified object from this object. The error and unit of the
	 *         new object are computed. The time of the new object is set to the
	 *         time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be subtracted.
	 * @see Unit
	 */
	public Measurement sub(Measurement m) {
		return new Measurement(value - m.value, error + m.error, unit
				.sub(m.unit), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the subtraction of
	 * the specified value from this object.
	 * 
	 * @param d The value that will be subtracted from this object.
	 * @param u The <code>Unit</code> object of the specified value.
	 * @return A new <code>Measurement</code> object that is the subtraction of
	 *         the specified value from this object. The unit of the new object
	 *         is computed. The error and time of the new object is set to the
	 *         error and time of this object.
	 * @throws ArithmeticException If the <code>Unit</code> objects of this object
	 *         and the specified object cannot be subtracted.
	 * @see Unit
	 */
	public Measurement sub(double d, Unit u) {
		return new Measurement(value - d, error, unit.sub(u), time);
	}

	/**
	 * Returns a new <code>Measurement</code> object that is the subtraction of
	 * the specified value from this object.
	 * 
	 * @param d The value that will be subtracted from this object.
	 * @return A new <code>Measurement</code> object that is the subtraction of
	 *         the specified value from this object. The error, unit and time of
	 *         the new object is set to the error, <code>Unit</code> object and
	 *         time of this object.
	 */
	public Measurement sub(double d) {
		return new Measurement(value - d, error, unit, time);
	}

	/**
	 * Returns a <code>String</code> object representing this <code>Measurement</code>
	 * object.
	 * 
	 * @return a <code>String</code> object representing this <code>Measurement</code>
	 *         object.
	 */
	public String toString() {
		String result = name;
		if (result == null) {
			StringBuffer sb = new StringBuffer();
			sb.append(value);
			if (error != 0.0d) {
				sb.append(" +/- ");
				sb.append(error);
			}
			String u = unit.toString();
			if (u.length() > 0) {
				sb.append(" ");
				sb.append(u);
			}
			result = sb.toString();
			name = result;
		}
		return result;
	}

	/**
	 * Compares this object with the specified object for order. Returns a
	 * negative integer, zero, or a positive integer if this object is less
	 * than, equal to, or greater than the specified object.
	 * 
	 * <p>
	 * Note: This class has a natural ordering that is inconsistent with equals.
	 * For this method, another <code>Measurement</code> object is considered
	 * equal if there is some <code>x</code> such that
	 * 
	 * <pre>
	 * getValue() - getError() &lt;= x &lt;= getValue() + getError()
	 * </pre>
	 * 
	 * for both <code>Measurement</code> objects being compared.
	 * 
	 * @param obj The object to be compared.
	 * @return A negative integer, zero, or a positive integer if this object is
	 *         less than, equal to, or greater than the specified object.
	 * 
	 * @throws ClassCastException If the specified object is not of type
	 *         <code>Measurement</code>.
	 * @throws ArithmeticException If the unit of the specified
	 *         <code>Measurement</code> object is not equal to the <code>Unit</code>
	 *         object of this object.
	 */
	public int compareTo(Object obj) {
		if (this == obj) {
			return 0;
		}
		Measurement that = (Measurement) obj;
		if (!unit.equals(that.unit)) {
			throw new ArithmeticException("Cannot compare " + this + " and "
					+ that);
		}
		int result = Double.compare(value, that.value);
		if (result == 0) {
			return 0;
		}
		if (result < 0) {
			if (Double.compare(value + error, that.value - that.error) >= 0) {
				return 0;
			}
			return -1;
		}
		if (Double.compare(value - error, that.value + that.error) <= 0) {
			return 0;
		}
		return 1;
	}

	/**
	 * Returns a hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 */
	public int hashCode() {
		int h = hashCode;
		if (h == 0) {
			long bits = Double.doubleToLongBits(value);
			h = 31 * 17 + ((int) (bits ^ (bits >>> 32)));
			bits = Double.doubleToLongBits(error);
			h = 31 * h + ((int) (bits ^ (bits >>> 32)));
			h = 31 * h + unit.hashCode();
			hashCode = h;
		}
		return h;
	}

	/**
	 * Returns whether the specified object is equal to this object. Two
	 * <code>Measurement</code> objects are equal if they have same value, error
	 * and <code>Unit</code>.
	 * 
	 * <p>
	 * Note: This class has a natural ordering that is inconsistent with equals.
	 * See {@link #compareTo}.
	 * 
	 * @param obj The object to compare with this object.
	 * @return <code>true</code> if this object is equal to the specified object;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Measurement)) {
			return false;
		}
		Measurement that = (Measurement) obj;
		return (Double.compare(value, that.value) == 0)
				&& (Double.compare(error, that.error) == 0)
				&& unit.equals(that.unit);
	}
}
