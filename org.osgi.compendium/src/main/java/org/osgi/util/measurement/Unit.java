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

import java.util.Hashtable;

/**
 * A unit system for measurements.
 * 
 * This class contains definitions of the most common SI units.
 * <p>
 * 
 * <p>
 * This class only support exponents for the base SI units in the range -64 to
 * +63. Any operation which produces an exponent outside of this range will
 * result in a <code>Unit</code> object with undefined exponents.
 * 
 * @Immutable
 * @version $Revision: 5715 $
 */
/*
 * This local class maintains the information about units. It can calculate new
 * units when two values are multiplied, divided, added or subtracted. <p> The
 * unit works with the 7 basic SI types + rad + up to 2^6 custom types. For each
 * type, the unit keeps a bit mask with the exponents of the basic types. Eg.
 * m/s is m = 1, s = -1. Multiplying one unit with another means that the bit
 * masks are added, dividing means that the bit masks are subtracted. <p> This
 * class can handle any reasonable combination of SI units. However, it will
 * always try to coerce results back into the basic set. E.g. when you do V*A
 * you should get W and not m2.kg/s3 . Only when the existing types do not match
 * does the unit fallback to the expanded form. <p> This class uses offset
 * arithmetic. This means that the exponents are stored in an long. The special
 * field is used for units that should not be arithmetically divided or
 * multiplied, like longitude and lattitude. These special units can however, be
 * divided and multiplied by the basic 7 constants of the SI, e.g. deg/s.
 */
