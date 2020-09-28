package de.sdsd.projekt.prototype.data;

import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;

/**
 * Represents a wikinormia draft format, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DraftFormat implements Comparable<DraftFormat> {
	public static final String ID = "_id", USER = "user", CONTENT = "content", 
			IDENTIFIER = "identifier", LABEL = "label", COMMENT = "shortDescription", MIME = "mimeType", ARTYPE = "artype";
	public static final String CONTENT_IDENTIFIER = CONTENT + '.' + IDENTIFIER;

	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static Bson filter(User user, String identifier, @Nullable ObjectId id) {
		return id == null ? Filters.and(filter(user), Filters.eq(CONTENT_IDENTIFIER, identifier))
				: Filters.and(Filters.not(Filters.eq(id)), filter(user), Filters.eq(CONTENT_IDENTIFIER, identifier));
	}
	
	public static Document create(User user, JSONObject content) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(USER, user.getName())
				.append(CONTENT, Document.parse(content.toString()));
		return doc;
	}
	
	public static DraftFormat getDefault(User user, ObjectId id) {
		return new DraftFormat(id, user);
	}

	private final ObjectId id;
	private final String user;
	private Document content;

	public DraftFormat(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.content = doc.get(CONTENT, Document.class);
	}
	
	protected DraftFormat(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.content = new Document();
	}
	
	public Bson filter() {
		return Filters.eq(id);
	}

	public ObjectId getId() {
		return id;
	}
	
	public String getAuthor() {
		return user;
	}
	
	public Document getContent() {
		return content;
	}
	
	public JSONObject getJson() {
		return new JSONObject(content.toJson());
	}
	
	public Bson setContent(JSONObject content) {
		this.content = Document.parse(content.toString());
		return Updates.set(CONTENT, this.content);
	}
	
	public String getIdentifier() {
		return content.getString(IDENTIFIER);
	}
	
	public String getLabel() {
		return content.getString(LABEL);
	}
	
	public String getComment() {
		return content.getString(COMMENT);
	}
	
	public String getMimeType() {
		return content.getString(MIME);
	}
	
	public ARMessageType getArType() throws ARException {
		return ARMessageType.fromOrThrow(content.getString(ARTYPE));
	}

	@Override
	public int compareTo(DraftFormat o) {
		return getLabel().compareToIgnoreCase(o.getLabel());
	}
}
