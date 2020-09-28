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
	public static final String TIME = "time", LEVEL = "level", TEXT = "text";
	
	public static Document create(Instant time, String level, String text) {
		return new Document()
				.append(TIME, Date.from(time))
				.append(LEVEL, level)
				.append(TEXT, text);
	}
	
	private final Instant time;
	private final String level;
	private final String text;

	public LogEntry(Document doc) {
		this.time = doc.getDate(TIME).toInstant();
		this.level = doc.getString(LEVEL);
		this.text = doc.getString(TEXT);
	}

	public Instant getTime() {
		return time;
	}

	public String getLevel() {
		return level;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return "LogEntry [time=" + time + ", level=" + level + ", text=" + text + "]";
	}

	@Override
	public int compareTo(LogEntry o) {
		return time.compareTo(o.time);
	}
}
