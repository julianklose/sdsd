package de.sdsd.projekt.prototype.data;

import java.time.Instant;

import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;

/**
 * Represents a timelog value from cassandra.
 * 
 * @see TimelogPosition
 * @see ValueInfo
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class Timelog {
	
	public final Key key;
	public final Instant time;
	public final long value;

	public Timelog(Key key, Instant time, long value) {
		this.key = key;
		this.time = time;
		this.value = value;
	}
	
	public Key getKey() {
		return key;
	}

	public Instant getTime() {
		return time;
	}

	public long getValue() {
		return value;
	}

}
