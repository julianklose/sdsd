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
import java.util.HashMap;
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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ParserAPI implements AutoCloseable {
	
	public static Map<String, WikiInstance> getWikinormiaInstances(Resource type, boolean local) throws JsonRpcException {
		Map<String, WikiInstance> out = null;
		try (RestClient client = new RestClient(local)) {
			JSONObject result = client.execute("api", "getWikinormiaInstances", null, type.getURI());
			out = ServiceAPI.result(result, WikiInstance::new);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return out;
	}
	
	private static final String CSV_SEPARATOR = ";";
	private static final String CSV_LINEEND = "\r\n";
	private static final NumberFormat format;
	static {
		format = NumberFormat.getInstance(Locale.US);
		format.setGroupingUsed(false);
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(7);
	}
	
	private final ZipOutputStream zip;
	private final JSONObject meta = new JSONObject();
	private ParserEntryWriter currentEntry = null;

	public ParserAPI(OutputStream out) {
		this.zip = new ZipOutputStream(out, Charset.forName("Cp437"));
	}
	
	public ParserAPI setParseTime(long milliseconds) {
		meta.put("parseTime", milliseconds);
		return this;
	}
	
	public ParserAPI setErrors(@Nullable List<String> errors) {
		if(errors != null && errors.size() > 0)
			meta.put("errors", new JSONArray(errors));
		else
			meta.remove("errors");
		return this;
	}
	
	protected void writeMeta() throws IOException {
		try (MetaWriter mw = new MetaWriter()) {
			mw.write(meta);
		}
	}
	
	public ParserAPI writeTriples(@Nullable Model model) throws IOException {
		if(meta.has("triples"))
			throw new IOException("You can only write one model");
		if(model != null && !model.isEmpty()) {
			try (TripleWriter tw = new TripleWriter()) {
				tw.write(model);
			}
		}
		return this;
	}
	
	public GeoWriter writeGeo() throws IOException {
		if(meta.has("geo"))
			throw new IOException("You can only write one geo collection");
		return new GeoWriter();
	}
	
	public TimeLogWriter addTimeLog(TimeLog timelog, List<? extends ValueInfo> valueInfos) throws IOException {
		return new TimeLogWriter(timelog, valueInfos);
	}
	
	public GridWriter addGrid(Grid grid, int rowCount, int columnCount) throws IOException {
		return new GridWriter(grid, rowCount, columnCount);
	}

	@Override
	public void close() throws IOException {
		writeMeta();
		zip.close();
	}
	
	public abstract class ParserEntryWriter extends Writer {
		public final String name;
		
		ParserEntryWriter(String name, String extension) throws IOException {
			this.name = name;
			
			if(currentEntry != null)
				throw new IOException("Close '" + currentEntry.name + "' writer before starting a new entry");
			ZipEntry entry = new ZipEntry(name + extension);
			zip.putNextEntry(entry);
			addToMeta(entry.getName());
			currentEntry = this;
		}
		
		protected abstract void addToMeta(String entryName);
		
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			ByteBuffer b = StandardCharsets.UTF_8.encode(CharBuffer.wrap(cbuf, off, len));
			zip.write(b.array(), 0, b.limit());
		}
		
		@Override
		public void write(String str) throws IOException {
			zip.write(str.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public void flush() throws IOException {
			zip.flush();
		}
		
		@Override
		public void close() throws IOException {
			zip.closeEntry();
			currentEntry = null;
		}
		
	}
	
	private class MetaWriter extends ParserEntryWriter {

		MetaWriter() throws IOException {
			super("meta", ".json");
		}
		
		@Override
		protected void addToMeta(String entryName) {
		}
		
		public MetaWriter write(JSONObject meta) throws IOException {
			write(meta.toString());
			return this;
		}
		
	}
	
	private class TripleWriter extends ParserEntryWriter {

		TripleWriter() throws IOException {
			super("triples", ".ttl");
		}
		
		@Override
		protected void addToMeta(String entryName) {
			meta.put("triples", entryName);
		}
		
		public TripleWriter write(Model model) throws IOException {
			model.setNsPrefix("rdf", RDF.uri);
			model.setNsPrefix("rdfs", RDFS.uri);
			model.setNsPrefix("owl", OWL.NS);
			model.setNsPrefix("dcterms", DCTerms.NS);
			model.setNsPrefix("xsd", XSD.NS);
			model.setNsPrefix("wkn", ServiceAPI.WIKI_URI);
			
			StmtIterator stmtit = model.listStatements();
			try {
				while(stmtit.hasNext()) {
					Statement stmt = stmtit.next();
					String predicate = stmt.getPredicate().getURI();
					if(predicate.startsWith(ServiceAPI.WIKI_URI)) {
						int ind = predicate.lastIndexOf('#');
						if(ind > 0) {
							model.setNsPrefix(predicate.substring(ServiceAPI.WIKI_URI.length(), ind), predicate.substring(0, ind+1));
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
	
	public class GeoWriter extends ParserEntryWriter {
		private int count = 0;

		GeoWriter() throws IOException {
			super("geo", ".json");
			write("{\"type\":\"FeatureCollection\",\"features\":[");
		}
		
		@Override
		public void close() throws IOException {
			write("]}");
			super.close();
		}
		
		@Override
		protected void addToMeta(String entryName) {
			meta.put("geo", entryName);
		}
		
		protected boolean checkGeometry(JSONObject geometry) throws JSONException {
			return !geometry.getString("type").isEmpty() && geometry.getJSONArray("coordinates").length() > 0;
		}
		
		public GeoWriter writeFeature(JSONObject geojson, ElementType type, String uri, String label) throws IOException, JSONException {
			if(uri == null || uri.isEmpty())
				throw new IllegalArgumentException("No URI specified");
			if(label == null)
				throw new IllegalArgumentException("No label specified");
			if(!"Feature".equals(geojson.getString("type")) || !checkGeometry(geojson.getJSONObject("geometry")))
				throw new JSONException("Given geojson is no valid GeoJSON feature");
			geojson.put("id", uri);
			if(type != ElementType.Other)
				geojson.put("elementType", type.name());
			geojson.put("label", label);
			if(count > 0) write(",");
			write(geojson.toString());
			++count;
			return this;
		}
		
		public GeoWriter writeGeometry(JSONObject geojson, ElementType type, String uri, @Nullable JSONObject properties, String label) throws IOException, JSONException {
			if(!checkGeometry(geojson))
				throw new JSONException("Given geojson is no valid GeoJSON geometry");
			return writeFeature(new JSONObject()
					.put("type", "Feature")
					.put("geometry", geojson)
					.put("properties", properties != null ? properties : new JSONObject()), type, uri, label);
		}
		
	}
	
	private abstract static class ValueBucket {
		public final WikiType basetype;
		public final String uri;
		public final String name;
		
		public ValueBucket(WikiType type, String uri, String name) {
			if(uri == null || uri.isEmpty())
				throw new IllegalArgumentException("No URI specified");
			if(name == null || name.isEmpty())
				throw new IllegalArgumentException("No name specivied");
			this.basetype = type;
			this.uri = uri;
			this.name = name;
		}
		
		public Resource writeTo(Model model, Resource type) {
			Resource res = model.createResource(uri, type);
			res.addLiteral(basetype.prop("name"), Util.lit(name));
			return res;
		}

		public String getUri() {
			return uri;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(uri);
		}

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
	
	public static class TimeLog extends ValueBucket {
		public final Instant from, until;
		public final int count;
		
		public TimeLog(String uri, String name, Instant from, Instant until, int count) {
			super(Util.TIMELOG, uri, name);
			this.from = from;
			this.until = until;
			this.count = count;
		}
		
		@Override
		public Resource writeTo(Model model, Resource type) {
			Resource res = super.writeTo(model, type);
			res.addLiteral(basetype.prop("from"), Util.lit(from));
			res.addLiteral(basetype.prop("until"), Util.lit(until));
			res.addLiteral(basetype.prop("count"), Util.lit(count));
			return res;
		}
	}
	
	public static class Grid extends ValueBucket {
		public final double northMin, eastMin, northCellSize, eastCellSize;
		
		public Grid(String uri, String name, double northMin, double eastMin, double northCellSize, double eastCellSize) {
			super(Util.GRID, uri, name);
			this.northMin = northMin;
			this.eastMin = eastMin;
			this.northCellSize = northCellSize;
			this.eastCellSize = eastCellSize;
		}

		public double getNorthMin() {
			return northMin;
		}

		public double getEastMin() {
			return eastMin;
		}

		public double getNorthCellSize() {
			return northCellSize;
		}

		public double getEastCellSize() {
			return eastCellSize;
		}
	}
	
	public static class ValueInfo {
		public static final WikiType TYPE = Util.VALUEINFO;
		
		public final String valueUri;
		private String designator;
		private long offset;
		private double scale;
		private int numberOfDecimals;
		private String unit;
		
		private Map<String, ValueBucket> parents = new HashMap<>();
		
		public ValueInfo(String valueUri) {
			if(valueUri.indexOf(CSV_SEPARATOR) >= 0 || valueUri.indexOf(CSV_LINEEND) >= 0)
				throw new IllegalArgumentException("Invalid characters in valueUri");
			this.valueUri = valueUri;
			this.designator = "";
			this.offset = 0;
			this.scale = 1.;
			this.numberOfDecimals = 0;
			this.unit = "";
		}
		
		public final Resource writeTo(Model model, WikiType type) {
			if(parents.isEmpty()) throw new IllegalStateException("There is no timelog or grid set for this valueinfo");
			Resource res = writeToNoParent(model, type);
			for(String uri : parents.keySet()) {
				res.addProperty(DCTerms.isPartOf, model.createResource(uri));
			}
			return res;
		}
		
		public final Resource writeToNoParent(Model model, WikiType type) {
			Resource res = model.createResource(valueUri, type);
			if(!designator.isEmpty()) res.addLiteral(TYPE.prop("designator"), Util.lit(designator));
			if(offset != 0) res.addLiteral(TYPE.prop("offset"), Util.lit(offset));
			if(scale != 1.) res.addLiteral(TYPE.prop("scale"), Util.lit(scale));
			if(numberOfDecimals != 0) res.addLiteral(TYPE.prop("numberOfDecimals"), Util.lit(numberOfDecimals));
			if(!unit.isEmpty()) res.addLiteral(TYPE.prop("unit"), Util.lit(unit));
			return res;
		}
		
		public String getValueUri() {
			return valueUri;
		}
		
		public String getDesignator() {
			return designator;
		}

		public ValueInfo setDesignator(String designator) {
			this.designator = designator != null ? designator : "";
			return this;
		}

		public long getOffset() {
			return offset;
		}

		public ValueInfo setOffset(long offset) {
			this.offset = offset;
			return this;
		}

		public double getScale() {
			return scale;
		}

		public ValueInfo setScale(double scale) {
			this.scale = scale;
			return this;
		}

		public int getNumberOfDecimals() {
			return numberOfDecimals;
		}

		public ValueInfo setNumberOfDecimals(int numberOfDecimals) {
			this.numberOfDecimals = numberOfDecimals;
			return this;
		}

		public String getUnit() {
			return unit;
		}

		public ValueInfo setUnit(String unit) {
			this.unit = unit != null ? unit : "";
			return this;
		}
		
		public List<TimeLog> getTimeLogs() {
			List<TimeLog> list = new ArrayList<>(parents.size());
			for(ValueBucket p : parents.values()) {
				if(p instanceof TimeLog)
					list.add((TimeLog) p);
			}
			return list;
		}
		
		public ValueInfo addTimeLog(TimeLog timelog) {
			this.parents.put(timelog.uri, timelog);
			return this;
		}
		
		public List<Grid> getGrids() {
			List<Grid> list = new ArrayList<>(parents.size());
			for(ValueBucket p : parents.values()) {
				if(p instanceof Grid)
					list.add((Grid) p);
			}
			return list;
		}
		
		public ValueInfo addGrid(Grid grid) {
			this.parents.put(grid.uri, grid);
			return this;
		}

		public double translateValue(long value) {
			return new BigDecimal(value)
					.add(new BigDecimal(offset))
					.multiply(new BigDecimal(scale))
					.doubleValue();
		}
		
		public String formatValue(double translatedValue) {
			NumberFormat format = DecimalFormat.getInstance();
			format.setMaximumFractionDigits(numberOfDecimals);
			return format.format(translatedValue);
		}

		@Override
		public int hashCode() {
			return Objects.hash(valueUri);
		}

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
	
	public class TimeLogWriter extends ParserEntryWriter {
		private final List<? extends ValueInfo> valueInfos;
		
		TimeLogWriter(TimeLog timelog, List<? extends ValueInfo> valueInfos) throws IOException {
			super(timelog.name, ".csv");
			this.valueInfos = valueInfos;
			
			write(timelog.uri + CSV_LINEEND);
			
			write("time" + CSV_SEPARATOR + "latitude" + CSV_SEPARATOR + "longitude" + CSV_SEPARATOR + "altitude");
			for(ValueInfo desc : valueInfos) {
				write(CSV_SEPARATOR);
				write(desc.valueUri);
			}
			write(CSV_LINEEND);
		}
		
		@Override
		protected void addToMeta(String entryName) {
			JSONArray timelogs = meta.optJSONArray("timelogs");
			if(timelogs == null) meta.put("timelogs", timelogs = new JSONArray());
			timelogs.put(entryName);
		}
		
		public TimeLogWriter write(Instant time, double latitude, double longitude, double altitude, Long[] values) throws IOException {
			if(values.length != valueInfos.size())
				throw new IOException("Values count must match the count valueInfos: " + valueInfos.size());
			write(Long.toString(time.getEpochSecond()));
			write(CSV_SEPARATOR);
			format.setMaximumFractionDigits(7);
			write(format.format(latitude));
			write(CSV_SEPARATOR);
			write(format.format(longitude));
			write(CSV_SEPARATOR);
			if(Double.isFinite(altitude)) {
				format.setMaximumFractionDigits(3);
				write(format.format(altitude));
			}
			
			for(int i = 0; i < values.length; ++i) {
				write(CSV_SEPARATOR);
				if(values[i] != null) {
					write(values[i].toString());
				}
			}
			write(CSV_LINEEND);
			return this;
		}
		
	}
	
	public class GridWriter extends ParserEntryWriter {
		private final int rowCount, columnCount;
		private int writtenRows;

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
		
		@Override
		protected void addToMeta(String entryName) {
			JSONArray grids = meta.optJSONArray("grids");
			if(grids == null) meta.put("grids", grids = new JSONArray());
			grids.put(entryName);
		}
		
		public GridWriter startGridValue(ValueInfo info) throws IOException {
			if(writtenRows < rowCount)
				throw new IOException("The last grid value is not completed, please add " + (rowCount - writtenRows) + " rows");
			write(info.valueUri);
			write(CSV_LINEEND);
			writtenRows = 0;
			return this;
		}
		
		public GridWriter writeGridRow(Long[] values) throws IOException {
			if(values.length != columnCount)
				throw new IOException("Values count must match the column count");
			if(writtenRows >= rowCount) 
				throw new IOException("The current grid value is completed, start a new one");
			for(int i = 0; i < columnCount; ++i) {
				if(i > 0) write(CSV_SEPARATOR);
				if(values[i] != null) {
					write(values[i].toString());
				}
			}
			write(CSV_LINEEND);
			++writtenRows;
			return this;
		}
		
	}
	
}