public class Unit {
	private final static long	UNITY		= createType(0, 0, 0, 0, 0, 0, 0,
													0, 0);
	private final static long	ZERO		= 0x40L;
	private final static long	MASK		= 0x7fL;
	private final static int	m_SHIFT		= 0;
	private final static int	s_SHIFT		= 7;
	private final static int	kg_SHIFT	= 14;
	private final static int	K_SHIFT		= 21;
	private final static int	A_SHIFT		= 28;
	private final static int	mol_SHIFT	= 35;
	private final static int	cd_SHIFT	= 42;
	private final static int	rad_SHIFT	= 49;
	private final static int	x_SHIFT		= 56;
	private final static long	x_MASK		= MASK << x_SHIFT;
	/** No Unit (Unity) */
	public final static Unit	unity		= new Unit("", UNITY);					// Unity
	/* SI Base Units */
	/** The length unit meter (m) */
	public final static Unit	m			= new Unit("m", createType(0, 0, 0,
													0, 0, 0, 0, 0, 1));			// Distance
	// meter
	/** The time unit second (s) */
	public final static Unit	s			= new Unit("s", createType(0, 0, 0,
													0, 0, 0, 0, 1, 0));			// Time
	// Seconds
	// s
	/** The mass unit kilogram (kg) */
	public final static Unit	kg			= new Unit("kg", createType(0, 0,
													0, 0, 0, 0, 1, 0, 0));			// Mass
	// kilogram
	// kg
	/** The temperature unit kelvin (K) */
	public final static Unit	K			= new Unit("K", createType(0, 0, 0,
													0, 0, 1, 0, 0, 0));			// Temperature
	// kelvin
	// K
	/** The electric current unit ampere (A) */
	public final static Unit	A			= new Unit("A", createType(0, 0, 0,
													0, 1, 0, 0, 0, 0));			// Current
	// ampere
	// A
	/** The amount of substance unit mole (mol) */
	public final static Unit	mol			= new Unit("mol", createType(0, 0,
													0, 1, 0, 0, 0, 0, 0));			// Substance
	// mole
	// mol
	/** The luminous intensity unit candela (cd) */
	public final static Unit	cd			= new Unit("cd", createType(0, 0,
													1, 0, 0, 0, 0, 0, 0));			// Light
	// candela
	// cd
	/* SI Derived Units */
	/** The speed unit meter per second (m/s) */
	public final static Unit	m_s			= new Unit("m/s", createType(0, 0,
													0, 0, 0, 0, 0, -1, 1));		// Speed
	// m/s
	/** The acceleration unit meter per second squared (m/s <sup>2 </sup>) */
	public final static Unit	m_s2		= new Unit("m/s2", createType(0, 0,
													0, 0, 0, 0, 0, -2, 1));		// Acceleration
	// m/s^2
	/** The area unit square meter(m <sup>2 </sup>) */
	public final static Unit	m2			= new Unit("m2", createType(0, 0,
													0, 0, 0, 0, 0, 0, 2));			// Surface
	// m^2
	/** The volume unit cubic meter (m <sup>3 </sup>) */
	public final static Unit	m3			= new Unit("m3", createType(0, 0,
													0, 0, 0, 0, 0, 0, 3));			// Volume
	// m^3
	/**
	 * The frequency unit hertz (Hz).
	 * <p>
	 * hertz is expressed in SI units as 1/s
	 */
	public final static Unit	Hz			= new Unit("Hz", createType(0, 0,
													0, 0, 0, 0, 0, -1, 0));		// Frequency
	// 1/s
	/**
	 * The force unit newton (N).
	 * <p>
	 * N is expressed in SI units as m&#183;kg/s <sup>2 </sup>
	 */
	public final static Unit	N			= new Unit("N", createType(0, 0, 0,
													0, 0, 0, 1, -2, 1));			// Force
	// newton
	// (m*kg)/s^2
	/**
	 * The pressure unit pascal (Pa).
	 * <p>
	 * Pa is equal to N/m <sup>2 </sup> or is expressed in SI units as
	 * kg/m&#183;s <sup>2 </sup>
	 */
	public final static Unit	Pa			= new Unit("Pa", createType(0, 0,
													0, 0, 0, 0, 1, -2, -1));		// Pressure
	// pascal
	// kg/(m*s^2)
	/**
	 * The energy unit joule (J).
	 * <p>
	 * joule is equal to N&#183;m or is expressed in SI units as m <sup>2
	 * </sup>&#183;kg/s <sup>2
	 */
	public final static Unit	J			= new Unit("J", createType(0, 0, 0,
													0, 0, 0, 1, -2, 2));			// Energy
	// joule
	// (m^2*kg)/s^2
	/**
	 * The power unit watt (W).
	 * <p>
	 * watt is equal to J/s or is expressed in SI units as m <sup>2
	 * </sup>&#183;kg/s <sup>3 </sup>
	 */
	public final static Unit	W			= new Unit("W", createType(0, 0, 0,
													0, 0, 0, 1, -3, 2));			// Power
	// watt
	// (m^2*kg)/s^3
	/**
	 * The electric charge unit coulomb (C).
	 * <p>
	 * coulomb is expressed in SI units as s&#183;A
	 */
	public final static Unit	C			= new Unit("C", createType(0, 0, 0,
													0, 1, 0, 0, 1, 0));			// Charge
	// coulumb
	// s*A
	/**
	 * The electric potential difference unit volt (V).
	 * <p>
	 * volt is equal to W/A or is expressed in SI units as m <sup>2
	 * </sup>&#183;kg/s <sup>3 </sup>&#183;A
	 */
	public final static Unit	V			= new Unit("V", createType(0, 0, 0,
													0, -1, 0, 1, -3, 2));			// El.
	// Potent.
	// volt
	// (m^2*kg)/(s^3*A)
	/**
	 * The capacitance unit farad (F).
	 * <p>
	 * farad is equal to C/V or is expressed in SI units as s <sup>4
	 * </sup>&#183;A <sup>2 </sup>/m <sup>2 </sup>&#183;kg
	 */
	public final static Unit	F			= new Unit("F", createType(0, 0, 0,
													0, 2, 0, -1, 4, -2));			// Capacitance
	// farad
	// (s^4*A^2)/(m^2*kg)
	/**
	 * The electric resistance unit ohm.
	 * <p>
	 * ohm is equal to V/A or is expressed in SI units as m <sup>2
	 * </sup>&#183;kg/s <sup>3 </sup>&#183;A <sup>2 </sup>
	 */
	public final static Unit	Ohm			= new Unit("Ohm", createType(0, 0,
													0, 0, -2, 0, 1, -3, 2));		// Resistance
	// ohm
	// (m^2*kg)/(s^3*A^2)
	/**
	 * The electric conductance unit siemens (S).
	 * <p>
	 * siemens is equal to A/V or is expressed in SI units as s <sup>3
	 * </sup>&#183;A <sup>2 </sup>/m <sup>2 </sup>&#183;kg
	 */
	public final static Unit	S			= new Unit("S", createType(0, 0, 0,
													0, 2, 0, -1, 3, -2));			// Conductance
	// siemens
	// (s^3*A^2)/(m^2*kg)
	/**
	 * The magnetic flux unit weber (Wb).
	 * <p>
	 * weber is equal to V&#183;s or is expressed in SI units as m <sup>2
	 * </sup>&#183;kg/s <sup>2 </sup>&#183;A
	 */
	public final static Unit	Wb			= new Unit("Wb", createType(0, 0,
													0, 0, -1, 0, 1, -2, 2));		// Magn.
	// Flux
	// weber
	// (m^2*kg)/(s^2*A)
	/**
	 * The magnetic flux density unit tesla (T).
	 * <p>
	 * tesla is equal to Wb/m <sup>2 </sup> or is expressed in SI units as kg/s
	 * <sup>2 </sup>&#183;A
	 */
	public final static Unit	T			= new Unit("T", createType(0, 0, 0,
													0, -1, 0, 1, -2, 0));			// Magn.
	// Flux
	// Dens.
	// tesla
	// kg/(s^2*A)
	/**
	 * The illuminance unit lux (lx).
	 * <p>
	 * lux is expressed in SI units as cd/m <sup>2 </sup>
	 */
	public final static Unit	lx			= new Unit("lx", createType(0, 0,
													1, 0, 0, 0, 0, 0, -2));		// Illuminace
	// lux
	// cd/m^2
	/**
	 * The absorbed dose unit gray (Gy).
	 * <p>
	 * Gy is equal to J/kg or is expressed in SI units as m <sup>2 </sup>/s
	 * <sup>2 </sup>
	 */
	public final static Unit	Gy			= new Unit("Gy", createType(0, 0,
													0, 0, 0, 0, 0, -2, 2));		// Absorbed
	// dose
	// gray
	// m^2/s^2
	/**
	 * The catalytic activity unit katal (kat).
	 * <p>
	 * katal is expressed in SI units as mol/s
	 */
	public final static Unit	kat			= new Unit("kat", createType(0, 0,
													0, 1, 0, 0, 0, -1, 0));		// Catalytic
	// Act.
	// katal
	// mol/s
	/** The angle unit radians (rad) */
	public final static Unit	rad			= new Unit("rad", createType(0, 1,
													0, 0, 0, 0, 0, 0, 0));			// Angle
	// radians
	// rad
	/**
	 * An array containing all units defined. The first seven items must be m,
	 * s, kg, K, A, mol, cd, rad in this order!
	 */
	private final static Unit[]	allUnits	= new Unit[] {m, s, kg, K, A, mol,
			cd, rad, m_s, m_s2, m2, m3, Hz, N, Pa, J, W, C, V, F, Ohm, S, Wb,
			T, lx, Gy, kat, unity			};
	
