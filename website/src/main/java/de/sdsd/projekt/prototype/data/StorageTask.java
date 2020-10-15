package de.sdsd.projekt.prototype.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

/**
 * Represents a storage task, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class StorageTask {
	
	/** The Constant STORE_UNTIL. */
	public static final String ID = "_id", USER = "user", LABEL = "label",
			TYPE = "type", TYPE_EQ = "typeEquals",
			SOURCE = "source", SOURCE_EQ = "sourceEquals",
			FROM = "from", UNTIL = "until",
			STORE_FOR = "storeFor", STORE_UNTIL = "storeUntil";

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param label the label
	 * @return the storage task builder
	 */
	public static StorageTaskBuilder create(User user, String label) {
		return new StorageTaskBuilder(user, label);
	}
	
	/**
	 * Builder class for a storage task.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class StorageTaskBuilder {
		
		/** The doc. */
		private final Document doc;
		
		/**
		 * Instantiates a new storage task builder.
		 *
		 * @param user the user
		 * @param label the label
		 */
		public StorageTaskBuilder(User user, String label) {
			doc = new Document()
					.append(USER, user.getName())
					.append(LABEL, label);
		}
		
		/**
		 * Sets the type.
		 *
		 * @param type the type
		 * @param equals the equals
		 * @return the storage task builder
		 */
		public StorageTaskBuilder setType(String type, boolean equals) {
			doc.append(TYPE, type).append(TYPE_EQ, equals);
			return this;
		}
		
		/**
		 * Sets the source.
		 *
		 * @param source the source
		 * @param equals the equals
		 * @return the storage task builder
		 */
		public StorageTaskBuilder setSource(String source, boolean equals) {
			doc.append(SOURCE, source).append(SOURCE_EQ, equals);
			return this;
		}
		
		/**
		 * Sets the from.
		 *
		 * @param from the from
		 * @return the storage task builder
		 */
		public StorageTaskBuilder setFrom(Instant from) {
			doc.append(FROM, Date.from(from));
			return this;
		}
		
		/**
		 * Sets the until.
		 *
		 * @param until the until
		 * @return the storage task builder
		 */
		public StorageTaskBuilder setUntil(Instant until) {
			doc.append(UNTIL, Date.from(until));
			return this;
		}

		/**
		 * Builds the.
		 *
		 * @param storeFor the store for
		 * @return the document
		 */
		public Document build(Duration storeFor) {
			doc.remove(STORE_UNTIL);
			return doc.append(STORE_FOR, storeFor.toDays());
		}
		
		/**
		 * Builds the.
		 *
		 * @param storeUntil the store until
		 * @return the document
		 */
		public Document build(Instant storeUntil) {
			doc.remove(STORE_FOR);
			return doc.append(STORE_UNTIL, Date.from(storeUntil));
		}
	}
	
	/**
	 * Gets the default.
	 *
	 * @param id the id
	 * @param user the user
	 * @return the default
	 */
	public static StorageTask getDefault(ObjectId id, User user) {
		return new StorageTask(id, user);
	}
	
	/** The id. */
	private final ObjectId id;
	
	/** The user. */
	private final String user;
	
	/** The label. */
	private final String label;
	
	/** The type. */
	@Nullable
	private final String type;
	
	/** The type equals. */
	private final boolean typeEquals;
	
	/** The source. */
	@Nullable
	private final String source;
	
	/** The source equals. */
	private final boolean sourceEquals;
	
	/** The from. */
	@Nullable
	private final Instant from;
	
	/** The until. */
	@Nullable
	private final Instant until;
	
	/** The store for. */
	@Nullable
	private final Duration storeFor;
	
	/** The store until. */
	@Nullable
	private final Instant storeUntil;
	
	/**
	 * Instantiates a new storage task.
	 *
	 * @param doc the doc
	 */
	public StorageTask(Document doc) {
		id = doc.getObjectId(ID);
		user = doc.getString(USER);
		label = doc.getString(LABEL);
		
		type = doc.getString(TYPE);
		typeEquals = doc.getBoolean(TYPE_EQ, false);
		source = doc.getString(SOURCE);
		sourceEquals = doc.getBoolean(SOURCE_EQ, false);
		Date d = doc.getDate(FROM);
		from = d != null ? d.toInstant() : null;
		d = doc.getDate(UNTIL);
		until = d != null ? d.toInstant() : null;
		
		Long l = doc.getLong(STORE_FOR);
		storeFor = l != null ? Duration.ofDays(l) : null;
		d = doc.getDate(STORE_UNTIL);
		storeUntil = d != null ? d.toInstant() : null;
	}
	
	/**
	 * Instantiates a new storage task.
	 *
	 * @param id the id
	 * @param user the user
	 */
	protected StorageTask(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.label = "Unknown task";
		
		this.type = null;
		this.typeEquals = false;
		this.source = null;
		this.sourceEquals = false;
		this.from = null;
		this.until = null;
		
		this.storeFor = null;
		this.storeUntil = Instant.now();
	}
	
	/**
	 * Check.
	 *
	 * @param type the type
	 * @param source the source
	 * @param created the created
	 * @return true, if successful
	 */
	public boolean check(String type, String source, Instant created) {
		return type.equals(this.type) == typeEquals
				&& source.equals(this.source) == sourceEquals
				&& (from == null || created.isAfter(from))
				&& (until == null || created.isBefore(until));
	}
	
	/**
	 * Gets the expire.
	 *
	 * @return the expire
	 */
	public Instant getExpire() {
		if(storeUntil != null) return storeUntil;
		if(storeFor != null) return Instant.now().plus(storeFor);
		return Instant.MAX;
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(id);
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public ObjectId getId() {
		return id;
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
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	@Nullable
	public String getType() {
		return type;
	}

	/**
	 * Checks if is type equals.
	 *
	 * @return true, if is type equals
	 */
	public boolean isTypeEquals() {
		return typeEquals;
	}

	/**
	 * Gets the source.
	 *
	 * @return the source
	 */
	@Nullable
	public String getSource() {
		return source;
	}

	/**
	 * Checks if is source equals.
	 *
	 * @return true, if is source equals
	 */
	public boolean isSourceEquals() {
		return sourceEquals;
	}

	/**
	 * Gets the from.
	 *
	 * @return the from
	 */
	@Nullable
	public Instant getFrom() {
		return from;
	}

	/**
	 * Gets the until.
	 *
	 * @return the until
	 */
	@Nullable
	public Instant getUntil() {
		return until;
	}

	/**
	 * Gets the store for.
	 *
	 * @return the store for
	 */
	@Nullable
	public Duration getStoreFor() {
		return storeFor;
	}

	/**
	 * Gets the store until.
	 *
	 * @return the store until
	 */
	@Nullable
	public Instant getStoreUntil() {
		return storeUntil;
	}
}
