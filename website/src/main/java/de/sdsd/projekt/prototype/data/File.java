package de.sdsd.projekt.prototype.data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Represents a file, stored in MongoDB.
 *
 * @see FileContent
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class File implements Serializable, Comparable<File> {

	private static final long serialVersionUID = 980396403670339158L;
	public static final String ID = "_id", USER = "user", NAME = "filename",
			SIZE = "size", TYPE = "type",
			CREATED = "created", LEVERAGED = "leveraged",  SOURCE = "source", TAGS = "tags", 
			EXPIRES = "expires", MODIFIED = "modified", CORE = "coredata";

	public static Bson filter(User user) {
		return Filters.eq(USER, user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static Bson filter(String filename) {
		return Filters.eq(NAME, filename);
	}
	
	public static Bson filterType(String type) {
		return Filters.eq(TYPE, type);
	}
	
	public static Bson filterSource(String source) {
		return Filters.eq(SOURCE, source);
	}
	
	public static Bson filterCreated(@Nullable Instant from, @Nullable Instant until) {
		if(from != null && until != null)
			return Filters.and(Filters.gte(CREATED, Date.from(from)), Filters.lte(CREATED, Date.from(until)));
		else if(from != null)
			return Filters.gte(CREATED, Date.from(from));
		else if(until != null)
			return Filters.lte(CREATED, Date.from(until));
		else
			return Filters.exists(CREATED);
	}

	public static final String SOURCE_USER_UPLOAD = "User upload";
	
	public static Document create(User user, String filename, int size, String type, 
			Instant created, String source, @Nullable Instant expires) {
		Document doc = new Document()
				.append(ID, new ObjectId())
				.append(USER, user.getName())
				.append(NAME, filename)
				.append(SIZE, size)
				.append(TYPE, type)
				.append(CREATED, Date.from(created))
				.append(SOURCE, source)
				.append(MODIFIED, Date.from(Instant.now()));
		if(expires != null)
			doc.append(EXPIRES, Date.from(expires));
		return doc;
	}
	
	public static File getDefault(User user, ObjectId id) {
		return new File(id, user);
	}

	private final ObjectId id;
	private String user;
	private String filename;
	private int size;
	private String type;
	private final Instant created;
	private Instant modified;
	@CheckForNull
	private final String source;
	private final Set<String> tags;
	@CheckForNull
	private Instant leveraged;
	@CheckForNull
	private final Instant expires;
	private boolean coreData;

	public File(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.filename = doc.getString(NAME);
		this.size = doc.getInteger(SIZE, 0);
		this.type = doc.getString(TYPE);
		Date date = doc.getDate(CREATED);
		this.created = date != null ? date.toInstant() : null;
		date = doc.getDate(MODIFIED);
		this.modified = date != null ? date.toInstant() : null;
		this.source = doc.getString(SOURCE);
		this.tags = Util
				.stringstreamFromMongo(doc.get(TAGS))
				.collect(Collectors.toSet());
		date = doc.getDate(LEVERAGED);
		this.leveraged = date != null ? date.toInstant() : null;
		date = doc.getDate(EXPIRES);
		this.expires = date != null ? date.toInstant() : null;
		this.coreData = doc.getBoolean(CORE, false);
	}
	
	protected File(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.filename = "File deleted";
		this.size = 0;
		this.type = "";
		this.created = null;
		this.leveraged = null;
		this.source = null;
		this.tags = Collections.emptySet();
		this.expires = null;
		this.coreData = false;
	}
	
	public static final String TYPE_TIMELOG = "https://app.sdsd-projekt.de/wikinormia.html?page=efdiTimelog";
	public static final String TYPE_GPSINFO = "https://app.sdsd-projekt.de/wikinormia.html?page=gps";
	public boolean isTimeLog() {
		return TYPE_TIMELOG.equals(type);
	}
	public boolean isGpsInfo() {
		return TYPE_GPSINFO.equals(type);
	}
	
	public Bson filter() {
		return Filters.eq(id);
	}

	public ObjectId getId() {
		return id;
	}

	public static final String toURI(String fileid) {
		return "sdsd:" + fileid;
	}
	public static final String toURI(ObjectId fileid) {
		return "sdsd:" + fileid.toHexString();
	}
	
	private static final Pattern fileIdRegex = Pattern.compile("[0-9A-Fa-f]{24}");
	public static final ObjectId toID(String fileUri) throws IllegalArgumentException {
		Matcher matcher = fileIdRegex.matcher(fileUri);
		if(matcher.find())
			return new ObjectId(matcher.group());
		throw new IllegalArgumentException("Input does not contain a valid file ID");
	}
	
	public String getURI() {
		return toURI(id);
	}
	
	public String getUser() {
		return user;
	}

	public Bson setUser(User user) {
		this.user = user.getName();
		return Updates.combine(
				Updates.set(USER, this.user), 
				setModified(Instant.now()));
	}

	public String getFilename() {
		return filename;
	}
	
	public Bson rename(String filename) {
		this.filename = filename;
		return Updates.combine(
				Updates.set(NAME, filename), 
				setModified(Instant.now()));
	}

	public int getSize() {
		return size;
	}
	
	public Bson setSize(int bytes) {
		this.size = bytes;
		return Updates.combine(
				Updates.set(SIZE, bytes), 
				setModified(Instant.now()));
	}
	
	public String getType() {
		return type;
	}

	public Bson setType(String type) {
		this.type = type;
		return Updates.combine(
				Updates.set(TYPE, type), 
				setModified(Instant.now()));
	}

	public Instant getCreated() {
		return created;
	}

	public Instant getModified() {
		return modified;
	}

	public Bson setModified(Instant modified) {
		this.modified = modified;
		return Updates.set(MODIFIED, Date.from(modified));
	}

	@CheckForNull
	public String getSource() {
		return source;
	}

	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}
	
	public Bson addTags(Collection<String> newtags) {
		tags.addAll(newtags);
		return Updates.set(TAGS, tags.stream()
				.collect(Util.asMongoStringList()));
	}
	
	public Bson deleteTags(Collection<String> deltags) {
		tags.removeAll(deltags);
		return Updates.set(TAGS, tags.stream()
				.collect(Util.asMongoStringList()));
	}
	
	public Bson clearTags() {
		tags.clear();
		return Updates.unset(TAGS);
	}

	@CheckForNull
	public Instant getLeveraged() {
		return leveraged;
	}
	
	public Bson setLeveraged(Instant leveraged) {
		this.leveraged = leveraged;
		return Updates.set(LEVERAGED, Date.from(leveraged));
	}
	
	@CheckForNull
	public Instant getExpires() {
		return expires;
	}
	
	public boolean isCoreData() {
		return coreData;
	}
	
	public Bson setCoreData(boolean coreData) {
		this.coreData = coreData;
		return coreData ? Updates.set(CORE, true) : Updates.unset(CORE);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof File))
			return false;
		File other = (File) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public int compareTo(File o) {
		return filename.compareToIgnoreCase(o.filename);
	}
	
	public static Comparator<File> CMP_RECENT = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			Instant m1 = o1.getModified(), m2 = o2.getModified();
			if(m1 != m2) {
				if(m1 == null) return 1;
				if(m2 == null) return -1;
				int cmp = m2.compareTo(m1);
				if(cmp != 0) return cmp;
			}
			Instant t1 = o1.getCreated(), t2 = o2.getCreated();
			if(t1 != t2) {
				if(t1 == null) return 1;
				if(t2 == null) return -1;
				int cmp = t2.compareTo(t1);
				if(cmp != 0) return cmp;
			}
			return o1.compareTo(o2);
		}
	};
	
	public static Comparator<File> CMP_RECENT_CORE = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			if(o1.isCoreData() != o2.isCoreData())
				return o1.isCoreData() ? -1 : 1;
			return File.CMP_RECENT.compare(o1, o2);
		}
	};

}
