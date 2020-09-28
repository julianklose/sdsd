package de.sdsd.projekt.parser;

import static de.sdsd.projekt.api.Util.lit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.gml2.GMLHandler;
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
public class AntragRLP {

	public final CoordinateReferenceSystem crs;
	private final GeometryFactory gf;
	private final SAXParser parser;
	
	private final byte[] antrag, geometries;
	private final List<String> errors;
	
	private int year;
	
	private static final Pattern XMLFILE = Pattern.compile("SchlagDaten_\\d+_(\\d+)\\.xml", Pattern.CASE_INSENSITIVE);
	
	public AntragRLP(InputStream antragzip, List<String> errors) 
			throws IOException, ParserConfigurationException, ZipException, SAXException, NoSuchAuthorityCodeException, FactoryException {
		this.crs = CRS.decode("EPSG:25832", false);
		this.gf = new GeometryFactory(new PrecisionModel(), 25832);
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		saxfactory.setNamespaceAware(true);
		this.parser = saxfactory.newSAXParser();
		this.errors = errors;
		
		byte[] antrag = null, geometries = null;
		try (ZipInputStream stream = new ZipInputStream(antragzip, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = stream.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				String name = new File(entry.getName()).getName().toLowerCase();
				Matcher matcher = XMLFILE.matcher(name);
				if(name.endsWith("schlagdatenlafis.gml"))
					geometries = IOUtils.toByteArray(stream);
				else if(matcher.matches()) {
					antrag = IOUtils.toByteArray(stream);
					year = Integer.parseInt(matcher.group(1));
				}
			}
		}
		
		if(antrag == null)
			throw new ZipException("File doesn't contain a .xml file");
		this.antrag = antrag;
		if(geometries == null)
			throw new ZipException("File doesn't contain a .gml file");
		this.geometries = geometries;
	}
	
	public Map<Integer, Geometry> readTeilschlaggeometrien() throws FileNotFoundException, SAXException, IOException {
		AntragGMLHandler antragGMLHandler = new AntragGMLHandler();
		InputSource is = new InputSource(new ByteArrayInputStream(geometries));
		is.setEncoding("UTF-8");
		parser.parse(is, antragGMLHandler);
		return antragGMLHandler.getGeometries();
	}
	
	public Map<Integer, SchlagInfo> readFlaechenverzeichnis() throws FileNotFoundException, SAXException, IOException {
		AntragXMLHandler antragXMLHandler = new AntragXMLHandler();
		InputSource is = new InputSource(new ByteArrayInputStream(antrag));
		is.setEncoding("UTF-8");
		parser.parse(is, antragXMLHandler);
		return antragXMLHandler.getSchlaege();
	}
	
	public List<String> getErrors() {
		return errors;
	}
	
	private class AntragGMLHandler extends DefaultHandler {
		private final Map<Integer, Geometry> geometries = new HashMap<>();
		private StringBuilder sb = null;
		private Integer schlag = null;
		private GMLHandler gml = null;
		private Geometry geo = null;
		
