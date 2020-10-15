package de.sdsd.projekt.parser;

import static de.sdsd.projekt.api.Util.lit;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import de.sdsd.projekt.api.ParserAPI.Validation;
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
public class AntragNRW {

	/** The crs. */
	public final CoordinateReferenceSystem crs;
	
	/** The gf. */
	private final GeometryFactory gf;
	
	/** The parser. */
	private final SAXParser parser;
	
	/** The content. */
	private final Map<String, byte[]> content = new HashMap<>();
	
	/** The errors. */
	private final Validation errors;
	
	/**
	 * Instantiates a new antrag NRW.
	 *
	 * @param antragzip the antragzip
	 * @param errors the errors
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws ZipException the zip exception
	 * @throws SAXException the SAX exception
	 * @throws NoSuchAuthorityCodeException the no such authority code exception
	 * @throws FactoryException the factory exception
	 */
	public AntragNRW(InputStream antragzip, Validation errors) 
			throws IOException, ParserConfigurationException, ZipException, SAXException, NoSuchAuthorityCodeException, FactoryException {
		this.crs = CRS.decode("EPSG:25832", false);
		this.gf = new GeometryFactory(new PrecisionModel(), 25832);
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		saxfactory.setNamespaceAware(true);
		this.parser = saxfactory.newSAXParser();
		this.errors = errors;
		
		try (ZipInputStream stream = new ZipInputStream(antragzip, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = stream.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				String name = new File(entry.getName()).getName().toLowerCase();
				content.put(name, IOUtils.toByteArray(stream));
			}
		}
	}
	
	/**
	 * Parses the xml.
	 *
	 * @param name the name
	 * @param handler the handler
	 * @throws SAXException the SAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileNotFoundException the file not found exception
	 */
	private void parseXml(String name, DefaultHandler handler) throws SAXException, IOException, FileNotFoundException {
		byte[] bin = content.get(name.toLowerCase());
		if(bin == null) throw new FileNotFoundException("Couldn't find " + name);
		InputSource is = new InputSource(new ByteArrayInputStream(bin));
		is.setEncoding("UTF-8");
		parser.parse(is, handler);
	}
	
	/**
	 * Read teilschlaggeometrien.
	 *
	 * @return the map
	 * @throws FileNotFoundException the file not found exception
	 * @throws SAXException the SAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Map<String, Geometry> readTeilschlaggeometrien() throws FileNotFoundException, SAXException, IOException {
		AntragGMLHandler antragGMLHandler = new AntragGMLHandler();
		parseXml("teilschlaggeometrien.gml", antragGMLHandler);
		return antragGMLHandler.getGeometries();
	}
	
	/**
	 * Read flaechenverzeichnis.
	 *
	 * @return the map
	 * @throws FileNotFoundException the file not found exception
	 * @throws SAXException the SAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Map<String, SchlagInfo> readFlaechenverzeichnis() throws FileNotFoundException, SAXException, IOException {
		AntragXMLHandler antragXMLHandler = new AntragXMLHandler();
		parseXml("flächenverzeichnis.xml", antragXMLHandler);
		return antragXMLHandler.getSchlaege();
	}
	
	/**
	 * Gets the errors.
	 *
	 * @return the errors
	 */
	public Validation getErrors() {
		return errors;
	}
	
	/**
	 * The Class AntragGMLHandler.
	 */
	private class AntragGMLHandler extends DefaultHandler {
		
		/** The geometries. */
		private final Map<String, Geometry> geometries = new HashMap<>();
		
		/** The teilb. */
		private StringBuilder schlagb = null, teilb = null;
		
		/** The teil. */
		private String schlag = null, teil = null;
		
		/** The gml. */
		private GMLHandler gml = null;
		
		/** The geo. */
		private Geometry geo = null;
		
		/**
		 * Gets the geometries.
		 *
		 * @return the geometries
		 */
		public Map<String, Geometry> getGeometries() {
			return geometries;
		}

