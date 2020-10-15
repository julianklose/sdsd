package de.sdsd.projekt.prototype.data;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import de.sdsd.projekt.prototype.applogic.TableFunctions.ElementKey;

/**
 * Represents a geo element, stored in MongoDB.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class GeoElement {
	
	/** The Constant MINMAX. */
	public static final String ID = "_id", URI = "uri", TLG = "tlg", USER = "user", FILE = "file", 
			TYPE = "type", LABEL = "label", AREA = "area",
			FEATURE = "feature", GEOMETRY = "geometry", MINMAX = "minmax";
	
	/** The Constant GEOFIELD. */
	public static final String GEOFIELD = FEATURE + '.' + GEOMETRY;

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filter(User user) {
		return filterUser(user.getName());
	}

	/**
	 * Filter.
	 *
	 * @param user the user
	 * @param id the id
	 * @return the bson
	 */
	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	/**
	 * Filter user.
	 *
	 * @param user the user
	 * @return the bson
	 */
	public static Bson filterUser(String user) {
		return Filters.eq(USER, user);
	}
	
	/**
	 * Filter file.
	 *
	 * @param fileid the fileid
	 * @return the bson
	 */
	public static Bson filterFile(ObjectId fileid) {
		return Filters.eq(FILE, fileid);
	}
	
	/**
	 * Filter uri.
	 *
	 * @param uri the uri
	 * @return the bson
	 */
	public static Bson filterUri(String uri) {
		return Filters.eq(URI, uri);
	}
	
	/**
	 * Filter tlg.
	 *
	 * @param tlgName the tlg name
	 * @return the bson
	 */
	public static Bson filterTlg(String tlgName) {
		return Filters.eq(TLG, tlgName);
	}
	
	/**
	 * Filter type.
	 *
	 * @param type the type
	 * @return the bson
	 */
	public static Bson filterType(ElementType type) {
		return Filters.eq(TYPE, type.name());
	}
	
	/**
	 * Filter type.
	 *
	 * @param type the type
	 * @return the bson
	 */
	public static Bson filterType(GeoType type) {
		return Filters.eq(FEATURE + '.' + GEOMETRY + ".type", type.name());
	}
	
	/**
	 * Filter.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param type the type
	 * @return the bson
	 */
	public static Bson filter(File file, @Nullable String uri, ElementType type) {
		Bson filter = Filters.and(filterFile(file.getId()), filterType(type));
		if(uri != null) filter = Filters.and(filter, filterUri(uri));
		return filter;
	}
	
	/**
	 * Filter tlg.
	 *
	 * @param tlgKey the tlg key
	 * @return the bson
	 */
	public static Bson filterTlg(ElementKey tlgKey) {
		return Filters.and(filterUser(tlgKey.user), filterFile(File.toID(tlgKey.file)), filterTlg(tlgKey.name));
	}
	
	/**
	 * Filter within.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @return the bson
	 */
	public static Bson filterWithin(User user, Bson geoJsonGeometry) {
		return Filters.geoWithin(GEOFIELD, geoJsonGeometry);
	}
	
	/**
	 * Filter within.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @return the bson
	 */
	public static Bson filterWithin(User user, JSONObject geoJsonGeometry) {
		return Filters.geoWithin(GEOFIELD, Document.parse(geoJsonGeometry.toString()));
	}
	
	/**
	 * Filter within sphere.
	 *
	 * @param user the user
	 * @param north the north
	 * @param east the east
	 * @param radius the radius
	 * @return the bson
	 */
	public static Bson filterWithinSphere(User user, double north, double east, double radius) {
		return Filters.geoWithinCenterSphere(GEOFIELD, east, north, radius/6378137.);
	}
	
	/**
	 * Filter intersects.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @return the bson
	 */
	public static Bson filterIntersects(User user, Bson geoJsonGeometry) {
		return Filters.geoIntersects(GEOFIELD, geoJsonGeometry);
	}
	
	/**
	 * Filter intersects.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @return the bson
	 */
	public static Bson filterIntersects(User user, JSONObject geoJsonGeometry) {
		return Filters.geoIntersects(GEOFIELD, Document.parse(geoJsonGeometry.toString()));
	}
	
	/**
	 * Filter intersects.
	 *
	 * @param user the user
	 * @param north the north
	 * @param east the east
	 * @return the bson
	 */
	public static Bson filterIntersects(User user, double north, double east) {
		return Filters.geoIntersects(GEOFIELD, new Point(new Position(east, north)));
	}
	
	/**
	 * Filter near.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @param maxDistance the max distance
	 * @param minDistance the min distance
	 * @return the bson
	 */
	public static Bson filterNear(User user, Bson geoJsonGeometry, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, geoJsonGeometry, maxDistance, minDistance);
	}
	
	/**
	 * Filter near.
	 *
	 * @param user the user
	 * @param geoJsonGeometry the geo json geometry
	 * @param maxDistance the max distance
	 * @param minDistance the min distance
	 * @return the bson
	 */
	public static Bson filterNear(User user, JSONObject geoJsonGeometry, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, Document.parse(geoJsonGeometry.toString()), maxDistance, minDistance);
	}
	
	/**
	 * Filter near.
	 *
	 * @param user the user
	 * @param north the north
	 * @param east the east
	 * @param maxDistance the max distance
	 * @param minDistance the min distance
	 * @return the bson
	 */
	public static Bson filterNear(User user, double north, double east, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, east, north, maxDistance/6378137., minDistance/6378137.);
	}
	
	/**
	 * Creates the.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param type the type
	 * @param geoJsonFeature the geo json feature
	 * @param area the area
	 * @param label the label
	 * @param values the values
	 * @return the document
	 */
	public static Document create(File file, String uri, ElementType type, String geoJsonFeature, 
			double area, String label, @Nullable MinMaxValues values) {
		return new Document()
				.append(ID, new ObjectId())
				.append(URI, uri)
				.append(USER, file.getUser())
				.append(FILE, file.getId())
				.append(TYPE, type.name())
				.append(FEATURE, Document.parse(geoJsonFeature))
				.append(LABEL, label)
				.append(AREA, Double.isFinite(area) ? area : null)
				.append(MINMAX, values != null ? values.doc() : null);
	}
	
	/**
	 * Creates the tlg.
	 *
	 * @param file the file
	 * @param uri the uri
	 * @param tlgName the tlg name
	 * @param geoJsonFeature the geo json feature
	 * @return the document
	 */
	public static Document createTlg(File file, String uri, String tlgName, String geoJsonFeature) {
		return create(file, uri, ElementType.TimeLog, geoJsonFeature, Double.NaN, tlgName + " Boundary", null)
				.append(TLG, tlgName);
	}
	
	/**
	 * Update.
	 *
	 * @param geoJsonFeature the geo json feature
	 * @param area the area
	 * @return the bson
	 */
	public static Bson update(String geoJsonFeature, double area) {
		return Updates.combine(
				Updates.set(FEATURE, Document.parse(geoJsonFeature)),
				Double.isFinite(area) ? Updates.set(AREA, area) : Updates.unset(AREA));
	}
	
	/**
	 * Update.
	 *
	 * @param geoJsonFeature the geo json feature
	 * @param area the area
	 * @param label the label
	 * @return the bson
	 */
	public static Bson update(String geoJsonFeature, double area, String label) {
		return Updates.combine(
				Updates.set(FEATURE, Document.parse(geoJsonFeature)), 
				Updates.set(LABEL, label),
				Double.isFinite(area) ? Updates.set(AREA, area) : Updates.unset(AREA));
	}
	
	/** The id. */
	private final ObjectId id;
	
	/** The tlg. */
	private final Optional<String> tlg;
	
	/** The uri. */
	private final String user, uri;
	
	/** The file. */
	private final ObjectId file;
	
	/** The type. */
	private final ElementType type;
	
	/** The feature. */
	private final Document feature;
	
	/** The label. */
	private final String label;
	
	/** The area. */
	private final double area;
	
	/** The values. */
	@CheckForNull
	private final MinMaxValues values;

	/**
	 * Instantiates a new geo element.
	 *
	 * @param doc the doc
	 */
	public GeoElement(Document doc) {
		this.id = doc.getObjectId(ID);
		this.uri = doc.getString(URI);
		this.tlg = Optional.ofNullable(doc.getString(TLG));
		this.user = doc.getString(USER);
		this.file = doc.getObjectId(FILE);
		this.type = type(doc.getString(TYPE));
		this.feature = doc.get(FEATURE, Document.class);
		this.label = doc.getString(LABEL);
		Double area = doc.getDouble(AREA);
		this.area = area != null ? area : Double.NaN;
		this.values = MinMaxValues.read(doc.get(MINMAX, Document.class));
	}
	
	/**
	 * Filter.
	 *
	 * @return the bson
	 */
	public Bson filter() {
		return Filters.eq(id);
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public ObjectId getId() {
		return id;
	}

	/**
	 * Gets the uri.
	 *
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * Checks if is tlg.
	 *
	 * @return true, if is tlg
	 */
	public boolean isTlg() {
		return type == ElementType.TimeLog;
	}
	
	/**
	 * Gets the tlg name.
	 *
	 * @return the tlg name
	 */
	public Optional<String> getTlgName() {
		return tlg;
	}
	
	/**
	 * Gets the tlg key.
	 *
	 * @return the tlg key
	 */
	public Optional<ElementKey> getTlgKey() {
		return tlg.isPresent() ? Optional.of(new ElementKey(user, File.toURI(file), tlg.get())) : Optional.empty();
	}

	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Gets the file.
	 *
	 * @return the file
	 */
	public ObjectId getFile() {
		return file;
	}
	
	/**
	 * Type.
	 *
	 * @param type the type
	 * @return the element type
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static ElementType type(@Nullable String type) throws IllegalArgumentException {
		if(type == null) return ElementType.Other;
		return ElementType.valueOf(type);
	}
	
	/**
	 * The Enum ElementType.
	 */
	public static enum ElementType {
		
		/** The Other. */
		Other, 
 /** The Time log. */
 TimeLog, 
 /** The Field. */
 Field, 
 /** The Treatment zone. */
 TreatmentZone, 
 /** The Guidance pattern. */
 GuidancePattern, 
 /** The Field access. */
 FieldAccess
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public ElementType getType() {
		return type;
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Gets the area.
	 *
	 * @return the area
	 */
	public double getArea() {
		return area;
	}
	
	/**
	 * Gets the values.
	 *
	 * @return the values
	 */
	@CheckForNull
	public MinMaxValues getValues() {
		return values;
	}
	
	/**
	 * Gets the full label.
	 *
	 * @return the full label
	 */
	public String getFullLabel() {
		if(isTlg()) return String.format("Simplified %s %s", type.name(), label != null ? label : getTlgName());
		else return String.format("%s %s %s", type.name(), getGeoType().name(), label != null ? label : "");
	}

	/**
	 * Gets the feature.
	 *
	 * @return the feature
	 */
	public Document getFeature() {
		return feature;
	}
	
	/**
	 * Gets the feature json.
	 *
	 * @return the feature json
	 */
	public JSONObject getFeatureJson() {
		return new JSONObject(feature.toJson());
	}
	
	/**
	 * Gets the geometry.
	 *
	 * @return the geometry
	 */
	public Document getGeometry() {
		return feature.get(GEOMETRY, Document.class);
	}
	
	/**
	 * Gets the geometry json.
	 *
	 * @return the geometry json
	 */
	public JSONObject getGeometryJson() {
		return new JSONObject(getGeometry().toJson());
	}
	
	/**
	 * The Enum GeoType.
	 */
	public static enum GeoType {
		
		/** The Point. */
		Point, 
 /** The Line string. */
 LineString, 
 /** The Polygon. */
 Polygon, 
 /** The Multi point. */
 MultiPoint, 
 /** The Multi line string. */
 MultiLineString, 
 /** The Multi polygon. */
 MultiPolygon, 
 /** The Geometry collection. */
 GeometryCollection
	}
	
	/**
	 * Gets the geo type.
	 *
	 * @return the geo type
	 */
	public GeoType getGeoType() {
		return GeoType.valueOf(feature.getEmbedded(Arrays.asList("geometry", "type"), String.class));
	}
	
	/**
	 * Filter within.
	 *
	 * @return the bson
	 */
	public Bson filterWithin() {
		return Filters.geoWithin(GEOFIELD, getGeometry());
	}
	
	/**
	 * Filter intersects.
	 *
	 * @return the bson
	 */
	public Bson filterIntersects() {
		return Filters.geoIntersects(GEOFIELD, getGeometry());
	}
	
	/**
	 * Filter near.
	 *
	 * @param maxDistance the max distance
	 * @param minDistance the min distance
	 * @return the bson
	 */
	public Bson filterNear(double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, getGeometry(), maxDistance, minDistance);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeoElement other = (GeoElement) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	/**
	 * The Class MinMax.
	 */
	public static class MinMax {
		
		/** The Constant MAX. */
		public static final String MIN = "min", MAX = "max";
		
		/** The max. */
		private double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		
		/**
		 * Instantiates a new min max.
		 */
		public MinMax() {
			this.min = Double.MAX_VALUE;
			this.max = Double.MIN_VALUE;
		}
		
		/**
		 * Read.
		 *
		 * @param doc the doc
		 * @return the min max
		 */
		@CheckForNull
		static MinMax read(Document doc) {
			return doc != null ? new MinMax(doc) : null;
		}
		
		/**
		 * Instantiates a new min max.
		 *
		 * @param doc the doc
		 */
		private MinMax(Document doc) {
			this.min = doc.getDouble(MIN);
			this.max = doc.getDouble(MAX);
		}
		
		/**
		 * To doc.
		 *
		 * @return the document
		 */
		private Document toDoc() {
			return new Document()
					.append(MIN, min)
					.append(MAX, max);
		}
		
		/**
		 * Adds the value.
		 *
		 * @param val the val
		 */
		public void addValue(double val) {
			if(val < min) min = val;
			if(val > max) max = val;
		}
		
		/**
		 * Rel.
		 *
		 * @param val the val
		 * @return the double
		 */
		public double rel(double val) {
			return (val - min) / (max - min);
		}
		
		/**
		 * Checks if is range.
		 *
		 * @return true, if is range
		 */
		public boolean isRange() {
			return min < max;
		}
	}
	
	/**
	 * The Class MinMaxValues.
	 */
	public static class MinMaxValues implements Iterable<Map.Entry<String, MinMax>> {
		
		/** The doc. */
		private final Document doc;
		
		/**
		 * Read.
		 *
		 * @param doc the doc
		 * @return the min max values
		 */
		@CheckForNull
		static MinMaxValues read(Document doc) {
			return doc != null ? new MinMaxValues(doc) : null;
		}
		
		/**
		 * Instantiates a new min max values.
		 *
		 * @param doc the doc
		 */
		private MinMaxValues(Document doc) {
			this.doc = doc;
		}
		
		/**
		 * Instantiates a new min max values.
		 */
		public MinMaxValues() {
			this.doc = new Document();
		}
		
		/**
		 * Doc.
		 *
		 * @return the document
		 */
		private Document doc() {
			return this.doc;
		}

		/**
		 * Size.
		 *
		 * @return the int
		 */
		public int size() {
			return doc.size();
		}

		/**
		 * Checks if is empty.
		 *
		 * @return true, if is empty
		 */
		public boolean isEmpty() {
			return doc.isEmpty();
		}

		/**
		 * Gets the.
		 *
		 * @param key the key
		 * @return the min max
		 */
		public MinMax get(String key) {
			return MinMax.read(doc.get(key, Document.class));
		}
		
		/**
		 * Put.
		 *
		 * @param key the key
		 * @param minmax the minmax
		 */
		public void put(String key, MinMax minmax) {
			if(minmax.isRange())
				doc.put(key, minmax.toDoc());
		}

		/**
		 * Key set.
		 *
		 * @return the sets the
		 */
		public Set<String> keySet() {
			return doc.keySet();
		}

		/**
		 * Iterator.
		 *
		 * @return the iterator
		 */
		@Override
		public Iterator<Map.Entry<String, MinMax>> iterator() {
			return new Iterator<Map.Entry<String,MinMax>>() {
				private Iterator<Map.Entry<String, Object>> it = doc.entrySet().iterator();
				
				@Override
				public Map.Entry<String, MinMax> next() {
					Map.Entry<String, Object> e = it.next();
					return new SimpleImmutableEntry<>(e.getKey(), MinMax.read((Document)e.getValue()));
				}
				
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}
			};
		}
		
		/**
		 * To json.
		 *
		 * @return the JSON object
		 */
		public JSONObject toJson() {
			return new JSONObject(doc.toJson());
		}

	}

}
