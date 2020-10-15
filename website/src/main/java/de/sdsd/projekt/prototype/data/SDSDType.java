package de.sdsd.projekt.prototype.data;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Resource;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;

/**
 * Represents a SDSD data type, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class SDSDType implements Comparable<SDSDType> {
	
	/** The Constant TESTCOMMAND. */
	public static final String URI = "uri", AUTHOR = "author", NAME = "name",
			MIME = "mime", ARTYPE = "artype", CREATED = "created", 
			PARSER = "parser", PARSECOMMAND = "parseCommand",  TESTCOMMAND = "testCommand";

	/**
	 * Filter.
	 *
	 * @param author the author
	 * @return the bson
	 */
	public static Bson filter(User author) {
		return Filters.eq(AUTHOR, author.getName());
	}

	/**
	 * Filter.
	 *
	 * @param uri the uri
	 * @return the bson
	 */
	public static Bson filter(Resource uri) {
		return Filters.eq(URI, uri.getURI());
	}
	
	/**
	 * Filter.
	 *
	 * @param mimetype the mimetype
	 * @return the bson
	 */
	public static Bson filter(String mimetype) {
		return Filters.eq(MIME, mimetype);
	}
	
	/**
	 * Filter.
	 *
	 * @param artype the artype
	 * @return the bson
	 */
	public static Bson filter(ARMessageType artype) {
		return Filters.eq(ARTYPE, artype.technicalMessageType());
	}
	
	/**
	 * Filter.
	 *
	 * @param mimetype the mimetype
	 * @param artype the artype
	 * @return the bson
	 */
	public static Bson filter(@Nullable String mimetype, @Nullable ARMessageType artype) {
		Bson mimefilter = mimetype != null ? Filters.eq(MIME, mimetype) : null;
		Bson arfilter = artype != null ? Filters.eq(ARTYPE, artype.technicalMessageType()) : null;
		if(mimefilter != null && arfilter != null)
			return Filters.and(mimefilter, arfilter);
		else if(mimefilter != null)
			return mimefilter;
		else if(arfilter != null)
			return arfilter;
		else
			throw new IllegalArgumentException("At least one filter type must be non null");
	}

	/**
	 * Creates the.
	 *
	 * @param author the author
	 * @param uri the uri
	 * @param name the name
	 * @param mimetype the mimetype
	 * @param artype the artype
	 * @return the document
	 */
	public static Document create(User author, String uri, String name, String mimetype, @Nullable ARMessageType artype) {
		Document doc = new Document()
				.append(URI, uri)
				.append(AUTHOR, author.getName())
				.append(NAME, name)
				.append(MIME, mimetype)
				.append(ARTYPE, artype != null ? artype.technicalMessageType() : null)
				.append(CREATED, new Date());
		return doc;
	}
	
	/**
	 * Gets the default.
	 *
	 * @param user the user
	 * @param uri the uri
	 * @return the default
	 */
	public static SDSDType getDefault(@Nullable User user, String uri) {
		return new SDSDType(user, uri);
	}
	
	/** The name. */
	private final String author, uri, name;
	
	/** The mimetype. */
	private String mimetype;
	
	/** The artype. */
	private ARMessageType artype;
	
	/** The created. */
	private final Instant created;

	/** The test command. */
	private Optional<String> parser, parseCommand, testCommand;

	/**
	 * Instantiates a new SDSD type.
	 *
	 * @param doc the doc
	 */
	public SDSDType(Document doc) {
		this.author = doc.getString(AUTHOR);
		this.uri = doc.getString(URI);
		this.name = doc.getString(NAME);
		this.mimetype = doc.getString(MIME);
		String artype = doc.getString(ARTYPE);
		this.artype = (artype != null && !artype.isEmpty()) ? 
				ARMessageType.from(artype)
				: ARMessageType.fromMimetype(mimetype);
		this.created = doc.getDate(CREATED).toInstant();
		
		this.parser = Optional.ofNullable(doc.getString(PARSER));
		this.parseCommand = Optional.ofNullable(doc.getString(PARSECOMMAND));
		this.testCommand = Optional.ofNullable(doc.getString(TESTCOMMAND));
	}
	
	/**
	 * Instantiates a new SDSD type.
	 *
	 * @param author the author
	 * @param uri the uri
	 */
	protected SDSDType(@Nullable User author, String uri) {
		this.author = author != null ? author.getName() : "sdsd";
		this.uri = uri;
		this.name = uri.substring(TripleFunctions.NS_WIKI.length());
		this.mimetype = "application/octet-stream";
		this.artype = ARMessageType.OTHER;
		this.created = Instant.now();
		this.parser = Optional.empty();
		this.parseCommand = Optional.empty();
		this.testCommand = Optional.empty();
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(URI, uri);
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
	 * Gets the uri.
	 *
	 * @return the uri
	 */
	public String getUri() {
		return uri;
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
	 * Gets the created.
	 *
	 * @return the created
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Gets the mime type.
	 *
	 * @return the mime type
	 */
	public String getMimeType() {
		return mimetype;
	}

	/**
	 * Sets the mime type.
	 *
	 * @param mimetype the mimetype
	 * @return the bson
	 */
	public Bson setMimeType(String mimetype) {
		this.mimetype = mimetype;
		return Updates.set(MIME, mimetype);
	}

	/**
	 * Gets the AR type.
	 *
	 * @return the AR type
	 */
	public ARMessageType getARType() {
		return artype;
	}

	/**
	 * Sets the AR type.
	 *
	 * @param artype the artype
	 * @return the bson
	 */
	public Bson setARType(ARMessageType artype) {
		this.artype = artype;
		return Updates.set(ARTYPE, artype.technicalMessageType());
	}

	/**
	 * Gets the parser.
	 *
	 * @return the parser
	 */
	public Optional<String> getParser() {
		return parser;
	}

	/**
	 * Gets the parses the command.
	 *
	 * @return the parses the command
	 */
	public Optional<String> getParseCommand() {
		return parseCommand;
	}

	/**
	 * Gets the test command.
	 *
	 * @return the test command
	 */
	public Optional<String> getTestCommand() {
		return testCommand;
	}
	
	/**
	 * Sets the parser.
	 *
	 * @param parser the parser
	 * @param parseCommand the parse command
	 * @param testCommand the test command
	 * @return the bson
	 */
	public Bson setParser(String parser, String parseCommand, @Nullable String testCommand) {
		this.parser = Optional.of(parser);
		if(parseCommand.isEmpty()) parseCommand = null;
		if(testCommand.isEmpty()) testCommand = null;
		this.parseCommand = Optional.ofNullable(parseCommand);
		this.testCommand = Optional.ofNullable(testCommand);
		return Updates.combine(Updates.set(PARSER, parser), 
				Updates.set(PARSECOMMAND, parseCommand), 
				Updates.set(TESTCOMMAND, testCommand));
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return uri;
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(SDSDType o) {
		return name.compareToIgnoreCase(o.name);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SDSDType other = (SDSDType) obj;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
}