		/**
		 * Start element.
		 *
		 * @param uri the uri
		 * @param localName the local name
		 * @param qName the q name
		 * @param attributes the attributes
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(gml != null)
				gml.startElement(uri, localName, qName, attributes);
			else if(localName.equals("SCHLAGNR"))
				schlagb = new StringBuilder();
			else if(localName.equals("TEILSCHLAG"))
				teilb = new StringBuilder();
			else if(localName.equals("GEO_COORD_"))
				gml = new GMLHandler(gf, null);
		}

		/**
		 * End element.
		 *
		 * @param uri the uri
		 * @param localName the local name
		 * @param qName the q name
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(localName.equals("GEO_COORD_")) {
				if(gml.isGeometryComplete())
					geo = gml.getGeometry();
				gml = null;
			} else if(gml != null)
				gml.endElement(uri, localName, qName);
			else if(localName.equals("SCHLAGNR")) {
				schlag = schlagb.toString();
				schlagb = null;
			} else if(localName.equals("TEILSCHLAG")) {
				teil = teilb.toString();
				teilb = null;
			} else if(localName.equals("tschlag")) {
				if(schlag != null && geo != null)
					geometries.put(teil != null ? schlag + teil : schlag, geo);
			}
		}

		/**
		 * Characters.
		 *
		 * @param ch the ch
		 * @param start the start
		 * @param length the length
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if(gml != null)
				gml.characters(ch, start, length);
			else if(schlagb != null)
				schlagb.append(ch, start, length);
			else if(teilb != null)
				teilb.append(ch, start, length);
		}

		/**
		 * Ignorable whitespace.
		 *
		 * @param ch the ch
		 * @param start the start
		 * @param length the length
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			if(gml != null)
				gml.ignorableWhitespace(ch, start, length);
		}

		/**
		 * Warning.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void warning(SAXParseException e) throws SAXException {
			errors.warn(e.toString());
		}

		/**
		 * Error.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void error(SAXParseException e) throws SAXException {
			errors.error(e.toString());
		}

		/**
		 * Fatal error.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}
	}
	
	/** The Constant KULTUR. */
	private static final WikiType ANTRAG = Util.UNKNOWN.res("applicationField"),
			KULTUR = Util.UNKNOWN.res("kultur");
	
	/** The Constant AREA. */
	private static final WikiAttr YEAR = ANTRAG.prop("year"),
			NUMMER = ANTRAG.prop("number"),
			NAME = ANTRAG.prop("name"),
			FLIK = ANTRAG.prop("flik"),
			USAGE = ANTRAG.prop("usage"),
			USAGEPREV = ANTRAG.prop("prevUsage"),
			AREA = ANTRAG.prop("area");
	
	/** The Constant TYPE. */
	private static final WikiType TYPE = Util.format("antragNRW").res("teilschlag");
	
	/** The Constant TEIL. */
	private static final WikiAttr TEIL = TYPE.prop("teil");
	
	/** The Constant KULTUREN. */
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
	
	/**
	 * The Class SchlagInfo.
	 */
	public static class SchlagInfo {
		
		/** The uri. */
		public final String uri = Util.createRandomUri();
		
		/** The year. */
		private StringBuilder year = null;
		
		/** The teil. */
		private String nummer = null, name = null, teil = null;
		
		/** The area. */
		private String area = null;
		
		/** The flik. */
		private String flik = null;
		
		/** The usage cur. */
		private String usagePrev = null, usageCur = null;
		
		/**
		 * Instantiates a new schlag info.
		 *
		 * @param year the year
		 */
		SchlagInfo(StringBuilder year) {
			this.year = year;
		}
		
		/**
		 * Gets the nummer.
		 *
		 * @return the nummer
		 */
		public String getNummer() {
			return nummer;
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
		 * Gets the teil.
		 *
		 * @return the teil
		 */
		public String getTeil() {
			return teil;
		}
		
		/**
		 * Gets the area.
		 *
		 * @return the area
		 */
		public String getArea() {
			return area;
		}
		
		/**
		 * Gets the flik.
		 *
		 * @return the flik
		 */
		public String getFlik() {
			return flik;
		}
		
		/**
		 * Gets the usage prev.
		 *
		 * @return the usage prev
		 */
		public String getUsagePrev() {
			return usagePrev;
		}
		
		/**
		 * Gets the usage cur.
		 *
		 * @return the usage cur
		 */
		public String getUsageCur() {
			return usageCur;
		}
		
		/**
		 * Write.
		 *
		 * @param model the model
		 * @return the resource
		 * @throws NumberFormatException the number format exception
		 */
		public Resource write(Model model) throws NumberFormatException {
			Resource res = model.createResource(uri)
					.addProperty(RDF.type, TYPE)
					.addLiteral(YEAR, lit(Integer.parseInt(year.toString())))
					.addLiteral(NUMMER, lit(Integer.parseInt(nummer)));
			if(name != null && !name.isEmpty())
				res.addLiteral(NAME, lit(name)).addLiteral(RDFS.label, lit(name));
			if(teil != null && !teil.isEmpty())
				res.addLiteral(TEIL, lit(teil));
			if(area != null && !area.isEmpty())
				res.addLiteral(AREA, lit(Double.parseDouble(area)/10000.));
			if(flik != null && !flik.isEmpty())
				res.addLiteral(FLIK, lit(flik));
			if(usagePrev != null && !usagePrev.isEmpty()) {
				int code = Integer.parseInt(usagePrev);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				if(k != null) res.addProperty(USAGEPREV, k.res);
				else res.addProperty(USAGEPREV, KULTUR.inst(Integer.toString(code)));
			}
			if(usageCur != null && !usageCur.isEmpty()) {
				int code = Integer.parseInt(usageCur);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				if(k != null) res.addProperty(USAGE, k.res);
				else res.addProperty(USAGE, KULTUR.inst(Integer.toString(code)));
			}
			return res;
		}
		
		/**
		 * Properties.
		 *
		 * @return the JSON object
		 * @throws NumberFormatException the number format exception
		 */
		public JSONObject properties() throws NumberFormatException {
			JSONObject prop = new JSONObject()
					.put("Antragsjahr", Integer.parseInt(year.toString()))
					.put("Schlagnummer", Integer.parseInt(nummer));
			if(name != null && !name.isEmpty())
				prop.put("Bezeichnung", name);
			if(teil != null && !teil.isEmpty())
				prop.put("Teilschlag", teil);
			if(area != null && !area.isEmpty())
				prop.put("Fläche (ha)", Double.parseDouble(area)/10000.);
			if(flik != null && !flik.isEmpty())
				prop.put("FLIK", flik);
			if(usagePrev != null && !usagePrev.isEmpty()) {
				int code = Integer.parseInt(usagePrev);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				prop.put("Nutzung Vorjahr", k != null ? k.getLabel() : code);
			}
			if(usageCur != null && !usageCur.isEmpty()) {
				int code = Integer.parseInt(usageCur);
				WikiInstance k = KULTUREN.get(Integer.toString(code));
				prop.put("Nutzung", k != null ? k.getLabel() : code);
			}
			return prop;
		}
	}
	
