package de.sdsd.projekt.api;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Hex;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.DCTerms;
import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceResult.AscendingIterator;
import de.sdsd.projekt.api.ServiceResult.Position;
import de.sdsd.projekt.api.ServiceResult.Positions;
import de.sdsd.projekt.api.ServiceResult.TimeLog;
import de.sdsd.projekt.api.ServiceResult.TimeLogEntry;
import de.sdsd.projekt.api.ServiceResult.TimedPosition;
import de.sdsd.projekt.api.ServiceResult.TimedValue;
import de.sdsd.projekt.api.ServiceResult.TimedValueList;

/**
 * Utility class of the SDSD API.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public abstract class Util {

	/**
	 * Creates the random uri.
	 *
	 * @return random sdsd uri
	 */
	public static String createRandomUri() {
		return "sdsd:" + UUID.randomUUID().toString();
	}

	/**
	 * Creates the uri for the given identifier.
	 *
	 * @param identifier the identifier
	 * @return sdsd uri
	 */
	public static String createUri(String identifier) {
		return "sdsd:" + identifier;
	}

	/**
	 * Converts the given text to camelCase and deleting all special characters.
	 *
	 * @param input the input
	 * @param upper whether the string should start with a upper case
	 * @return CamelCase string
	 */
	public static String toCamelCase(String input, boolean upper) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); ++i) {
			char c = input.charAt(i);
			if (c == ' ')
				upper = true;
			else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
				sb.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			} else
				upper = false;
		}
		return sb.toString();
	}

	/**
	 * Creates an integer literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(int value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Creates a long literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(long value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Creates a float literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(float value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Creates a double literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(double value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Creates a bool literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(boolean value) {
		return ResourceFactory.createTypedLiteral(value);
	}

	/**
	 * Creates a string literal without language.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(String value) {
		return ResourceFactory.createStringLiteral(value);
	}

	/**
	 * Creates a DateTime literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(Instant value) {
		return ResourceFactory
				.createTypedLiteral(GregorianCalendar.from(ZonedDateTime.ofInstant(value, ZoneOffset.UTC)));
	}

	/**
	 * Creates a hex binary literal.
	 *
	 * @param value the value
	 * @return the literal
	 */
	public static Literal lit(byte[] value) {
		return ResourceFactory.createTypedLiteral(Hex.encodeHexString(value, false), XSDDatatype.XSDhexBinary);
	}

	/** The wikinormia UNIT property. */
	public static final Property UNIT = ResourceFactory.createProperty("sdsd:unit");

	/** The wikinormia UNKNOWN format. */
	public static final WikiFormat UNKNOWN = new WikiFormat("unknown");

	/** The wikinormia SERVICERESULT format. */
	public static final WikiFormat SERVICERESULT = new WikiFormat("serviceresult");

	/** The wikinormia DEFAULT type base class. */
	public static final WikiType DEFAULT = new WikiType(UNKNOWN, "default");

	/** The wikinormia FILE. */
	public static final WikiType FILE = new WikiType(UNKNOWN, "file");

	/** The wikinormia FIELD. */
	public static final WikiType FIELD = new WikiType(UNKNOWN, "field");

	/** The wikinormia MACHINE. */
	public static final WikiType MACHINE = new WikiType(UNKNOWN, "machine");

	/** The wikinormia TIMELOG. */
	public static final WikiType TIMELOG = new WikiType(UNKNOWN, "timelog");

	/** The wikinormia GRID. */
	public static final WikiType GRID = new WikiType(UNKNOWN, "grid");

	/** The wikinormia VALUEINFO. */
	public static final WikiType VALUEINFO = new WikiType(UNKNOWN, "valueinfo");

	/** The wikinormia DDI. */
	public static final WikiType DDI = new WikiType(UNKNOWN, "ddi");

	/** The RegEx pattern for wikinormia DDI uris. */
	public static final Pattern DDI_REGEX = Pattern.compile(Pattern.quote(DDI.inst("").getURI()) + "(\\d+)");

	/**
	 * Creates the wikinormia DDI uri.
	 *
	 * @param num the DDI
	 * @return the wikinormia DDI uri
	 */
	public static WikiInst ddi(int num) {
		return DDI.inst(num);
	}

	/**
	 * Extracts the DDI from a wikinormia DDI uri.
	 *
	 * @param uri the wikinormia DDI uri
	 * @return the DDI
	 */
	public static int ddi(String uri) {
		Matcher matcher = DDI_REGEX.matcher(uri);
		if (matcher.matches())
			return Integer.parseInt(matcher.group(1));
		else
			throw new NumberFormatException(uri + " is no DDI URI " + DDI_REGEX.pattern());
	}

	/**
	 * Extracts the DDI from a wikinormia DDI resource.
	 *
	 * @param res the wikinormia DDI resource
	 * @return the DDI
	 */
	public static int ddi(Resource res) {
		return ddi(res.getURI());
	}

	/**
	 * Extracts the DDI from a wikinormia DDI uri node.
	 *
	 * @param node the wikinormia DDI uri node
	 * @return the DDI
	 */
	public static int ddi(Node node) {
		return ddi(node.getURI());
	}

	/**
	 * Creates a wikinormia format resource.
	 *
	 * @param identifier the identifier
	 * @return the wiki format
	 */
	public static WikiFormat format(String identifier) {
		return new WikiFormat(identifier);
	}

	/**
	 * The Class WikiFormat.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class WikiFormat extends ResourceImpl {

		/**
		 * Instantiates a new wiki format.
		 *
		 * @param identifier the identifier
		 */
		public WikiFormat(String identifier) {
			super(ServiceAPI.WIKI_URI + toCamelCase(identifier, false));
		}

		/**
		 * Instantiates a new wiki format.
		 *
		 * @param format     the format
		 * @param identifier the identifier
		 */
		protected WikiFormat(WikiFormat format, String identifier) {
			super(format.getURI().equals(UNKNOWN.getURI()) ? ServiceAPI.WIKI_URI + toCamelCase(identifier, false)
					: format.getURI() + toCamelCase(identifier, true));
		}

		/**
		 * Creates a wikinormia type resource.
		 *
		 * @param identifier the identifier
		 * @return the wiki type
		 */
		public WikiType res(String identifier) {
			return new WikiType(this, identifier);
		}
	}

	/**
	 * The Class WikiType.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class WikiType extends WikiFormat {

		/**
		 * Instantiates a new wiki type.
		 *
		 * @param format     the format
		 * @param identifier the identifier
		 */
		public WikiType(WikiFormat format, String identifier) {
			super(format, identifier);
		}

		/**
		 * Creates a wikinormia attribute property.
		 *
		 * @param identifier the identifier
		 * @return the wiki attr
		 */
		public WikiAttr prop(String identifier) {
			return new WikiAttr(this, identifier);
		}

		/**
		 * Creates a wikinormia instance resource.
		 *
		 * @param identifier the identifier
		 * @return the wiki inst
		 */
		public WikiInst inst(String identifier) {
			return new WikiInst(this, identifier);
		}

		/**
		 * Creates a wikinormia instance resource.
		 *
		 * @param identifier the identifier
		 * @return the wiki inst
		 */
		public WikiInst inst(int identifier) {
			return new WikiInst(this, Integer.toString(identifier));
		}
	}

	/**
	 * The Class WikiAttr.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class WikiAttr extends PropertyImpl {

		/**
		 * Instantiates a new wiki attr.
		 *
		 * @param type       the type
		 * @param identifier the identifier
		 */
		public WikiAttr(WikiType type, String identifier) {
			super(type.getURI() + '#', toCamelCase(identifier, false));
		}
	}

	/**
	 * The Class WikiInst.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class WikiInst extends ResourceImpl {

		/**
		 * Instantiates a new wiki instance.
		 *
		 * @param type       the type
		 * @param identifier the identifier
		 */
		public WikiInst(WikiType type, String identifier) {
			super(type.getURI() + '_' + toCamelCase(identifier, false));
		}
	}

	/**
	 * Creates the random uri resource.
	 *
	 * @param model    the model
	 * @param wikiType the wiki type
	 * @param parent   the parent
	 * @return the resource
	 */
	public static Resource createRandomUriResource(Model model, WikiType wikiType, @Nullable Resource parent) {
		Resource res = model.createResource(createRandomUri(), wikiType);
		if (parent != null)
			res.addProperty(DCTerms.isPartOf, parent);
		return res;
	}

	/**
	 * Creates the resource.
	 *
	 * @param model      the model
	 * @param identifier the identifier
	 * @param wikiType   the wiki type
	 * @param parent     the parent
	 * @return the resource
	 */
	public static Resource createResource(Model model, String identifier, WikiType wikiType,
			@Nullable Resource parent) {
		Resource res = model.createResource(createUri(identifier), wikiType);
		if (parent != null)
			res.addProperty(DCTerms.isPartOf, parent);
		return res;
	}

	/**
	 * Calculates the orientation of the last returned position in the given
	 * iterator. It uses the previous positions to estimate the direction.
	 * 
	 * @param posIterator Iterator with a current position.
	 * @return Current orientation in degrees [0-360). 0=North, 90=East, 180=South,
	 *         270=West
	 */
	public static double orientation(AscendingIterator<TimedPosition> posIterator) {
		int curIndex = Math.min(posIterator.index + 1, posIterator.list.size() - 1);
		return orientation(posIterator.list, curIndex);
	}

	/** The Constant DEGREES_TO_RADIANS. */
	private static final double EARTH_RADIUS = 6372795.477598, DEGREES_TO_RADIANS = Math.PI / 180.;

	/**
	 * Calculates the orientation of the machine at the given position. It uses the
	 * previous positions to estimate the direction.
	 * 
	 * @param list  List of positions.
	 * @param index Index of the position in the list.
	 * @return Orientation in degrees [0-360). 0=North, 90=East, 180=South, 270=West
	 */
	public static double orientation(List<? extends Position> list, int index) {
		Position next = list.get(index);
		for (int i = index + 1; i < list.size(); ++i) {
			Position p = list.get(i);
			double dlat = next.getLatitude() - p.getLatitude(), dlng = next.getLongitude() - p.getLongitude();
			double dist = Math.sqrt(Math.pow(dlat, 2) + Math.pow(dlng, 2));
			if (dist > 2e-5) {
				double angle = Math.acos(dlat / dist) / DEGREES_TO_RADIANS;
				return dlng < 0 ? 360 - angle : angle;
			}
		}
		for (int i = index - 1; i >= 0; --i) {
			Position p = list.get(i);
			double dlat = p.getLatitude() - next.getLatitude(), dlng = p.getLongitude() - next.getLongitude();
			double dist = Math.sqrt(Math.pow(dlat, 2) + Math.pow(dlng, 2));
			if (dist > 2e-5) {
				double angle = Math.acos(dlat / dist) / DEGREES_TO_RADIANS;
				return dlng < 0 ? 360 - angle : angle;
			}
		}
		return 0;
	}

	/**
	 * Calculates the direction from position a to position b.
	 * 
	 * @param a the start position
	 * @param b the position for the direction
	 * @return Direction in degrees [0-360). 0=North, 90=East, 180=South, 270=West
	 */
	public static double direction(Position a, Position b) {
		double dlat = b.getLatitude() - a.getLatitude(), dlng = b.getLongitude() - a.getLongitude();
		double dist = Math.sqrt(Math.pow(dlat, 2) + Math.pow(dlng, 2));
		if (dist <= 2e-5)
			throw new IllegalArgumentException("Positions are too close");
		double angle = Math.acos(dlat / dist) / DEGREES_TO_RADIANS;
		return dlng < 0 ? 360 - angle : angle;
	}

	/**
	 * Calculates the distance between two positions by approximating the geoid to a
	 * sphere of radius {@link #EARTH_RADIUS} m (radius quadric medium), so the
	 * calculation could have a distance error of 0.3%, particularly in the polar
	 * extremes.
	 * 
	 * @param p1 Position 1
	 * @param p2 Position 2
	 * @return Distance in meters
	 */
	public static double distance(Position p1, Position p2) {
		double latA = p1.getLatitude() * DEGREES_TO_RADIANS;
		double lonA = p1.getLongitude() * DEGREES_TO_RADIANS;
		double latB = p2.getLatitude() * DEGREES_TO_RADIANS;
		double lonB = p2.getLongitude() * DEGREES_TO_RADIANS;
		return EARTH_RADIUS
				* Math.acos(Math.sin(latA) * Math.sin(latB) + Math.cos(latA) * Math.cos(latB) * Math.cos(lonA - lonB));
	}

	/**
	 * Calculates the destination point.
	 * 
	 * @param start    The starting point
	 * @param angle    The direction to move in degrees [0-360)
	 * @param distance The distance to move in meters
	 * @return Destination position
	 */
	public static Position move(Position start, double angle, double distance) {
		if (Math.abs(distance) < 0.01)
			return start;
		double latA = start.getLatitude() * DEGREES_TO_RADIANS;
		double lonA = start.getLongitude() * DEGREES_TO_RADIANS;
		double angularDistance = distance / EARTH_RADIUS;
		angle %= 360;
		if (angle < 0)
			angle += 360;
		double trueCourse = angle * DEGREES_TO_RADIANS;

		double lat = Math.asin(Math.sin(latA) * Math.cos(angularDistance)
				+ Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));
		double dlon = Math.atan2(Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA),
				Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));
		double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;
		return new Position(lat / DEGREES_TO_RADIANS, lon / DEGREES_TO_RADIANS, start.altitude);
	}

	/**
	 * The Class CombinedTimeLogEntry.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static class CombinedTimeLogEntry implements TimedValue {

		/** The time. */
		public final Instant time;

		/** The values. */
		public final double values[];

		/**
		 * Instantiates a new combined time log entry.
		 *
		 * @param time   the time
		 * @param values the values
		 */
		CombinedTimeLogEntry(Instant time, double[] values) {
			this.time = time;
			this.values = values;
		}

		/**
		 * Gets the time.
		 *
		 * @return the time
		 */
		@Override
		public Instant getTime() {
			return time;
		}

		/**
		 * Gets the value.
		 *
		 * @param index the index
		 * @return the value
		 */
		public double getValue(int index) {
			return values[index];
		}

		/**
		 * Value count.
		 *
		 * @return value count
		 */
		public int valueCount() {
			return values.length;
		}

	}

	/**
	 * Returns a combined timelog by repeating the last known value if entries are
	 * missing.
	 * 
	 * @param skipEmpty skips the leading entries, where not every value is set.
	 * @param timelogs  the timelogs to combine.
	 * @return a descending list of combined timelog entries.
	 */
	public static TimedValueList<CombinedTimeLogEntry> combine(boolean skipEmpty, TimeLog... timelogs) {
		int[] indexes = new int[timelogs.length];
		double[] values = new double[timelogs.length];
		Instant time;

		List<CombinedTimeLogEntry> result = new ArrayList<>();
		while (true) {
			time = Instant.MIN;
			for (int i = 0; i < timelogs.length; ++i) {
				if (indexes[i] < timelogs[i].size()) {
					TimeLogEntry tle = timelogs[i].get(indexes[i]);
					if (tle.time.isAfter(time))
						time = tle.time;
				} else if (skipEmpty)
					return new TimedValueList<>(result);
			}
			if (time.equals(Instant.MIN))
				return new TimedValueList<>(result);

			for (int i = 0; i < timelogs.length; ++i) {
				if (indexes[i] < timelogs[i].size()) {
					TimeLogEntry tle = timelogs[i].get(indexes[i]);
					values[i] = tle.value;
					if (tle.time.equals(time))
						++indexes[i];
				} else
					values[i] = 0.;
			}
			result.add(new CombinedTimeLogEntry(time, values.clone()));
		}
	}

	/**
	 * Converts the positions to GeoJSON.
	 *
	 * @param positions the positions
	 * @return the GeoJSON object
	 */
	public static JSONObject toGeoJson(Positions positions) {
		return toGeoJson(positions.ascending());
	}

	/**
	 * Converts the positions to GeoJSON.
	 *
	 * @param positions the positions
	 * @return the GeoJSON object
	 */
	public static JSONObject toGeoJson(Iterable<? extends Position> positions) {
		JSONArray coords = new JSONArray();
		for (Position p : positions) {
			JSONArray c = new JSONArray().put(p.longitude).put(p.latitude);
			if (Double.isFinite(p.altitude))
				c.put(p.altitude);
			coords.put(c);
		}
		return new JSONObject().put("type", "LineString").put("coordinates", coords);
	}
}