	/* @GuardedBy("this") */
	private static Hashtable	base;
	private final String		name;
	private final long			type;

	/**
	 * Creates a new <code>Unit</code> instance.
	 * 
	 * @param name the name of the <code>Unit</code>
	 * @param type the type of the <code>Unit</code>
	 */
	private Unit(String name, long type) {
		if (name == null) {
			name = computeName(type);
		}
		this.name = name;
		this.type = type;
		//System.out.println( name + " " + Long.toHexString( type ) );
	}

	/**
	 * Create a type field from the base SI unit exponent values.
	 *  
	 */
	private static long createType(int x, int rad, int cd, int mol, int A,
			int K, int kg, int s, int m) {
		return (((ZERO + m) & MASK) << m_SHIFT)
				| (((ZERO + s) & MASK) << s_SHIFT)
				| (((ZERO + kg) & MASK) << kg_SHIFT)
				| (((ZERO + K) & MASK) << K_SHIFT)
				| (((ZERO + A) & MASK) << A_SHIFT)
				| (((ZERO + mol) & MASK) << mol_SHIFT)
				| (((ZERO + cd) & MASK) << cd_SHIFT)
				| (((ZERO + rad) & MASK) << rad_SHIFT)
				| (((long) x) << x_SHIFT);
	}

	/**
	 * Checks whether this <code>Unit</code> object is equal to the specified
	 * <code>Unit</code> object. The <code>Unit</code> objects are considered equal
	 * if their exponents are equal.
	 * 
	 * @param obj the <code>Unit</code> object that should be checked for equality
	 * 
	 * @return true if the specified <code>Unit</code> object is equal to this
	 *         <code>Unit</code> object.
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Unit)) {
			return false;
		}
		return ((Unit) obj).type == type;
	}

	/**
	 * Returns the hash code for this object.
	 * 
	 * @return This object's hash code.
	 */
	public int hashCode() {
		return 31 * 17 + (int) (type ^ (type >>> 32));
	}

	/**
	 * Returns a new <code>Unit</code> that is the multiplication of this
	 * <code>Unit</code> and the <code>Unit</code> specified
	 * 
	 * @param that the <code>Unit</code> that will be multiplied with this
	 *        <code>Unit</code>
	 * 
	 * @return a new <code>Unit</code> that is the multiplication of this
	 *         <code>Unit</code> and the <code>Unit</code> specified
	 * 
	 * @throws RuntimeException if both <code>Unit</code> s are special
	 * 
	 * @see Unit#isSpecial
	 */
	Unit mul(Unit that) {
		if (this.isSpecial() && that.isSpecial()) {
			throw new ArithmeticException("Cannot multiply " + this + " with "
					+ that);
		}
		return find(this.type - UNITY + that.type);
	}

