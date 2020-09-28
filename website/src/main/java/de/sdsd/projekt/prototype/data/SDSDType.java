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
	public static final String URI = "uri", AUTHOR = "author", NAME = "name",
			MIME = "mime", ARTYPE = "artype", CREATED = "created", 
			PARSER = "parser", PARSECOMMAND = "parseCommand",  TESTCOMMAND = "testCommand";

	public static Bson filter(User author) {
		return Filters.eq(AUTHOR, author.getName());
	}

	public static Bson filter(Resource uri) {
		return Filters.eq(URI, uri.getURI());
	}
	
	public static Bson filter(String mimetype) {
		return Filters.eq(MIME, mimetype);
	}
	
	public static Bson filter(ARMessageType artype) {
		return Filters.eq(ARTYPE, artype.technicalMessageType());
	}
	
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
	
	public static SDSDType getDefault(@Nullable User user, String uri) {
		return new SDSDType(user, uri);
	}
	
	private final String author, uri, name;
	private String mimetype;
	private ARMessageType artype;
	private final Instant created;

	private Optional<String> parser, parseCommand, testCommand;

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
	
	public Bson filter() {
		return Filters.eq(URI, uri);
	}
	
	public String getAuthor() {
		return author;
	}

	public String getUri() {
		return uri;
	}

	public String getName() {
		return name;
	}

	public Instant getCreated() {
		return created;
	}

	public String getMimeType() {
		return mimetype;
	}

	public Bson setMimeType(String mimetype) {
		this.mimetype = mimetype;
		return Updates.set(MIME, mimetype);
	}

	public ARMessageType getARType() {
		return artype;
	}

	public Bson setARType(ARMessageType artype) {
		this.artype = artype;
		return Updates.set(ARTYPE, artype.technicalMessageType());
	}

	public Optional<String> getParser() {
		return parser;
	}

	public Optional<String> getParseCommand() {
		return parseCommand;
	}

	public Optional<String> getTestCommand() {
		return testCommand;
	}
	
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
	
	@Override
	public String toString() {
		return uri;
	}

	@Override
	public int compareTo(SDSDType o) {
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

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