		public Map<Integer, Geometry> getGeometries() {
			return geometries;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(gml != null)
				gml.startElement(uri, localName, qName, attributes);
			else if(localName.equals("SLNR"))
				sb = new StringBuilder();
			else if(localName.equals("Jahr"))
				sb = new StringBuilder();
			else if(localName.equals("GEOM"))
				gml = new GMLHandler(gf, null);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(localName.equals("GEOM")) {
				if(gml.isGeometryComplete())
					geo = gml.getGeometry();
				gml = null;
			} else if(gml != null)
				gml.endElement(uri, localName, qName);
			else if(localName.equals("SLNR")) {
				try {
					schlag = Integer.valueOf(sb.toString());
				} catch (NumberFormatException e) {
					errors.add("Invalid SLNR: " + sb.toString());
				}
				sb = null;
			} else if(localName.equals("Jahr")) {
				try {
					year = Integer.parseInt(sb.toString());
				} catch (NumberFormatException e) {
					errors.add("Invalid Jahr: " + sb.toString());
				}
				sb = null;
			} else if(localName.equals("Schlaege")) {
				if(schlag != null && geo != null)
					geometries.put(schlag, geo);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if(gml != null)
				gml.characters(ch, start, length);
			else if(sb != null)
				sb.append(ch, start, length);
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			if(gml != null)
				gml.ignorableWhitespace(ch, start, length);
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

	private class AntragXMLHandler extends DefaultHandler {
		private final Map<Integer, SchlagInfo> schlaege = new HashMap<>();
		private final StringBuilder sb = new StringBuilder();
		private String vorjahr, antragsjahr;
		private SchlagInfo info = null;
		
		public Map<Integer, SchlagInfo> getSchlaege() {
			return schlaege;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(localName.equals("SchlagEintrag"))
				info = new SchlagInfo();
			sb.setLength(0);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(localName.equals("Vorjahr"))
				vorjahr = sb.toString();
			else if(localName.equals("Antragsjahr"))
				antragsjahr = sb.toString();
			else if(localName.equals("SchlagEintrag")) {
				if(info != null && info.nummer != null)
					schlaege.put(info.nummer, info);
				info = null;
			} else if(info != null) {
				try {
					switch(localName) {
					case "Schlag":
						info.nummer = Integer.parseInt(antragsjahr);
						break;
					case "Lage":
						info.name = antragsjahr;
						break;
					case "FlaecheKult":
						info.area += Double.parseDouble(antragsjahr);
						break;
					case "Flik":
						info.flik.add(antragsjahr);
						break;
					case "Kulturart":
						info.usageCur = Integer.parseInt(antragsjahr);
						info.usagePrev = Integer.parseInt(vorjahr);
						break;
					case "OekoSchlag":
						info.oeko = antragsjahr.equals("1");
						break;
					case "Oevf":
						info.oevf = antragsjahr.equals("1");
						break;
					}
				} catch (NumberFormatException e) {
					errors.add("Invalid " + localName + ": " + antragsjahr);
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			sb.append(ch, start, length);
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
	
	private static final WikiType ANTRAG = Util.UNKNOWN.res("applicationField"),
			KULTUR = Util.UNKNOWN.res("kultur");
	private static final WikiAttr YEAR = ANTRAG.prop("year"),
			NUMMER = ANTRAG.prop("number"),
			NAME = ANTRAG.prop("name"),
			FLIK = ANTRAG.prop("flik"),
			USAGE = ANTRAG.prop("usage"),
			USAGEPREV = ANTRAG.prop("prevUsage"),
			AREA = ANTRAG.prop("area");
	
	private static final WikiType TYPE = Util.format("antragRLP").res("schlag");
	private static final WikiAttr OEKO= TYPE.prop("OekoSchlag"),
			OEVF= TYPE.prop("Oevf");
	
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
	
	public class SchlagInfo {
		public final String uri = Util.createRandomUri();
		private Integer nummer;
		private String name;
		private double area = 0.;
		private final List<String> flik = new ArrayList<>();
		private int usagePrev, usageCur;
		private boolean oeko, oevf;
		
		public int getNummer() {
			return nummer;
		}
		public String getName() {
			return name;
		}
		public double getArea() {
			return area;
		}
		public List<String> getFlik() {
			return Collections.unmodifiableList(flik);
		}
		public int getUsagePrev() {
			return usagePrev;
		}
		public int getUsageCur() {
			return usageCur;
		}
		public boolean isOeko() {
			return oeko;
		}
		public boolean isOevf() {
			return oevf;
		}
		
		public Resource write(Model model) {
			Resource res = model.createResource(uri)
					.addProperty(RDF.type, TYPE)
					.addLiteral(YEAR, lit(year))
					.addLiteral(NUMMER, lit(nummer));
			if(name != null && !name.isEmpty())
				res.addLiteral(NAME, lit(name)).addLiteral(RDFS.label, lit(name));
			if(area != 0.)
				res.addLiteral(AREA, lit(area / 10000.));
			for(String fl : flik) {
				if(!fl.isEmpty())
					res.addLiteral(FLIK, lit(fl));
			}
			if(usagePrev != 0) {
				WikiInstance k = KULTUREN.get(Integer.toString(usagePrev));
				if(k != null) res.addProperty(USAGEPREV, k.res);
				else res.addProperty(USAGEPREV, KULTUR.inst(Integer.toString(usagePrev)));
			}
			if(usageCur != 0) {
				WikiInstance k = KULTUREN.get(Integer.toString(usageCur));
				if(k != null) res.addProperty(USAGE, k.res);
				else res.addProperty(USAGE, KULTUR.inst(Integer.toString(usageCur)));
			}
			res.addLiteral(OEKO, lit(oeko));
			res.addLiteral(OEVF, lit(oevf));
			return res;
		}
		
		public JSONObject properties() {
			JSONObject prop = new JSONObject()
					.put("Antragsjahr", year)
					.put("Schlagnummer", nummer);
			if(name != null && !name.isEmpty())
				prop.put("Bezeichnung", name);
			if(area != 0.)
				prop.put("Fläche (ha)", area / 10000.);
			JSONArray fliks = new JSONArray();
			for(String fl : flik) {
				if(!fl.isEmpty())
					fliks.put(fl);
			}
			prop.put("FLIK", fliks);
			if(usagePrev != 0) {
				WikiInstance k = KULTUREN.get(Integer.toString(usagePrev));
				prop.put("Nutzung Vorjahr", k != null ? k.getLabel() : usagePrev);
			}
			if(usageCur != 0) {
				WikiInstance k = KULTUREN.get(Integer.toString(usageCur));
				prop.put("Nutzung", k != null ? k.getLabel() : usageCur);
			}
			prop.put("Öko", oeko);
			prop.put("ÖVF", oevf);
			return prop;
		}
	}
 
}
