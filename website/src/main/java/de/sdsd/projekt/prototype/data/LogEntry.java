package de.sdsd.projekt.prototype.data;

import java.time.Instant;
import java.util.Date;

import org.bson.Document;

/**
 * Represents an user specific logging entry, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class LogEntry implements Comparable<LogEntry> {
	
	/** The Constant TEXT. */
	public static final String TIME = "time", LEVEL = "level", TEXT = "text";
	
	/**
	 * Creates the.
	 *
	 * @param time the time
	 * @param level the level
	 * @param text the text
	 * @return the document
	 */
	public static Document create(Instant time, String level, String text) {
		return new Document()
				.append(TIME, Date.from(time))
				.append(LEVEL, level)
				.append(TEXT, text);
	}
	
	/** The time. */
	private final Instant time;
	
	/** The level. */
	private final String level;
	
	/** The text. */
	private final String text;

	/**
	 * Instantiates a new log entry.
	 *
	 * @param doc the doc
	 */
	public LogEntry(Document doc) {
		this.time = doc.getDate(TIME).toInstant();
		this.level = doc.getString(LEVEL);
		this.text = doc.getString(TEXT);
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
	 * Gets the level.
	 *
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * Gets the text.
	 *
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "LogEntry [time=" + time + ", level=" + level + ", text=" + text + "]";
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(LogEntry o) {
		return time.compareTo(o.time);
	}
}
