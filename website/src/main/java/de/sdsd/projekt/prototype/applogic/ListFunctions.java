package de.sdsd.projekt.prototype.applogic;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.ws.rs.NotSupportedException;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import de.sdsd.projekt.agrirouter.ARConfig;
import de.sdsd.projekt.agrirouter.request.AREndpoint;
import de.sdsd.projekt.prototype.data.ARCaps;
import de.sdsd.projekt.prototype.data.AREndpointStore;
import de.sdsd.projekt.prototype.data.DraftFormat;
import de.sdsd.projekt.prototype.data.DraftItem;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.LogEntry;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.StorageTask;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.websocket.SDSDEvent;

/**
 * Provides functions for all lists.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ListFunctions {
	
	/** The app. */
	private final ApplicationLogic app;
	
	/** The endpoints. */
	public final ListFunction<AREndpoint> endpoints;
	
	/** The capabilities. */
	public final ListFunction<ARCaps> capabilities;
	
	/** The files. */
	public final ListFunction<File> files;
	
	/** The storage tasks. */
	public final ListFunction<StorageTask> storageTasks;
	
	/** The logs. */
	public final ListFunction<LogEntry> logs;
	
	/** The types. */
	public final TypeList types;
	
	/** The draft formats. */
	public final ListFunction<DraftFormat> draftFormats;
	
	/** The draft items. */
	public final ListFunction<DraftItem> draftItems;

	/**
	 * Instantiates a new list functions.
	 *
	 * @param app the app
	 */
	ListFunctions(ApplicationLogic app) {
		this.app = app;
		this.endpoints = new EndpointList();
		this.capabilities = new CapabilityList();
		this.files = new FileList();
		this.storageTasks = new StorageTaskList();
		this.logs = new Log();
		this.types = new TypeList();
		this.draftFormats = new DraftFormatList();
		this.draftItems = new DraftItemList();
	}
	
	/**
	 * Clear all.
	 *
	 * @param user the user
	 */
	public void clearAll(User user) {
		endpoints.clear(user);
		capabilities.clear(user);
		files.clear(user);
		storageTasks.clear(user);
		logs.clear(user);
		draftFormats.clear(user);
		draftItems.clear(user);
	}
	
	/**
	 * Acts as an interface for functions, that can be applied to a list.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 * @param <T> type of the items in the list
	 */
	public abstract class ListFunction<T> extends SDSDEvent<User, T> {
		
		/** The mongo. */
		final MongoCollection<Document> mongo;
		
		/**
		 * Instantiates a new list function.
		 *
		 * @param collection the collection
		 */
		public ListFunction(@Nullable String collection) {
			this.mongo = collection != null ? app.mongo.sdsd.getCollection(collection) : null;
		}

		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		public boolean exists(User user, String id) {
			try {
				return exists(user, new ObjectId(id));
			} catch (IllegalArgumentException e) {
				throw new NotSupportedException();
			}
		}
		
		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		public boolean exists(User user, ObjectId id) {
			return exists(user, Filters.eq(id));
		}
		
		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		public boolean exists(User user, Bson filter) {
			return mongo.find(filter).iterator().hasNext();
		}
		
		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		public abstract List<T> getList(User user);

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the t
		 */
		public T get(User user, String id) {
			try {
				return get(user, new ObjectId(id));
			} catch (IllegalArgumentException e) {
				throw new NotSupportedException();
			}
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the t
		 */
		public abstract T get(User user, ObjectId id);

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		public abstract List<T> get(User user, Bson filter);

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		public boolean delete(User user, String id) {
			try {
				return delete(user, new ObjectId(id));
			} catch (IllegalArgumentException e) {
				throw new NotSupportedException();
			}
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		public boolean delete(User user, ObjectId id) {
			return delete(user, Filters.eq(id));
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		public abstract boolean delete(User user, Bson filter);
		
		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		public abstract boolean clear(User user);

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the t
		 */
		public abstract T add(User user, Document doc);

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		public abstract boolean update(User user, T entry, Bson update);
		
	}

	/**
	 * The Class EndpointList.
	 */
	private class EndpointList extends ListFunction<AREndpoint> {

		/**
		 * Instantiates a new endpoint list.
		 */
		public EndpointList() {
			super(null);
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<AREndpoint> getList(User user) {
			Collection<AREndpoint> endpoints = new AREndpointStore(app.redis, user).readAll();
			return endpoints.stream().sorted().collect(toList());
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the AR endpoint
		 */
		@Override
		public AREndpoint get(User user, String id) {
			return new AREndpointStore(app.redis, user).read(id);
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, String id) {
			new AREndpointStore(app.redis, user).delete(id);
			return true;
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			new AREndpointStore(app.redis, user).clear();
			return true;
		}

		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean exists(User user, String id) {
			return new AREndpointStore(app.redis, user).exists(id);
		}
		
		/**
		 * Sets the listener.
		 *
		 * @param user the user
		 * @param listener the listener
		 */
		@Override
		public void setListener(User user, SDSDListener<User, AREndpoint> listener) {
			throw new NotSupportedException();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the AR endpoint
		 */
		@Override
		public AREndpoint get(User user, ObjectId id) {
			throw new NotSupportedException();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<AREndpoint> get(User user, Bson filter) {
			throw new NotSupportedException();
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			throw new NotSupportedException();
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the AR endpoint
		 */
		@Override
		public AREndpoint add(User user, Document doc) {
			throw new NotSupportedException();
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, AREndpoint entry, Bson update) {
			throw new NotSupportedException();
		}
	};
	
	/**
	 * The Class CapabilityList.
	 */
	private class CapabilityList extends ListFunction<ARCaps> {

		/**
		 * Instantiates a new capability list.
		 */
		public CapabilityList() {
			super("capabilities");
			mongo.createIndex(Indexes.ascending(ARCaps.USER), new IndexOptions().unique(true));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<ARCaps> getList(User user) {
			Document doc = mongo.find(ARCaps.filter(user)).first();
			return doc == null ? Collections.emptyList() 
					: Collections.singletonList(new ARCaps(doc));
		}
		
		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean exists(User user, String id) {
			return mongo.countDocuments(ARCaps.filter(user)) > 0;
		}
		
		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the AR caps
		 */
		@Override
		public ARCaps get(User user, String id) {
			Document doc = mongo.find(ARCaps.filter(user)).first();
			if(doc != null) return new ARCaps(doc);
			try {
				ARConfig config = app.agrirouter.getOnboarding(user).getConfig();
				return ARCaps.getDefault(user, config);
			} catch (SDSDException e) {
				throw new RuntimeException(e);
			}
		}
		
		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the AR caps
		 */
		@Override
		public ARCaps add(User user, Document doc) {
			ARCaps arCaps = new ARCaps(doc);
			mongo.insertOne(doc);
			return arCaps;
		}
		
		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, ARCaps entry, Bson update) {
			if(!user.getName().equals(entry.getUser())) return false;
			return mongo.updateOne(entry.filter(), update).wasAcknowledged();
		}
		
		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, String id) {
			return mongo.deleteOne(ARCaps.filter(user)).wasAcknowledged();
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			return mongo.deleteOne(ARCaps.filter(user)).wasAcknowledged();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the AR caps
		 */
		@Override
		public ARCaps get(User user, ObjectId id) {
			throw new NotSupportedException();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<ARCaps> get(User user, Bson filter) {
			throw new NotSupportedException();
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			throw new NotSupportedException();
		}
		
	}

	/**
	 * The Class FileList.
	 */
	private class FileList extends ListFunction<File> {
		
		/**
		 * Instantiates a new file list.
		 */
		public FileList() {
			super("fileUploads");
			mongo.createIndex(Indexes.ascending(File.USER, File.SOURCE, File.TYPE, File.CREATED));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<File> getList(User user) {
			ArrayList<File> list = new ArrayList<>();
			for (Document d : mongo.find(File.filter(user))) {
				File file = new File(d);
				list.add(file);
			}
			return list;
		}
		
		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, ObjectId id) {
			if(app.file.deleteFile(user, get(user, id))) {
				trigger(user, null);
				return true;
			}
			return false;
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			boolean ok = true;
			for (Document d : mongo.find(File.filter(user))) {
				ok &= app.file.deleteFile(user, new File(d));
			}
			trigger(user, null);
			return ok;
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the file
		 */
		@Override
		public File get(User user, ObjectId id) {
			Document doc = mongo.find(File.filter(user, id)).first();
			if(doc == null) return File.getDefault(user, id);
			return new File(doc);
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<File> get(User user, Bson filter) {
			return StreamSupport.stream(mongo.find(Filters.and(File.filter(user), filter)).spliterator(), false)
					.map(File::new)
					.collect(toList());
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the file
		 */
		@Override
		public File add(User user, Document doc) {
			File file = new File(doc);
			mongo.insertOne(doc);
			trigger(user, file);
			return file;
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, File entry, Bson update) {
			if(mongo.updateOne(entry.filter(), update).wasAcknowledged()) {
				trigger(user, entry);
				return true;
			}
			return false;
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			throw new NotSupportedException();
		}
	};

	/**
	 * The Class StorageTaskList.
	 */
	private class StorageTaskList extends ListFunction<StorageTask> {
		
		/**
		 * Instantiates a new storage task list.
		 */
		public StorageTaskList() {
			super("storageTasks");
			mongo.createIndex(Indexes.ascending(StorageTask.USER));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<StorageTask> getList(User user) {
			return StreamSupport.stream(mongo.find(StorageTask.filter(user)).spliterator(), false)
					.map(StorageTask::new)
					.collect(toList());
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, ObjectId id) {
			return mongo.deleteOne(StorageTask.filter(user, id)).wasAcknowledged();
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			return mongo.deleteMany(StorageTask.filter(user)).wasAcknowledged();
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the storage task
		 */
		@Override
		public StorageTask add(User user, Document doc) {
			StorageTask storageTask = new StorageTask(doc);
			mongo.insertOne(doc);
			trigger(user, storageTask);
			return storageTask;
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the storage task
		 */
		@Override
		public StorageTask get(User user, ObjectId id) {
			Document doc = mongo.find(StorageTask.filter(user, id)).first();
			return doc != null ? new StorageTask(doc) : StorageTask.getDefault(id, user);
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<StorageTask> get(User user, Bson filter) {
			return StreamSupport.stream(mongo.find(Filters.and(StorageTask.filter(user), filter)).spliterator(), false)
					.map(StorageTask::new)
					.collect(toList());
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			return mongo.deleteMany(Filters.and(StorageTask.filter(user), filter)).wasAcknowledged();
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, StorageTask entry, Bson update) {
			return mongo.updateOne(entry.filter(), update).wasAcknowledged();
		}

		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean exists(User user, Bson filter) {
			return mongo.find(Filters.and(StorageTask.filter(user), filter)).iterator().hasNext();
		}
	};

	/**
	 * The Class Log.
	 */
	private class Log extends ListFunction<LogEntry> {
		
		/** The Constant LOGS. */
		public static final String USER = "user", LOGS = "logs";
		
		/** The Constant MAX_SIZE. */
		public static final int MAX_SIZE = 100;

		/**
		 * Instantiates a new log.
		 */
		public Log() {
			super("logs");
			mongo.createIndex(Indexes.ascending(Log.USER), new IndexOptions().unique(true));
		}

		/**
		 * Filter.
		 *
		 * @param user the user
		 * @return the bson
		 */
		private Bson filter(User user) {
			return Filters.eq(USER, user.getName());
		}

		/**
		 * Creates the.
		 *
		 * @param user the user
		 * @param log the log
		 * @return the document
		 */
		private Document create(User user, Document log) {
			return new Document()
					.append(USER, user.getName())
					.append(LOGS, Collections.singletonList(log));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<LogEntry> getList(User user) {
			Document doc = mongo.find(filter(user)).first();
			if(doc == null) return Collections.emptyList();
			Object obj = doc.get(LOGS);
			if(obj instanceof List<?>) 
				return ((List<?>)obj).stream()
						.filter(l -> l instanceof Document)
						.map(l -> (Document)l)
						.map(LogEntry::new)
						.collect(toList());
			else 
				return Collections.emptyList();
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			return mongo.deleteOne(filter(user)).wasAcknowledged();
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the log entry
		 */
		@Override
		public LogEntry add(User user, Document doc) {
			LogEntry entry = new LogEntry(doc);
			UpdateResult update = mongo.updateOne(filter(user), Updates.pushEach(LOGS, Collections.singletonList(doc), 
					new PushOptions().slice(-MAX_SIZE)));
			if(update.getMatchedCount() < 1)
				mongo.insertOne(create(user, doc));
			trigger(user, entry);
			return entry;
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the log entry
		 */
		@Override
		public LogEntry get(User user, ObjectId id) {
			throw new NotSupportedException();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<LogEntry> get(User user, Bson filter) {
			throw new NotSupportedException();
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			throw new NotSupportedException();
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, LogEntry entry, Bson update) {
			throw new NotSupportedException();
		}
	}

	/**
	 * The Class TypeList.
	 */
	public final class TypeList extends ListFunction<SDSDType> {
		
		/**
		 * Instantiates a new type list.
		 */
		private TypeList() {
			super("types");
			mongo.createIndex(Indexes.ascending(SDSDType.URI), new IndexOptions().unique(true));
			mongo.createIndex(Indexes.ascending(SDSDType.MIME, SDSDType.ARTYPE));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<SDSDType> getList(@Nullable User user) {
			return StreamSupport.stream((user != null ? mongo.find(SDSDType.filter(user)) : mongo.find()).spliterator(), false)
					.map(SDSDType::new)
					.sorted()
					.collect(Collectors.toList());
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			throw new NotSupportedException();
		}
		
		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param id the id
		 * @return true, if successful
		 */
		@Override
		public boolean exists(@Nullable User user, String id) {
			Bson filter = SDSDType.filter(id.startsWith(TripleFunctions.NS_WIKI) 
					? ResourceFactory.createResource(id) : new TripleFunctions.WikiFormat(id));
			if(user != null) filter = Filters.and(filter, SDSDType.filter(user));
			return mongo.find(filter).iterator().hasNext();
		}
		
		/**
		 * Exists.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean exists(@Nullable User user, Bson filter) {
			if(user != null) filter = Filters.and(filter, SDSDType.filter(user));
			return mongo.find(filter).iterator().hasNext();
		}
		
		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the SDSD type
		 */
		@Override
		public SDSDType get(@Nullable User user, String id) {
			Resource res = id.startsWith(TripleFunctions.NS_WIKI) ? ResourceFactory.createResource(id) : new TripleFunctions.WikiFormat(id);
			Bson filter = SDSDType.filter(res);
			if(user != null) filter = Filters.and(filter, SDSDType.filter(user));
			Document doc = mongo.find(filter).first();
			if(doc != null) return new SDSDType(doc);
			else {
				if(user == null) user = app.user.getUser("sdsd");
				return SDSDType.getDefault(user, res.getURI());
			}
		}
		
		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<SDSDType> get(@Nullable User user, Bson filter) {
			if(user != null) filter = Filters.and(filter, SDSDType.filter(user));
			return StreamSupport.stream(mongo.find(filter).spliterator(), false)
			.map(SDSDType::new)
			.sorted()
			.collect(Collectors.toList());
		}
		
		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the SDSD type
		 */
		@Override
		public SDSDType add(User user, Document doc) {
			SDSDType type = new SDSDType(doc);
			mongo.insertOne(doc);
			return type;
		}
		
		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			return mongo.deleteMany(Filters.and(SDSDType.filter(user), filter)).wasAcknowledged();
		}
		
		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, SDSDType entry, Bson update) {
			return mongo.updateOne(Filters.and(SDSDType.filter(user), entry.filter()), update).wasAcknowledged();
		}
		
		/**
		 * Sets the listener.
		 *
		 * @param user the user
		 * @param listener the listener
		 */
		@Override
		public void setListener(User user, SDSDListener<User, SDSDType> listener) {
			throw new NotSupportedException();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the SDSD type
		 */
		@Override
		public SDSDType get(User user, ObjectId id) {
			throw new NotSupportedException();
		}
		
	}
	
	/**
	 * The Class DraftFormatList.
	 */
	private class DraftFormatList extends ListFunction<DraftFormat> {
		
		/**
		 * Instantiates a new draft format list.
		 */
		public DraftFormatList() {
			super("draftFormats");
			mongo.createIndex(Indexes.ascending(DraftFormat.USER, DraftFormat.CONTENT_IDENTIFIER), new IndexOptions().unique(true));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<DraftFormat> getList(User user) {
			return StreamSupport.stream(mongo.find(DraftFormat.filter(user)).spliterator(), false)
					.map(DraftFormat::new)
					.sorted()
					.collect(Collectors.toList());
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			return mongo.deleteMany(DraftFormat.filter(user)).wasAcknowledged();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the draft format
		 */
		@Override
		public DraftFormat get(User user, ObjectId id) {
			Document doc = mongo.find(DraftFormat.filter(user, id)).first();
			return doc != null ? new DraftFormat(doc) : DraftFormat.getDefault(user, id);
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<DraftFormat> get(User user, Bson filter) {
			return StreamSupport.stream(mongo.find(Filters.and(DraftFormat.filter(user), filter)).spliterator(), false)
					.map(DraftFormat::new)
					.sorted()
					.collect(Collectors.toList());
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			return mongo.deleteMany(Filters.and(DraftFormat.filter(user), filter)).wasAcknowledged();
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the draft format
		 */
		@Override
		public DraftFormat add(User user, Document doc) {
			DraftFormat obj = new DraftFormat(doc);
			mongo.insertOne(doc);
			return obj;
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, DraftFormat entry, Bson update) {
			return mongo.updateOne(entry.filter(), update).wasAcknowledged();
		}
	}
	
	/**
	 * The Class DraftItemList.
	 */
	private class DraftItemList extends ListFunction<DraftItem> {
		
		/**
		 * Instantiates a new draft item list.
		 */
		public DraftItemList() {
			super("draftItems");
			mongo.createIndex(Indexes.ascending(DraftItem.USER, DraftItem.FORMAT, DraftItem.CONTENT_IDENTIFIER), new IndexOptions().unique(true));
		}

		/**
		 * Gets the list.
		 *
		 * @param user the user
		 * @return the list
		 */
		@Override
		public List<DraftItem> getList(User user) {
			return StreamSupport.stream(mongo.find(DraftItem.filter(user)).spliterator(), false)
					.map(DraftItem::new)
					.sorted()
					.collect(Collectors.toList());
		}

		/**
		 * Clear.
		 *
		 * @param user the user
		 * @return true, if successful
		 */
		@Override
		public boolean clear(User user) {
			return mongo.deleteMany(DraftItem.filter(user)).wasAcknowledged();
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param id the id
		 * @return the draft item
		 */
		@Override
		public DraftItem get(User user, ObjectId id) {
			Document doc = mongo.find(DraftItem.filter(user, id)).first();
			return doc != null ? new DraftItem(doc) : DraftItem.getDefault(user, id);
		}

		/**
		 * Gets the.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return the list
		 */
		@Override
		public List<DraftItem> get(User user, Bson filter) {
			return StreamSupport.stream(mongo.find(Filters.and(DraftItem.filter(user), filter)).spliterator(), false)
					.map(DraftItem::new)
					.sorted()
					.collect(Collectors.toList());
		}

		/**
		 * Delete.
		 *
		 * @param user the user
		 * @param filter the filter
		 * @return true, if successful
		 */
		@Override
		public boolean delete(User user, Bson filter) {
			return mongo.deleteMany(Filters.and(DraftFormat.filter(user), filter)).wasAcknowledged();
		}

		/**
		 * Adds the.
		 *
		 * @param user the user
		 * @param doc the doc
		 * @return the draft item
		 */
		@Override
		public DraftItem add(User user, Document doc) {
			DraftItem obj = new DraftItem(doc);
			mongo.insertOne(doc);
			return obj;
		}

		/**
		 * Update.
		 *
		 * @param user the user
		 * @param entry the entry
		 * @param update the update
		 * @return true, if successful
		 */
		@Override
		public boolean update(User user, DraftItem entry, Bson update) {
			return mongo.updateOne(entry.filter(), update).wasAcknowledged();
		}
	}

}
