package de.sdsd.projekt.prototype.data;

import java.time.Instant;

import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;

/**
 * Represents a timelog position from cassandra.
 * 
 * @see Timelog
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class TimelogPosition implements Comparable<TimelogPosition> {
	
	public final Instant time;
	public final Coordinate pos;
	
	public TimelogPosition(Instant time, Coordinate pos) {
		this.time = time;
		this.pos = pos;
	}
	
	public Instant getTime() {
		return time;
	}
	
	public Coordinate getPos() {
		return pos;
	}

	public boolean isValid() {
		return (pos.x != 0 || pos.y != 0) 
				&& pos.x <= 180. && pos.x >= -180.
				&& pos.y < 90. && pos.y > -90.;
	}
	
	public JSONArray toGeoJson() {
		return new JSONArray()
				.put(pos.x)
				.put(pos.y)
				.put(Double.isNaN(pos.getZ()) ? 0 : pos.getZ())
				.put(time.toEpochMilli());
	}

	@Override
	public String toString() {
		return "Position [time=" + time + ", latitude=" + pos.y + ", longitude=" + pos.x + ", altitude="
				+ pos.getZ() + "]";
	}

	@Override
	public int compareTo(TimelogPosition o) {
		return this.time.compareTo(o.time);
	}
}
