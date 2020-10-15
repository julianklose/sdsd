package de.sdsd.projekt.prototype.data;

import java.time.Instant;

import de.sdsd.projekt.prototype.applogic.TableFunctions.Key;

/**
 * Represents a timelog value from cassandra.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see TimelogPosition
 * @see ValueInfo
 */
public class Timelog {
	
	/** The key. */
	public final Key key;
	
	/** The time. */
	public final Instant time;
	
	/** The value. */
	public final long value;

	/**
	 * Instantiates a new timelog.
	 *
	 * @param key the key
	 * @param time the time
	 * @param value the value
	 */
	public Timelog(Key key, Instant time, long value) {
		this.key = key;
		this.time = time;
		this.value = value;
	}
	
	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public Key getKey() {
		return key;
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
	 * Gets the value.
	 *
	 * @return the value
	 */
	public long getValue() {
		return value;
	}

}
