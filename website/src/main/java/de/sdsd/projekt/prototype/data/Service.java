package de.sdsd.projekt.prototype.data;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Represents a service, stored in MongoDB.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ServiceInstance
 */
public class Service implements Comparable<Service> {

	/** The Constant VISIBLE. */
	public static final String ID = "_id", TOKEN = "token", NAME = "name", PARAMETER = "parameter", ACCESS = "access",
			AUTHOR = "author", ADDED = "added", VISIBLE = "visible";

	/**
	 * Filter.
	 *
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(ObjectId id) {
		return Filters.eq(id);
	}
	
	/**
	 * Filter token.
	 *
	 * @param token the token
	 * @return the bson
	 */
	public static Bson filterToken(String token) {
		return Filters.eq(TOKEN, token);
	}
	
	/**
	 * Filter name.
	 *
	 * @param name the name
	 * @return the bson
	 */
	public static Bson filterName(String name) {
		return Filters.eq(NAME, name);
	}
	
	/**
	 * Filter author.
	 *
	 * @param author the author
	 * @return the bson
	 */
	public static Bson filterAuthor(User author) {
		return Filters.eq(AUTHOR, author.getName());
	}
	
	/**
	 * Filter visible.
	 *
	 * @param visible the visible
	 * @return the bson
	 */
	public static Bson filterVisible(boolean visible) {
		return Filters.eq(VISIBLE, visible);
	}
	
	/**
	 * Creates the.
	 *
	 * @param author the author
	 * @param name the name
	 * @param parameter the parameter
	 * @param access the access
	 * @return the document
	 */
	public static Document create(User author, String name, JSONArray parameter, Set<String> access) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(TOKEN, Util.createSecureToken())
				.append(NAME, name)
				.append(PARAMETER, parameter.toString())
				.append(ACCESS, access.stream().collect(Util.asMongoStringList()))
				.append(AUTHOR, author.getName())
				.append(ADDED, Date.from(Instant.now()))
				.append(VISIBLE, false);
		return doc;
	}
	
	/**
	 * Gets the default.
	 *
	 * @param id the id
	 * @return the default
	 */
	public static Service getDefault(ObjectId id) {
		return new Service(id);
	}

	/** The id. */
	private final ObjectId id;
	
	/** The token. */
	private final String token;
	
	/** The name. */
	private final String name;
	
	/** The parameter. */
	private final JSONArray parameter;
	
	/** The access. */
	private final Set<String> access;
	
	/** The author. */
	private final String author;
	
	/** The added. */
	private final Instant added;
	
	/** The visible. */
	private boolean visible;

	/**
	 * Instantiates a new service.
	 *
	 * @param doc the doc
	 */
	public Service(Document doc) {
		this.id = doc.getObjectId(ID);
		this.token = doc.getString(TOKEN);
		this.name = doc.getString(NAME);
		this.parameter = new JSONArray(doc.getString(PARAMETER));
		this.access = Util.stringstreamFromMongo(doc.get(ACCESS)).collect(Collectors.toSet());
		this.author = doc.getString(AUTHOR);
		this.added = doc.getDate(ADDED).toInstant();
		this.visible = doc.getBoolean(VISIBLE);
	}
	
	/**
	 * Instantiates a new service.
	 *
	 * @param id the id
	 */
	protected Service(ObjectId id) {
		this.id = id;
		this.token = "";
		this.name = "Unknown Service";
		this.parameter = new JSONArray();
		this.access = Collections.emptySet();
		this.author = "Unknown Author";
		this.added = Instant.now();
		this.visible = false;
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
	 * Gets the token.
	 *
	 * @return the token
	 */
	public String getToken() {
		return token;
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
	 * Gets the parameter.
	 *
	 * @return the parameter
	 */
	public JSONArray getParameter() {
		return parameter;
	}
	
	/**
	 * Gets the access.
	 *
	 * @return the access
	 */
	public Set<String> getAccess() {
		return access;
	}

	/**
	 * Gets the author.
	 *
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Gets the added.
	 *
	 * @return the added
	 */
	public Instant getAdded() {
		return added;
	}

	/**
	 * Checks if is visible.
	 *
	 * @return true, if is visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Sets the visible.
	 *
	 * @param visible the visible
	 * @return the bson
	 */
	public Bson setVisible(boolean visible) {
		this.visible = visible;
		return Updates.set(VISIBLE, visible);
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(Service o) {
		return name.compareToIgnoreCase(o.name);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
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
		if (!(obj instanceof Service))
			return false;
		Service other = (Service) obj;
		return Objects.equals(id, other.id);
	}

}
