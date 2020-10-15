package de.sdsd.projekt.prototype.applogic;

import static de.sdsd.projekt.prototype.data.GeoElement.GEOFIELD;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONObject;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.UpdateResult;

import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.GeoElement;
import de.sdsd.projekt.prototype.data.GeoElement.ElementType;
import de.sdsd.projekt.prototype.data.GeoElement.MinMaxValues;
import de.sdsd.projekt.prototype.data.User;

/**
 * Provides all geometry functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class GeoFunctions {
	
	/** The app. */
	@SuppressWarnings("unused")
	private final ApplicationLogic app;
	
	/** The mongo. */
	final MongoCollection<Document> mongo;

	/**
	 * Instantiates a new geo functions.
	 *
	 * @param app the app
	 */
	public GeoFunctions(ApplicationLogic app) {
		this.app = app;
		this.mongo = app.mongo.sdsd.getCollection("geo");
		
		mongo.createIndex(Indexes.geo2dsphere(GEOFIELD));
		mongo.createIndex(Indexes.ascending(GeoElement.USER, GeoElement.URI, GeoElement.FILE, GeoElement.TYPE));
	}
	
	/**
	 * Insert.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param type the type
	 * @param geoJsonFeature the geo json feature
	 * @param label the label
	 * @param values the values
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void insert(File file, String uri, ElementType type, JSONObject geoJsonFeature, String label, @Nullable MinMaxValues values) throws IOException {
		if(type == ElementType.Field) insertField(file, uri, geoJsonFeature, label);
		else if(type == ElementType.TimeLog) throw new IllegalArgumentException("Use insertTlg for inserting TimeLogs");
		else mongo.insertOne(GeoElement.create(file, uri, type, geoJsonFeature.toString(), Double.NaN, label, values));
	}
	
	/**
	 * Insert field.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param geoJsonFeature the geo json feature
	 * @param label the label
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void insertField(File file, String uri, JSONObject geoJsonFeature, String label) throws IOException {
		JSONObject geoJsonGeometry = geoJsonFeature.getJSONObject("geometry");
		double area = calcArea(geoJsonGeometry.toString());
		
		String geoJsonFeatureString = geoJsonFeature.toString();
		UpdateResult res = mongo.updateOne(GeoElement.filter(file, uri, ElementType.Field), GeoElement.update(geoJsonFeatureString, area, label));
		if(uri == null || !res.wasAcknowledged() || res.getMatchedCount() == 0)
			mongo.insertOne(GeoElement.create(file, uri, ElementType.Field, geoJsonFeatureString, area, label, null));
	}
	
	/**
	 * Insert tlg.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param tlgName the tlg name
	 * @param geoJsonFeatureString the geo json feature string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void insertTlg(File file, String uri, String tlgName, String geoJsonFeatureString) throws IOException {
		UpdateResult res = mongo.updateOne(GeoElement.filterTlg(new TableFunctions.ElementKey(file.getUser(), file.getURI(), tlgName)), 
				GeoElement.update(geoJsonFeatureString, Double.NaN));
		if(!res.wasAcknowledged() || res.getMatchedCount() == 0)
			mongo.insertOne(GeoElement.createTlg(file, uri, tlgName, geoJsonFeatureString));
	}
	
	/**
	 * Insert tlg.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param tlgName the tlg name
	 * @param coords the coords
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void insertTlg(File file, String uri, String tlgName, Collection<Coordinate> coords) throws IOException {
		Coordinate[] points = coords.toArray(new Coordinate[coords.size()]);
		String geo = toGeoJson(createFeature(simplify(points, 0.001), uri));
		insertTlg(file, uri, tlgName, geo);
	}
	
	/**
	 * Delete from.
	 *
	 * @param user the user
	 * @param file the file
	 * @return true, if successful
	 */
	public boolean deleteFrom(User user, ObjectId file) {
		Bson filter = Filters.and(GeoElement.filterFile(file), GeoElement.filter(user));
		return mongo.deleteMany(filter).wasAcknowledged();
	}
	
	/**
	 * Tidy up.
	 *
	 * @param fileIds the file ids
	 */
	void tidyUp(Set<ObjectId> fileIds) {
		Set<ObjectId> delete = new HashSet<>();
		for(Document doc : mongo.find(Filters.nin(GeoElement.FILE, fileIds))) {
			delete.add(doc.getObjectId(GeoElement.ID));
		}
		
		System.out.println("Delete " + delete.size() + " geometries: " + delete.stream()
				.map(ObjectId::toHexString).collect(Collectors.joining(", ")));
		
		if(delete.size() > 0)
			System.out.println("Success: " + mongo.deleteMany(Filters.in(GeoElement.ID, delete)).wasAcknowledged());
	}
	
	/**
	 * Find.
	 *
	 * @param user the user
	 * @param filter the filter
	 * @return the list
	 */
	public List<GeoElement> find(User user, @Nullable Bson filter) {
		if(filter == null)
			filter = GeoElement.filter(user);
		else
			filter = Filters.and(GeoElement.filter(user), filter);
		return StreamSupport.stream(mongo.find(filter).spliterator(), false)
				.map(GeoElement::new)
				.collect(Collectors.toList());
	}
	
	/** The Constant geofactory. */
	private static final GeometryFactory geofactory = new GeometryFactory();
	
	/** The Constant geojson. */
	private static final GeometryJSON geojson = new GeometryJSON(7);
	
	/** The Constant featurejson. */
	private static final FeatureJSON featurejson = new FeatureJSON(geojson);
	
	/** The Constant wgs84FeatureBuilder. */
	private static final SimpleFeatureBuilder wgs84FeatureBuilder;
	static {
		SimpleFeatureTypeBuilder sftbuilder = new SimpleFeatureTypeBuilder();
		sftbuilder.setName("SDSD Feature");
		sftbuilder.setCRS(DefaultGeographicCRS.WGS84);
		sftbuilder.add("geom", Geometry.class);
		wgs84FeatureBuilder = new SimpleFeatureBuilder(sftbuilder.buildFeatureType());
	}
	
	/**
	 * Creates the feature.
	 *
	 * @param geom the geom
	 * @param id the id
	 * @return the simple feature
	 */
	public static SimpleFeature createFeature(Geometry geom, @Nullable String id) {
		wgs84FeatureBuilder.add(geom);
		return wgs84FeatureBuilder.buildFeature(id);
	}
	
	/**
	 * To geo json.
	 *
	 * @param geom the geom
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String toGeoJson(Geometry geom) throws IOException {
		StringWriter sw = new StringWriter();
		geojson.write(geom, sw);
		return sw.toString();
	}
	
	/**
	 * To geo json.
	 *
	 * @param feature the feature
	 * @return the string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String toGeoJson(SimpleFeature feature) throws IOException {
		StringWriter sw = new StringWriter();
		featurejson.writeFeature(feature, sw);
		return sw.toString();
	}
	
	/**
	 * Convex hull.
	 *
	 * @param points the points
	 * @return the geometry
	 */
	public static Geometry convexHull(Coordinate[] points) {
		return new ConvexHull(points, geofactory).getConvexHull();
	}
	
	/**
	 * Simplify.
	 *
	 * @param points the points
	 * @param distanceTolerance the distance tolerance
	 * @return the geometry
	 */
	public static Geometry simplify(Coordinate[] points, double distanceTolerance) {
		if(points.length == 1) return geofactory.createPoint(points[1]);
		Geometry simplified = DouglasPeuckerSimplifier.simplify(geofactory.createLineString(points), distanceTolerance);
		if(simplified instanceof LineString && simplified.getNumPoints() == 2) {
			if(simplified.getLength() < distanceTolerance) 
				return ((LineString)simplified).getStartPoint();
		}
		return simplified;
	}
	
	/**
	 * Read geo json.
	 *
	 * @param geoJsonGeometry the geo json geometry
	 * @return the geometry
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Geometry readGeoJson(String geoJsonGeometry) throws IOException {
		try(StringReader reader = new StringReader(geoJsonGeometry)) {
			return geojson.read(reader);
		}
	}
	
	/**
	 * Read geo json polygon.
	 *
	 * @param geoJsonGeometry the geo json geometry
	 * @return the polygon
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Polygon readGeoJsonPolygon(String geoJsonGeometry) throws IOException {
		try(StringReader reader = new StringReader(geoJsonGeometry)) {
			return geojson.readPolygon(reader);
		}
	}
	
	/**
	 * Projection.
	 *
	 * @param polygon the polygon
	 * @return the polygon
	 * @throws TransformException the transform exception
	 */
	public static Polygon projection(Polygon polygon) throws TransformException {
		try {
			Point centroid = polygon.getCentroid();
			String code = "AUTO:42001," + centroid.getX() + "," + centroid.getY();
			CoordinateReferenceSystem auto = CRS.decode(code);
			MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
			return (Polygon) JTS.transform(polygon, transform);
		} catch (FactoryException e) {
			throw new RuntimeException(e); // rethrow unchecked
		}
	}
	
	/**
	 * Calc area.
	 *
	 * @param geoJsonGeometry the geo json geometry
	 * @return the double
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static double calcArea(String geoJsonGeometry) throws IOException {
		try {
			return projection(readGeoJsonPolygon(geoJsonGeometry)).getArea();
		} catch (TransformException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Equals.
	 *
	 * @param g1 the g 1
	 * @param g2 the g 2
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static boolean equals(GeoElement g1, GeoElement g2) throws IOException {
		if(g1.getType() != ElementType.Field || g2.getType() != ElementType.Field) return false;
		if(Double.isFinite(g1.getArea()) && Double.isFinite(g2.getArea())
				&& Math.abs(g2.getArea() / g1.getArea() - 1.) > 0.1) return false;
		Polygon p1 = readGeoJsonPolygon(g1.getGeometry().toJson());
		Polygon p2 = readGeoJsonPolygon(g2.getGeometry().toJson());
		double intersectionArea = p1.intersection(p2).getArea();
		return p1.getArea() / intersectionArea < 1.1;
	}

}
