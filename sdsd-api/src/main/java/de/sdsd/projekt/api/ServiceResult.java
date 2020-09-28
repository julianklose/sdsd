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

public final class ServiceResult {
	private ServiceResult() {}
	
	public static class SDSDFile {
		public final SDSDFileMeta metadata;
		public final byte[] content;
		
		SDSDFile(JSONObject file) {
			this.metadata = new SDSDFileMeta(file);
			this.content = Base64.getDecoder().decode(file.getString("content"));
		}

		public SDSDFileMeta getMetadata() {
			return metadata;
		}
		
		public byte[] getContent() {
			return content;
		}

		@Override
		public int hashCode() {
			return Objects.hash(metadata);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFile))
				return false;
			SDSDFile other = (SDSDFile) obj;
			return Objects.equals(metadata, other.metadata);
		}

		@Override
		public String toString() {
			return "SDSDFile [metadata=" + metadata + ", content=byte[]]";
		}
	}
	
	public static class SDSDFileAppended {
		public final SDSDFileMeta metadata;
		public final byte[] appended;
		
		SDSDFileAppended(JSONObject file) {
			this.metadata = new SDSDFileMeta(file);
			this.appended = Base64.getDecoder().decode(file.getString("appended"));
		}

		public SDSDFileMeta getMetadata() {
			return metadata;
		}

		public byte[] getAppended() {
			return appended;
		}

		@Override
		public int hashCode() {
			return Objects.hash(metadata);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFileAppended))
				return false;
			SDSDFileAppended other = (SDSDFileAppended) obj;
			return Objects.equals(metadata, other.metadata);
		}

		@Override
		public String toString() {
			return "SDSDFileAppended [metadata=" + metadata + ", appended=byte[]]";
		}
	}
	
	public static class SDSDFileMeta {
		public final String uri;
		public final String filename;
		public final String source;
		public final Instant created;
		public final Instant lastmodified;
		public final Instant leveraged;
		public final Instant expires;
		public final long size;
		public final String type;
		
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

		public String getUri() {
			return uri;
		}

		public String getFilename() {
			return filename;
		}

		public String getSource() {
			return source;
		}

		public Instant getCreated() {
			return created;
		}

		public Instant getLastmodified() {
			return lastmodified;
		}

		public Instant getLeveraged() {
			return leveraged;
		}

		public Instant getExpires() {
			return expires;
		}

		public long getSize() {
			return size;
		}

		public String getType() {
			return type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDFileMeta))
				return false;
			SDSDFileMeta other = (SDSDFileMeta) obj;
			return Objects.equals(uri, other.uri);
		}

		@Override
		public String toString() {
			return "SDSDFileMeta [uri=" + uri + ", filename=" + filename + ", source=" + source + ", created=" + created
					+ ", lastmodified=" + lastmodified + ", leveraged=" + leveraged + ", expires=" + expires + ", size="
					+ size + ", type=" + type + "]";
		}
	}
	
	public static class SDSDObject {
		
		private final Resource res;
		
		SDSDObject(JSONObject res) {
			Model model = ModelFactory.createDefaultModel();
			model.read(new StringReader(res.toString()), null, "JSON-LD");
			this.res = model.getResource(res.getString("@id"));
		}
		
		public Resource getResouce() {
			return res;
		}
		
		public String getLabel() {
			Statement stmt = res.getProperty(RDFS.label);
			return stmt != null ? stmt.getLiteral().getString() : "";
		}
		
		public Optional<String> getUri(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getResource().getURI()) : Optional.empty();
		}
		public Optional<String> getParentUri() {
			return getUri(DCTerms.isPartOf);
		}
		
		public Optional<Object> getValue(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral().getValue()) : Optional.empty();
		}
		
		public Optional<String> getString(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral().getString()) : Optional.empty();
		}
		
		public Optional<Instant> getTimestamp(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(((Calendar)stmt.getLiteral().getValue()).toInstant()) : Optional.empty();
		}
		
		public Optional<Literal> getLiteral(Property p) {
			Statement stmt = res.getProperty(p);
			return stmt != null ? Optional.of(stmt.getLiteral()) : Optional.empty();
		}
		
		@Override
		public String toString() {
			StringWriter sw = new StringWriter();
			res.getModel().write(sw, "JSON-LD");
			return sw.toString();
		}
	}
	
	public static class Attr {
		public final String value;
		public final Optional<Resource> type;
		
		Attr(JSONObject res) {
			this.value = res.getString("value");
			this.type = res.has("type") ? Optional.of(ResourceFactory.createResource(res.getString("type"))) : Optional.empty();
		}

		public String getValue() {
			return value;
		}

		public Optional<Resource> getType() {
			return type;
		}
		
		@Override
		public String toString() {
			return (type.isPresent() ? type.get().getURI() + ": " : "") + value;
		}
	}
	
	public static class FindResult {
		public final String uri;
		public final Optional<Resource> type;
		private final JSONObject res;
		
		FindResult(JSONObject res) {
			this.res = res;
			this.uri = (String)res.remove("uri");
			this.type = res.has("type") ? Optional.of(ResourceFactory.createResource((String)res.remove("type"))) : Optional.empty();
		}
		
		public String getUri() {
			return uri;
		}

		public Optional<Resource> getType() {
			return type;
		}

		public Set<String> attrSet() {
			return res.keySet();
		}
		
		public Optional<String> getAttribute(Property attribute) {
			return Optional.ofNullable(res.optString(attribute.getURI()));
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(uri);
			if(type.isPresent())
				sb.append('(').append(type.get().getURI()).append(')');
			if(res.length() > 0) 
				sb.append(": ").append(res.toString());
			return sb.toString();
		}
	}
	
	public static class DeviceElementProperties extends HashMap<Integer, DeviceProperty> {
		private static final long serialVersionUID = -4539049533668765872L;

		DeviceElementProperties(JSONObject res) {
			for(String key : res.keySet()) {
				DeviceProperty dpt = new DeviceProperty(res.getJSONObject(key));
				put(dpt.ddi, dpt);
			}
		}
	}
	
	public static class TimeInterval {
		public final Instant from, until;
		
		public TimeInterval(Instant from, Instant until) {
			this.from = from;
			this.until = until;
		}
		
		public Instant getFrom() {
			return from;
		}

		public Instant getUntil() {
			return until;
		}

		@CheckForNull
		public JSONObject toJson() {
			JSONObject out = new JSONObject();
			if(from != null && from.isAfter(Instant.EPOCH))
				out.put("from", from.toString());
			if(until != null && until.isBefore(Instant.now()))
				out.put("until", until.toString());
			return out.length() > 0 ? out : null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(from, until);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeInterval))
				return false;
			TimeInterval other = (TimeInterval) obj;
			return Objects.equals(from, other.from) && Objects.equals(until, other.until);
		}

		@Override
		public String toString() {
			return "TimeInterval [from=" + from + ", until=" + until + "]";
		}
	}
	
	public static class DeviceProperty {
		public final int ddi;
		public final int value;
		public final ValueInfo info;
		
		DeviceProperty(JSONObject res) {
			this.ddi = res.getInt("ddi");
			this.value = res.getInt("value");
			this.info = new ValueInfo(res);
		}

		public int getDdi() {
			return ddi;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "DeviceProperty [ddi=" + ddi + ", value=" + value + ", info=" + info.toString() + "]";
		}
		
	}
	
	public static interface TimedValue {
		public Instant getTime();
	}
	
	public static class TimedValueList<T extends TimedValue> extends AbstractList<T> {
		protected final List<T> list;
		
		public TimedValueList(List<T> list) {
			this.list = list;
		}

		@Override
		public T get(int index) {
			return list.get(index);
		}

		@Override
		public int size() {
			return list.size();
		}
		
		public AscendingTimedValueList<T> ascending() {
			return new AscendingTimedValueList<T>(list);
		}
		
		public AscendingIterator<T> ascendingIterator() {
			return new AscendingIterator<T>(list);
		}
		
	}
	
	public static class AscendingIterator<T extends TimedValue> implements Iterator<T> {
		
		final List<T> list;
		int index;
		
		AscendingIterator(List<T> list) {
			this.list = list;
			this.index = list.size()-1;
		}

		@Override
		public boolean hasNext() {
			return index >= 0;
		}

		@Override
		public T next() {
			return list.get(index--);
		}
		
		/**
		 * Returns the value for the given time.
		 * If there is a value for the given time, it is returned.
		 * If there is no value, the last known value is returned.
		 * If there were no values before this time, the value is not present.
		 * @param time a given instant (mostly from the positions list)
		 * @return the last known value, if available
		 */
		public Optional<T> nextUntil(Instant time) {
			T entry = null;
			for(int i = Math.min(index+1, list.size()-1); i >= 0; index = --i) {
				T e = list.get(i);
				if(e.getTime().isAfter(time))
					break;
				entry = e;
			}
			return Optional.ofNullable(entry);
		}

	}
	
	public static class AscendingTimedValueList<T extends TimedValue> extends AbstractList<T> {
		private final List<T> list;
		
		AscendingTimedValueList(List<T> list) {
			this.list = list;
		}

		@Override
		public T get(int index) {
			return list.get(list.size() - index - 1);
		}

		@Override
		public int size() {
			return list.size();
		}
		
		@Override
		public Iterator<T> iterator() {
			return new AscendingIterator<T>(list);
		}
	}
	
	public static class Position {
		public final double latitude, longitude;
		/**
		 * Can be NaN.
		 */
		public final double altitude;

		public Position(double latitude, double longitude, double altitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		/**
		 * @return The altitude or NaN, if there is no altitude.
		 */
		public double getAltitude() {
			return altitude;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(altitude, latitude, longitude);
		}

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

		@Override
		public String toString() {
			return "Position [latitude: " + latitude + "° / longitude: " + longitude + "°, altitude: " + altitude + "m]";
		}
		
	}
	
	public static class TimedPosition extends Position implements TimedValue {
		public final Instant time;
		
		TimedPosition(JSONObject res) {
			super(res.getDouble("latitude"), res.getDouble("longitude"), res.optDouble("altitude"));
			this.time = Instant.parse(res.getString("time"));
		}
		
		public TimedPosition(Instant time, double latitude, double longitude, double altitude) {
			super(latitude, longitude, altitude);
			this.time = time;
		}
		
		@Override
		public Instant getTime() {
			return time;
		}

		@Override
		public int hashCode() {
			return Objects.hash(time);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimedPosition))
				return false;
			TimedPosition other = (TimedPosition) obj;
			return Objects.equals(time, other.time);
		}

		@Override
		public String toString() {
			return "TimedPosition [time=" + time + ", latitude: " + latitude 
					+ "° / longitude: " + longitude + "°, altitude: " + altitude + "m]";
		}
	}
	
	public static class Positions extends TimedValueList<TimedPosition> {

		Positions(JSONObject res) {
			super(ServiceAPI.result(res.getJSONArray("positions"), TimedPosition::new));
		}
		
	}
	
	@FunctionalInterface
	public interface TimeLogCreator<R extends TimeLogKey> {
		R apply(TimeLogElementKey<R> elementKey, String valueUri, JSONObject res);
	}
	public static class TimeLogKeys<T extends TimeLogKey> {
		public final List<TimeLogFileKey<T>> files;

		TimeLogKeys(JSONObject res, TimeLogCreator<T> creator) {
			List<TimeLogFileKey<T>> files = new ArrayList<>(res.length());
			for(String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if(obj != null)
					files.add(new TimeLogFileKey<>(this, key, obj, creator));
			}
			this.files = Collections.unmodifiableList(files);
		}
		
		public Stream<T> streamAll() {
			return files.stream().flatMap(TimeLogFileKey::streamAll);
		}
		
		public Stream<TimeLogElementKey<T>> streamElements() {
			return files.stream().flatMap(f -> f.elements.stream());
		}
	}
	public static class TimeLogFileKey<T extends TimeLogKey> {
		public final TimeLogKeys<T> keys;
		public final String fileUri;
		public final List<TimeLogElementKey<T>> elements;
		
		TimeLogFileKey(TimeLogKeys<T> keys, String fileUri, JSONObject res, TimeLogCreator<T> creator) {
			this.keys = keys;
			this.fileUri = fileUri;
			
			List<TimeLogElementKey<T>> elements = new ArrayList<>(res.length());
			for(String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if(obj != null)
					elements.add(new TimeLogElementKey<>(this, key, obj, creator));
			}
			this.elements = Collections.unmodifiableList(elements);
		}
		
		public Stream<T> streamAll() {
			return elements.stream().flatMap(TimeLogElementKey::streamAll);
		}
		
		public String getFileUri() {
			return fileUri;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fileUri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogFileKey))
				return false;
			TimeLogFileKey<?> other = (TimeLogFileKey<?>) obj;
			return Objects.equals(fileUri, other.fileUri);
		}
		
		@Override
		public String toString() {
			return "TimeLogFileKey [fileUri=" + fileUri + "]";
		}
	}
	public static class TimeLogElementKey<T extends TimeLogKey> {
		public final TimeLogFileKey<T> fileKey;
		public final String name;
		public final Optional<TimeLogInfo> info;
		public final List<T> keys;
		
		TimeLogElementKey(TimeLogFileKey<T> fileKey, String name, JSONObject res, TimeLogCreator<T> creator) {
			this.fileKey = fileKey;
			this.name = name;
			this.info = TimeLogInfo.read(res);
			
			List<T> keys = new ArrayList<>(res.length());
			for(String key : res.keySet()) {
				JSONObject obj = res.optJSONObject(key);
				if(obj != null)
					keys.add(creator.apply(this, key, obj));
			}
			this.keys = Collections.unmodifiableList(keys);
		}
		
		public Stream<T> streamAll() {
			return keys.stream();
		}

		public String getFileUri() {
			return fileKey.fileUri;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fileKey, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogElementKey))
				return false;
			TimeLogElementKey<?> other = (TimeLogElementKey<?>) obj;
			return Objects.equals(fileKey, other.fileKey) && Objects.equals(name, other.name);
		}

		@Override
		public String toString() {
			return "TimeLogElementKey [fileUri=" + getFileUri() + ", name=" + name + "]";
		}
	}
	
	public static class TimeLogKey {
		public final TimeLogElementKey<? extends TimeLogKey> elementKey;
		public final String valueUri;

		public TimeLogKey(TimeLogElementKey<? extends TimeLogKey> elementKey, String valueUri, @Nullable JSONObject res) {
			this.elementKey = elementKey;
			this.valueUri = valueUri;
		}

		public String getFileUri() {
			return elementKey.fileKey.fileUri;
		}

		public String getName() {
			return elementKey.name;
		}

		public String getValueUri() {
			return valueUri;
		}

		@Override
		public int hashCode() {
			return Objects.hash(elementKey, valueUri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogKey))
				return false;
			TimeLogKey other = (TimeLogKey) obj;
			return Objects.equals(elementKey, other.elementKey) && Objects.equals(valueUri, other.valueUri);
		}

		@Override
		public String toString() {
			return "TimeLogKey [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri + "]";
		}
	}
	
	public static class TimeLogKeyDdi extends TimeLogKey implements Comparable<TimeLogKeyDdi> {
		public final int ddi;

		public TimeLogKeyDdi(TimeLogElementKey<? extends TimeLogKeyDdi> elementKey, String valueUri, JSONObject res) {
			super(elementKey, valueUri, res);
			this.ddi = res.getInt("ddi");
		}
		
		public int getDdi() {
			return ddi;
		}

		@Override
		public String toString() {
			return "TimeLogKeyDdi [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri
					+ ", ddi=" + ddi + "]";
		}

		@Override
		public int compareTo(TimeLogKeyDdi o) {
			return Integer.compare(ddi, o.ddi);
		}
	}
	
	public static class Total extends TimeLogKeyDdi {
		public final Instant start, stop;
		public final long value;

		public Total(TimeLogElementKey<? extends TimeLogKeyDdi> elementKey, String valueUri, JSONObject res) {
			super(elementKey, valueUri, res);
			this.start = Instant.parse(res.getString("start"));
			this.stop = Instant.parse(res.getString("stop"));
			this.value = res.getLong("value");
		}

		public Instant getStart() {
			return start;
		}

		public Instant getStop() {
			return stop;
		}

		public long getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "Total [fileUri=" + getFileUri() + ", name=" + getName() + ", valueUri=" + valueUri
					+ ", ddi=" + ddi + ", start=" + start + ", stop=" + stop + ", value=" + value + "]";
		}
	}
	
	public static class TimeLogEntry implements TimedValue {
		public final Instant time;
		public final long value;
		
		TimeLogEntry(JSONObject res) {
			this.time = Instant.parse(res.getString("time"));
			this.value = res.getLong("value");
		}

		@Override
		public Instant getTime() {
			return time;
		}
		
		public long getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(time);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TimeLogEntry))
				return false;
			TimeLogEntry other = (TimeLogEntry) obj;
			return Objects.equals(time, other.time);
		}

		@Override
		public String toString() {
			return "TimeLogEntry [time=" + time + ", value=" + value + "]";
		}
	}
	
	public static class TimeLog extends TimedValueList<TimeLogEntry> {

		TimeLog(JSONObject res) {
			super(new ArrayList<>());
			JSONArray tl = res.getJSONArray("timelog");
			((ArrayList<?>)list).ensureCapacity(tl.length());
			for(int i = 0; i < tl.length(); ++i) {
				list.add(new TimeLogEntry(tl.getJSONObject(i)));
			}
		}
		
	}
	
	public static class ValueInfo {
		public final String valueUri;
		public final String designator;
		public final long offset;
		public final double scale;
		public final int numberOfDecimals;
		public final String unit;
		
		ValueInfo(JSONObject res) {
			this.valueUri = res.getString("valueUri");
			this.designator = res.optString("designator", "");
			this.offset = res.optLong("offset", 0);
			this.scale = res.optDouble("scale", 1.);
			this.numberOfDecimals = res.optInt("numberOfDecimals", 0);
			this.unit = res.optString("unit", "");
		}
		
		public String valueUri() {
			return valueUri;
		}

		public String getDesignator() {
			return designator;
		}
		
		public long getOffset() {
			return offset;
		}
		
		public double getScale() {
			return scale;
		}

		public int getNumberOfDecimals() {
			return numberOfDecimals;
		}

		public String getUnit() {
			return unit;
		}
		
		public double translateValue(long value) {
			return new BigDecimal(value)
					.add(new BigDecimal(offset))
					.multiply(new BigDecimal(scale))
					.doubleValue();
		}
		
		public String formatValue(double translatedValue) {
			NumberFormat format = DecimalFormat.getInstance();
			format.setMaximumFractionDigits(numberOfDecimals);
			return format.format(translatedValue);
		}

		@Override
		public int hashCode() {
			return Objects.hash(valueUri);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ValueInfo))
				return false;
			ValueInfo other = (ValueInfo) obj;
			return Objects.equals(valueUri, other.valueUri);
		}

		@Override
		public String toString() {
			return "ValueInfo [designator=" + designator + ", offset=" + offset + ", scale=" + scale
					+ ", numberOfDecimals=" + numberOfDecimals + ", unit=" + unit + "]";
		}
	}
	
	public static class TimeLogInfo {
		public final long count;
		public final TimeInterval timeRange;
		
		private TimeLogInfo(JSONObject res) {
			this.count = ((Number)res.remove("count")).longValue();
			this.timeRange = new TimeInterval(Instant.parse((String)res.remove("from")), Instant.parse((String)res.remove("until")));
		}
		
		static Optional<TimeLogInfo> read(JSONObject res) {
			if(res.has("count") || res.has("from") || res.has("until"))
				return Optional.of(new TimeLogInfo(res));
			else
				return Optional.empty();
		}
		
		public long getCount() {
			return count;
		}
		
		public TimeInterval getTimeRange() {
			return timeRange;
		}

		@Override
		public String toString() {
			return "TimeLogInfo [count=" + count + ", from=" + timeRange.from + ", until=" + timeRange.until + "]";
		}
	}
	
	public static class WikiInstance {
		public final Resource res;
		public final String label;

		WikiInstance(JSONObject res) {
			this.res = ResourceFactory.createResource(res.getString("uri"));
			this.label = res.getString("label");
		}
		
		public Resource getRes() {
			return res;
		}

		public String getUri() {
			return res.getURI();
		}

		public String getLabel() {
			return label;
		}

		@Override
		public int hashCode() {
			return Objects.hash(res);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof WikiInstance))
				return false;
			WikiInstance other = (WikiInstance) obj;
			return Objects.equals(res, other.res);
		}

		@Override
		public String toString() {
			return "WikiInstance [res=" + res + ", label=" + label + "]";
		}
	}
	
	public static class Device {
		public final Optional<String> designator, serialNumber;
		public final Map<String, DeviceElement> elements;
		
		Device(JSONObject res) {
			this.designator = Optional.ofNullable(res.optString("designator"));
			this.serialNumber = Optional.ofNullable(res.optString("serial"));
			
			JSONObject dets = res.getJSONObject("elements");
			Map<String, DeviceElement> elements = new HashMap<>(dets.length());
			for(String key : dets.keySet()) {
				elements.put(key, new DeviceElement(this, dets.getJSONObject(key)));
			}
			this.elements = Collections.unmodifiableMap(elements);
		}
		
		public Optional<String> getDesignator() {
			return designator;
		}
		
		public Optional<String> getSerialNumber() {
			return serialNumber;
		}
		
		public Map<String, DeviceElement> getElements() {
			return elements;
		}
		
		@Override
		public String toString() {
			if(designator.isPresent())
				return serialNumber.isPresent() ? designator.get() + " (" + serialNumber.get() + ')' : designator.get();
			else
				return serialNumber.orElse("");
		}
	}
	public static class DeviceElement {
		public final Device device;
		public final Optional<String> designator;
		public final String type;
		public final int[] ddis;
		
		DeviceElement(Device device, JSONObject res) {
			this.device = device;
			this.designator = Optional.ofNullable(res.optString("designator"));
			this.type = res.getString("type");
			JSONArray ddis = res.getJSONArray("ddis");
			this.ddis = IntStream.range(0, ddis.length()).map(ddis::getInt).toArray();
		}
		
		public Device getDevice() {
			return device;
		}
		
		public Optional<String> getDesignator() {
			return designator;
		}
		
		public String getType() {
			return type;
		}
		
		public int[] getDdis() {
			return ddis;
		}
		
		@Override
		public String toString() {
			return designator.isPresent() ? designator.get() + " (" + type + ')' : '(' + type + ')';
		}
	}
}
