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
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;

/**
 * Main class of the Shape parser containing the two interface methods
 * {@link #shape(InputStream, OutputStream) shape} and
 * {@link #testShape(InputStream, OutputStream) testShape}. These functions are
 * called using corresponding command line arguments {@code parse} and
 * {@code test}.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * @see #shape(InputStream, OutputStream)
 * @see #testShape(InputStream, OutputStream)
 */
public class MainParser {

	/**
	 * The main method processes the command line arguments passed to the parser.
	 * There are two code paths leading either to the execution of the
	 * {@link #shape(InputStream, OutputStream) shape} or
	 * {@link #testShape(InputStream, OutputStream) testShape} method.
	 *
	 * @param args Command line arguments passed to the parser.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see #shape(InputStream, OutputStream)
	 * @see #testShape(InputStream, OutputStream)
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch (args[0].toLowerCase()) {
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

	/** The Constant geojson. */
	private static final GeometryJSON geojson = new GeometryJSON(7);

	/** The Constant featurejson. */
	private static final FeatureJSON featurejson = new FeatureJSON(geojson);

	/** The Constant FORMAT. */
	private static final WikiFormat FORMAT = Util.format("shape");

	/** The Constant ATTR. */
	private static final WikiType GEO = FORMAT.res("geo"), ATTR = FORMAT.res("attr");

	/** The Constant ATTR_VALUE. */
	private static final WikiAttr GEO_ATTR = GEO.prop("attr"), ATTR_NAME = ATTR.prop("name"),
			ATTR_VALUE = ATTR.prop("value");

	/**
	 * The {@code shape} method represents the {@code parse} method seen in other
	 * parser implementations. In this case the {@code input} contains shape data
	 * representing field polygons for example. These geometries are extracted from
	 * the input stream by using the {@code getFeatures} method of the
	 * {@code ShapeParser} class. Metadata of a geo feature is extracted,
	 * transformed into an RDF model and saved into the triple store. The geometries
	 * themselves are written into the MongoDB GeoStore using the {@code writeGeo}
	 * method provided by the SDSD Parser API.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void shape(InputStream input, OutputStream output) {
		Validation errors = new Validation();
		List<SimpleFeature> features = new ArrayList<>();
		long t1 = System.nanoTime();

		try (ShapeParser shape = new ShapeParser(input)) {
			SimpleFeatureCollection collection = shape.getFeatures();
			try (SimpleFeatureIterator it = collection.features()) {
				while (it.hasNext()) {
					features.add(it.next());
				}
			}
		} catch (Throwable e) {
			errors.fatal(e.getMessage());
		}

		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);

			if (!features.isEmpty()) {
				Model model = ModelFactory.createDefaultModel();

				try (GeoWriter geo = api.writeGeo()) {
					StringWriter sw = new StringWriter();
					for (int i = 0; i < features.size(); ++i) {
						SimpleFeature feature = features.get(i);

						Resource res = Util.createRandomUriResource(model, GEO, null);
						for (Property prop : feature.getProperties()) {
							Object value = prop.getValue();
							Literal lit = null;
							if (value instanceof Number)
								lit = model.createTypedLiteral(value);
							else if (value instanceof String)
								lit = Util.lit((String) value);

							if (lit != null) {
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

	/**
	 * The {@code testShape} method is used to decide whether a given input stream
	 * can be processed by this parser.
	 * 
	 * @param input  Input stream to be tested.
	 * @param output Unused
	 * @return {True} if this parser is likely capable to process the provided input
	 *         stream, {@code false} otherwise.
	 */
	public static boolean testShape(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName().toLowerCase();
				if (name.endsWith(".shp"))
					return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
}