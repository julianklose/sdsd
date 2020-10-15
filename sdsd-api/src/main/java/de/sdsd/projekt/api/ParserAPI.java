package de.sdsd.projekt.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;
import de.sdsd.projekt.api.ServiceResult.WikiInstance;
import de.sdsd.projekt.api.Util.WikiType;

/**
 * The SDSD ParserAPI.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public class ParserAPI implements AutoCloseable {

	/**
	 * Gets the wikinormia instances.
	 *
	 * @param type  the type
	 * @param local the local
	 * @return the wikinormia instances
	 * @throws JsonRpcException the json rpc exception
	 */
	public static Map<String, WikiInstance> getWikinormiaInstances(Resource type, boolean local)
			throws JsonRpcException {
		Map<String, WikiInstance> out = null;
		try (RestClient client = new RestClient(local)) {
			JSONObject result = client.execute("api", "getWikinormiaInstances", null, type.getURI());
			out = ServiceAPI.result(result, WikiInstance::new);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return out;
	}

	/** The Constant CSV_SEPARATOR. */
	private static final String CSV_SEPARATOR = ";";

	/** The Constant CSV_LINEEND. */
	private static final String CSV_LINEEND = "\r\n";

	/** The Constant format. */
	private static final NumberFormat format;
	static {
		format = NumberFormat.getInstance(Locale.US);
		format.setGroupingUsed(false);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(7);
	}

	/** The zip. */
	private final ZipOutputStream zip;

	/** The meta. */
	private final JSONObject meta = new JSONObject();

	/** The current entry. */
	private ParserEntryWriter currentEntry = null;

	/**
	 * Instantiates a new parser API.
	 *
	 * @param out the out
	 */
	public ParserAPI(OutputStream out) {
		this.zip = new ZipOutputStream(out, Charset.forName("Cp437"));
	}

	/**
	 * Sets the parse time.
	 *
	 * @param milliseconds the milliseconds
	 * @return the parser API
	 */
	public ParserAPI setParseTime(long milliseconds) {
		meta.put("parseTime", milliseconds);
		return this;
	}

	/**
	 * The Class Validation.
	 */
	public static class Validation implements Iterable<String> {

		/** The Constant WARN. */
		public static final String FATAL = "[FATAL] ", ERROR = "[ERROR] ", WARN = "[WARNING] ";

		/** The fatals. */
		private final List<String> warnings, errors, fatals;

		/**
		 * Instantiates a new validation.
		 */
		public Validation() {
			this.warnings = new ArrayList<>(0);
			this.errors = new ArrayList<>(0);
			this.fatals = new ArrayList<>(0);
		}

		/**
		 * Instantiates a new validation.
		 *
		 * @param validation the validation
		 */
		public Validation(Validation validation) {
			this.warnings = new ArrayList<>(validation.warnings.size());
			this.errors = new ArrayList<>(validation.errors.size());
			this.fatals = new ArrayList<>(validation.fatals.size());
			addAll(validation);
		}

		/**
		 * Instantiates a new validation.
		 *
		 * @param validation   the validation
		 * @param prefixformat the prefixformat
		 * @param args         the args
		 */
		public Validation(Validation validation, String prefixformat, Object... args) {
			this.warnings = new ArrayList<>(validation.warnings.size());
			this.errors = new ArrayList<>(validation.errors.size());
			this.fatals = new ArrayList<>(validation.fatals.size());
			addAll(validation, prefixformat, args);
		}

		/**
		 * Instantiates a new validation.
		 *
		 * @param json the json
		 */
		private Validation(JSONObject json) {
			JSONArray arr = json.getJSONArray("warnings");
			this.warnings = new ArrayList<>(arr.length());
			for (int i = 0; i < arr.length(); ++i)
				warnings.add(arr.getString(i));
			arr = json.getJSONArray("errors");
			this.errors = new ArrayList<>(arr.length());
			for (int i = 0; i < arr.length(); ++i)
				errors.add(arr.getString(i));
			arr = json.getJSONArray("fatals");
			this.fatals = new ArrayList<>(arr.length());
			for (int i = 0; i < arr.length(); ++i)
				fatals.add(arr.getString(i));
		}

		/**
		 * From json.
		 *
		 * @param json the json
		 * @return the validation
		 */
		public static Validation fromJson(JSONObject json) {
			return new Validation(json);
		}

		/**
		 * To json.
		 *
		 * @return the JSON object
		 */
		public JSONObject toJson() {
			return new JSONObject().put("warnings", warnings).put("errors", errors).put("fatals", fatals);
		}

		/**
		 * Warn.
		 *
		 * @param format the format
		 * @param args   the args
		 * @return the validation
		 */
		public Validation warn(String format, Object... args) {
			warnings.add(String.format(format, args));
			return this;
		}

		/**
		 * Error.
		 *
		 * @param format the format
		 * @param args   the args
		 * @return the validation
		 */
		public Validation error(String format, Object... args) {
			errors.add(String.format(format, args));
			return this;
		}

		/**
		 * Fatal.
		 *
		 * @param format the format
		 * @param args   the args
		 * @return the validation
		 */
		public Validation fatal(String format, Object... args) {
			fatals.add(String.format(format, args));
			return this;
		}

		/**
		 * Adds the all.
		 *
		 * @param validation the validation
		 * @return the validation
		 */
		public Validation addAll(Validation validation) {
			warnings.addAll(validation.warnings);
			errors.addAll(validation.errors);
			fatals.addAll(validation.fatals);
			return this;
		}

		/**
		 * Adds the all.
		 *
		 * @param validation   the validation
		 * @param prefixformat the prefixformat
		 * @param args         the args
		 * @return the validation
		 */
		public Validation addAll(Validation validation, String prefixformat, Object... args) {
			String prefix = String.format(prefixformat, args);
			for (String msg : validation.warnings)
				warnings.add(prefix + msg);
			for (String msg : validation.errors)
				errors.add(prefix + msg);
			for (String msg : validation.fatals)
				fatals.add(prefix + msg);
			return this;
		}

		/**
		 * Clear.
		 *
		 * @return the validation
		 */
		public Validation clear() {
			warnings.clear();
			errors.clear();
			fatals.clear();
			return this;
		}

		/**
		 * Checks for warnings.
		 *
		 * @return true, if successful
		 */
		public boolean hasWarnings() {
			return !warnings.isEmpty();
		}

		/**
		 * Warnings.
		 *
		 * @return the list
		 */
		public List<String> warnings() {
			return Collections.unmodifiableList(warnings);
		}

		/**
		 * Checks for errors.
		 *
		 * @return true, if successful
		 */
		public boolean hasErrors() {
			return !errors.isEmpty();
		}

		/**
		 * Errors.
		 *
		 * @return the list
		 */
		public List<String> errors() {
			return Collections.unmodifiableList(errors);
		}

		/**
		 * Checks for fatals.
		 *
		 * @return true, if successful
		 */
		public boolean hasFatals() {
			return !fatals.isEmpty();
		}

		/**
		 * Fatals.
		 *
		 * @return the list
		 */
		public List<String> fatals() {
			return Collections.unmodifiableList(fatals);
		}

		/**
		 * Gets the.
		 *
		 * @param i the i
		 * @return the string
		 */
		public String get(int i) {
			if (i < fatals.size())
				return FATAL + fatals.get(i);
			i -= fatals.size();
			if (i < errors.size())
				return ERROR + errors.get(i);
			i -= errors.size();
			return WARN + warnings.get(i);
		}

		/**
		 * Size.
		 *
		 * @return the int
		 */
		public int size() {
			return fatals.size() + errors.size() + warnings.size();
		}

		/**
		 * Checks if is empty.
		 *
		 * @return true, if is empty
		 */
		public boolean isEmpty() {
			return fatals.isEmpty() && errors.isEmpty() && warnings.isEmpty();
		}

		/**
		 * Iterator.
		 *
		 * @return the iterator
		 */
		@Override
		public Iterator<String> iterator() {
			return new Iterator<String>() {
				private int index = 0;

				@Override
				public String next() {
					return get(index++);
				}

				@Override
				public boolean hasNext() {
					return index < size();
				}
			};
		}
	}

	/**
	 * Sets the errors.
	 *
	 * @param errors the errors
	 * @return the parser API
	 */
	public ParserAPI setErrors(@Nullable Validation errors) {
		if (errors != null && errors.size() > 0)
			meta.put("errors", errors.toJson());
		else
			meta.remove("errors");
		return this;
	}

	/**
	 * Write meta.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void writeMeta() throws IOException {
		try (MetaWriter mw = new MetaWriter()) {
			mw.write(meta);
		}
	}

	/**
	 * Write triples.
	 *
	 * @param model the model
	 * @return the parser API
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ParserAPI writeTriples(@Nullable Model model) throws IOException {
		if (meta.has("triples"))
			throw new IOException("You can only write one model");
		if (model != null && !model.isEmpty()) {
			try (TripleWriter tw = new TripleWriter()) {
				tw.write(model);
			}
		}
		return this;
	}

	/**
	 * Write geo.
	 *
	 * @return the geo writer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public GeoWriter writeGeo() throws IOException {
		if (meta.has("geo"))
			throw new IOException("You can only write one geo collection");
		return new GeoWriter();
	}

	/**
	 * Adds the time log.
	 *
	 * @param timelog    the timelog
	 * @param valueInfos the value infos
	 * @return the time log writer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public TimeLogWriter addTimeLog(TimeLog timelog, List<? extends ValueInfo> valueInfos) throws IOException {
		return new TimeLogWriter(timelog, valueInfos);
	}

	/**
	 * Adds the grid.
	 *
	 * @param grid        the grid
	 * @param rowCount    the row count
	 * @param columnCount the column count
	 * @return the grid writer
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public GridWriter addGrid(Grid grid, int rowCount, int columnCount) throws IOException {
		return new GridWriter(grid, rowCount, columnCount);
	}

	/**
	 * Close.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public void close() throws IOException {
		writeMeta();
		zip.close();
	}

	/**
	 * The Class ParserEntryWriter.
	 */
	public abstract class ParserEntryWriter extends Writer {

		/** The name. */
		public final String name;

		/**
		 * Instantiates a new parser entry writer.
		 *
		 * @param name      the name
		 * @param extension the extension
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		ParserEntryWriter(String name, String extension) throws IOException {
			this.name = name;

			if (currentEntry != null)
				throw new IOException("Close '" + currentEntry.name + "' writer before starting a new entry");
			ZipEntry entry = new ZipEntry(name + extension);
			zip.putNextEntry(entry);
			addToMeta(entry.getName());
			currentEntry = this;
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		protected abstract void addToMeta(String entryName);

		/**
		 * Write.
		 *
		 * @param cbuf the cbuf
		 * @param off  the off
		 * @param len  the len
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			ByteBuffer b = StandardCharsets.UTF_8.encode(CharBuffer.wrap(cbuf, off, len));
			zip.write(b.array(), 0, b.limit());
		}

		/**
		 * Write.
		 *
		 * @param str the str
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@Override
		public void write(String str) throws IOException {
			zip.write(str.getBytes(StandardCharsets.UTF_8));
		}

		/**
		 * Flush.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@Override
		public void flush() throws IOException {
			zip.flush();
		}

		/**
		 * Close.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@Override
		public void close() throws IOException {
			zip.closeEntry();
			currentEntry = null;
		}

	}

	/**
	 * The Class MetaWriter.
	 */
	private class MetaWriter extends ParserEntryWriter {

		/**
		 * Instantiates a new meta writer.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		MetaWriter() throws IOException {
			super("meta", ".json");
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		@Override
		protected void addToMeta(String entryName) {
		}

		/**
		 * Write.
		 *
		 * @param meta the meta
		 * @return the meta writer
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public MetaWriter write(JSONObject meta) throws IOException {
			write(meta.toString());
			return this;
		}

	}

	/**
	 * The Class TripleWriter.
	 */
	private class TripleWriter extends ParserEntryWriter {

		/**
		 * Instantiates a new triple writer.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		TripleWriter() throws IOException {
			super("triples", ".ttl");
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		@Override
		protected void addToMeta(String entryName) {
			meta.put("triples", entryName);
		}

		/**
		 * Write.
		 *
		 * @param model the model
		 * @return the triple writer
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public TripleWriter write(Model model) throws IOException {
			model.setNsPrefix("rdf", RDF.uri);
			model.setNsPrefix("rdfs", RDFS.uri);
			model.setNsPrefix("owl", OWL.NS);
			model.setNsPrefix("dcterms", DCTerms.NS);
			model.setNsPrefix("xsd", XSD.NS);
			model.setNsPrefix("wkn", ServiceAPI.WIKI_URI);

			StmtIterator stmtit = model.listStatements();
			try {
				while (stmtit.hasNext()) {
					Statement stmt = stmtit.next();
					String predicate = stmt.getPredicate().getURI();
					if (predicate.startsWith(ServiceAPI.WIKI_URI)) {
						int ind = predicate.lastIndexOf('#');
						if (ind > 0) {
							model.setNsPrefix(predicate.substring(ServiceAPI.WIKI_URI.length(), ind),
									predicate.substring(0, ind + 1));
						}
					}
				}
			} finally {
				stmtit.close();
			}

			model.write(this, "TTL");
			return this;
		}

	}

	/**
	 * The Class GeoWriter.
	 */
	public class GeoWriter extends ParserEntryWriter {

		/** The count. */
		private int count = 0;

		/**
		 * Instantiates a new geo writer.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		GeoWriter() throws IOException {
			super("geo", ".json");
			write("{\"type\":\"FeatureCollection\",\"features\":[");
		}

		/**
		 * Close.
		 *
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		@Override
		public void close() throws IOException {
			write("]}");
			super.close();
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		@Override
		protected void addToMeta(String entryName) {
			meta.put("geo", entryName);
		}

		/**
		 * Check geometry.
		 *
		 * @param geometry the geometry
		 * @return true, if successful
		 * @throws JSONException the JSON exception
		 */
		protected boolean checkGeometry(JSONObject geometry) throws JSONException {
			return !geometry.getString("type").isEmpty() && geometry.getJSONArray("coordinates").length() > 0;
		}

		/**
		 * Write feature.
		 *
		 * @param geojson the geojson
		 * @param type    the type
		 * @param uri     the uri
		 * @param label   the label
		 * @return the geo writer
		 * @throws IOException   Signals that an I/O exception has occurred.
		 * @throws JSONException the JSON exception
		 */
		public GeoWriter writeFeature(JSONObject geojson, ElementType type, String uri, String label)
				throws IOException, JSONException {
			if (uri == null || uri.isEmpty())
				throw new IllegalArgumentException("No URI specified");
			if (label == null)
				throw new IllegalArgumentException("No label specified");
			if (!"Feature".equals(geojson.getString("type")) || !checkGeometry(geojson.getJSONObject("geometry")))
				throw new JSONException("Given geojson is no valid GeoJSON feature");
			geojson.put("id", uri);
			if (type != ElementType.Other)
				geojson.put("elementType", type.name());
			geojson.put("label", label);
			if (count > 0)
				write(",");
			write(geojson.toString());
			++count;
			return this;
		}

		/**
		 * Write geometry.
		 *
		 * @param geojson    the geojson
		 * @param type       the type
		 * @param uri        the uri
		 * @param properties the properties
		 * @param label      the label
		 * @return the geo writer
		 * @throws IOException   Signals that an I/O exception has occurred.
		 * @throws JSONException the JSON exception
		 */
		public GeoWriter writeGeometry(JSONObject geojson, ElementType type, String uri,
				@Nullable JSONObject properties, String label) throws IOException, JSONException {
			if (!checkGeometry(geojson))
				throw new JSONException("Given geojson is no valid GeoJSON geometry");
			return writeFeature(new JSONObject().put("type", "Feature").put("geometry", geojson).put("properties",
					properties != null ? properties : new JSONObject()), type, uri, label);
		}

	}

	/**
	 * The Class ValueBucket.
	 */
	private abstract static class ValueBucket {

		/** The basetype. */
		public final WikiType basetype;

		/** The uri. */
		public final String uri;

		/** The name. */
		public final String name;

		/**
		 * Instantiates a new value bucket.
		 *
		 * @param type the type
		 * @param uri  the uri
		 * @param name the name
		 */
		public ValueBucket(WikiType type, String uri, String name) {
			if (uri == null || uri.isEmpty())
				throw new IllegalArgumentException("No URI specified");
			if (name == null || name.isEmpty())
				throw new IllegalArgumentException("No name specivied");
			this.basetype = type;
			this.uri = uri;
			this.name = name;
		}

		/**
		 * Write to.
		 *
		 * @param model the model
		 * @param type  the type
		 * @return the resource
		 */
		public Resource writeTo(Model model, Resource type) {
			Resource res = model.createResource(uri, type);
			res.addLiteral(basetype.prop("name"), Util.lit(name));
			return res;
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
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(uri);
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
			if (!(obj instanceof TimeLog))
				return false;
			TimeLog other = (TimeLog) obj;
			return Objects.equals(uri, other.uri);
		}
	}

	/**
	 * The Class TimeLog.
	 */
	public static class TimeLog extends ValueBucket {

		/** The until. */
		public final Instant from, until;

		/** The count. */
		public final int count;

		/**
		 * Instantiates a new time log.
		 *
		 * @param uri   the uri
		 * @param name  the name
		 * @param from  the from
		 * @param until the until
		 * @param count the count
		 */
		public TimeLog(String uri, String name, Instant from, Instant until, int count) {
			super(Util.TIMELOG, uri, name);
			this.from = from;
			this.until = until;
			this.count = count;
		}

		/**
		 * Write to.
		 *
		 * @param model the model
		 * @param type  the type
		 * @return the resource
		 */
		@Override
		public Resource writeTo(Model model, Resource type) {
			Resource res = super.writeTo(model, type);
			res.addLiteral(basetype.prop("from"), Util.lit(from));
			res.addLiteral(basetype.prop("until"), Util.lit(until));
			res.addLiteral(basetype.prop("count"), Util.lit(count));
			return res;
		}
	}

	/**
	 * The Class Grid.
	 */
	public static class Grid extends ValueBucket {

		/** The east cell size. */
		public final double northMin, eastMin, northCellSize, eastCellSize;

		/**
		 * Instantiates a new grid.
		 *
		 * @param uri           the uri
		 * @param name          the name
		 * @param northMin      the north min
		 * @param eastMin       the east min
		 * @param northCellSize the north cell size
		 * @param eastCellSize  the east cell size
		 */
		public Grid(String uri, String name, double northMin, double eastMin, double northCellSize,
				double eastCellSize) {
			super(Util.GRID, uri, name);
			this.northMin = northMin;
			this.eastMin = eastMin;
			this.northCellSize = northCellSize;
			this.eastCellSize = eastCellSize;
		}

		/**
		 * Gets the north min.
		 *
		 * @return the north min
		 */
		public double getNorthMin() {
			return northMin;
		}

		/**
		 * Gets the east min.
		 *
		 * @return the east min
		 */
		public double getEastMin() {
			return eastMin;
		}

		/**
		 * Gets the north cell size.
		 *
		 * @return the north cell size
		 */
		public double getNorthCellSize() {
			return northCellSize;
		}

		/**
		 * Gets the east cell size.
		 *
		 * @return the east cell size
		 */
		public double getEastCellSize() {
			return eastCellSize;
		}
	}

	/**
	 * The Class ValueInfo.
	 */
	public static class ValueInfo {

		/** The Constant TYPE. */
		public static final WikiType TYPE = Util.VALUEINFO;

		/** The value uri. */
		public final String valueUri;

		/** The designator. */
		private String designator;

		/** The offset. */
		private long offset;

		/** The scale. */
		private double scale;

		/** The number of decimals. */
		private int numberOfDecimals;

		/** The unit. */
		private String unit;

		/** The parents. */
		private Map<String, ValueBucket> parents = new HashMap<>();

		/**
		 * Instantiates a new value info.
		 *
		 * @param valueUri the value uri
		 */
		public ValueInfo(String valueUri) {
			if (valueUri.indexOf(CSV_SEPARATOR) >= 0 || valueUri.indexOf(CSV_LINEEND) >= 0)
				throw new IllegalArgumentException("Invalid characters in valueUri");
			this.valueUri = valueUri;
			this.designator = "";
			this.offset = 0;
			this.scale = 1.;
			this.numberOfDecimals = 0;
			this.unit = "";
		}

		/**
		 * Write to.
		 *
		 * @param model the model
		 * @param type  the type
		 * @return the resource
		 */
		public final Resource writeTo(Model model, WikiType type) {
			if (parents.isEmpty())
				throw new IllegalStateException("There is no timelog or grid set for this valueinfo");
			Resource res = writeToNoParent(model, type);
			for (String uri : parents.keySet()) {
				res.addProperty(DCTerms.isPartOf, model.createResource(uri));
			}
			return res;
		}

		/**
		 * Write to no parent.
		 *
		 * @param model the model
		 * @param type  the type
		 * @return the resource
		 */
		public final Resource writeToNoParent(Model model, WikiType type) {
			Resource res = model.createResource(valueUri, type);
			if (!designator.isEmpty())
				res.addLiteral(TYPE.prop("designator"), Util.lit(designator));
			if (offset != 0)
				res.addLiteral(TYPE.prop("offset"), Util.lit(offset));
			if (scale != 1.)
				res.addLiteral(TYPE.prop("scale"), Util.lit(scale));
			if (numberOfDecimals != 0)
				res.addLiteral(TYPE.prop("numberOfDecimals"), Util.lit(numberOfDecimals));
			if (!unit.isEmpty())
				res.addLiteral(TYPE.prop("unit"), Util.lit(unit));
			return res;
		}

		/**
		 * Gets the value uri.
		 *
		 * @return the value uri
		 */
		public String getValueUri() {
			return valueUri;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public String getDesignator() {
			return designator;
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the value info
		 */
		public ValueInfo setDesignator(String designator) {
			this.designator = designator != null ? designator : "";
			return this;
		}

		/**
		 * Gets the offset.
		 *
		 * @return the offset
		 */
		public long getOffset() {
			return offset;
		}

		/**
		 * Sets the offset.
		 *
		 * @param offset the offset
		 * @return the value info
		 */
		public ValueInfo setOffset(long offset) {
			this.offset = offset;
			return this;
		}

		/**
		 * Gets the scale.
		 *
		 * @return the scale
		 */
		public double getScale() {
			return scale;
		}

		/**
		 * Sets the scale.
		 *
		 * @param scale the scale
		 * @return the value info
		 */
		public ValueInfo setScale(double scale) {
			this.scale = scale;
			return this;
		}

		/**
		 * Gets the number of decimals.
		 *
		 * @return the number of decimals
		 */
		public int getNumberOfDecimals() {
			return numberOfDecimals;
		}

		/**
		 * Sets the number of decimals.
		 *
		 * @param numberOfDecimals the number of decimals
		 * @return the value info
		 */
		public ValueInfo setNumberOfDecimals(int numberOfDecimals) {
			this.numberOfDecimals = numberOfDecimals;
			return this;
		}

		/**
		 * Gets the unit.
		 *
		 * @return the unit
		 */
		public String getUnit() {
			return unit;
		}

		/**
		 * Sets the unit.
		 *
		 * @param unit the unit
		 * @return the value info
		 */
		public ValueInfo setUnit(String unit) {
			this.unit = unit != null ? unit : "";
			return this;
		}

		/**
		 * Gets the time logs.
		 *
		 * @return the time logs
		 */
		public List<TimeLog> getTimeLogs() {
			List<TimeLog> list = new ArrayList<>(parents.size());
			for (ValueBucket p : parents.values()) {
				if (p instanceof TimeLog)
					list.add((TimeLog) p);
			}
			return list;
		}

		/**
		 * Adds the time log.
		 *
		 * @param timelog the timelog
		 * @return the value info
		 */
		public ValueInfo addTimeLog(TimeLog timelog) {
			this.parents.put(timelog.uri, timelog);
			return this;
		}

		/**
		 * Gets the grids.
		 *
		 * @return the grids
		 */
		public List<Grid> getGrids() {
			List<Grid> list = new ArrayList<>(parents.size());
			for (ValueBucket p : parents.values()) {
				if (p instanceof Grid)
					list.add((Grid) p);
			}
			return list;
		}

		/**
		 * Adds the grid.
		 *
		 * @param grid the grid
		 * @return the value info
		 */
		public ValueInfo addGrid(Grid grid) {
			this.parents.put(grid.uri, grid);
			return this;
		}

		/**
		 * Translate value.
		 *
		 * @param value the value
		 * @return the double
		 */
		public double translateValue(long value) {
			return new BigDecimal(value).add(new BigDecimal(offset)).multiply(new BigDecimal(scale)).doubleValue();
		}

		/**
		 * Format value.
		 *
		 * @param translatedValue the translated value
		 * @return the string
		 */
		public String formatValue(double translatedValue) {
			NumberFormat format = DecimalFormat.getInstance();
			format.setMaximumFractionDigits(numberOfDecimals);
			return format.format(translatedValue);
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(valueUri);
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
			if (!(obj instanceof ValueInfo))
				return false;
			ValueInfo other = (ValueInfo) obj;
			return Objects.equals(valueUri, other.valueUri);
		}
	}

	/**
	 * The Class TimeLogWriter.
	 */
	public class TimeLogWriter extends ParserEntryWriter {

		/** The value infos. */
		private final List<? extends ValueInfo> valueInfos;

		/**
		 * Instantiates a new time log writer.
		 *
		 * @param timelog    the timelog
		 * @param valueInfos the value infos
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		TimeLogWriter(TimeLog timelog, List<? extends ValueInfo> valueInfos) throws IOException {
			super(timelog.name, ".csv");
			this.valueInfos = valueInfos;

			write(timelog.uri + CSV_LINEEND);

			write("time" + CSV_SEPARATOR + "latitude" + CSV_SEPARATOR + "longitude" + CSV_SEPARATOR + "altitude");
			for (ValueInfo desc : valueInfos) {
				write(CSV_SEPARATOR);
				write(desc.valueUri);
			}
			write(CSV_LINEEND);
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		@Override
		protected void addToMeta(String entryName) {
			JSONArray timelogs = meta.optJSONArray("timelogs");
			if (timelogs == null)
				meta.put("timelogs", timelogs = new JSONArray());
			timelogs.put(entryName);
		}

		/**
		 * Write.
		 *
		 * @param time      the time
		 * @param latitude  the latitude
		 * @param longitude the longitude
		 * @param altitude  the altitude
		 * @param values    the values
		 * @return the time log writer
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public TimeLogWriter write(Instant time, double latitude, double longitude, double altitude, Long[] values)
				throws IOException {
			if (values.length != valueInfos.size())
				throw new IOException("Values count must match the count valueInfos: " + valueInfos.size());
			write(Long.toString(time.getEpochSecond()));
			write(CSV_SEPARATOR);
			format.setMaximumFractionDigits(7);
			write(format.format(latitude));
			write(CSV_SEPARATOR);
			write(format.format(longitude));
			write(CSV_SEPARATOR);
			if (Double.isFinite(altitude)) {
				format.setMaximumFractionDigits(3);
				write(format.format(altitude));
			}

			for (int i = 0; i < values.length; ++i) {
				write(CSV_SEPARATOR);
				if (values[i] != null) {
					write(values[i].toString());
				}
			}
			write(CSV_LINEEND);
			return this;
		}

	}

	/**
	 * The Class GridWriter.
	 */
	public class GridWriter extends ParserEntryWriter {

		/** The column count. */
		private final int rowCount, columnCount;

		/** The written rows. */
		private int writtenRows;

		/**
		 * Instantiates a new grid writer.
		 *
		 * @param grid        the grid
		 * @param rowCount    the row count
		 * @param columnCount the column count
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		GridWriter(Grid grid, int rowCount, int columnCount) throws IOException {
			super(grid.name, ".csv");
			this.rowCount = rowCount;
			this.columnCount = columnCount;
			this.writtenRows = rowCount;

			format.setMaximumFractionDigits(7);
			write(Integer.toString(rowCount));
			write(CSV_SEPARATOR);
			write(Integer.toString(columnCount));
			write(CSV_SEPARATOR);
			write(format.format(grid.northMin));
			write(CSV_SEPARATOR);
			write(format.format(grid.eastMin));
			write(CSV_SEPARATOR);
			write(format.format(grid.northCellSize));
			write(CSV_SEPARATOR);
			write(format.format(grid.eastCellSize));
			write(CSV_LINEEND);
		}

		/**
		 * Adds the to meta.
		 *
		 * @param entryName the entry name
		 */
		@Override
		protected void addToMeta(String entryName) {
			JSONArray grids = meta.optJSONArray("grids");
			if (grids == null)
				meta.put("grids", grids = new JSONArray());
			grids.put(entryName);
		}

		/**
		 * Start grid value.
		 *
		 * @param info the info
		 * @return the grid writer
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public GridWriter startGridValue(ValueInfo info) throws IOException {
			if (writtenRows < rowCount)
				throw new IOException(
						"The last grid value is not completed, please add " + (rowCount - writtenRows) + " rows");
			write(info.valueUri);
			write(CSV_LINEEND);
			writtenRows = 0;
			return this;
		}

		/**
		 * Write grid row.
		 *
		 * @param values the values
		 * @return the grid writer
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public GridWriter writeGridRow(Long[] values) throws IOException {
			if (values.length != columnCount)
				throw new IOException("Values count must match the column count");
			if (writtenRows >= rowCount)
				throw new IOException("The current grid value is completed, start a new one");
			for (int i = 0; i < columnCount; ++i) {
				if (i > 0)
					write(CSV_SEPARATOR);
				if (values[i] != null) {
					write(values[i].toString());
				}
			}
			write(CSV_LINEEND);
			++writtenRows;
			return this;
		}

	}

}
