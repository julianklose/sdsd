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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see FileContent
 */
public class File implements Serializable, Comparable<File> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 980396403670339158L;
	
	/** The Constant CORE. */
	public static final String ID = "_id", USER = "user", NAME = "filename",
			SIZE = "size", TYPE = "type", VALIDATION = "validation",
			CREATED = "created", LEVERAGED = "leveraged",  SOURCE = "source", TAGS = "tags", 
			EXPIRES = "expires", MODIFIED = "modified", CORE = "coredata";

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
	 * @param filename the filename
	 * @return the bson
	 */
	public static Bson filter(String filename) {
		return Filters.eq(NAME, filename);
	}
	
	/**
	 * Filter type.
	 *
	 * @param type the type
	 * @return the bson
	 */
	public static Bson filterType(String type) {
		return Filters.eq(TYPE, type);
	}
	
	/**
	 * Filter source.
	 *
	 * @param source the source
	 * @return the bson
	 */
	public static Bson filterSource(String source) {
		return Filters.eq(SOURCE, source);
	}
	
	/**
	 * Filter created.
	 *
	 * @param from the from
	 * @param until the until
	 * @return the bson
	 */
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

	/** The Constant SOURCE_USER_UPLOAD. */
	public static final String SOURCE_USER_UPLOAD = "User upload";
	
	/**
	 * Creates the.
	 *
	 * @param user the user
	 * @param filename the filename
	 * @param size the size
	 * @param type the type
	 * @param created the created
	 * @param source the source
	 * @param expires the expires
	 * @return the document
	 */
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
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the default
	 */
	public static File getDefault(User user, ObjectId id) {
		return new File(id, user);
	}
	
	/**
	 * The Enum Validation.
	 */
	public static enum Validation {
		
		/** The unvalidated. */
		UNVALIDATED, 
 /** The no error. */
 NO_ERROR, 
 /** The warnings. */
 WARNINGS, 
 /** The errors. */
 ERRORS, 
 /** The fatal errors. */
 FATAL_ERRORS;
	}

	/** The id. */
	private final ObjectId id;
	
	/** The user. */
	private String user;
	
	/** The filename. */
	private String filename;
	
	/** The size. */
	private int size;
	
	/** The type. */
	private String type;
	
	/** The validation. */
	private Validation validation;
	
	/** The created. */
	private final Instant created;
	
	/** The modified. */
	private Instant modified;
	
	/** The source. */
	@CheckForNull
	private final String source;
	
	/** The tags. */
	private final Set<String> tags;
	
	/** The leveraged. */
	@CheckForNull
	private Instant leveraged;
	
	/** The expires. */
	@CheckForNull
	private final Instant expires;
	
	/** The core data. */
	private boolean coreData;

	/**
	 * Instantiates a new file.
	 *
	 * @param doc the doc
	 */
	public File(Document doc) {
		this.id = doc.getObjectId(ID);
		this.user = doc.getString(USER);
		this.filename = doc.getString(NAME);
		this.size = doc.getInteger(SIZE, 0);
		this.type = doc.getString(TYPE);
		String vali = doc.getString(VALIDATION);
		this.validation = vali == null || vali.isEmpty() ? 
				Validation.UNVALIDATED : Validation.valueOf(vali);
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
	
	/**
	 * Instantiates a new file.
	 *
	 * @param id the id
	 * @param user the user
	 */
	protected File(ObjectId id, User user) {
		this.id = id;
		this.user = user.getName();
		this.filename = "File deleted";
		this.size = 0;
		this.type = "";
		this.validation = Validation.UNVALIDATED;
		this.created = null;
		this.leveraged = null;
		this.source = null;
		this.tags = Collections.emptySet();
		this.expires = null;
		this.coreData = false;
	}
	
	/** The Constant TYPE_TIMELOG. */
	public static final String TYPE_TIMELOG = "https://app.sdsd-projekt.de/wikinormia.html?page=efdiTimelog";
	
	/** The Constant TYPE_GPSINFO. */
	public static final String TYPE_GPSINFO = "https://app.sdsd-projekt.de/wikinormia.html?page=gps";
	
	/**
	 * Checks if is time log.
	 *
	 * @return true, if is time log
	 */
	public boolean isTimeLog() {
		return TYPE_TIMELOG.equals(type);
	}
	
	/**
	 * Checks if is gps info.
	 *
	 * @return true, if is gps info
	 */
	public boolean isGpsInfo() {
		return TYPE_GPSINFO.equals(type);
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
	 * To URI.
	 *
	 * @param fileid the fileid
	 * @return the string
	 */
	public static final String toURI(String fileid) {
		return "sdsd:" + fileid;
	}
	
	/**
	 * To URI.
	 *
	 * @param fileid the fileid
	 * @return the string
	 */
	public static final String toURI(ObjectId fileid) {
		return "sdsd:" + fileid.toHexString();
	}
	
	/** The Constant fileIdRegex. */
	private static final Pattern fileIdRegex = Pattern.compile("[0-9A-Fa-f]{24}");
	
	/**
	 * To ID.
	 *
	 * @param fileUri the file uri
	 * @return the object id
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static final ObjectId toID(String fileUri) throws IllegalArgumentException {
		Matcher matcher = fileIdRegex.matcher(fileUri);
		if(matcher.find())
			return new ObjectId(matcher.group());
		throw new IllegalArgumentException("Input does not contain a valid file ID");
	}
	
	/**
	 * Gets the uri.
	 *
	 * @return the uri
	 */
	public String getURI() {
		return toURI(id);
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
	 * Sets the user.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public Bson setUser(User user) {
		this.user = user.getName();
		return Updates.combine(
				Updates.set(USER, this.user), 
				setModified(Instant.now()));
	}

	/**
	 * Gets the filename.
	 *
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Rename.
	 *
	 * @param filename the filename
	 * @return the bson
	 */
	public Bson rename(String filename) {
		this.filename = filename;
		return Updates.combine(
				Updates.set(NAME, filename), 
				setModified(Instant.now()));
	}

	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * Sets the size.
	 *
	 * @param bytes the bytes
	 * @return the bson
	 */
	public Bson setSize(int bytes) {
		this.size = bytes;
		return Updates.combine(
				Updates.set(SIZE, bytes), 
				setModified(Instant.now()));
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type.
	 *
	 * @param type the type
	 * @return the bson
	 */
	public Bson setType(String type) {
		this.type = type;
		return Updates.combine(
				Updates.set(TYPE, type), 
				setModified(Instant.now()));
	}
	
	/**
	 * Gets the validation.
	 *
	 * @return the validation
	 */
	public Validation getValidation() {
		return validation;
	}
	
	/**
	 * Sets the validation.
	 *
	 * @param validation the validation
	 * @return the bson
	 */
	public Bson setValidation(Validation validation) {
		if(validation == null) validation = Validation.UNVALIDATED;
		this.validation = validation;
		return validation == Validation.UNVALIDATED ? 
				Updates.unset(VALIDATION) : Updates.set(VALIDATION, validation.name());
	}

	/**
	 * Gets the created.
	 *
	 * @return the created
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Gets the modified.
	 *
	 * @return the modified
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Sets the modified.
	 *
	 * @param modified the modified
	 * @return the bson
	 */
	public Bson setModified(Instant modified) {
		this.modified = modified;
		return Updates.set(MODIFIED, Date.from(modified));
	}

	/**
	 * Gets the source.
	 *
	 * @return the source
	 */
	@CheckForNull
	public String getSource() {
		return source;
	}

	/**
	 * Gets the tags.
	 *
	 * @return the tags
	 */
	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}
	
	/**
	 * Adds the tags.
	 *
	 * @param newtags the newtags
	 * @return the bson
	 */
	public Bson addTags(Collection<String> newtags) {
		tags.addAll(newtags);
		return Updates.set(TAGS, tags.stream()
				.collect(Util.asMongoStringList()));
	}
	
	/**
	 * Delete tags.
	 *
	 * @param deltags the deltags
	 * @return the bson
	 */
	public Bson deleteTags(Collection<String> deltags) {
		tags.removeAll(deltags);
		return Updates.set(TAGS, tags.stream()
				.collect(Util.asMongoStringList()));
	}
	
	/**
	 * Clear tags.
	 *
	 * @return the bson
	 */
	public Bson clearTags() {
		tags.clear();
		return Updates.unset(TAGS);
	}

	/**
	 * Gets the leveraged.
	 *
	 * @return the leveraged
	 */
	@CheckForNull
	public Instant getLeveraged() {
		return leveraged;
	}
	
	/**
	 * Sets the leveraged.
	 *
	 * @param leveraged the leveraged
	 * @return the bson
	 */
	public Bson setLeveraged(Instant leveraged) {
		this.leveraged = leveraged;
		return Updates.set(LEVERAGED, Date.from(leveraged));
	}
	
	/**
	 * Gets the expires.
	 *
	 * @return the expires
	 */
	@CheckForNull
	public Instant getExpires() {
		return expires;
	}
	
	/**
	 * Checks if is core data.
	 *
	 * @return true, if is core data
	 */
	public boolean isCoreData() {
		return coreData;
	}
	
	/**
	 * Sets the core data.
	 *
	 * @param coreData the core data
	 * @return the bson
	 */
	public Bson setCoreData(boolean coreData) {
		this.coreData = coreData;
		return coreData ? Updates.set(CORE, true) : Updates.unset(CORE);
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
		if (!(obj instanceof File))
			return false;
		File other = (File) obj;
		return Objects.equals(id, other.id);
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(File o) {
		return filename.compareToIgnoreCase(o.filename);
	}
	
	/** The cmp recent. */
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
	
	/** The cmp recent core. */
	public static Comparator<File> CMP_RECENT_CORE = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			if(o1.isCoreData() != o2.isCoreData())
				return o1.isCoreData() ? -1 : 1;
			return File.CMP_RECENT.compare(o1, o2);
		}
	};

}
