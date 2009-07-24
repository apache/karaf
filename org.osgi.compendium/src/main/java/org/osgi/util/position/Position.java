/*
 * Copyright (c) OSGi Alliance (2002, 2009). All Rights Reserved.
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
package org.osgi.util.position;

import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;

/**
 * Position represents a geographic location, based on the WGS84 System (World
 * Geodetic System 1984).
 * <p>
 * The <code>org.osgi.util.measurement.Measurement</code> class is used to
 * represent the values that make up a position.
 * <p>
 * <p>
 * A given position object may lack any of it's components, i.e. the altitude
 * may not be known. Such missing values will be represented by null.
 * <p>
 * Position does not override the implementation of either equals() or
 * hashCode() because it is not clear how missing values should be handled. It
 * is up to the user of a position to determine how best to compare two position
 * objects. A <code>Position</code> object is immutable.
 * 
 * @Immutable
 * @version $Revision: 6860 $
 */
public class Position {
	private final Measurement	altitude;
	private final Measurement	longitude;
	private final Measurement	latitude;
	private final Measurement	speed;
	private final Measurement	track;

	/**
	 * Constructs a <code>Position</code> object with the given values.
	 * 
	 * @param lat a <code>Measurement</code> object specifying the latitude in
	 *        radians, or null
	 * @param lon a <code>Measurement</code> object specifying the longitude in
	 *        radians, or null
	 * @param alt a <code>Measurement</code> object specifying the altitude in
	 *        meters, or null
	 * @param speed a <code>Measurement</code> object specifying the speed in
	 *        meters per second, or null
	 * @param track a <code>Measurement</code> object specifying the track in
	 *        radians, or null
	 */
	public Position(Measurement lat, Measurement lon, Measurement alt,
			Measurement speed, Measurement track) {
		if (lat != null) {
			if (!Unit.rad.equals(lat.getUnit())) {
				throw new IllegalArgumentException("Invalid Latitude");
			}
		}
		if (lon != null) {
			if (!Unit.rad.equals(lon.getUnit())) {
				throw new IllegalArgumentException("Invalid Longitude");
			}
		}
		if (alt != null) {
			if (!Unit.m.equals(alt.getUnit())) {
				throw new IllegalArgumentException("Invalid Altitude");
			}
		}
		if (speed != null) {
			if (!Unit.m_s.equals(speed.getUnit())) {
				throw new IllegalArgumentException("Invalid Speed");
			}
		}
		if (track != null) {
			if (!Unit.rad.equals(track.getUnit())) {
				throw new IllegalArgumentException("Invalid Track");
			}
		}

		/*
		 * Verify the longitude and latitude parameters so they fit the normal
		 * coordinate system. A latitude is between -90 (south) and +90 (north).
		 * A longitude is between -180 (Western hemisphere) and +180 (eastern
		 * hemisphere). This method first normalizes the latitude and longitude
		 * between +/- 180. If the |latitude| > 90, then the longitude is added
		 * 180 and the latitude is normalized to fit +/-90. (Example are with
		 * degrees though radians are used) <br> No normalization takes place
		 * when either lon or lat is null.
		 */
		normalizeLatLon: {
			if (lat == null || lon == null) {
				break normalizeLatLon;
			}
			double dlat = lat.getValue();
			double dlon = lon.getValue();
			if (dlon >= -LON_RANGE && dlon < LON_RANGE && dlat >= -LAT_RANGE
					&& dlat <= LAT_RANGE) {
				break normalizeLatLon;
			}
			dlon = normalize(dlon, LON_RANGE);
			dlat = normalize(dlat, LAT_RANGE * 2.0D); // First over 180 degree
			// Check if we have to move to other side of the earth
			if (dlat > LAT_RANGE || dlat < -LAT_RANGE) {
				dlon = normalize(dlon - LON_RANGE, LON_RANGE);
				dlat = normalize((LAT_RANGE * 2.0D) - dlat, LAT_RANGE);
			}
			lon = new Measurement(dlon, lon.getError(), lon.getUnit(), lon
					.getTime());
			lat = new Measurement(dlat, lat.getError(), lat.getUnit(), lat
					.getTime());
		}

		/*
		 * Normalize track to be a value such that: 0 <= value < +2PI. This
		 * corresponds to 0 deg to +360 deg. 0 is North, 0.5PI is East, PI is
		 * South, 1.5PI is West
		 */
		normalizeTrack: {
			if (track == null) {
				break normalizeTrack;
			}
			double dtrack = track.getValue();
			if ((0.0D <= dtrack) && (dtrack < TRACK_RANGE)) {
				break normalizeTrack; /* value is already normalized */
			}
			dtrack %= TRACK_RANGE;
			if (dtrack < 0.0D) {
				dtrack += TRACK_RANGE;
			}
			track = new Measurement(dtrack, track.getError(), track.getUnit(),
					track.getTime());
		}

		this.latitude = lat;
		this.longitude = lon;
		this.altitude = alt;
		this.speed = speed;
		this.track = track;
	}

