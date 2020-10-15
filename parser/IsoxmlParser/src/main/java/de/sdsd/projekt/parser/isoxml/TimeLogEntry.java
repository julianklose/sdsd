package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.xml.sax.SAXException;

import de.sdsd.projekt.parser.isoxml.TimeLog.GpsTime;
import de.sdsd.projekt.parser.isoxml.TimeLog.ValueDescription;

/**
 * Represents a single row of a isoxml timelog.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class TimeLogEntry {

	/** The config. */
	private final TimeLogConfig config;

	/** The header. */
	private final Object[] header;

	/** The values. */
	private final Integer[] values;

	/** The error. */
	private String error = null;

	/**
	 * Instantiates a new time log entry.
	 *
	 * @param config the config
	 * @param data   the data
	 * @param last   the last
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public TimeLogEntry(TimeLogConfig config, ByteBuffer data, @Nullable TimeLogEntry last) throws IOException {
		this.config = config;
		List<Attribute<?>> headerlist = config.getHeader();
		this.header = new Object[headerlist.size()];
		this.values = new Integer[config.getDlvs().size()];

		try {
			int startpos = data.position();
			GpsTime lasttime = last != null ? (GpsTime) last.header[0] : null;
			GpsTime thistime = TimeLog.findNextValidTime(data, lasttime);
			header[0] = thistime;
			if (data.position() > startpos + 6)
				error = "Invalid entry found";
			else if (lasttime != null) {
				if (thistime.ms == lasttime.ms && thistime.days == lasttime.days)
					error = "Same timestamp as before";
				else if (thistime.ms < lasttime.ms && thistime.days == lasttime.days)
					error = (lasttime.ms - thistime.ms) + "ms earlier timestamp than before";
			}
			for (int i = 1; i < header.length; ++i) {
				Attribute<?> attr = headerlist.get(i);
				header[i] = TimeLog.headerRead(attr, data);
			}

			int dlvs = Byte.toUnsignedInt(data.get());
			for (int i = 0; i < dlvs; ++i) {
				int index = Byte.toUnsignedInt(data.get());
				values[index] = data.getInt();
			}
		} catch (BufferUnderflowException e) {
			error = "Input data incomplete";
			data.position(data.limit());
		} catch (IndexOutOfBoundsException e) {
			error = "Input data invalid";
		}
	}

	/**
	 * Gets the config.
	 *
	 * @return the config
	 */
	TimeLogConfig getConfig() {
		return config;
	}

	/**
	 * Gets the header count.
	 *
	 * @return the header count
	 */
	public int getHeaderCount() {
		return header.length;
	}

	/**
	 * Gets the head.
	 *
	 * @param <T>  the generic type
	 * @param name the name
	 * @param cls  the cls
	 * @return the head
	 */
	@CheckForNull
	public <T> T getHead(String name, Class<T> cls) {
		List<Attribute<?>> list = config.getHeader();
		for (int i = 0; i < list.size(); ++i) {
			if (list.get(i).getName().equalsIgnoreCase(name))
				return getHead(i, cls);
		}
		return null;
	}

	/**
	 * Gets the head.
	 *
	 * @param <T>   the generic type
	 * @param index the index
	 * @param cls   the cls
	 * @return the head
	 */
	public <T> T getHead(int index, Class<T> cls) {
		if (index == 0 && header[index] != null && cls.isAssignableFrom(Instant.class))
			return cls.cast(((GpsTime) header[index]).toLocalInstant());
		if (index >= 0 && index < header.length && cls.isInstance(header[index]))
			return cls.cast(header[index]);
		else
			return null;
	}

	/**
	 * Size.
	 *
	 * @return the int
	 */
	public int size() {
		return values.length;
	}

	/**
	 * Checks for value.
	 *
	 * @param index the index
	 * @return true, if successful
	 */
	public boolean hasValue(int index) {
		return index >= 0 && index < values.length && values[index] != null;
	}

	/**
	 * Gets the value.
	 *
	 * @param index the index
	 * @return the value
	 */
	public int getValue(int index) {
		return values[index].intValue();
	}

	/**
	 * Gets the value description.
	 *
	 * @param index the index
	 * @return the value description
	 * @throws SAXException the SAX exception
	 */
	public ValueDescription getValueDescription(int index) throws SAXException {
		return config.getValueDescriptions().get(index);
	}

	/**
	 * Gets the scaled value.
	 *
	 * @param index the index
	 * @return the scaled value
	 * @throws IOException  Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 */
	public double getScaledValue(int index) throws IOException, SAXException {
		ValueDescription vd = getValueDescription(index);
		if (vd != null)
			return vd.translateValue(values[index].longValue());
		else
			return values[index].doubleValue();
	}

	/**
	 * Gets the formatted value.
	 *
	 * @param index the index
	 * @return the formatted value
	 * @throws IOException  Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 */
	public String getFormattedValue(int index) throws IOException, SAXException {
		ValueDescription vd = getValueDescription(index);
		return vd.formatValue(getScaledValue(index)) + ' ' + vd.getUnit();
	}

	/**
	 * Checks for error.
	 *
	 * @return true, if successful
	 */
	public boolean hasError() {
		return error != null;
	}

	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	public String getError() {
		return error;
	}
}