	/**
	 * Returns a new <code>Unit</code> that is the division of this <code>Unit</code>
	 * and the <code>Unit</code> specified
	 * 
	 * @param that the <code>Unit</code> that this <code>Unit</code> will be divided
	 *        with
	 * @return a new <code>Unit</code> that is the division of this <code>Unit</code>
	 *         and the <code>Unit</code> specified
	 * 
	 * @throws RuntimeException if both <code>Unit</code> s are special
	 * 
	 * @see Unit#isSpecial
	 */
	Unit div(Unit that) {
		if (this.isSpecial() && that.isSpecial()) {
			if (this.type == that.type) {
				return Unit.unity;
			}
			throw new ArithmeticException("Cannot divide " + this + " by "
					+ that);
		}
		return find(this.type - that.type + UNITY);
	}

	/**
	 * Returns a new <code>Unit</code> that is the addition of this <code>Unit</code>
	 * and the <code>Unit</code> specified.
	 * 
	 * @param that the <code>Unit</code> that should be added to this
	 *        <code>Unit</code>
	 * 
	 * @return a new <code>Unit</code> that is the addition of this <code>Unit</code>
	 *         and the <code>Unit</code> specified.
	 * 
	 * @throws RuntimeException if the two <code>Unit</code> s are not the same
	 */
	Unit add(Unit that) {
		if (!this.equals(that)) {
			throw new ArithmeticException("Cannot add " + this + " to " + that);
		}
		return this;
	}

	/**
	 * Returns a new <code>Unit</code> that is the subtraction between this
	 * <code>Unit</code> and the <code>Unit</code> specified.
	 * 
	 * @param that the <code>Unit</code> that will be subtracted from this
	 *        <code>Unit</code>
	 * @return a new <code>Unit</code> that is the subtraction between this
	 *         <code>Unit</code> and the <code>Unit</code> specified.
	 * 
	 * @throws RuntimeException if the <code>Unit</code> specified is not the same
	 *         as this <code>Unit</code>
	 */
	Unit sub(Unit that) {
		if (!this.equals(that)) {
			throw new ArithmeticException("Cannot subtract " + that + " from "
					+ this);
		}
		return this;
	}

	/**
	 * Finds a <code>Unit</code> based on a type. If the <code>Unit</code> is not
	 * found, it will be created and added to the list of all units under a null
	 * name.
	 * 
	 * @param type the type of the <code>Unit</code> to find
	 * 
	 * @return the <code>Unit</code>
	 */
	static synchronized Unit find(long type) {
		if (base == null) {
			int size = allUnits.length;
			base = new Hashtable(size << 1);
			for (int i = 0; i < size; i++) {
				base.put(allUnits[i], allUnits[i]);
			}
		}
		Unit unit = new Unit(null, type);
		Unit out = (Unit) base.get(unit);
		if (out == null) {
			base.put(unit, unit);
			out = unit;
		}
		return out;
	}

	/**
	 * Returns a <code>String</code> object representing the <code>Unit</code>
	 * 
	 * @return A <code>String</code> object representing the <code>Unit</code>
	 */
	public String toString() {
		return name;
	}

	private static String computeName(long type) {
		int m = (int) (((type >> m_SHIFT) & MASK) - ZERO);
		int s = (int) (((type >> s_SHIFT) & MASK) - ZERO);
		int kg = (int) (((type >> kg_SHIFT) & MASK) - ZERO);
		int K = (int) (((type >> K_SHIFT) & MASK) - ZERO);
		int A = (int) (((type >> A_SHIFT) & MASK) - ZERO);
		int mol = (int) (((type >> mol_SHIFT) & MASK) - ZERO);
		int cd = (int) (((type >> cd_SHIFT) & MASK) - ZERO);
		int rad = (int) (((type >> rad_SHIFT) & MASK) - ZERO);
		StringBuffer numerator = new StringBuffer();
		StringBuffer denominator = new StringBuffer();
		addSIname(m, "m", numerator, denominator);
		addSIname(s, "s", numerator, denominator);
		addSIname(kg, "kg", numerator, denominator);
		addSIname(K, "K", numerator, denominator);
		addSIname(A, "A", numerator, denominator);
		addSIname(mol, "mol", numerator, denominator);
		addSIname(cd, "cd", numerator, denominator);
		addSIname(rad, "rad", numerator, denominator);
		if (denominator.length() > 0) {
			if (numerator.length() == 0) {
				numerator.append("1");
			}
			numerator.append("/");
			numerator.append(denominator.toString()); 
		}
		return numerator.toString();
	}
	
	private static void addSIname(int si, String name, StringBuffer numerator,
			StringBuffer denominator) {
		if (si != 0) {
			StringBuffer sb = (si > 0) ? numerator : denominator;
			if (sb.length() > 0) {
				sb.append("*");
			}
			sb.append(name);
			int power = Math.abs(si);
			if (power > 1) {
				sb.append("^");
				sb.append(power);
			}
		}
	}

	/**
	 * Checks whether the unit has a special type, i.e. not a SI unit.
	 * 
	 * @return true if the type is special, otherwise false.
	 */
	private boolean isSpecial() {
		return (type & x_MASK) != 0;
	}
}
