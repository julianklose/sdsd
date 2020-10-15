package de.sdsd.projekt.prototype.data;

import java.time.Instant;

import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;

/**
 * Represents a timelog position from cassandra.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see Timelog
 */
public class TimelogPosition implements Comparable<TimelogPosition> {
	
	/** The time. */
	public final Instant time;
	
	/** The pos. */
	public final Coordinate pos;
	
	/**
	 * Instantiates a new timelog position.
	 *
	 * @param time the time
	 * @param pos the pos
	 */
	public TimelogPosition(Instant time, Coordinate pos) {
		this.time = time;
		this.pos = pos;
	}
	
	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public Instant getTime() {
		return time;
	}
	
	/**
	 * Gets the pos.
	 *
	 * @return the pos
	 */
	public Coordinate getPos() {
		return pos;
	}

	/**
	 * Checks if is valid.
	 *
	 * @return true, if is valid
	 */
	public boolean isValid() {
		return (pos.x != 0 || pos.y != 0) 
				&& pos.x <= 180. && pos.x >= -180.
				&& pos.y < 90. && pos.y > -90.;
	}
	
	/**
	 * To geo json.
	 *
	 * @return the JSON array
	 */
	public JSONArray toGeoJson() {
		return new JSONArray()
				.put(pos.x)
				.put(pos.y)
				.put(Double.isNaN(pos.getZ()) ? 0 : pos.getZ())
				.put(time.toEpochMilli());
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Position [time=" + time + ", latitude=" + pos.y + ", longitude=" + pos.x + ", altitude="
				+ pos.getZ() + "]";
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(TimelogPosition o) {
		return this.time.compareTo(o.time);
	}
}
