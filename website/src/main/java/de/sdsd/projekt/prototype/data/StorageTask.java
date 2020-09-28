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
	
	public static final String ID = "_id", USER = "user", LABEL = "label",
			TYPE = "type", TYPE_EQ = "typeEquals",
			SOURCE = "source", SOURCE_EQ = "sourceEquals",
			FROM = "from", UNTIL = "until",
			STORE_FOR = "storeFor", STORE_UNTIL = "storeUntil";

	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static StorageTaskBuilder create(User user, String label) {
		return new StorageTaskBuilder(user, label);
	}
	
	/**
	 * Builder class for a storage task.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class StorageTaskBuilder {
		private final Document doc;
		
		public StorageTaskBuilder(User user, String label) {
			doc = new Document()
					.append(USER, user.getName())
					.append(LABEL, label);
		}
		
		public StorageTaskBuilder setType(String type, boolean equals) {
			doc.append(TYPE, type).append(TYPE_EQ, equals);
			return this;
		}
		
		public StorageTaskBuilder setSource(String source, boolean equals) {
			doc.append(SOURCE, source).append(SOURCE_EQ, equals);
			return this;
		}
		
		public StorageTaskBuilder setFrom(Instant from) {
			doc.append(FROM, Date.from(from));
			return this;
		}
		
		public StorageTaskBuilder setUntil(Instant until) {
			doc.append(UNTIL, Date.from(until));
			return this;
		}

		public Document build(Duration storeFor) {
			doc.remove(STORE_UNTIL);
			return doc.append(STORE_FOR, storeFor.toDays());
		}
		
		public Document build(Instant storeUntil) {
			doc.remove(STORE_FOR);
			return doc.append(STORE_UNTIL, Date.from(storeUntil));
		}
	}
	
	public static StorageTask getDefault(ObjectId id, User user) {
		return new StorageTask(id, user);
	}
	
	private final ObjectId id;
	private final String user;
	private final String label;
	
	@Nullable
	private final String type;
	private final boolean typeEquals;
	@Nullable
	private final String source;
	private final boolean sourceEquals;
	@Nullable
	private final Instant from;
	@Nullable
	private final Instant until;
	
	@Nullable
	private final Duration storeFor;
	@Nullable
	private final Instant storeUntil;
	
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
	
	public boolean check(String type, String source, Instant created) {
		return type.equals(this.type) == typeEquals
				&& source.equals(this.source) == sourceEquals
				&& (from == null || created.isAfter(from))
				&& (until == null || created.isBefore(until));
	}
	
	public Instant getExpire() {
		if(storeUntil != null) return storeUntil;
		if(storeFor != null) return Instant.now().plus(storeFor);
		return Instant.MAX;
	}
	
	public Bson filter() {
		return Filters.eq(id);
	}

	public ObjectId getId() {
		return id;
	}

	public String getUser() {
		return user;
	}

	public String getLabel() {
		return label;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public boolean isTypeEquals() {
		return typeEquals;
	}

	@Nullable
	public String getSource() {
		return source;
	}

	public boolean isSourceEquals() {
		return sourceEquals;
	}

	@Nullable
	public Instant getFrom() {
		return from;
	}

	@Nullable
	public Instant getUntil() {
		return until;
	}

	@Nullable
	public Duration getStoreFor() {
		return storeFor;
	}

	@Nullable
	public Instant getStoreUntil() {
		return storeUntil;
	}
}
