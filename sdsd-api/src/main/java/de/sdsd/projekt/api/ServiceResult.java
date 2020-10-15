package de.sdsd.projekt.api;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The Class ServiceResult.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public final class ServiceResult {

	/**
	 * Instantiates a new service result.
	 */
	private ServiceResult() {
	}

	/**
	 * The Class SDSDFile.
	 */
	public static class SDSDFile {

		/** The metadata. */
		public final SDSDFileMeta metadata;

		/** The content. */
		public final byte[] content;

		/**
		 * Instantiates a new SDSD file.
		 *
		 * @param file the file
		 */
		SDSDFile(JSONObject file) {
			this.metadata = new SDSDFileMeta(file);
			this.content = Base64.getDecoder().decode(file.getString("content"));
		}

		/**
		 * Gets the metadata.
		 *
		 * @return the metadata
		 */
		public SDSDFileMeta getMetadata() {
			return metadata;
		}

		/**
		 * Gets the content.
		 *
		 * @return the content
		 */
		public byte[] getContent() {
			return content;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(metadata);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFile))
				return false;
			SDSDFile other = (SDSDFile) obj;
			return Objects.equals(metadata, other.metadata);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "SDSDFile [metadata=" + metadata + ", content=byte[]]";
		}
	}

	/**
	 * The Class SDSDFileAppended.
	 */
	public static class SDSDFileAppended {

		/** The metadata. */
		public final SDSDFileMeta metadata;

		/** The appended. */
		public final byte[] appended;

		/**
		 * Instantiates a new SDSD file appended.
		 *
		 * @param file the file
		 */
		SDSDFileAppended(JSONObject file) {
			this.metadata = new SDSDFileMeta(file);
			this.appended = Base64.getDecoder().decode(file.getString("appended"));
		}

		/**
		 * Gets the metadata.
		 *
		 * @return the metadata
		 */
		public SDSDFileMeta getMetadata() {
			return metadata;
		}

		/**
		 * Gets the appended.
		 *
		 * @return the appended
		 */
		public byte[] getAppended() {
			return appended;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(metadata);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFileAppended))
				return false;
			SDSDFileAppended other = (SDSDFileAppended) obj;
			return Objects.equals(metadata, other.metadata);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "SDSDFileAppended [metadata=" + metadata + ", appended=byte[]]";
		}
	}

	/**
	 * The Class SDSDFileMeta.
	 */
	public static class SDSDFileMeta {

		/** The uri. */
		public final String uri;

		/** The filename. */
		public final String filename;

		/** The source. */
		public final String source;

		/** The created. */
		public final Instant created;

		/** The lastmodified. */
		public final Instant lastmodified;

		/** The leveraged. */
		public final Instant leveraged;

		/** The expires. */
		public final Instant expires;

		/** The size. */
		public final long size;

		/** The type. */
		public final String type;

		/**
		 * Instantiates a new SDSD file meta.
		 *
		 * @param file the file
		 */
		SDSDFileMeta(JSONObject file) {
			this.uri = file.getString("uri");
			this.filename = file.getString("filename");
			this.source = file.optString("source");
			String date = file.optString("created");
			this.created = date.isEmpty() ? null : Instant.parse(date);
			date = file.optString("lastmodified");
			this.lastmodified = date.isEmpty() ? null : Instant.parse(date);
			date = file.optString("leveraged");
			this.leveraged = date.isEmpty() ? null : Instant.parse(date);
			date = file.optString("expires");
			this.expires = date.isEmpty() ? null : Instant.parse(date);
			this.size = file.getLong("size");
			this.type = file.getString("type");
		}

		/**
		 * Gets the uri.
		 *
		 * @return the uri
		 */
		public String getUri() {
			return uri;
		}

		/**
		 * Gets the filename.
		 *
		 * @return the filename
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * Gets the source.
		 *
		 * @return the source
		 */
		public String getSource() {
			return source;
		}

		/**
		 * Gets the created.
		 *
		 * @return the created
		 */
		public Instant getCreated() {
			return created;
		}

		/**
		 * Gets the lastmodified.
		 *
		 * @return the lastmodified
		 */
		public Instant getLastmodified() {
			return lastmodified;
		}

		/**
		 * Gets the leveraged.
		 *
		 * @return the leveraged
		 */
		public Instant getLeveraged() {
			return leveraged;
		}

		/**
		 * Gets the expires.
		 *
		 * @return the expires
		 */
		public Instant getExpires() {
			return expires;
		}

		/**
		 * Gets the size.
		 *
		 * @return the size
		 */
		public long getSize() {
			return size;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFileMeta))
				return false;
			SDSDFileMeta other = (SDSDFileMeta) obj;
			return Objects.equals(uri, other.uri);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "SDSDFileMeta [uri=" + uri + ", filename=" + filename + ", source=" + source + ", created=" + created
					+ ", lastmodified=" + lastmodified + ", leveraged=" + leveraged + ", expires=" + expires + ", size="
					+ size + ", type=" + type + "]";
		}
	}

	/**
	 * The Class SDSDObject.
	 */
	public static class SDSDObject {

		/** The res. */
		private final Resource res;

		/**
		 * Instantiates a new SDSD object.
		 *
		 * @param res the res
		 */
		SDSDObject(JSONObject res) {
			Model model = ModelFactory.createDefaultModel();
			model.read(new StringReader(res.toString()), null, "JSON-LD");
			this.res = model.getResource(res.getString("@id"));
		}

		/**
		 * Gets the resouce.
		 *
		 * @return the resouce
		 */
		public Resource getResouce() {
			return res;
		}

		/**
		 * Gets the label.
		 *
		 * @return the label
		 */
		public String getLabel() {
			Statement stmt = res.getProperty(RDFS.label);
			return stmt != null ? stmt.getLiteral().getString() : "";
		}

		/**
		 * Gets the uri.
		 *
		 * @param p the p
		 * @return the uri
		 */
		public Optional<String> getUri(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getResource().getURI()) : Optional.empty();
		}

		/**
		 * Gets the parent uri.
		 *
		 * @return the parent uri
		 */
		public Optional<String> getParentUri() {
			return getUri(DCTerms.isPartOf);
		}

		/**
		 * Gets the value.
		 *
		 * @param p the p
		 * @return the value
		 */
		public Optional<Object> getValue(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral().getValue()) : Optional.empty();
		}

		/**
		 * Gets the string.
		 *
		 * @param p the p
		 * @return the string
		 */
		public Optional<String> getString(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral().getString()) : Optional.empty();
		}

		/**
		 * Gets the timestamp.
		 *
		 * @param p the p
		 * @return the timestamp
		 */
		public Optional<Instant> getTimestamp(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(((Calendar) stmt.getLiteral().getValue()).toInstant()) : Optional.empty();
		}

		/**
		 * Gets the literal.
		 *
		 * @param p the p
		 * @return the literal
		 */
		public Optional<Literal> getLiteral(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral()) : Optional.empty();
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			StringWriter sw = new StringWriter();
			res.getModel().write(sw, "JSON-LD");
			return sw.toString();
		}
	}

	/**
	 * The Class Attr.
	 */
	public static class Attr {

		/** The value. */
		public final String value;

		/** The type. */
		public final Optional<Resource> type;

		/**
		 * Instantiates a new attr.
		 *
		 * @param res the res
		 */
		Attr(JSONObject res) {
			this.value = res.getString("value");
			this.type = res.has("type") ? Optional.of(ResourceFactory.createResource(res.getString("type")))
					: Optional.empty();
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public Optional<Resource> getType() {
			return type;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return (type.isPresent() ? type.get().getURI() + ": " : "") + value;
		}
	}

	/**
	 * The Class FindResult.
	 */
	public static class FindResult {

		/** The uri. */
		public final String uri;

		/** The type. */
		public final Optional<Resource> type;

		/** The res. */
		private final JSONObject res;

		/**
		 * Instantiates a new find result.
		 *
		 * @param res the res
		 */
		FindResult(JSONObject res) {
			this.res = res;
			this.uri = (String) res.remove("uri");
			this.type = res.has("type") ? Optional.of(ResourceFactory.createResource((String) res.remove("type")))
					: Optional.empty();
		}

		/**
		 * Gets the uri.
		 *
		 * @return the uri
		 */
		public String getUri() {
			return uri;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public Optional<Resource> getType() {
			return type;
		}

		/**
		 * Attr set.
		 *
		 * @return the sets the
		 */
		public Set<String> attrSet() {
			return res.keySet();
		}

		/**
		 * Gets the attribute.
		 *
		 * @param attribute the attribute
		 * @return the attribute
		 */
		public Optional<String> getAttribute(Property attribute) {
			return Optional.ofNullable(res.optString(attribute.getURI()));
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(uri);
			if (type.isPresent())
				sb.append('(').append(type.get().getURI()).append(')');
			if (res.length() > 0)
				sb.append(": ").append(res.toString());
			return sb.toString();
		}
	}

	/**
	 * The Class DeviceElementProperties.
	 */
	public static class DeviceElementProperties extends HashMap<Integer, DeviceProperty> {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -4539049533668765872L;

		/**
		 * Instantiates a new device element properties.
		 *
		 * @param res the res
		 */
		DeviceElementProperties(JSONObject res) {
			for (String key : res.keySet()) {
				DeviceProperty dpt = new DeviceProperty(res.getJSONObject(key));
				put(dpt.ddi, dpt);
			}
		}
	}

	/**
	 * The Class TimeInterval.
	 */
	public static class TimeInterval {

		/** The until. */
		public final Instant from, until;

		/**
		 * Instantiates a new time interval.
		 *
		 * @param from  the from
		 * @param until the until
		 */
		public TimeInterval(Instant from, Instant until) {
			this.from = from;
			this.until = until;
		}

		/**
		 * Gets the from.
		 *
		 * @return the from
		 */
		public Instant getFrom() {
			return from;
		}

		/**
		 * Gets the until.
		 *
		 * @return the until
		 */
		public Instant getUntil() {
			return until;
		}

		/**
		 * To json.
		 *
		 * @return the JSON object
		 */
		@CheckForNull
		public JSONObject toJson() {
			JSONObject out = new JSONObject();
			if (from != null && from.isAfter(Instant.EPOCH))
				out.put("from", from.toString());
			if (until != null && until.isBefore(Instant.now()))
				out.put("until", until.toString());
			return out.length() > 0 ? out : null;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(from, until);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeInterval))
				return false;
			TimeInterval other = (TimeInterval) obj;
			return Objects.equals(from, other.from) && Objects.equals(until, other.until);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeInterval [from=" + from + ", until=" + until + "]";
		}
	}

	/**
	 * The Class DeviceProperty.
	 */
	public static class DeviceProperty {

		/** The ddi. */
		public final int ddi;

		/** The value. */
		public final int value;

		/** The info. */
		public final ValueInfo info;

		/**
		 * Instantiates a new device property.
		 *
		 * @param res the res
		 */
		DeviceProperty(JSONObject res) {
			this.ddi = res.getInt("ddi");
			this.value = res.getInt("value");
			this.info = new ValueInfo(res);
		}

		/**
		 * Gets the ddi.
		 *
		 * @return the ddi
		 */
		public int getDdi() {
			return ddi;
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		public int getValue() {
			return value;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "DeviceProperty [ddi=" + ddi + ", value=" + value + ", info=" + info.toString() + "]";
		}

	}

	/**
	 * The Interface TimedValue.
	 */
	public static interface TimedValue {

		/**
		 * Gets the time.
		 *
		 * @return the time
		 */
		public Instant getTime();
	}

	/**
	 * The Class TimedValueList.
	 *
	 * @param <T> the generic type
	 */
	public static class TimedValueList<T extends TimedValue> extends AbstractList<T> {

		/** The list. */
		protected final List<T> list;

		/**
		 * Instantiates a new timed value list.
		 *
		 * @param list the list
		 */
		public TimedValueList(List<T> list) {
			this.list = list;
		}

		/**
		 * Gets the.
		 *
		 * @param index the index
		 * @return the t
		 */
		@Override
		public T get(int index) {
			return list.get(index);
		}

		/**
		 * Size.
		 *
		 * @return the int
		 */
		@Override
		public int size() {
			return list.size();
		}

		/**
		 * Ascending.
		 *
		 * @return the ascending timed value list
		 */
		public AscendingTimedValueList<T> ascending() {
			return new AscendingTimedValueList<T>(list);
		}

		/**
		 * Ascending iterator.
		 *
		 * @return the ascending iterator
		 */
		public AscendingIterator<T> ascendingIterator() {
			return new AscendingIterator<T>(list);
		}

	}

	/**
	 * The Class AscendingIterator.
	 *
	 * @param <T> the generic type
	 */
	public static class AscendingIterator<T extends TimedValue> implements Iterator<T> {

		/** The list. */
		final List<T> list;

		/** The index. */
		int index;

		/**
		 * Instantiates a new ascending iterator.
		 *
		 * @param list the list
		 */
		AscendingIterator(List<T> list) {
			this.list = list;
			this.index = list.size() - 1;
		}

		/**
		 * Checks for next.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean hasNext() {
			return index >= 0;
		}

		/**
		 * Next.
		 *
		 * @return the t
		 */
		@Override
		public T next() {
			return list.get(index--);
		}

		/**
		 * Returns the value for the given time. If there is a value for the given time,
		 * it is returned. If there is no value, the last known value is returned. If
		 * there were no values before this time, the value is not present.
		 * 
		 * @param time a given instant (mostly from the positions list)
		 * @return the last known value, if available
		 */
		public Optional<T> nextUntil(Instant time) {
			T entry = null;
			for (int i = Math.min(index + 1, list.size() - 1); i >= 0; index = --i) {
				T e = list.get(i);
				if (e.getTime().isAfter(time))
					break;
				entry = e;
			}
			return Optional.ofNullable(entry);
		}

	}

	/**
	 * The Class AscendingTimedValueList.
	 *
	 * @param <T> the generic type
	 */
	public static class AscendingTimedValueList<T extends TimedValue> extends AbstractList<T> {

		/** The list. */
		private final List<T> list;

		/**
		 * Instantiates a new ascending timed value list.
		 *
		 * @param list the list
		 */
		AscendingTimedValueList(List<T> list) {
			this.list = list;
		}

		/**
		 * Gets the.
		 *
		 * @param index the index
		 * @return the t
		 */
		@Override
		public T get(int index) {
			return list.get(list.size() - index - 1);
		}

		/**
		 * Size.
		 *
		 * @return the int
		 */
		@Override
		public int size() {
			return list.size();
		}

		/**
		 * Iterator.
		 *
		 * @return the iterator
		 */
		@Override
		public Iterator<T> iterator() {
			return new AscendingIterator<T>(list);
		}
	}

	/**
	 * The Class Position.
	 */
	public static class Position {

		/** The longitude. */
		public final double latitude, longitude;
		/**
		 * Can be NaN.
		 */
		public final double altitude;

		/**
		 * Instantiates a new position.
		 *
		 * @param latitude  the latitude
		 * @param longitude the longitude
		 * @param altitude  the altitude
		 */
		public Position(double latitude, double longitude, double altitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
		}

		/**
		 * Gets the latitude.
		 *
		 * @return the latitude
		 */
		public double getLatitude() {
			return latitude;
		}

		/**
		 * Gets the longitude.
		 *
		 * @return the longitude
		 */
		public double getLongitude() {
			return longitude;
		}

		/**
		 * Gets the altitude.
		 *
		 * @return The altitude or NaN, if there is no altitude.
		 */
		public double getAltitude() {
			return altitude;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(altitude, latitude, longitude);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Position))
				return false;
			Position other = (Position) obj;
			return Double.doubleToLongBits(altitude) == Double.doubleToLongBits(other.altitude)
					&& Double.doubleToLongBits(latitude) == Double.doubleToLongBits(other.latitude)
					&& Double.doubleToLongBits(longitude) == Double.doubleToLongBits(other.longitude);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "Position [latitude: " + latitude + "° / longitude: " + longitude + "°, altitude: " + altitude
					+ "m]";
		}

	}

	/**
	 * The Class TimedPosition.
	 */
	public static class TimedPosition extends Position implements TimedValue {

		/** The time. */
		public final Instant time;

		/**
		 * Instantiates a new timed position.
		 *
		 * @param res the res
		 */
		TimedPosition(JSONObject res) {
			super(res.getDouble("latitude"), res.getDouble("longitude"), res.optDouble("altitude"));
			this.time = Instant.parse(res.getString("time"));
		}

		/**
		 * Instantiates a new timed position.
		 *
		 * @param time      the time
		 * @param latitude  the latitude
		 * @param longitude the longitude
		 * @param altitude  the altitude
		 */
		public TimedPosition(Instant time, double latitude, double longitude, double altitude) {
			super(latitude, longitude, altitude);
			this.time = time;
		}

		/**
		 * Gets the time.
		 *
		 * @return the time
		 */
		@Override
		public Instant getTime() {
			return time;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(time);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimedPosition))
				return false;
			TimedPosition other = (TimedPosition) obj;
			return Objects.equals(time, other.time);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimedPosition [time=" + time + ", latitude: " + latitude + "° / longitude: " + longitude
					+ "°, altitude: " + altitude + "m]";
		}
	}

	/**
	 * The Class Positions.
	 */
	public static class Positions extends TimedValueList<TimedPosition> {

		/**
		 * Instantiates a new positions.
		 *
		 * @param res the res
		 */
		Positions(JSONObject res) {
			super(ServiceAPI.result(res.getJSONArray("positions"), TimedPosition::new));
		}

	}

	/**
	 * The Interface TimeLogCreator.
	 *
	 * @param <R> the generic type
	 */
	@FunctionalInterface
	public interface TimeLogCreator<R extends TimeLogKey> {

		/**
		 * Apply.
		 *
		 * @param elementKey the element key
		 * @param valueUri   the value uri
		 * @param res        the res
		 * @return the r
		 */
		R apply(TimeLogElementKey<R> elementKey, String valueUri, JSONObject res);
	}

	/**
	 * The Class TimeLogKeys.
	 *
	 * @param <T> the generic type
	 */
	public static class TimeLogKeys<T extends TimeLogKey> {

		/** The files. */
		public final List<TimeLogFileKey<T>> files;

		/**
		 * Instantiates a new time log keys.
		 *
		 * @param res     the res
		 * @param creator the creator
		 */
		TimeLogKeys(JSONObject res, TimeLogCreator<T> creator) {
			List<TimeLogFileKey<T>> files = new ArrayList<>(res.length());
			for (String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if (obj != null)
					files.add(new TimeLogFileKey<>(this, key, obj, creator));
			}
			this.files = Collections.unmodifiableList(files);
		}

		/**
		 * Stream all.
		 *
		 * @return the stream
		 */
		public Stream<T> streamAll() {
			return files.stream().flatMap(TimeLogFileKey::streamAll);
		}

		/**
		 * Stream elements.
		 *
		 * @return the stream
		 */
		public Stream<TimeLogElementKey<T>> streamElements() {
			return files.stream().flatMap(f -> f.elements.stream());
		}
	}

	/**
	 * The Class TimeLogFileKey.
	 *
	 * @param <T> the generic type
	 */
	public static class TimeLogFileKey<T extends TimeLogKey> {

		/** The keys. */
		public final TimeLogKeys<T> keys;

		/** The file uri. */
		public final String fileUri;

		/** The elements. */
		public final List<TimeLogElementKey<T>> elements;

		/**
		 * Instantiates a new time log file key.
		 *
		 * @param keys    the keys
		 * @param fileUri the file uri
		 * @param res     the res
		 * @param creator the creator
		 */
		TimeLogFileKey(TimeLogKeys<T> keys, String fileUri, JSONObject res, TimeLogCreator<T> creator) {
			this.keys = keys;
			this.fileUri = fileUri;

			List<TimeLogElementKey<T>> elements = new ArrayList<>(res.length());
			for (String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if (obj != null)
					elements.add(new TimeLogElementKey<>(this, key, obj, creator));
			}
			this.elements = Collections.unmodifiableList(elements);
		}

		/**
		 * Stream all.
		 *
		 * @return the stream
		 */
		public Stream<T> streamAll() {
			return elements.stream().flatMap(TimeLogElementKey::streamAll);
		}

		/**
		 * Gets the file uri.
		 *
		 * @return the file uri
		 */
		public String getFileUri() {
			return fileUri;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(fileUri);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogFileKey))
				return false;
			TimeLogFileKey<?> other = (TimeLogFileKey<?>) obj;
			return Objects.equals(fileUri, other.fileUri);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogFileKey [fileUri=" + fileUri + "]";
		}
	}

	/**
	 * The Class TimeLogElementKey.
	 *
	 * @param <T> the generic type
	 */
	public static class TimeLogElementKey<T extends TimeLogKey> {

		/** The file key. */
		public final TimeLogFileKey<T> fileKey;

		/** The name. */
		public final String name;

		/** The info. */
		public final Optional<TimeLogInfo> info;

		/** The keys. */
		public final List<T> keys;

		/**
		 * Instantiates a new time log element key.
		 *
		 * @param fileKey the file key
		 * @param name    the name
		 * @param res     the res
		 * @param creator the creator
		 */
		TimeLogElementKey(TimeLogFileKey<T> fileKey, String name, JSONObject res, TimeLogCreator<T> creator) {
			this.fileKey = fileKey;
			this.name = name;
			this.info = TimeLogInfo.read(res);

			List<T> keys = new ArrayList<>(res.length());
			for (String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if (obj != null)
					keys.add(creator.apply(this, key, obj));
			}
			this.keys = Collections.unmodifiableList(keys);
		}

		/**
		 * Stream all.
		 *
		 * @return the stream
		 */
		public Stream<T> streamAll() {
			return keys.stream();
		}

		/**
		 * Gets the file uri.
		 *
		 * @return the file uri
		 */
		public String getFileUri() {
			return fileKey.fileUri;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(fileKey, name);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogElementKey))
				return false;
			TimeLogElementKey<?> other = (TimeLogElementKey<?>) obj;
			return Objects.equals(fileKey, other.fileKey) && Objects.equals(name, other.name);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogElementKey [fileUri=" + getFileUri() + ", name=" + name + "]";
		}
	}

	/**
	 * The Class TimeLogKey.
	 */
	public static class TimeLogKey {

		/** The element key. */
		public final TimeLogElementKey<? extends TimeLogKey> elementKey;

		/** The value uri. */
		public final String valueUri;

		/**
		 * Instantiates a new time log key.
		 *
		 * @param elementKey the element key
		 * @param valueUri   the value uri
		 * @param res        the res
		 */
		public TimeLogKey(TimeLogElementKey<? extends TimeLogKey> elementKey, String valueUri,
				@Nullable JSONObject res) {
			this.elementKey = elementKey;
			this.valueUri = valueUri;
		}

		/**
		 * Gets the file uri.
		 *
		 * @return the file uri
		 */
		public String getFileUri() {
			return elementKey.fileKey.fileUri;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return elementKey.name;
		}

		/**
		 * Gets the value uri.
		 *
		 * @return the value uri
		 */
		public String getValueUri() {
			return valueUri;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(elementKey, valueUri);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogKey))
				return false;
			TimeLogKey other = (TimeLogKey) obj;
			return Objects.equals(elementKey, other.elementKey) && Objects.equals(valueUri, other.valueUri);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogKey [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri + "]";
		}
	}

	/**
	 * The Class TimeLogKeyDdi.
	 */
	public static class TimeLogKeyDdi extends TimeLogKey implements Comparable<TimeLogKeyDdi> {

		/** The ddi. */
		public final int ddi;

		/**
		 * Instantiates a new time log key ddi.
		 *
		 * @param elementKey the element key
		 * @param valueUri   the value uri
		 * @param res        the res
		 */
		public TimeLogKeyDdi(TimeLogElementKey<? extends TimeLogKeyDdi> elementKey, String valueUri, JSONObject res) {
			super(elementKey, valueUri, res);
			this.ddi = res.getInt("ddi");
		}

		/**
		 * Gets the ddi.
		 *
		 * @return the ddi
		 */
		public int getDdi() {
			return ddi;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogKeyDdi [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri
					+ ", ddi=" + ddi + "]";
		}

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 */
		@Override
		public int compareTo(TimeLogKeyDdi o) {
			return Integer.compare(ddi, o.ddi);
		}
	}

	/**
	 * The Class Total.
	 */
	public static class Total extends TimeLogKeyDdi {

		/** The stop. */
		public final Instant start, stop;

		/** The value. */
		public final long value;

		/**
		 * Instantiates a new total.
		 *
		 * @param elementKey the element key
		 * @param valueUri   the value uri
		 * @param res        the res
		 */
		public Total(TimeLogElementKey<? extends TimeLogKeyDdi> elementKey, String valueUri, JSONObject res) {
			super(elementKey, valueUri, res);
			this.start = Instant.parse(res.getString("start"));
			this.stop = Instant.parse(res.getString("stop"));
			this.value = res.getLong("value");
		}

		/**
		 * Gets the start.
		 *
		 * @return the start
		 */
		public Instant getStart() {
			return start;
		}

		/**
		 * Gets the stop.
		 *
		 * @return the stop
		 */
		public Instant getStop() {
			return stop;
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		public long getValue() {
			return value;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "Total [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri + ", ddi=" + ddi
					+ ", start=" + start + ", stop=" + stop + ", value=" + value + "]";
		}
	}

	/**
	 * The Class TimeLogEntry.
	 */
	public static class TimeLogEntry implements TimedValue {

		/** The time. */
		public final Instant time;

		/** The value. */
		public final long value;

		/**
		 * Instantiates a new time log entry.
		 *
		 * @param res the res
		 */
		TimeLogEntry(JSONObject res) {
			this.time = Instant.parse(res.getString("time"));
			this.value = res.getLong("value");
		}

		/**
		 * Gets the time.
		 *
		 * @return the time
		 */
		@Override
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

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(time);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogEntry))
				return false;
			TimeLogEntry other = (TimeLogEntry) obj;
			return Objects.equals(time, other.time);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogEntry [time=" + time + ", value=" + value + "]";
		}
	}

	/**
	 * The Class TimeLog.
	 */
	public static class TimeLog extends TimedValueList<TimeLogEntry> {

		/**
		 * Instantiates a new time log.
		 *
		 * @param res the res
		 */
		TimeLog(JSONObject res) {
			super(new ArrayList<>());
			JSONArray tl = res.getJSONArray("timelog");
			((ArrayList<?>) list).ensureCapacity(tl.length());
			for (int i = 0; i < tl.length(); ++i) {
				list.add(new TimeLogEntry(tl.getJSONObject(i)));
			}
		}

	}

	/**
	 * The Class ValueInfo.
	 */
	public static class ValueInfo {

		/** The value uri. */
		public final String valueUri;

		/** The designator. */
		public final String designator;

		/** The offset. */
		public final long offset;

		/** The scale. */
		public final double scale;

		/** The number of decimals. */
		public final int numberOfDecimals;

		/** The unit. */
		public final String unit;

		/**
		 * Instantiates a new value info.
		 *
		 * @param res the res
		 */
		ValueInfo(JSONObject res) {
			this.valueUri = res.getString("valueUri");
			this.designator = res.optString("designator", "");
			this.offset = res.optLong("offset", 0);
			this.scale = res.optDouble("scale", 1.);
			this.numberOfDecimals = res.optInt("numberOfDecimals", 0);
			this.unit = res.optString("unit", "");
		}

		/**
		 * Value uri.
		 *
		 * @return the string
		 */
		public String valueUri() {
			return valueUri;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public String getDesignator() {
			return designator;
		}

		/**
		 * Gets the offset.
		 *
		 * @return the offset
		 */
		public long getOffset() {
			return offset;
		}

		/**
		 * Gets the scale.
		 *
		 * @return the scale
		 */
		public double getScale() {
			return scale;
		}

		/**
		 * Gets the number of decimals.
		 *
		 * @return the number of decimals
		 */
		public int getNumberOfDecimals() {
			return numberOfDecimals;
		}

		/**
		 * Gets the unit.
		 *
		 * @return the unit
		 */
		public String getUnit() {
			return unit;
		}

		/**
		 * Translate value.
		 *
		 * @param value the value
		 * @return the double
		 */
		public double translateValue(long value) {
			return new BigDecimal(value).add(new BigDecimal(offset)).multiply(new BigDecimal(scale)).doubleValue();
		}

		/**
		 * Format value.
		 *
		 * @param translatedValue the translated value
		 * @return the string
		 */
		public String formatValue(double translatedValue) {
			NumberFormat format = DecimalFormat.getInstance();
			format.setMaximumFractionDigits(numberOfDecimals);
			return format.format(translatedValue);
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(valueUri);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ValueInfo))
				return false;
			ValueInfo other = (ValueInfo) obj;
			return Objects.equals(valueUri, other.valueUri);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "ValueInfo [designator=" + designator + ", offset=" + offset + ", scale=" + scale
					+ ", numberOfDecimals=" + numberOfDecimals + ", unit=" + unit + "]";
		}
	}

	/**
	 * The Class TimeLogInfo.
	 */
	public static class TimeLogInfo {

		/** The count. */
		public final long count;

		/** The time range. */
		public final TimeInterval timeRange;

		/**
		 * Instantiates a new time log info.
		 *
		 * @param res the res
		 */
		private TimeLogInfo(JSONObject res) {
			this.count = ((Number) res.remove("count")).longValue();
			this.timeRange = new TimeInterval(Instant.parse((String) res.remove("from")),
					Instant.parse((String) res.remove("until")));
		}

		/**
		 * Read.
		 *
		 * @param res the res
		 * @return the optional
		 */
		static Optional<TimeLogInfo> read(JSONObject res) {
			if (res.has("count") || res.has("from") || res.has("until"))
				return Optional.of(new TimeLogInfo(res));
			else
				return Optional.empty();
		}

		/**
		 * Gets the count.
		 *
		 * @return the count
		 */
		public long getCount() {
			return count;
		}

		/**
		 * Gets the time range.
		 *
		 * @return the time range
		 */
		public TimeInterval getTimeRange() {
			return timeRange;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimeLogInfo [count=" + count + ", from=" + timeRange.from + ", until=" + timeRange.until + "]";
		}
	}

	/**
	 * The Class WikiInstance.
	 */
	public static class WikiInstance {

		/** The res. */
		public final Resource res;

		/** The label. */
		public final String label;

		/**
		 * Instantiates a new wiki instance.
		 *
		 * @param res the res
		 */
		WikiInstance(JSONObject res) {
			this.res = ResourceFactory.createResource(res.getString("uri"));
			this.label = res.getString("label");
		}

		/**
		 * Gets the res.
		 *
		 * @return the res
		 */
		public Resource getRes() {
			return res;
		}

		/**
		 * Gets the uri.
		 *
		 * @return the uri
		 */
		public String getUri() {
			return res.getURI();
		}

		/**
		 * Gets the label.
		 *
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(res);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof WikiInstance))
				return false;
			WikiInstance other = (WikiInstance) obj;
			return Objects.equals(res, other.res);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "WikiInstance [res=" + res + ", label=" + label + "]";
		}
	}

	/**
	 * The Class Device.
	 */
	public static class Device {

		/** The serial number. */
		public final Optional<String> designator, serialNumber;

		/** The elements. */
		public final Map<String, DeviceElement> elements;

		/**
		 * Instantiates a new device.
		 *
		 * @param res the res
		 */
		Device(JSONObject res) {
			this.designator = Optional.ofNullable(res.optString("designator"));
			this.serialNumber = Optional.ofNullable(res.optString("serial"));

			JSONObject dets = res.getJSONObject("elements");
			Map<String, DeviceElement> elements = new HashMap<>(dets.length());
			for (String key : dets.keySet()) {
				elements.put(key, new DeviceElement(this, dets.getJSONObject(key)));
			}
			this.elements = Collections.unmodifiableMap(elements);
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public Optional<String> getDesignator() {
			return designator;
		}

		/**
		 * Gets the serial number.
		 *
		 * @return the serial number
		 */
		public Optional<String> getSerialNumber() {
			return serialNumber;
		}

		/**
		 * Gets the elements.
		 *
		 * @return the elements
		 */
		public Map<String, DeviceElement> getElements() {
			return elements;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			if (designator.isPresent())
				return serialNumber.isPresent() ? designator.get() + " (" + serialNumber.get() + ')' : designator.get();
			else
				return serialNumber.orElse("");
		}
	}

	/**
	 * The Class DeviceElement.
	 */
	public static class DeviceElement {

		/** The device. */
		public final Device device;

		/** The designator. */
		public final Optional<String> designator;

		/** The type. */
		public final String type;

		/** The ddis. */
		public final int[] ddis;

		/**
		 * Instantiates a new device element.
		 *
		 * @param device the device
		 * @param res    the res
		 */
		DeviceElement(Device device, JSONObject res) {
			this.device = device;
			this.designator = Optional.ofNullable(res.optString("designator"));
			this.type = res.getString("type");
			JSONArray ddis = res.getJSONArray("ddis");
			this.ddis = IntStream.range(0, ddis.length()).map(ddis::getInt).toArray();
		}

		/**
		 * Gets the device.
		 *
		 * @return the device
		 */
		public Device getDevice() {
			return device;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public Optional<String> getDesignator() {
			return designator;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * Gets the ddis.
		 *
		 * @return the ddis
		 */
		public int[] getDdis() {
			return ddis;
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return designator.isPresent() ? designator.get() + " (" + type + ')' : '(' + type + ')';
		}
	}
}
