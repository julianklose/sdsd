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
	
	private final ApplicationLogic app;
	
	final CqlSession cassandra;
	
	private final PreparedStatement cassandraSelectPositionKeys, cassandraSelectTimelogKeys, cassandraSelectGridKeys;
	private final PreparedStatement cassandraSelectPositions, cassandraSelectPositionsFiltered, cassandraSelectPositionsInfo;
	private final PreparedStatement cassandraSelectTimelogs, cassandraSelectTimelogsFiltered;
	private final PreparedStatement cassandraSelectGridStatics, cassandraSelectGrid;
	
	private final PreparedStatement cassandraInsertTimelogKey, cassandraInsertPositionKey, cassandraInsertGridKey;
	private final PreparedStatement cassandraInsertTimelog, cassandraInsertPosition, cassandraInsertGrid;
	private final PreparedStatement cassandraDeleteTimelogKey, cassandraDeletePositionKey, cassandraDeleteGridKey;
	private final PreparedStatement cassandraDeleteTimelog, cassandraDeletePosition, cassandraDeleteGrid;

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
		public final String user;
		public final String file;
		
		public FileKey(String user, String fileUri) {
			this.user = user;
			this.file = fileUri;
		}
		
		public String getUser() {
			return user;
		}

		public String getFile() {
			return file;
		}

		@Override
		public int hashCode() {
			return Objects.hash(file, user);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof FileKey))
				return false;
			FileKey other = (FileKey) obj;
			return Objects.equals(file, other.file) && Objects.equals(user, other.user);
		}
		
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
		public final String name;
		
		public ElementKey(String user, String fileUri, String name) {
			super(user, fileUri);
			this.name = name;
		}

		public String getName() {
			return name;
		}
		
		public FileKey toFileKey() {
			return new FileKey(user, file);
		}
		
		protected boolean check(Row row) {
			return row.getString(2).equals(name);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(name);
			return result;
		}

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
		public final String valueUri;
		
		public Key(String user, String fileUri, String name, String valueUri) {
			super(user, fileUri, name);
			this.valueUri = valueUri;
		}
		
		public String getValueURI() {
			return valueUri;
		}
		
		public ElementKey toElementKey() {
			return new ElementKey(user, file, name);
		}
		
		@Override
		protected boolean check(Row row) {
			return super.check(row) && row.getString(3).equals(valueUri);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(valueUri);
			return result;
		}

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

		@Override
		public String toString() {
			return "Key [user=" + user + ", file=" + file + ", name=" + name + ", valueUri=" + valueUri + "]";
		}
	}
	
	List<ElementKey> listPositionKeys() {
		List<ElementKey> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name FROM position_keys")) {
			list.add(new ElementKey(row.getString(0), row.getString(1), row.getString(2)));
		}
		return list;
	}
	
	List<Key> listTimelogKeys() {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM timelog_keys")) {
			list.add(new Key(row.getString(0), row.getString(1), row.getString(2), row.getString(3)));
		}
		return list;
	}
	
	List<Key> listGridKeys() {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute("SELECT user, file, name, value_uri FROM grid_keys")) {
			list.add(new Key(row.getString(0), row.getString(1), row.getString(2), row.getString(3)));
		}
		return list;
	}
 	
	public List<ElementKey> listPositionKeys(FileKey key) {
		List<ElementKey> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectPositionKeys.bind(key.user, key.file))) {
			list.add(new ElementKey(key.user, key.file, row.getString(2)));
		}
		return list;
	}
	
	public List<Key> listTimelogKeys(FileKey key) {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectTimelogKeys.bind(key.user, key.file))) {
			list.add(new Key(key.user, key.file, row.getString(2), row.getString(3)));
		}
		return list;
	}
	
	public List<String> listTimelogs(FileKey key) {
		List<String> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectPositionKeys.bind(key.user, key.file))) {
			list.add(row.getString(3));
		}
		return list;
	}
	
	public List<String> listTimelogValueUris(ElementKey key) {
		List<String> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectTimelogKeys.bind(key.user, key.file))) {
			if(key.check(row))
				list.add(row.getString(3));
		}
		return list;
	}
	
	public List<Key> listGridKeys(FileKey key) {
		List<Key> list = new ArrayList<>();
		for(Row row : cassandra.execute(cassandraSelectGridKeys.bind(key.user, key.file))) {
			list.add(new Key(key.user, key.file, row.getString(2), row.getString(3)));
		}
		return list;
	}
	
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
		public final long count;
		public final Instant from, until;
		
		protected TimelogInfo(long count, Instant from, Instant until) {
			this.count = count;
			this.from = from;
			this.until = until;
		}

		public long getCount() {
			return count;
		}

		public Instant getFrom() {
			return from;
		}

		public Instant getUntil() {
			return until;
		}

		@Override
		public String toString() {
			return "TimelogInfo [count=" + count + ", from=" + from + ", until=" + until + "]";
		}
	}
	
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
		@CheckForNull
		private final Instant from, until;
		
		public TimeInterval(@Nullable Instant from, @Nullable Instant until) {
			this.from = from;
			this.until = until;
		}
		
		@CheckForNull
		public static TimeInterval from(JSONObject timeFilter) {
			return timeFilter == null || (!timeFilter.has("from") && !timeFilter.has("until")) ? null : new TimeInterval(
					timeFilter.has("from") ? Instant.parse(timeFilter.getString("from")) : null,
					timeFilter.has("until") ? Instant.parse(timeFilter.getString("until")) : null);
		}

		public boolean isFrom() {
			return from != null;
		}

		public boolean isUntil() {
			return until != null;
		}
		
		public Instant from() {
			return from != null ? from : Instant.EPOCH;
		}

		public Instant until() {
			return until != null ? until : Instant.now();
		}

		@Override
		public String toString() {
			return "TimeInterval [from=" + from + ", until=" + until + "]";
		}
	}
	
	public static final int OUTPUT_MAX = 50000;
	
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
	
	public List<ValueInfo> getValueInfos(String fileUri, List<String> valueUris) {
		return ValueInfo.getValueInfos(app, fileUri, valueUris);
	}
	
	public ValueInfo getValueInfo(String fileUri, String valueUri) {
		return ValueInfo.getValueInfo(app, fileUri, valueUri);
	}
	
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
	
	public PositionBatch createPositionBatch(int total) {
		return new PositionBatch(total);
	}
	
	/**
	 * Batch inserter for timelog positions.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class PositionBatch extends CountKeyBatch<ElementKey> {
		private final Function<ElementKey, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertPositionKey.bind(key.user, key.file, key.name);
		
		private PositionBatch(int total) {
			super(total);
		}
		
		//user,file,timelog,time,latitude,longitude,altitude
		public void add(ElementKey key, Instant time, double latitude, double longitude, double altitude) {
			super.add(key, keyInserter, cassandraInsertPosition.bind(key.user, key.file, key.name)
					.setInstant(3, time)
					.setInt(4, pos(latitude))
					.setInt(5, pos(longitude))
					.setInt(6, alt(altitude)));
		}
	}
	
	public TimelogBatch createTimelogBatch(int total) {
		return new TimelogBatch(total);
	}
	
	/**
	 * Batch inserter for timelogs.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class TimelogBatch extends CountKeyBatch<Key> {
		private final Function<Key, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertTimelogKey.bind(key.user, key.file, key.name, key.valueUri);

		private TimelogBatch(int total) {
			super(total);
		}
		
		//user,file,name,uri,time,value
		public void add(Key key, Instant time, long value) {
			super.add(key, keyInserter, cassandraInsertTimelog.bind(key.user, key.file, key.name, key.valueUri)
					.setInstant(4, time)
					.setLong(5, value));
		}
	}
	
	public GridBatch createGridBatch(int total) {
		return new GridBatch(total);
	}
	
	/**
	 * Batch inserter for grid data.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class GridBatch extends CountKeyBatch<Key> {
		private final Function<Key, BatchableStatement<?>> keyInserter = 
				key -> cassandraInsertGridKey.bind(key.user, key.file, key.name, key.valueUri);
		
		private GridBatch(int total) {
			super(total);
		}
		
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
	
	public void deletePosition(ElementKey key) {
		cassandra.execute(cassandraDeletePosition.bind(key.user, key.file, key.name));
		cassandra.execute(cassandraDeletePositionKey.bind(key.user, key.file, key.name));
	}
	
	public void deleteTimelog(Key key) {
		cassandra.execute(cassandraDeleteTimelog.bind(key.user, key.file, key.name, key.valueUri));
		cassandra.execute(cassandraDeleteTimelogKey.bind(key.user, key.file, key.name, key.valueUri));
	}
	
	public void deleteGrid(Key key) {
		cassandra.execute(cassandraDeleteGrid.bind(key.user, key.file, key.name, key.valueUri));
		cassandra.execute(cassandraDeleteGridKey.bind(key.user, key.file, key.name, key.valueUri));
	}
	
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
	
	private static final int MAX_BATCH_SIZE = 0xFFF;
	/**
	 * Base class for counted batches.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class CountBatch {
		private final BatchStatementBuilder batch;
		private int count, total;
		
		protected CountBatch(int total) {
			this.batch = new BatchStatementBuilder(BatchType.UNLOGGED);
			this.count = 0;
			this.total = total;
		}
		
		protected void add(BatchableStatement<?> statement) {
			batch.addStatement(statement);
		}
		
		private void doExecute() {
			cassandra.execute(batch.build());
			count += batch.getStatementsCount();
			batch.clearStatements();
		}
		
		public boolean execute() {
			if(batch.getStatementsCount() > 0) {
				doExecute();
				return true;
			}
			return false;
		}
		
		public boolean executeIfFull() {
			if(batch.getStatementsCount() >= MAX_BATCH_SIZE) {
				doExecute();
				return true;
			}
			return false;
		}
		
		public void decTotal() {
			--total;
		}
		
		public int getPercent() {
			return (count * 100) / total;
		}
		
		public int getCount() {
			return count + batch.getStatementsCount();
		}
		
		public int getTotal() {
			return total;
		}
	}
	
	/**
	 * Base class for counted batches with key insertion.
	 * 
	 * @param <T> the key type
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class CountKeyBatch<T extends FileKey> extends CountBatch {
		private final HashSet<T> keys = new HashSet<>();
		private final CountBatch keyBatch = new CountBatch(0);

		protected CountKeyBatch(int total) {
			super(total);
		}
		
		protected void add(T key, Function<T, BatchableStatement<?>> keyInserter, BatchableStatement<?> statement) {
			if(keys.add(key))
				keyBatch.add(keyInserter.apply(key));
			super.add(statement);
		}
		
		@Override
		public boolean execute() {
			keyBatch.execute();
			return super.execute();
		}
		
		@Override
		public boolean executeIfFull() {
			keyBatch.executeIfFull();
			return super.executeIfFull();
		}
		
	}
	
	public static final double pos(int pos) {
		return pos * 1e-7;
	}
	
	public static final int pos(double pos) {
		return (int) (pos * 1e7);
	}
	
	public static final double alt(int altitude) {
		return altitude * 1e-3;
	}
	
	public static final int alt(double altitude) {
		return (int) (altitude * 1e3);
	}

	@Override
	public void close() throws IOException {
		cassandra.close();
	}

}
