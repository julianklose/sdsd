package de.sdsd.projekt.prototype.data;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;

import org.apache.commons.codec.binary.Hex;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.json.JSONArray;

/**
 * Provides static utilities.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public final class Util {

	/**
	 * Collect strings from a {@link Stream} into an mongo array.
	 * 
	 * @return mongo bson array for storing
	 */
	public static Collector<String, ?, BsonArray> asMongoStringList() {
		return Collector.of(BsonArray::new, 
				(a, s) -> a.add(new BsonString(s)), 
				(l, r) -> { l.addAll(r); return l; });
	}
	
	/**
	 * To JSON array.
	 *
	 * @return the collector
	 */
	public static Collector<Object, JSONArray, JSONArray> toJSONArray() {
		return Collector.of(JSONArray::new, JSONArray::put, (left, right) -> {
			right.forEach(o -> left.put(o));
			return left;
		});
	}

	/**
	 * Creates a stream of strings from the result of the mongo db.
	 * 
	 * @param mongoresult result returned by {@link Document#get(Object)}
	 * @return stream of strings
	 */
	public static Stream<String> stringstreamFromMongo(Object mongoresult) {
		if (mongoresult instanceof List<?>)
			return ((List<?>) mongoresult).stream().filter(s -> s instanceof String).map(s -> (String) s);
		else
			return Stream.empty();
	}
	
	/**
	 * Iterator stream.
	 *
	 * @param <T> the generic type
	 * @param it the it
	 * @return the stream
	 */
	public static <T> Stream<T> iteratorStream(Iterator<T> it) {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, false);
	}
	
	/**
	 * Iterate.
	 *
	 * @param <T> the generic type
	 * @param it the it
	 * @return the iterable
	 */
	public static <T> Iterable<T> iterate(Iterator<T> it) {
		return () -> it;
	}
	
	/**
	 * First.
	 *
	 * @param <T> the generic type
	 * @param it the it
	 * @return the t
	 */
	@CheckForNull
	public static <T> T first(Iterator<T> it) {
		return it.hasNext() ? it.next() : null;
	}
	
	/** The Constant OID_REGEX. */
	private static final Pattern OID_REGEX = Pattern.compile("[A-Fa-f0-9]{24}");
	
	/**
	 * Checks if is object id.
	 *
	 * @param input the input
	 * @return true, if is object id
	 */
	public static boolean isObjectId(String input) {
		return OID_REGEX.matcher(input).matches();
	}
	
	/** The Constant SECURE_TOKEN_LENGTH. */
	private static final int SECURE_TOKEN_LENGTH = 64;
	
	/** The Constant SYMBOLS. */
	private static final char[] SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	
	/** The Constant random. */
	private static final SecureRandom random = new SecureRandom();
	
	/**
	 * Creates the secure token.
	 *
	 * @return the string
	 */
	public static String createSecureToken() {
		char[] buf = new char[SECURE_TOKEN_LENGTH];
		for(int i = 0; i < buf.length; ++i) {
			buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
		}
		return new String(buf);
	}
	
	/**
	 * Creates the uuid uri.
	 *
	 * @return the string
	 */
	public static String createUuidUri() {
		return "sdsd:" + UUID.randomUUID().toString();
	}
	
	/**
	 * To camel case.
	 *
	 * @param input the input
	 * @param upper the upper
	 * @return the string
	 */
	public static String toCamelCase(String input, boolean upper) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); ++i) {
			char c = input.charAt(i);
			if(c == ' ') 
				upper = true;
			else if(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
				sb.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			} else
				upper = false;
		}
		return sb.toString();
	}
	
	/**
	 * Path.
	 *
	 * @param node the node
	 * @return the path
	 */
	public static Path path(RDFNode node) {
		return PathFactory.pathLink(node.asNode());
	}
	
	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(int value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(long value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(float value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(double value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(boolean value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(String value) {
		return ResourceFactory.createStringLiteral(value);
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(Instant value) {
		return ResourceFactory
				.createTypedLiteral(GregorianCalendar.from(ZonedDateTime.ofInstant(value, ZoneOffset.UTC)));
	}

	/**
	 * Lit.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(byte[] value) {
		return ResourceFactory.createTypedLiteral(Hex.encodeHexString(value, false), XSDDatatype.XSDhexBinary);
	}

}
