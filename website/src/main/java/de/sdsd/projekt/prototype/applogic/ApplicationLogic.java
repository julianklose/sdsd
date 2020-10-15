package de.sdsd.projekt.prototype.applogic;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.Config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import de.sdsd.projekt.prototype.Main;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.LogEntry;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.jsonrpc.JsonRpcEndpoint;
import de.sdsd.projekt.prototype.rest.RestResource;

/**
 * Main application logic class that bundles all provided functions.
 *
 * @author Markus Schr&ouml;der
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ApplicationLogic extends ResourceConfig {
	
	/**
	 * All application settings.
	 */
	final JSONObject settings;

	/**
	 * Thread pool for asynchronous tasks.
	 */
	public final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10); //TODO: raise pool size when needed
	
	/**
	 * Client to access the redis cache.
	 */
	final RedissonClient redis;
	
	/**
	 * Helper class to access the used mongo db collections.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class Mongo implements Cloneable {
		
		/** The mongo client. */
		private final MongoClient mongoClient;
		
		/** The sdsd. */
		public final MongoDatabase sdsd;
		
		/**
		 * Connect to the mongo db.
		 * @param mongoSettings mongo connection settings
		 */
		public Mongo(JSONObject mongoSettings) {
			Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
			mongoLogger.setLevel(Level.WARNING);

			MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder()
					.connectTimeout(15000).socketTimeout(60000);
			
			if(mongoSettings.has("user")) {
				
				mongoClient = new MongoClient(
						new ServerAddress(mongoSettings.getString("address")), 
						MongoCredential.createCredential(
								mongoSettings.getString("user"), 
								mongoSettings.getString("database"), 
								mongoSettings.getString("password").toCharArray()), 
						optionsBuilder.build()
					);
				
			} else {
			
				mongoClient = new MongoClient(
						new ServerAddress(mongoSettings.getString("address")),
						optionsBuilder.build()
					);
			}
			
			sdsd = mongoClient.getDatabase(mongoSettings.getString("database"));
		}
	}
	/**
	 * Access to mongo db collections.
	 */
	final Mongo mongo;
	
	/** The user. */
	public final UserFunctions user;
	
	/** The agrirouter. */
	public final AgrirouterFunctions agrirouter;
	
	/** The list. */
	public final ListFunctions list;
	
	/** The dedup. */
	final DuplicateFinderFunctions dedup;
	
	/** The parser. */
	final ParserFunctions parser;
	
	/** The file. */
	public final FileFunctions file;
	
	/** The triple. */
	public final TripleFunctions triple;
	
	/** The table. */
	public final TableFunctions table;
	
	/** The service. */
	public final ServiceFunctions service;
	
	/** The wiki. */
	public final WikinormiaFunctions wiki;
	
	/** The geo. */
	public final GeoFunctions geo;
	
	/** The endpoints. */
	private Map<String, JsonRpcEndpoint> endpoints;
	

	/**
	 * Constructs and initialize the application logic.
	 *
	 * @param settings application settings
	 * @throws Exception the exception
	 */
	public ApplicationLogic(JSONObject settings) throws Exception {
		this.settings = settings;
		this.endpoints = new HashMap<>();
		
		//adds the REST resources
		register(new AppLogicBinder());
		register(MultiPartFeature.class);
		register(RestResource.class);
		
		JSONObject redisSettings = settings.getJSONObject("redis");
		Config cfg = new Config();
		cfg.useSingleServer().setAddress(redisSettings.getString("address"));
		cfg.setCodec(new SerializationCodec());
		redis = Redisson.create(cfg);

		//connects to mongodb
		mongo = new Mongo(settings.getJSONObject("mongodb"));
		
		//for testing we use in-memory, later it is another server
		//dataset = DatasetFactory.create();
		
		//for testing load isoxml data from resources
		//Txn.executeWrite(dataset, () -> {
		//	dataset.getDefaultModel().read(ApplicationLogic.class.getResourceAsStream("/example/isoxml.ttl"), null, "TTL");
		//});
		
		this.user = new UserFunctions(this);
		this.triple = new TripleFunctions(this);
		this.table = new TableFunctions(this);
		this.agrirouter = new AgrirouterFunctions(this);
		this.list = new ListFunctions(this);
		this.dedup = new DuplicateFinderFunctions(this);
		this.parser = new ParserFunctions(this);
		this.file = new FileFunctions(this);
		this.service = new ServiceFunctions(this);
		this.wiki = new WikinormiaFunctions(this);
		this.geo = new GeoFunctions(this);
		
		if(!Main.DEBUG_MODE)
			user.connectAllMqtt();
		
		executor.schedule(testRunner, 10, TimeUnit.SECONDS);
	}
	
	/**
	 * Binder for injection of the ApplicationLogic instance to resources.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class AppLogicBinder extends AbstractBinder {
		
		/**
		 * Configure.
		 */
		@Override
		protected void configure() {
			bind(ApplicationLogic.this).to(ApplicationLogic.class);
		}
	}
	
	/** The test runner. */
	@SuppressWarnings("unused")
	private Runnable testRunner = new Runnable() {
		private void eachFile(@Nullable Bson filter) {
			Collection<User> users = user.listUsers(false);
			int i = 0;
			for(User u : users) {
				++i;
				List<File> files = filter != null ? list.files.get(u, filter) : list.files.getList(u);
				int j = 0;
				for(File f : files) {
					++j;
					exec(u, f);
					System.out.format("Completed User %3d/%3d: File %3d/%3d: %s: %s\n",
							i, users.size(), j, files.size(), u.getName(), f.getFilename());
				}
			}
		}
		
		private void exec(User u, File f) {
			try {
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try {
				
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Tidy up.
	 */
	void tidyUp() {
		Set<ObjectId> fileIds = file.listAllFileIDs();
		file.tidyUp(fileIds);
		geo.tidyUp(fileIds);
		triple.tidyUp(fileIds);
		table.tidyUp(fileIds);
	}
	
	/**
	 * Access to session information.
	 */
	final SessionManager sessions = new SessionManager();
	
	/**
	 * Helper class to use the redis database as a session store.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	class SessionManager {
		
		/**
		 * Get the user associated with the session id.
		 * 
		 * @param sessionId session id from the session cookie
		 * @return associated user or null
		 */
		@CheckForNull
		public User getUser(String sessionId) {
			RBucket<String> bucket = redis.getBucket("session:" + sessionId);
			String username = bucket.get();
			return username == null ? null : user.getUser(username);
		}
		
		/**
		 * Associate a user to a session id.
		 * The session expires after 24 hours.
		 * 
		 * @param sessionId session id from the session cookie
		 * @param user user to associate or null to invalidate the session
		 */

		public void setUser(String sessionId, @Nullable User user) {
			RBucket<String> bucket = redis.getBucket("session:" + sessionId);
			if(user != null)
				bucket.set(user.getName(), 24, TimeUnit.HOURS);
			else
				bucket.delete();
		}
		
		/**
		 * Sets the admin.
		 *
		 * @param sessionId the session id
		 * @param login the login
		 */
		public void setAdmin(String sessionId, boolean login) {
			RBucket<Boolean> bucket = redis.getBucket("adminSession:" + sessionId);
			if(login)
				bucket.set(true, 24, TimeUnit.HOURS);
			else
				bucket.delete();
		}
		
		/**
		 * Checks if is admin.
		 *
		 * @param sessionId the session id
		 * @return true, if is admin
		 */
		public boolean isAdmin(String sessionId) {
			RBucket<Boolean> bucket = redis.getBucket("adminSession:" + sessionId);
			return bucket.isExists() ? bucket.get() : false;
		}
	}

	/**
	 * Gets the user.
	 *
	 * @param sessionId the session id
	 * @return the user
	 */
	@CheckForNull
	public User getUser(String sessionId) {
		if(sessionId == null) return null;
		return sessions.getUser(sessionId);
	}
	
	/**
	 * Log info.
	 *
	 * @param user the user
	 * @param format the format
	 * @param args the args
	 */
	public void logInfo(User user, String format, Object...args) {
		list.logs.add(user, LogEntry.create(Instant.now(), "info", String.format(format, args)));
	}
	
	/**
	 * Log error.
	 *
	 * @param user the user
	 * @param format the format
	 * @param args the args
	 */
	public void logError(User user, String format, Object...args) {
		try {
			list.logs.add(user, LogEntry.create(Instant.now(), "error", String.format(format, args)));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	//endpoints
	
	/**
	 * Register endpoint.
	 *
	 * @param name the name
	 * @param endpoint the endpoint
	 * @return the json rpc endpoint
	 */
	public JsonRpcEndpoint registerEndpoint(String name, JsonRpcEndpoint endpoint) {
		endpoints.put(name, endpoint);
		return endpoint;
	}
	
	/**
	 * Endpoint.
	 *
	 * @param name the name
	 * @return the json rpc endpoint
	 */
	public JsonRpcEndpoint endpoint(String name) {
		return endpoints.get(name);
	}
	
	/**
	 * Endpoint.
	 *
	 * @param <T> the generic type
	 * @param name the name
	 * @param type the type
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T> T endpoint(String name, Class<T> type) {
		return (T) endpoints.get(name);
	}
	
	/**
	 * Gets the endpoints.
	 *
	 * @return the endpoints
	 */
	public Set<Entry<String, JsonRpcEndpoint>> getEndpoints() {
		return endpoints.entrySet();
	}

	/**
	 * Gets the settings.
	 *
	 * @return the settings
	 */
	public JSONObject getSettings() {
		return settings;
	}
	
}
