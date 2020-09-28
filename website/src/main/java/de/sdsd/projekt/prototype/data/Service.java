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
 * @see ServiceInstance
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class Service implements Comparable<Service> {

	public static final String ID = "_id", TOKEN = "token", NAME = "name", PARAMETER = "parameter", ACCESS = "access",
			AUTHOR = "author", ADDED = "added", VISIBLE = "visible";

	public static Bson filter(ObjectId id) {
		return Filters.eq(id);
	}
	
	public static Bson filterToken(String token) {
		return Filters.eq(TOKEN, token);
	}
	
	public static Bson filterName(String name) {
		return Filters.eq(NAME, name);
	}
	
	public static Bson filterAuthor(User author) {
		return Filters.eq(AUTHOR, author.getName());
	}
	
	public static Bson filterVisible(boolean visible) {
		return Filters.eq(VISIBLE, visible);
	}
	
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
	
	public static Service getDefault(ObjectId id) {
		return new Service(id);
	}

	private final ObjectId id;
	private final String token;
	private final String name;
	private final JSONArray parameter;
	private final Set<String> access;
	private final String author;
	private final Instant added;
	private boolean visible;

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
	
	public Bson filter() {
		return Filters.eq(id);
	}
	
	public ObjectId getId() {
		return id;
	}
	
	public String getToken() {
		return token;
	}

	public String getName() {
		return name;
	}
	
	public JSONArray getParameter() {
		return parameter;
	}
	
	public Set<String> getAccess() {
		return access;
	}

	public String getAuthor() {
		return author;
	}

	public Instant getAdded() {
		return added;
	}

	public boolean isVisible() {
		return visible;
	}

	public Bson setVisible(boolean visible) {
		this.visible = visible;
		return Updates.set(VISIBLE, visible);
	}

	@Override
	public int compareTo(Service o) {
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

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