	/**
	 * Returns the altitude of this position in meters.
	 * 
	 * @return a <code>Measurement</code> object in <code>Unit.m</code> representing
	 *         the altitude in meters above the ellipsoid <code>null</code> if the
	 *         altitude is not known.
	 */
	public Measurement getAltitude() {
		return altitude;
	}

	/**
	 * Returns the longitude of this position in radians.
	 * 
	 * @return a <code>Measurement</code> object in <code>Unit.rad</code>
	 *         representing the longitude, or <code>null</code> if the longitude
	 *         is not known.
	 */
	public Measurement getLongitude() {
		return longitude;
	}

	/**
	 * Returns the latitude of this position in radians.
	 * 
	 * @return a <code>Measurement</code> object in <code>Unit.rad</code>
	 *         representing the latitude, or <code>null</code> if the latitude is
	 *         not known..
	 */
	public Measurement getLatitude() {
		return latitude;
	}

	/**
	 * Returns the ground speed of this position in meters per second.
	 * 
	 * @return a <code>Measurement</code> object in <code>Unit.m_s</code>
	 *         representing the speed, or <code>null</code> if the speed is not
	 *         known..
	 */
	public Measurement getSpeed() {
		return speed;
	}

	/**
	 * Returns the track of this position in radians as a compass heading. The
	 * track is the extrapolation of previous previously measured positions to a
	 * future position.
	 * 
	 * @return a <code>Measurement</code> object in <code>Unit.rad</code>
	 *         representing the track, or <code>null</code> if the track is not
	 *         known..
	 */
	public Measurement getTrack() {
		return track;
	}

	private static final double	LON_RANGE	= Math.PI;
	private static final double	LAT_RANGE	= Math.PI / 2.0D;

	/**
	 * This function normalizes the a value according to a range. This is not
	 * simple modulo (as I thought when I started), but requires some special
	 * handling. For positive numbers we subtract 2*range from the number so
	 * that end up between -/+ range. For negative numbers we add this value.
	 * For example, if the value is 270 and the range is +/- 180. Then sign=1 so
	 * the (int) factor becomes 270+180/360 = 1. This means that 270-360=-90 is
	 * the result. (degrees are only used to make it easier to understand, this
	 * function is agnostic for radians/degrees). The result will be in
	 * [range,range&gt; The algorithm is not very fast, but it handling the
	 * [&gt; ranges made it very messy using integer arithmetic, and this is
	 * very readable. Note that it is highly unlikely that this method is called
	 * in normal situations. Normally input values to position are already
	 * normalized because they come from a GPS. And this is much more readable.
	 * 
	 * @param value The value that needs adjusting
	 * @param range -range = < value < range
	 */
	private static double normalize(double value, double range) {
		double twiceRange = 2.0D * range;
		while (value >= range) {
			value -= twiceRange;
		}
		while (value < -range) {
			value += twiceRange;
		}
		return value;
	}

	private static final double	TRACK_RANGE	= Math.PI * 2.0D;
}
