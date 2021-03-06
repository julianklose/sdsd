package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.GeoWriter;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.parser.AntragRLP.SchlagInfo;

/**
 * The Class MainParser.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class MainParser {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch (args[0].toLowerCase()) {
			case "parse":
				parse(in, out);
				break;
			case "test":
				System.exit(test(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse' or 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}

	/** The Constant geojson. */
	private static final GeometryJSON geojson = new GeometryJSON(7);

	/**
	 * Parses the.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void parse(InputStream input, OutputStream output) {
		Validation errors = new Validation();
		Model model = null;
		List<JSONObject> features = null;
		long t1 = System.nanoTime();

		try {
			AntragRLP antrag = new AntragRLP(input, errors);

			Map<Integer, Geometry> geometries = antrag.readTeilschlaggeometrien();
			Map<Integer, SchlagInfo> fields = antrag.readFlaechenverzeichnis();

			if (fields.size() > 0) {
				model = ModelFactory.createDefaultModel();
				for (Entry<Integer, SchlagInfo> e : fields.entrySet()) {
					if (!geometries.containsKey(e.getKey()))
						errors.error("Schlag " + e.getKey() + " missing geometry");
					e.getValue().write(model);
				}
			}

			if (geometries.size() > 0) {
				features = new ArrayList<>(geometries.size());
				for (Entry<Integer, Geometry> e : geometries.entrySet()) {
					SchlagInfo info = fields.get(e.getKey());
					if (info == null)
						errors.error("Schlag " + e.getKey() + " not found in SchlagDaten");
					else {
						try {
							Geometry geo = JTS.toGeographic(e.getValue(), antrag.crs);
							JSONObject feature = new JSONObject().put("type", "Feature").put("id", info.uri)
									.put("label", info.getName()).put("properties", info.properties());
							if (geo instanceof MultiPolygon) {
								MultiPolygon multi = (MultiPolygon) geo;
								for (int i = 0; i < multi.getNumGeometries(); ++i) {
									StringWriter json = new StringWriter();
									geojson.write(multi.getGeometryN(i), json);
									features.add(feature.put("geometry", new JSONObject(json.toString())));
								}
							} else {
								StringWriter json = new StringWriter();
								geojson.write(geo, json);
								features.add(feature.put("geometry", new JSONObject(json.toString())));
							}
						} catch (Exception e1) {
							errors.error(e.getKey() + ": " + e1.getMessage());
						}
					}
				}
			}

		} catch (Exception e) {
			errors.fatal(e.toString());
		}

		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);

			if (model != null)
				api.writeTriples(model);

			if (features != null && !features.isEmpty()) {
				try (GeoWriter geo = api.writeGeo()) {
					for (JSONObject feature : features) {
						geo.writeFeature(feature, ElementType.Field, feature.getString("id"),
								feature.getString("label"));
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test.
	 *
	 * @param input  the input
	 * @param output the output
	 * @return true, if successful
	 */
	public static boolean test(InputStream input, OutputStream output) {
		boolean teilschlaggeometrien = false, flaechenverzeichnis = false;
		try (ZipInputStream in = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName().toLowerCase();
				if (name.endsWith("schlagdatenlafis.gml"))
					teilschlaggeometrien = true;
				else if (name.startsWith("schlagdaten") && name.endsWith(".xml"))
					flaechenverzeichnis = true;
			}
		} catch (IOException e) {
		}
		return teilschlaggeometrien && flaechenverzeichnis;
	}

}