	/**
	 * The Class AntragXMLHandler.
	 */
	private class AntragXMLHandler extends DefaultHandler {
		
		/** The schlaege. */
		private final Map<String, SchlagInfo> schlaege = new HashMap<>();
		
		/** The sb. */
		private final StringBuilder sb = new StringBuilder();
		
		/** The year. */
		private final StringBuilder year = new StringBuilder();
		
		/** The info. */
		private SchlagInfo info = null;
		
		/** The nutzungvj. */
		private boolean schlag = false, nutzungaj = false, nutzungvj = false;
		
		/**
		 * Gets the schlaege.
		 *
		 * @return the schlaege
		 */
		public Map<String, SchlagInfo> getSchlaege() {
			return schlaege;
		}

		/**
		 * Start element.
		 *
		 * @param uri the uri
		 * @param localName the local name
		 * @param qName the q name
		 * @param attributes the attributes
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(localName.equals("parzelle"))
				info = new SchlagInfo(year);
			else if(localName.equals("schlag"))
				schlag = true;
			else if(localName.equals("nutzungaj"))
				nutzungaj = true;
			else if(localName.equals("nutzungvj"))
				nutzungvj = true;
			sb.setLength(0);
		}

		/**
		 * End element.
		 *
		 * @param uri the uri
		 * @param localName the local name
		 * @param qName the q name
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(localName.equals("antragsjahr"))
				year.append(sb);
			else if(localName.equals("parzelle")) {
				if(info != null && info.nummer != null)
					schlaege.put(info.teil != null ? info.nummer + info.teil : info.nummer, info);
				info = null;
			} else if(localName.equals("schlag"))
				schlag = false;
			else if(localName.equals("nutzungaj"))
				nutzungaj = false;
			else if(localName.equals("nutzungvj"))
				nutzungvj = false;
			else if(info != null) {
				if(schlag) {
					if(localName.equals("nummer"))
						info.nummer = sb.toString();
					else if(localName.equals("bezeichnung"))
						info.name = sb.toString();
				} else if(localName.equals("teilschlag"))
					info.teil = sb.toString();
				else if(localName.equals("nettoflaeche"))
					info.area = sb.toString();
				else if(localName.equals("feldblock"))
					info.flik = sb.toString();
				else if(nutzungaj && localName.equals("code"))
					info.usageCur = sb.toString();
				else if(nutzungvj && localName.equals("code"))
					info.usagePrev = sb.toString();
			}
		}

		/**
		 * Characters.
		 *
		 * @param ch the ch
		 * @param start the start
		 * @param length the length
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			sb.append(ch, start, length);
		}

		/**
		 * Warning.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void warning(SAXParseException e) throws SAXException {
			errors.warn(e.toString());
		}

		/**
		 * Error.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void error(SAXParseException e) throws SAXException {
			errors.error(e.toString());
		}

		/**
		 * Fatal error.
		 *
		 * @param e the e
		 * @throws SAXException the SAX exception
		 */
		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}
	}
 
}
