package de.sdsd.projekt.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.geotools.data.FileDataStore;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.memory.MemoryFeatureCollection;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;

import org.geotools.referencing.CRS;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.feature.type.FeatureType;
import org.opengis.metadata.Identifier;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;

import org.opengis.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
/*
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
*/

/**
 * Base class of the shape parser. It represents a complete shape zip file.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ShapeParser implements AutoCloseable {

	private final Path tempDir;
	private final FileDataStore store;
	private final FeatureJSON fjson;
	private final String source_WKT;
	
	private static final boolean debug = false;

	/**
	 * Open a shape zip file.
	 * If the file is no zip file, an {@link ZipException} is thrown.
	 * 
	 * @param shapezip stream of a shape zip file
	 * @throws IOException error while reading from the input stream
	 * @throws ZipException error while reading the zip file content
	 */
    
	public ShapeParser(InputStream shapezip) throws IOException, ZipException {
		this.tempDir = Files.createTempDirectory("SdsdShapeParser");
		Path shpfile = null;
		Path prjfile = null;
		ZipInputStream stream = new ZipInputStream(shapezip, Charset.forName("Cp437"));
		ZipEntry entry;
		while((entry = stream.getNextEntry()) != null) {
			if(entry.isDirectory()) continue;
			Path filename = Paths.get(entry.getName()).getFileName();
			Path filepath = tempDir.resolve(filename);
			if(Files.exists(filepath)) continue;
			if(filename.toString().toLowerCase().endsWith(".shp"))
				shpfile = filepath;
			else if(filename.toString().toLowerCase().endsWith(".prj"))
				prjfile = filepath;
			Files.copy(stream, filepath);
		}
		if(shpfile == null)
			throw new ZipException("File doesn't contain a .shp file");
		this.store = new ShapefileDataStoreFactory().createDataStore(shpfile.toUri().toURL());
		this.fjson = new FeatureJSON(new GeometryJSON(7));
		String wkt = null;
		if(prjfile != null) {
			wkt = Files.readString(prjfile);
			if ( debug ) System.err.println("source WKT: "+wkt);
		}
		this.source_WKT = wkt;
	}

	private static String DEST_WKT  = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";
    private static String DEST_CODE = "EPSG:4326";
	
    /*
    private static String geoToString( Geometry geometry ) { // just for debugging
    	if ( geometry instanceof MultiPolygon ) {
    		MultiPolygon mp = (MultiPolygon)geometry;
    		Coordinate c = mp.getCoordinate();
    		return "MultiPolygon( "+mp.getDimension()+", ( "+c.getX()+", "+c.getY()+" ) )";
    	}
    	return "geo: "+geometry.getClass();
    }
    */
    
	public SimpleFeatureCollection getFeatures() throws IOException {
		if ( this.source_WKT == null || this.source_WKT == DEST_WKT )
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
		if ( debug ) System.err.println("sourceCSR Name: "+sourceCRS.getName().getCode()+", Identifiers: "+String.join(", ",(Iterable)sourceCRS.getIdentifiers())+", Alias: "+String.join(", ",(Iterable)sourceCRS.getAlias())+", "+"Remarks: "+sourceCRS.toString()+", WKT: "+sourceCRS.toWKT());

		MemoryFeatureCollection destCollection = null;
		try {
			boolean longitudeFirst = true;
			CoordinateReferenceSystem targetCRS = CRS.decode(DEST_CODE, longitudeFirst); // the coordinates system you want to reproject the data to
			if ( debug ) {
				System.err.println("decoded: targetCSR: "+targetCRS.getName().getCode());
				System.err.println("targetCSR Name: "+targetCRS.getName().getCode()+", Identifiers: "+targetCRS.getIdentifiers().stream().map(Identifier::getCode).collect(Collectors.joining(", "))+", Alias: "+String.join(", ",(Iterable)targetCRS.getAlias())+", "+"Remarks: "+targetCRS.toString()+", WKT: "+targetCRS.toWKT());
			}
			
			// define a MathTransform object
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

			while (iterator.hasNext()) {
				SimpleFeature feature = (SimpleFeature) iterator.next();
				//Collection<Property> properties = feature.getProperties();

				// get the geometry of the actual feature and transform it into a new variable
				Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
				Geometry reprojectedGeometry = JTS.transform(sourceGeometry, transform);
				
				/*
				if ( debug ) {
					System.err.println("sourceGeometry: "+geoToString(sourceGeometry));
					System.err.println("reprojectedGeometry: "+geoToString(reprojectedGeometry));
				}
				 */
				
				// set the reprojected geometry as the geometry of the actual feature
				feature.setDefaultGeometry(reprojectedGeometry);
				
				if ( null == destCollection ) destCollection = new MemoryFeatureCollection(feature.getFeatureType());
				destCollection.add(feature);
			}
		}
		catch(Exception e) {
			System.err.println("transformation error: "+e.toString());
		}
		
		return (SimpleFeatureCollection) destCollection; //featureCollection;
	}
	
	public String toGeoJson() throws IOException {
		SimpleFeatureCollection features = store.getFeatureSource().getFeatures();
		try (StringWriter writer = new StringWriter()) {
			fjson.writeFeatureCollection(features, writer);
			return writer.toString();
		}
	}

	@Override
	public void close() throws IOException {
		if(store != null) store.dispose();
		FileUtils.deleteDirectory(tempDir.toFile());
	}

}
