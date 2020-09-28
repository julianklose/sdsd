package de.sdsd.projekt.parser;

import static de.sdsd.projekt.api.Util.lit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.geotools.data.FileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;
import de.sdsd.projekt.api.ServiceResult.WikiInstance;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiType;

/**
 * Base class of the NRW antrag parser. It represents a complete antrag zip file.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AntragNDS implements AutoCloseable {

	public final CoordinateReferenceSystem crs;
	private final SAXParser parser;
	private final Path tempDir;
	private final FileDataStore store;
	
	private final byte[] antrag;
	private final List<String> errors;
	
	public AntragNDS(InputStream antragzip, List<String> errors) 
			throws IOException, ParserConfigurationException, ZipException, SAXException, NoSuchAuthorityCodeException, FactoryException {
		this.crs = CRS.decode("EPSG:3044", false);
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		saxfactory.setNamespaceAware(true);
		this.parser = saxfactory.newSAXParser();
		this.tempDir = Files.createTempDirectory("SdsdAntragNDS");
		this.errors = errors;
		Path shpfile = null;
		byte[] antrag = null;
		
		try (ZipInputStream stream = new ZipInputStream(antragzip, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = stream.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				String name = new File(entry.getName()).getName().toLowerCase();
				if(name.endsWith(".xml"))
					antrag = IOUtils.toByteArray(stream);
				else if(name.startsWith("teilschlaege.")) {
					Path filepath = tempDir.resolve(name);
					if(Files.exists(filepath)) continue;
					if(name.endsWith(".shp"))
						shpfile = filepath;
					Files.copy(stream, filepath);
				}
			}
		}
		
		if(antrag == null)
			throw new ZipException("File doesn't contain a .xml file");
		this.antrag = antrag;
		if(shpfile == null)
			throw new ZipException("File doesn't contain a .shp file");
		this.store = new ShapefileDataStoreFactory().createDataStore(shpfile.toUri().toURL());
	}
	
	public Map<Integer, Geometry> readTeilschlaggeometrien() throws FileNotFoundException, SAXException, IOException {
		SimpleFeatureCollection features = store.getFeatureSource().getFeatures();
		Map<Integer, Geometry> geometries = new HashMap<>(features.size());
		try(SimpleFeatureIterator fit = features.features()) {
			while(fit.hasNext()) {
				SimpleFeature feat = fit.next();
				Integer oid = (Integer)feat.getAttribute("OBJEKT_ID");
				Geometry geo = (Geometry)feat.getDefaultGeometry();
				geometries.put(oid, geo);
			}
		}
		return geometries;
	}
	
	public List<SchlagInfo> readFlaechenverzeichnis() throws FileNotFoundException, SAXException, IOException {
		AntragXMLHandler antragXMLHandler = new AntragXMLHandler();
		InputSource is = new InputSource(new ByteArrayInputStream(antrag));
		is.setEncoding("UTF-8");
		parser.parse(is, antragXMLHandler);
		return antragXMLHandler.getSchlaege();
	}
	
	public List<String> getErrors() {
		return errors;
	}
	
	@Override
	public void close() throws IOException {
		if(store != null) store.dispose();
		FileUtils.deleteDirectory(tempDir.toFile());
	}
	
	private static final WikiType ANTRAG = Util.UNKNOWN.res("applicationField"),
			KULTUR = Util.UNKNOWN.res("kultur");
	private static final WikiAttr YEAR = ANTRAG.prop("year"),
			NUMMER = ANTRAG.prop("number"),
			NAME = ANTRAG.prop("name"),
			FLIK = ANTRAG.prop("flik"),
			USAGE = ANTRAG.prop("usage"),
			USAGEPREV = ANTRAG.prop("prevUsage"),
			AREA = ANTRAG.prop("area");
	private static final WikiType TEILSCHLAG = Util.format("antragNDS").res("teilschlag");
	private static final WikiAttr TEIL = TEILSCHLAG.prop("teil");
	
	private static final Map<String, WikiInstance> KULTUREN;
	static {
		Map<String, WikiInstance> kulturen = null;
		try {
			kulturen = ParserAPI.getWikinormiaInstances(KULTUR, true); //TODO: local
		} catch (JsonRpcException e) {
			e.printStackTrace();
			if(kulturen == null)
				kulturen = Collections.emptyMap();
		}
		KULTUREN = kulturen;
	}
	
	public static class TeilschlagInfo {
		public final String uri = Util.createRandomUri();
		private final SchlagInfo schlag;
		private final int objectID;
		private String teil = null;
		private String area = null;
		
		public TeilschlagInfo(SchlagInfo schlag, String objectID) {
			this.schlag = schlag;
			this.objectID = Integer.parseInt(objectID);
		}
		public int getObjectID() {
			return objectID;
		}
		public String getTeil() {
			return teil;
		}
		public String getArea() {
			return area;
		}
		public String getNummer() {
			return schlag.getNummer();
		}
		public String getName() {
			return schlag.getName();
		}
		public String getFlik() {
			return schlag.getFlik();
		}
		public String getUsage() {
			return schlag.getUsage();
		}
		public List<TeilschlagInfo> getTeilschlaege() {
			return schlag.getTeilschlaege();
		}
		
		public Resource write(Model model) throws NumberFormatException {
			Resource res = model.createResource(uri)
					.addProperty(RDF.type, TEILSCHLAG);
			schlag.write(res);
			String label = schlag.getName();
			if(teil != null && !teil.isEmpty()) {
				res.addLiteral(TEIL, lit(teil));
				if(getTeilschlaege().size() > 1)
					label += " " + teil;
			}
			if(area != null && !area.isEmpty())
				res.addLiteral(AREA, lit(Double.parseDouble(area)));
			res.addLiteral(RDFS.label, lit(label));
			return res;
		}
		
		public JSONObject properties() throws NumberFormatException {
			JSONObject prop = schlag.properties();
			if(teil != null && !teil.isEmpty())
				prop.put("Teil", teil);
			if(area != null && !area.isEmpty())
				prop.put("Fläche (ha)", Double.parseDouble(area));
			return prop;
		}
	}
	
	public static class SchlagInfo {
		private String year = null;
		private String nummer = null, name = null;
		private String flik = null;
		private String usage = null;
		private List<TeilschlagInfo> teile = new ArrayList<>();
		
		SchlagInfo(String year) {
			this.year = year;
		}
		public String getNummer() {
			return nummer;
		}
		public String getName() {
			return name;
		}
		public String getFlik() {
			return flik;
		}
		public String getUsage() {
			return usage;
		}
		public List<TeilschlagInfo> getTeilschlaege() {
			return Collections.unmodifiableList(teile);
		}
		
		Resource write(Resource res) throws NumberFormatException {
			res.addLiteral(YEAR, lit(Integer.parseInt(year)));
			res.addLiteral(NUMMER, lit(Integer.parseInt(nummer)));
			if(name != null && !name.isEmpty())
				res.addLiteral(NAME, lit(name));
			if(flik != null && !flik.isEmpty())
				res.addLiteral(FLIK, lit(flik));
			if(usage != null && !usage.isEmpty()) {
				int code = Integer.parseInt(usage);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				if(k != null) res.addProperty(USAGE, k.res);
				else res.addProperty(USAGE, KULTUR.inst(Integer.toString(code)));
			}
			return res;
		}
		
		public JSONObject properties() throws NumberFormatException {
			JSONObject prop = new JSONObject()
					.put("Antragsjahr", Integer.parseInt(year))
					.put("Schlagnummer", Integer.parseInt(nummer));
			if(name != null && !name.isEmpty())
				prop.put("Bezeichnung", name);
			if(flik != null && !flik.isEmpty())
				prop.put("FLIK", flik);
			if(usage != null && !usage.isEmpty()) {
				int code = Integer.parseInt(usage);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				prop.put("Nutzung", k != null ? k.getLabel() : code);
			}
			return prop;
		}
	}
	
	private class AntragXMLHandler extends DefaultHandler {
		private final List<SchlagInfo> schlaege = new ArrayList<>();
		private String year = null;
		private SchlagInfo info = null;
		
		public List<SchlagInfo> getSchlaege() {
			return schlaege;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(localName.equals("hauptantrag"))
				year = attributes.getValue("antragsjahr");
			else if(localName.equals("schlag")) {
				info = new SchlagInfo(year);
				info.nummer = attributes.getValue("nr");
				info.flik = attributes.getValue("flik");
				info.name = attributes.getValue("bezeichnung");
				info.usage = attributes.getValue("kultur_code");
			}
			else if(localName.equals("teilschlag")) {
				if(info != null) {
					TeilschlagInfo teil = new TeilschlagInfo(info, attributes.getValue("arkos_objekt_id"));
					teil.teil = attributes.getValue("bezeichnung");
					teil.area = attributes.getValue("geometrie_groesse");
					info.teile.add(teil);
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(localName.equals("schlag")) {
				if(info != null)
					schlaege.add(info);
				info = null;
			}
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			errors.add(e.toString());
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			errors.add(e.toString());
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}
	}
 
}
