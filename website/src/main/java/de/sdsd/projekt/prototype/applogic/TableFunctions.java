package de.sdsd.projekt.prototype.applogic;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;

import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GridCell;
import de.sdsd.projekt.prototype.data.Timelog;
import de.sdsd.projekt.prototype.data.TimelogPosition;
import de.sdsd.projekt.prototype.data.ValueInfo;

/**
 * Provides functions for interacting with the cassandra database.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class TableFunctions implements Closeable {
	
	/** The app. */
	private final ApplicationLogic app;
	
	/** The cassandra. */
	final CqlSession cassandra;
	
	/** The cassandra select grid keys. */
	private final PreparedStatement cassandraSelectPositionKeys, cassandraSelectTimelogKeys, cassandraSelectGridKeys;
	
	/** The cassandra select positions info. */
	private final PreparedStatement cassandraSelectPositions, cassandraSelectPositionsFiltered, cassandraSelectPositionsInfo;
	
	/** The cassandra select timelogs filtered. */
	private final PreparedStatement cassandraSelectTimelogs, cassandraSelectTimelogsFiltered;
	
	/** The cassandra select grid. */
	private final PreparedStatement cassandraSelectGridStatics, cassandraSelectGrid;
	
	/** The cassandra insert grid key. */
	private final PreparedStatement cassandraInsertTimelogKey, cassandraInsertPositionKey, cassandraInsertGridKey;
	
	/** The cassandra insert grid. */
	private final PreparedStatement cassandraInsertTimelog, cassandraInsertPosition, cassandraInsertGrid;
	
	/** The cassandra delete grid key. */
	private final PreparedStatement cassandraDeleteTimelogKey, cassandraDeletePositionKey, cassandraDeleteGridKey;
	
	/** The cassandra delete grid. */
	private final PreparedStatement cassandraDeleteTimelog, cassandraDeletePosition, cassandraDeleteGrid;

	/**
	 * Instantiates a new table functions.
	 *
	 * @param app the app
	 */
	TableFunctions(ApplicationLogic app) {
		this.app = app;
		JSONObject cassandra = app.settings.getJSONObject("cassandra");
		this.cassandra = CqlSession.builder()
				.addContactPoint(new InetSocketAddress(cassandra.getString("address"), cassandra.getInt("port")))
				.withLocalDatacenter("datacenter1")
				.withAuthCredentials(cassandra.getString("user"), cassandra.getString("password"))
				.withKeyspace(cassandra.getString("keyspace"))
				.build();
		
		cassandraSelectPositionKeys = this.cassandra.prepare("SELECT user, file, name FROM position_keys "
				+ "WHERE user=? AND file=?");
		cassandraSelectPositions = this.cassandra.prepare("SELECT time, latitude, longitude, altitude FROM position_generic "
				+ "WHERE user=? AND file=? AND name=? LIMIT ?");
		cassandraSelectPositionsFiltered = this.cassandra.prepare("SELECT time, latitude, longitude, altitude FROM position_generic "
				+ "WHERE user=? AND file=? AND name=? AND time>=? AND time<=? LIMIT ?");
		cassandraSelectPositionsInfo = this.cassandra.prepare("SELECT count(*), min(time), max(time) FROM position_generic "
				+ "WHERE user=? AND file=? AND name=?");
		
		cassandraSelectTimelogKeys = this.cassandra.prepare("SELECT user, file, name, value_uri FROM timelog_keys "
				+ "WHERE user=? AND file=?");
		cassandraSelectTimelogs = this.cassandra.prepare("SELECT time, value FROM timelog_generic "
				+ "WHERE user=? AND file=? AND name=? AND value_uri=? LIMIT ?");
		cassandraSelectTimelogsFiltered = this.cassandra.prepare("SELECT time, value FROM timelog_generic "
				+ "WHERE user=? AND file=? AND name=? AND value_uri=? AND time>=? AND time<=? LIMIT ?");
		
		cassandraSelectGridKeys = this.cassandra.prepare("SELECT user, file, name, value_uri FROM grid_keys "
				+ "WHERE user=? AND file=?");
		cassandraSelectGridStatics = this.cassandra.prepare("SELECT DISTINCT north_size, east_size FROM grid_generic "
				+ "WHERE user=? AND file=? AND name=? AND value_uri IN ?");
		cassandraSelectGrid = this.cassandra.prepare("SELECT north_min, east_min, value FROM grid_generic "
				+ "WHERE user=? AND file=? AND name=? AND value_uri=?");
		
		cassandraInsertPositionKey = this.cassandra.prepare("INSERT INTO position_keys "
				+ "(user,file,name) "
				+ "VALUES (?,?,?)");
		cassandraInsertTimelogKey = this.cassandra.prepare("INSERT INTO timelog_keys "
				+ "(user,file,name,value_uri) "
				+ "VALUES (?,?,?,?)");
		cassandraInsertGridKey = this.cassandra.prepare("INSERT INTO grid_keys "
				+ "(user,file,name,value_uri) "
				+ "VALUES (?,?,?,?)");
		
		cassandraInsertPosition = this.cassandra.prepare("INSERT INTO position_generic "
				+ "(user,file,name,time,latitude,longitude,altitude) "
				+ "VALUES (?,?,?,?,?,?,?)");
		cassandraInsertTimelog = this.cassandra.prepare("INSERT INTO timelog_generic "
				+ "(user,file,name,value_uri,time,value) "
				+ "VALUES (?,?,?,?,?,?)");
		cassandraInsertGrid = this.cassandra.prepare("INSERT INTO grid_generic "
				+ "(user,file,name,value_uri,north_min,east_min,north_size,east_size,value) "
				+ "VALUES (?,?,?,?,?,?,?,?,?)");
		
		cassandraDeletePositionKey = this.cassandra.prepare("DELETE FROM position_keys WHERE user=? AND file=? AND name=?");
		cassandraDeleteTimelogKey = this.cassandra.prepare("DELETE FROM timelog_keys WHERE user=? AND file=? AND name=? AND value_uri=?");
		cassandraDeleteGridKey = this.cassandra.prepare("DELETE FROM grid_keys WHERE user=? AND file=? AND name=? AND value_uri=?");
		cassandraDeletePosition = this.cassandra.prepare("DELETE FROM position_generic WHERE user=? AND file=? AND name=?");
		cassandraDeleteTimelog = this.cassandra.prepare("DELETE FROM timelog_generic WHERE user=? AND file=? AND name=? AND value_uri=?");
		cassandraDeleteGrid = this.cassandra.prepare("DELETE FROM grid_generic WHERE user=? AND file=? AND name=? AND value_uri=?");
	}
	
	/**
	 * Key for retrieving keys of the contents of the given file.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class FileKey {
		
		/** The user. */
		public final String user;
		
		/** The file. */
		public final String file;
		
		/**
		 * Instantiates a new file key.
		 *
		 * @param user the user
		 * @param fileUri the file uri
		 */
		public FileKey(String user, String fileUri) {
			this.user = user;
			this.file = fileUri;
		}
		
		/**
		 * Gets the user.
		 *
		 * @return the user
		 */
		public String getUser() {
			return user;
		}

		/**
		 * Gets the file.
		 *
		 * @return the file
		 */
		public String getFile() {
			return file;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(file, user);
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
			if (!(obj instanceof FileKey))
				return false;
			FileKey other = (FileKey) obj;
			return Objects.equals(file, other.file) && Objects.equals(user, other.user);
		}
		
		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "FileKey [user=" + user + ", file=" + file + "]";
		}
	}
	
	/**
	 * Key for retrieving timelog positions.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class ElementKey extends FileKey {
		
		/** The name. */
		public final String name;
		
		/**
		 * Instantiates a new element key.
		 *
		 * @param user the user
		 * @param fileUri the file uri
		 * @param name the name
		 */
		public ElementKey(String user, String fileUri, String name) {
			super(user, fileUri);
			this.name = name;
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
		 * To file key.
		 *
		 * @return the file key
		 */
		public FileKey toFileKey() {
			return new FileKey(user, file);
		}
		
		/**
		 * Check.
		 *
		 * @param row the row
		 * @return true, if successful
		 */
		protected boolean check(Row row) {
			return row.getString(2).equals(name);
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(name);
			return result;
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
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof ElementKey))
				return false;
			ElementKey other = (ElementKey) obj;
			return Objects.equals(name, other.name);
		}
		
		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "ElementKey [user=" + user + ", file=" + file + ", name=" + name + "]";
		}
	}
	
	/**
	 * Key for retrieving timelog sensor data and grid data.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Key extends ElementKey {
		
		/** The value uri. */
		public final String valueUri;
		
		/**
		 * Instantiates a new key.
		 *
		 * @param user the user
		 * @param fileUri the file uri
		 * @param name the name
		 * @param valueUri the value uri
		 */
		public Key(String user, String fileUri, String name, String valueUri) {
			super(user, fileUri, name);
			this.valueUri = valueUri;
		}
		
		/**
		 * Gets the value URI.
		 *
		 * @return the value URI
		 */
		public String getValueURI() {
			return valueUri;
		}
		
		/**
		 * To element key.
		 *
		 * @return the element key
		 */
		public ElementKey toElementKey() {
			return new ElementKey(user, file, name);
		}
		
		/**
		 * Check.
		 *
		 * @param row the row
		 * @return true, if successful
		 */
		@Override
		protected boolean check(Row row) {
			return super.check(row) && row.getString(3).equals(valueUri);
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(valueUri);
			return result;
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
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof Key))
				return false;
			Key other = (Key) obj;
			return Objects.equals(valueUri, other.valueUri);
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "Key [user=" + user + ", file=" + file + ", name=" + name + ", valueUri=" + valueUri + "]";
		}
	}
	
	/**
	 * List position keys.
	 *
	 * @return the list
	 */
	List<ElementKey> listPositionKeys() {
		List<ElementKey> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name FROM position_keys")) {
			list.add(new ElementKey(row.getString(0), row.getString(1), row.getString(2)));
		}
		return list;
	}
	
	/**
	 * List timelog keys.
	 *
	 * @return the list
	 */
	List<Key> listTimelogKeys() {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM timelog_keys")) {
			list.add(new Key(row.getString(0), row.getString(1), row.getString(2), row.getString(3)));
		}
		return list;
	}
	
	/**
	 * List grid keys.
	 *
	 * @return the list
	 */
	List<Key> listGridKeys() {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM grid_keys")) {
			list.add(new Key(row.getString(0), row.getString(1), row.getString(2), row.getString(3)));
		}
		return list;
	}
 	
	/**
	 * List position keys.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<ElementKey> listPositionKeys(FileKey key) {
		List<ElementKey> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectPositionKeys.bind(key.user, key.file))) {
			list.add(new ElementKey(key.user, key.file, row.getString(2)));
		}
		return list;
	}
	
	/**
	 * List timelog keys.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<Key> listTimelogKeys(FileKey key) {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectTimelogKeys.bind(key.user, key.file))) {
			list.add(new Key(key.user, key.file, row.getString(2), row.getString(3)));
		}
		return list;
	}
	
	/**
	 * List timelogs.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<String> listTimelogs(FileKey key) {
		List<String> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectPositionKeys.bind(key.user, key.file))) {
			list.add(row.getString(3));
		}
		return list;
	}
	
	/**
	 * List timelog value uris.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<String> listTimelogValueUris(ElementKey key) {
		List<String> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectTimelogKeys.bind(key.user, key.file))) {
			if(key.check(row))
				list.add(row.getString(3));
		}
		return list;
	}
	
	/**
	 * List grid keys.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<Key> listGridKeys(FileKey key) {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectGridKeys.bind(key.user, key.file))) {
			list.add(new Key(key.user, key.file, row.getString(2), row.getString(3)));
		}
		return list;
	}
	
	/**
	 * List grid value uris.
	 *
	 * @param key the key
	 * @return the list
	 */
	public List<String> listGridValueUris(ElementKey key) {
		List<String> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectGridKeys.bind(key.user, key.file))) {
			if(key.check(row))
				list.add(row.getString(3));
		}
		return list;
	}
	
	/**
	 * General information about a timelog.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class TimelogInfo {
		
		/** The count. */
		public final long count;
		
		/** The until. */
		public final Instant from, until;
		
		/**
		 * Instantiates a new timelog info.
		 *
		 * @param count the count
		 * @param from the from
		 * @param until the until
		 */
		protected TimelogInfo(long count, Instant from, Instant until) {
			this.count = count;
			this.from = from;
			this.until = until;
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
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return "TimelogInfo [count=" + count + ", from=" + from + ", until=" + until + "]";
		}
	}
	
	/**
	 * Gets the timelog info.
	 *
	 * @param key the key
	 * @return the timelog info
	 */
	public TimelogInfo getTimelogInfo(ElementKey key) {
		Row result = cassandra.execute(cassandraSelectPositionsInfo.bind(key.user, key.file, key.name)).one();
		return result.getLong(0) == 0 ? new TimelogInfo(0, Instant.EPOCH, Instant.EPOCH) 
				: new TimelogInfo(result.getLong(0), 
				result.getInstant(1), result.getInstant(2));
	}
	
	/**
	 * Represents a range in time.
	 * Both 'from' and 'until' are inclusive.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class TimeInterval {
		
		/** The until. */
		@CheckForNull
		private final Instant from, until;
		
		/**
		 * Instantiates a new time interval.
		 *
		 * @param from the from
		 * @param until the until
		 */
		public TimeInterval(@Nullable Instant from, @Nullable Instant until) {
			this.from = from;
			this.until = until;
		}
		
		/**
		 * From.
		 *
		 * @param timeFilter the time filter
		 * @return the time interval
		 */
		@CheckForNull
		public static TimeInterval from(JSONObject timeFilter) {
			return timeFilter == null || (!timeFilter.has("from") && !timeFilter.has("until")) ? null : new TimeInterval(
					timeFilter.has("from") ? Instant.parse(timeFilter.getString("from")) : null,
					timeFilter.has("until") ? Instant.parse(timeFilter.getString("until")) : null);
		}

		/**
		 * Checks if is from.
		 *
		 * @return true, if is from
		 */
		public boolean isFrom() {
			return from != null;
		}

		/**
		 * Checks if is until.
		 *
		 * @return true, if is until
		 */
		public boolean isUntil() {
			return until != null;
		}
		
		/**
		 * From.
		 *
		 * @return the instant
		 */
		public Instant from() {
			return from != null ? from : Instant.EPOCH;
		}

		/**
		 * Until.
		 *
		 * @return the instant
		 */
		public Instant until() {
			return until != null ? until : Instant.now();
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
	
	/** The Constant OUTPUT_MAX. */
	public static final int OUTPUT_MAX = 50000;
	
	/**
	 * Gets the positions.
	 *
	 * @param key the key
	 * @param timefilter the timefilter
	 * @param limit the limit
	 * @return the positions
	 */
	public List<TimelogPosition> getPositions(ElementKey key, @Nullable TimeInterval timefilter, int limit) {
		if(limit == 0) return Collections.emptyList();
		if(limit < 0) limit = OUTPUT_MAX;
		
		BoundStatement query = timefilter != null 
				? cassandraSelectPositionsFiltered.bind(key.user, key.file, key.name)
						.setInstant(3, timefilter.from())
						.setInstant(4, timefilter.until())
						.setInt(5, limit)
				: cassandraSelectPositions.bind(key.user, key.file, key.name)
						.setInt(3, limit);
		
		ArrayList<TimelogPosition> list = new ArrayList<>();
		for(Row row : cassandra.execute(query)) {
			Coordinate pos = new Coordinate(pos(row.getInt(2)), pos(row.getInt(1)), alt(row.getInt(3)));
			list.add(new TimelogPosition(row.getInstant(0), pos));
		}
		return list;
	}
	
	/**
	 * Gets the value infos.
	 *
	 * @param fileUri the file uri
	 * @param valueUris the value uris
	 * @return the value infos
	 */
	public List<ValueInfo> getValueInfos(String fileUri, List<String> valueUris) {
		return ValueInfo.getValueInfos(app, fileUri, valueUris);
	}
	
	/**
	 * Gets the value info.
	 *
	 * @param fileUri the file uri
	 * @param valueUri the value uri
	 * @return the value info
	 */
	public ValueInfo getValueInfo(String fileUri, String valueUri) {
		return ValueInfo.getValueInfo(app, fileUri, valueUri);
	}
	
	/**
	 * Gets the timelogs.
	 *
	 * @param key the key
	 * @param timefilter the timefilter
	 * @param limit the limit
	 * @return the timelogs
	 */
	public List<Timelog> getTimelogs(Key key, @Nullable TimeInterval timefilter, int limit) {
		if(limit == 0) return Collections.emptyList();
		if(limit < 0) limit = OUTPUT_MAX;
		
		BoundStatement query = timefilter != null
				? cassandraSelectTimelogsFiltered.bind(key.user, key.file, key.name, key.valueUri)
						.setInstant(4, timefilter.from())
						.setInstant(5, timefilter.until())
						.setInt(6, limit)
				: cassandraSelectTimelogs.bind(key.user, key.file, key.name, key.valueUri)
						.setInt(4, limit);
		
		List<Timelog> list = new ArrayList<>();
		for(Row r : cassandra.execute(query)) {
			list.add(new Timelog(key, r.getInstant(0), r.getLong(1)));
		}
		return list;
	}
	
	/**
	 * Gets the grid.
	 *
	 * @param key the key
	 * @return the grid
	 */
	public List<GridCell> getGrid(Key key) {
		Row inforow = cassandra.execute(cassandraSelectGridStatics.bind(key.user, key.file, key.name)
				.setList(3, Collections.singletonList(key.valueUri), String.class)).one();
		if(inforow == null) return Collections.emptyList();
		Coordinate size = new Coordinate(pos(inforow.getInt(1)), pos(inforow.getInt(0)));
		
		List<GridCell> grid = new ArrayList<>();
		for(Row r : cassandra.execute(cassandraSelectGrid.bind(key.user, key.file, key.name, key.valueUri))) {
			Coordinate pos = new Coordinate(pos(r.getInt(1)), pos(r.getInt(0)));
			grid.add(new GridCell(key, size, pos, r.getLong(2)));
		}
		return grid;
	}
	
	/**
	 * Creates the position batch.
	 *
	 * @param total the total
	 * @return the position batch
	 */
	public PositionBatch createPositionBatch(int total) {
		return new PositionBatch(total);
	}
	
	/**
	 * Batch inserter for timelog positions.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class PositionBatch extends CountKeyBatch<ElementKey> {
		
		/** The key inserter. */
		private final Function<ElementKey, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertPositionKey.bind(key.user, key.file, key.name);
		
		/**
		 * Instantiates a new position batch.
		 *
		 * @param total the total
		 */
		private PositionBatch(int total) {
			super(total);
		}
		
		/**
		 * Adds the.
		 *
		 * @param key the key
		 * @param time the time
		 * @param latitude the latitude
		 * @param longitude the longitude
		 * @param altitude the altitude
		 */
		//user,file,timelog,time,latitude,longitude,altitude
		public void add(ElementKey key, Instant time, double latitude, double longitude, double altitude) {
			super.add(key, keyInserter, cassandraInsertPosition.bind(key.user, key.file, key.name)
					.setInstant(3, time)
					.setInt(4, pos(latitude))
					.setInt(5, pos(longitude))
					.setInt(6, alt(altitude)));
		}
	}
	
	/**
	 * Creates the timelog batch.
	 *
	 * @param total the total
	 * @return the timelog batch
	 */
	public TimelogBatch createTimelogBatch(int total) {
		return new TimelogBatch(total);
	}
	
	/**
	 * Batch inserter for timelogs.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class TimelogBatch extends CountKeyBatch<Key> {
		
		/** The key inserter. */
		private final Function<Key, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertTimelogKey.bind(key.user, key.file, key.name, key.valueUri);

		/**
		 * Instantiates a new timelog batch.
		 *
		 * @param total the total
		 */
		private TimelogBatch(int total) {
			super(total);
		}
		
		/**
		 * Adds the.
		 *
		 * @param key the key
		 * @param time the time
		 * @param value the value
		 */
		//user,file,name,uri,time,value
		public void add(Key key, Instant time, long value) {
			super.add(key, keyInserter, cassandraInsertTimelog.bind(key.user, key.file, key.name, key.valueUri)
					.setInstant(4, time)
					.setLong(5, value));
		}
	}
	
	/**
	 * Creates the grid batch.
	 *
	 * @param total the total
	 * @return the grid batch
	 */
	public GridBatch createGridBatch(int total) {
		return new GridBatch(total);
	}
	
	/**
	 * Batch inserter for grid data.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class GridBatch extends CountKeyBatch<Key> {
		
		/** The key inserter. */
		private final Function<Key, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertGridKey.bind(key.user, key.file, key.name, key.valueUri);
		
		/**
		 * Instantiates a new grid batch.
		 *
		 * @param total the total
		 */
		private GridBatch(int total) {
			super(total);
		}
		
		/**
		 * Adds the.
		 *
		 * @param key the key
		 * @param size the size
		 * @param pos the pos
		 * @param value the value
		 */
		//user,file,name,value_uri,north_min,east_min,north_size,east_size,value
		public void add(Key key, Coordinate size, Coordinate pos, long value) {
			super.add(key, keyInserter, cassandraInsertGrid.bind(key.user, key.file, key.name, key.valueUri)
					.setInt(4, pos(pos.y))
					.setInt(5, pos(pos.x))
					.setInt(6, pos(size.y))
					.setInt(7, pos(size.x))
					.setLong(8, value));
		}
	}
	
	/**
	 * Delete position.
	 *
	 * @param key the key
	 */
	public void deletePosition(ElementKey key) {
		cassandra.execute(cassandraDeletePosition.bind(key.user, key.file, key.name));
		cassandra.execute(cassandraDeletePositionKey.bind(key.user, key.file, key.name));
	}
	
	/**
	 * Delete timelog.
	 *
	 * @param key the key
	 */
	public void deleteTimelog(Key key) {
		cassandra.execute(cassandraDeleteTimelog.bind(key.user, key.file, key.name, key.valueUri));
		cassandra.execute(cassandraDeleteTimelogKey.bind(key.user, key.file, key.name, key.valueUri));
	}
	
	/**
	 * Delete grid.
	 *
	 * @param key the key
	 */
	public void deleteGrid(Key key) {
		cassandra.execute(cassandraDeleteGrid.bind(key.user, key.file, key.name, key.valueUri));
		cassandra.execute(cassandraDeleteGridKey.bind(key.user, key.file, key.name, key.valueUri));
	}
	
	/**
	 * Tidy up.
	 *
	 * @param fileIds the file ids
	 */
	void tidyUp(Set<ObjectId> fileIds) {
		Set<String> files = new HashSet<>(fileIds.size());
		for(ObjectId fileid : fileIds) {
			files.add(File.toURI(fileid));
		}
		
		Set<ElementKey> deletePosition = new HashSet<>();
		Set<Key> deleteTimelog = new HashSet<>(), deleteGrid = new HashSet<>();
		
		for(Row row : cassandra.execute("SELECT user, file, name FROM position_keys")) {
			String g = row.getString(1);
			if(!files.contains(g))
				deletePosition.add(new ElementKey(row.getString(0), g, row.getString(2)));
		}
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM timelog_keys")) {
			String g = row.getString(1);
			if(!files.contains(g))
				deleteTimelog.add(new Key(row.getString(0), g, row.getString(2), row.getString(3)));
		}
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM grid_keys")) {
			String g = row.getString(1);
			if(!files.contains(g))
				deleteTimelog.add(new Key(row.getString(0), g, row.getString(2), row.getString(3)));
		}
		
		System.out.println("Delete " + deletePosition.size() + " positions: " + deletePosition.stream()
				.map(Object::toString).collect(Collectors.joining(", ")));
		System.out.println("Delete " + deleteTimelog.size() + " timelogs: " + deleteTimelog.stream()
				.map(Object::toString).collect(Collectors.joining(", ")));
		System.out.println("Delete " + deleteGrid.size() + " grids: " + deleteGrid.stream()
				.map(Object::toString).collect(Collectors.joining(", ")));
		
		int i = 0;
		for(ElementKey key : deletePosition) {
			System.out.format("Deleting Position %2d/%2d: %s...", ++i, deletePosition.size(), key.toString());
			try {
				deletePosition(key);
				System.out.println("OK");
			} catch(Throwable e) {
				System.out.println(e.getMessage());
			}
		}
		i = 0;
		for(Key key : deleteTimelog) {
			System.out.format("Deleting Timelog %2d/%2d: %s...", ++i, deleteTimelog.size(), key.toString());
			try {
				deleteTimelog(key);
				System.out.println("OK");
			} catch(Throwable e) {
				System.out.println(e.getMessage());
			}
		}
		i = 0;
		for(Key key : deleteGrid) {
			System.out.format("Deleting Grid %2d/%2d: %s...", ++i, deleteGrid.size(), key.toString());
			try {
				deleteGrid(key);
				System.out.println("OK");
			} catch(Throwable e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/** The Constant MAX_BATCH_SIZE. */
	private static final int MAX_BATCH_SIZE = 0xFFF;
	/**
	 * Base class for counted batches.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class CountBatch {
		
		/** The batch. */
		private final BatchStatementBuilder batch;
		
		/** The total. */
		private int count, total;
		
		/**
		 * Instantiates a new count batch.
		 *
		 * @param total the total
		 */
		protected CountBatch(int total) {
			this.batch = new BatchStatementBuilder(BatchType.UNLOGGED);
			this.count = 0;
			this.total = total;
		}
		
		/**
		 * Adds the.
		 *
		 * @param statement the statement
		 */
		protected void add(BatchableStatement<?> statement) {
			batch.addStatement(statement);
		}
		
		/**
		 * Do execute.
		 */
		private void doExecute() {
			cassandra.execute(batch.build());
			count += batch.getStatementsCount();
			batch.clearStatements();
		}
		
		/**
		 * Execute.
		 *
		 * @return true, if successful
		 */
		public boolean execute() {
			if(batch.getStatementsCount() > 0) {
				doExecute();
				return true;
			}
			return false;
		}
		
		/**
		 * Execute if full.
		 *
		 * @return true, if successful
		 */
		public boolean executeIfFull() {
			if(batch.getStatementsCount() >= MAX_BATCH_SIZE) {
				doExecute();
				return true;
			}
			return false;
		}
		
		/**
		 * Dec total.
		 */
		public void decTotal() {
			--total;
		}
		
		/**
		 * Gets the percent.
		 *
		 * @return the percent
		 */
		public int getPercent() {
			return (count * 100) / total;
		}
		
		/**
		 * Gets the count.
		 *
		 * @return the count
		 */
		public int getCount() {
			return count + batch.getStatementsCount();
		}
		
		/**
		 * Gets the total.
		 *
		 * @return the total
		 */
		public int getTotal() {
			return total;
		}
	}
	
	/**
	 * Base class for counted batches with key insertion.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 * @param <T> the key type
	 */
	public class CountKeyBatch<T extends FileKey> extends CountBatch {
		
		/** The keys. */
		private final HashSet<T> keys = new HashSet<>();
		
		/** The key batch. */
		private final CountBatch keyBatch = new CountBatch(0);

		/**
		 * Instantiates a new count key batch.
		 *
		 * @param total the total
		 */
		protected CountKeyBatch(int total) {
			super(total);
		}
		
		/**
		 * Adds the.
		 *
		 * @param key the key
		 * @param keyInserter the key inserter
		 * @param statement the statement
		 */
		protected void add(T key, Function<T, BatchableStatement<?>> keyInserter, BatchableStatement<?> statement) {
			if(keys.add(key))
				keyBatch.add(keyInserter.apply(key));
			super.add(statement);
		}
		
		/**
		 * Execute.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean execute() {
			keyBatch.execute();
			return super.execute();
		}
		
		/**
		 * Execute if full.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean executeIfFull() {
			keyBatch.executeIfFull();
			return super.executeIfFull();
		}
		
	}
	
	/**
	 * Pos.
	 *
	 * @param pos the pos
	 * @return the double
	 */
	public static final double pos(int pos) {
		return pos * 1e-7;
	}
	
	/**
	 * Pos.
	 *
	 * @param pos the pos
	 * @return the int
	 */
	public static final int pos(double pos) {
		return (int) (pos * 1e7);
	}
	
	/**
	 * Alt.
	 *
	 * @param altitude the altitude
	 * @return the double
	 */
	public static final double alt(int altitude) {
		return altitude * 1e-3;
	}
	
	/**
	 * Alt.
	 *
	 * @param altitude the altitude
	 * @return the int
	 */
	public static final int alt(double altitude) {
		return (int) (altitude * 1e3);
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		cassandra.close();
	}

}
