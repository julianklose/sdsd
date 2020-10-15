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
	
	/** The Constant ARTYPE. */
	public static final String ID = "_id", USER = "user", CONTENT = "content", 
			IDENTIFIER = "identifier", LABEL = "label", COMMENT = "shortDescription", MIME = "mimeType", ARTYPE = "artype";
	
	/** The Constant CONTENT_IDENTIFIER. */
	public static final String CONTENT_IDENTIFIER = CONTENT + '.' + IDENTIFIER;

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
	 * Filter.
	 *
	 * @param user the user
	 * @param identifier the identifier
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, String identifier, @Nullable ObjectId id) {
		return id == null ? Filters.and(filter(user), Filters.eq(CONTENT_IDENTIFIER, identifier))
				: Filters.and(Filters.not(Filters.eq(id)), filter(user), Filters.eq(CONTENT_IDENTIFIER, identifier));
	}
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param content the content
	 * @return the document
	 */
	public static Document create(User user, JSONObject content) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(USER, user.getName())
				.append(CONTENT, Document.parse(content.toString()));
		return doc;
	}
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the default
	 */
	public static DraftFormat getDefault(User user, ObjectId id) {
		return new DraftFormat(id, user);
	}

	/** The id. */
	private final ObjectId id;
	
	/** The user. */
	private final String user;
	
	/** The content. */
	private Document content;

	/**
	 * Instantiates a new draft format.
	 *
	 * @param doc the doc
	 */
	public DraftFormat(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.content = doc.get(CONTENT, Document.class);
	}
	
	/**
	 * Instantiates a new draft format.
	 *
	 * @param id the id
	 * @param user the user
	 */
	protected DraftFormat(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.content = new Document();
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
	 * Gets the author.
	 *
	 * @return the author
	 */
	public String getAuthor() {
		return user;
	}
	
	/**
	 * Gets the content.
	 *
	 * @return the content
	 */
	public Document getContent() {
		return content;
	}
	
	/**
	 * Gets the json.
	 *
	 * @return the json
	 */
	public JSONObject getJson() {
		return new JSONObject(content.toJson());
	}
	
	/**
	 * Sets the content.
	 *
	 * @param content the content
	 * @return the bson
	 */
	public Bson setContent(JSONObject content) {
		this.content = Document.parse(content.toString());
		return Updates.set(CONTENT, this.content);
	}
	
	/**
	 * Gets the identifier.
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return content.getString(IDENTIFIER);
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return content.getString(LABEL);
	}
	
	/**
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	public String getComment() {
		return content.getString(COMMENT);
	}
	
	/**
	 * Gets the mime type.
	 *
	 * @return the mime type
	 */
	public String getMimeType() {
		return content.getString(MIME);
	}
	
	/**
	 * Gets the ar type.
	 *
	 * @return the ar type
	 * @throws ARException the AR exception
	 */
	public ARMessageType getArType() throws ARException {
		return ARMessageType.fromOrThrow(content.getString(ARTYPE));
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(DraftFormat o) {
		return getLabel().compareToIgnoreCase(o.getLabel());
	}
}
