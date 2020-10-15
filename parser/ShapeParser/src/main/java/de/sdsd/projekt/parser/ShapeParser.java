package de.sdsd.projekt.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.geotools.data.FileDataStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
/*
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
*/
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Base class of the shape parser responsible for parsing an input stream
 * containing zipped shape information. This class is instantiated inside the
 * {@code shape} method of the corresponding {MainParser.java} source file.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class ShapeParser implements AutoCloseable {

	/** The temp dir. */
	private final Path tempDir;
	
	/** The store. */
	private final FileDataStore store;
	
	/** The fjson. */
	private final FeatureJSON fjson;
	
	/** The source WKT. */
	private final String source_WKT;

	/** The Constant debug. */
	private static final boolean debug = false;

	/**
	 * Calling the constructor of the {@code ShapeParser} class opens a ZIP file
	 * containing shape information. After that, the directory structure of that ZIP
	 * file is traversed in order to locate a {@code .shp} (shape) and {@code .prj}
	 * (project) file. If no shape file is found, a {@code ZipException} is thrown.
	 * If a shape file is found, its contents are mapped to a URI and a new
	 * {@code FileDataStore} is created using the {@code ShapefileDataStoreFactory}.
	 * 
	 * @param shapezip Input stream containing zipped shape file information.
	 * @throws IOException  Error while reading from the input stream.
	 * @throws ZipException Error while reading the content's of the ZIP file.
	 */
	public ShapeParser(InputStream shapezip) throws IOException, ZipException {
		this.tempDir = Files.createTempDirectory("SdsdShapeParser");
		Path shpfile = null;
		Path prjfile = null;
		ZipInputStream stream = new ZipInputStream(shapezip, Charset.forName("Cp437"));
		ZipEntry entry;
		while ((entry = stream.getNextEntry()) != null) {
			if (entry.isDirectory())
				continue;
			Path filename = Paths.get(entry.getName()).getFileName();
			Path filepath = tempDir.resolve(filename);
			if (Files.exists(filepath))
				continue;
			if (filename.toString().toLowerCase().endsWith(".shp"))
				shpfile = filepath;
			else if (filename.toString().toLowerCase().endsWith(".prj"))
				prjfile = filepath;
			Files.copy(stream, filepath);
		}
		if (shpfile == null)
			throw new ZipException("File doesn't contain a .shp file");
		this.store = new ShapefileDataStoreFactory().createDataStore(shpfile.toUri().toURL());
		this.fjson = new FeatureJSON(new GeometryJSON(7));
		String wkt = null;
		if (prjfile != null) {
			wkt = Files.readString(prjfile);
			if (debug)
				System.err.println("source WKT: " + wkt);
		}
		this.source_WKT = wkt;
	}

	/** The dest wkt. */
	private static String DEST_WKT = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";
	
	/** The dest code. */
	private static String DEST_CODE = "EPSG:4326";

	/*
	 * private static String geoToString( Geometry geometry ) { // just for
	 * debugging if ( geometry instanceof MultiPolygon ) { MultiPolygon mp =
	 * (MultiPolygon)geometry; Coordinate c = mp.getCoordinate(); return
	 * "MultiPolygon( "+mp.getDimension()+", ( "+c.getX()+", "+c.getY()+" ) )"; }
	 * return "geo: "+geometry.getClass(); }
	 */

	/**
	 * This method extracts geometries (polygons, lines, points etc.), which are
	 * called "features", from the shape file's data store. In this process, the
	 * features are calculated using the coordinate reference system (CRS) stored
	 * alongside the shape data. If the CRS cannot be determined dynamically at
	 * runtime, the {@code DEST_CODE} CRS is used.
	 *
	 * @return Collection of geometry features using either their native CRS if it
	 *         exists or the CRS defined by the {@code DEST_CODE} constant.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public SimpleFeatureCollection getFeatures() throws IOException {
		if (this.source_WKT == null || this.source_WKT == DEST_WKT)
			return store.getFeatureSource().getFeatures();

		// Transform from source_WKT to DEST_WKT
		SimpleFeatureSource featureSource = store.getFeatureSource();

		FeatureCollection featureCollection = featureSource.getFeatures();
		FeatureIterator iterator = featureCollection.features();

		// get dynamically the CRS of your data:
		FeatureType schema = featureSource.getSchema();

		CoordinateReferenceSystem sourceCRS = schema.getCoordinateReferenceSystem();
		// OR fallback to hardcoded 3035 if the above fails:
		// CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3875");
		if (debug)
			System.err.println("sourceCSR Name: " + sourceCRS.getName().getCode() + ", Identifiers: "
					+ String.join(", ", (Iterable) sourceCRS.getIdentifiers()) + ", Alias: "
					+ String.join(", ", (Iterable) sourceCRS.getAlias()) + ", " + "Remarks: " + sourceCRS.toString()
					+ ", WKT: " + sourceCRS.toWKT());

		MemoryFeatureCollection destCollection = null;
		try {
			boolean longitudeFirst = true;
			CoordinateReferenceSystem targetCRS = CRS.decode(DEST_CODE, longitudeFirst); // the coordinates system you
																							// want to reproject the
																							// data to
			if (debug) {
				System.err.println("decoded: targetCSR: " + targetCRS.getName().getCode());
				System.err.println("targetCSR Name: " + targetCRS.getName().getCode() + ", Identifiers: "
						+ targetCRS.getIdentifiers().stream().map(Identifier::getCode).collect(Collectors.joining(", "))
						+ ", Alias: " + String.join(", ", (Iterable) targetCRS.getAlias()) + ", " + "Remarks: "
						+ targetCRS.toString() + ", WKT: " + targetCRS.toWKT());
			}

			// define a MathTransform object
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

			while (iterator.hasNext()) {
				SimpleFeature feature = (SimpleFeature) iterator.next();
				// Collection<Property> properties = feature.getProperties();

				// get the geometry of the actual feature and transform it into a new variable
				Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
				Geometry reprojectedGeometry = JTS.transform(sourceGeometry, transform);

				/*
				 * if ( debug ) {
				 * System.err.println("sourceGeometry: "+geoToString(sourceGeometry));
				 * System.err.println("reprojectedGeometry: "+geoToString(reprojectedGeometry));
				 * }
				 */

				// set the reprojected geometry as the geometry of the actual feature
				feature.setDefaultGeometry(reprojectedGeometry);

				if (null == destCollection)
					destCollection = new MemoryFeatureCollection(feature.getFeatureType());
				destCollection.add(feature);
			}
		} catch (Exception e) {
			System.err.println("transformation error: " + e.toString());
		}

		return destCollection; // featureCollection;
	}

	/**
	 * Utility function which converts the geometry features inside the shape file's
	 * data store to GeoJSON.
	 *
	 * @return GeoJSON representation of the shape file's geometry features.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String toGeoJson() throws IOException {
		SimpleFeatureCollection features = store.getFeatureSource().getFeatures();
		try (StringWriter writer = new StringWriter()) {
			fjson.writeFeatureCollection(features, writer);
			return writer.toString();
		}
	}

	/**
	 * Closes the shape file's data store and removes the temporary working
	 * directory opened inside the {@link #getFeatures() getFeatures} method.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see #getFeatures()
	 */
	@Override
	public void close() throws IOException {
		if (store != null)
			store.dispose();
		FileUtils.deleteDirectory(tempDir.toFile());
	}
}