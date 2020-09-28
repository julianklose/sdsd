package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.json.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.GeoWriter;
import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;

public class MainParser {

	public static void main(String[] args) throws IOException {
		if(args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch(args[0].toLowerCase()) {
			case "parse":
				shape(in, out);
				break;
			case "test":
				System.exit(testShape(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse' or 'test')");
				break;
			}
		} else 
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}
	
	private static final GeometryJSON geojson = new GeometryJSON(7);
	private static final FeatureJSON featurejson = new FeatureJSON(geojson);
	
	private static final WikiFormat FORMAT = Util.format("shape");
	private static final WikiType GEO = FORMAT.res("geo"), ATTR = FORMAT.res("attr");
	private static final WikiAttr GEO_ATTR = GEO.prop("attr"), ATTR_NAME = ATTR.prop("name"), ATTR_VALUE = ATTR.prop("value");
	
	public static void shape(InputStream input, OutputStream output) {
		List<String> errors = new ArrayList<>();
		List<SimpleFeature> features = new ArrayList<>();
		long t1 = System.nanoTime();
		
		try (ShapeParser shape = new ShapeParser(input)) {
			SimpleFeatureCollection collection = shape.getFeatures();
			try (SimpleFeatureIterator it = collection.features()) {
				while(it.hasNext()) {
					features.add(it.next());
				}
			}
		} catch (Throwable e) {
			errors.add(e.getMessage());
		}
		
		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime()-t1)/1000000);
			api.setErrors(errors);
			
			if(!features.isEmpty()) {
				Model model = ModelFactory.createDefaultModel();
				
				try (GeoWriter geo = api.writeGeo()) {
					StringWriter sw = new StringWriter();
					for(int i = 0; i < features.size(); ++i) {
						SimpleFeature feature = features.get(i);
						
						Resource res = Util.createRandomUriResource(model, GEO, null);
						for(Property prop : feature.getProperties()) {
							Object value = prop.getValue();
							Literal lit = null;
							if(value instanceof Number)
								lit = model.createTypedLiteral(value);
							else if(value instanceof String)
								lit = Util.lit((String)value);
							
							if(lit != null) {
								Resource attr = Util.createRandomUriResource(model, ATTR, null)
										.addLiteral(ATTR_NAME, Util.lit(prop.getName().getLocalPart()))
										.addLiteral(ATTR_VALUE, lit);
								res.addProperty(GEO_ATTR, attr);
							}
						}
						
						sw.getBuffer().setLength(0);
						featurejson.writeFeature(feature, sw);
						JSONObject geojson = new JSONObject(sw.toString());
						Object id = geojson.remove("id");
						geo.writeFeature(geojson, ElementType.Other, res.getURI(), id.toString());
					}
				}
				
				api.writeTriples(model);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static boolean testShape(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = in.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				String name = entry.getName().toLowerCase();
				if(name.endsWith(".shp"))
					return true;
			}
		} catch (IOException e) {}
		return false;
	}

}
