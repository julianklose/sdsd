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
	
	public static final String ID = "_id", URI = "uri", TLG = "tlg", USER = "user", FILE = "file", 
			TYPE = "type", LABEL = "label", AREA = "area",
			FEATURE = "feature", GEOMETRY = "geometry", MINMAX = "minmax";
	public static final String GEOFIELD = FEATURE + '.' + GEOMETRY;

	public static Bson filter(User user) {
		return filterUser(user.getName());
	}

	public static Bson filter(User user, ObjectId id) {
		return Filters.and(filter(user), Filters.eq(id));
	}
	
	public static Bson filterUser(String user) {
		return Filters.eq(USER, user);
	}
	
	public static Bson filterFile(ObjectId fileid) {
		return Filters.eq(FILE, fileid);
	}
	
	public static Bson filterUri(String uri) {
		return Filters.eq(URI, uri);
	}
	
	public static Bson filterTlg(String tlgName) {
		return Filters.eq(TLG, tlgName);
	}
	
	public static Bson filterType(ElementType type) {
		return Filters.eq(TYPE, type.name());
	}
	
	public static Bson filterType(GeoType type) {
		return Filters.eq(FEATURE + '.' + GEOMETRY + ".type", type.name());
	}
	
	public static Bson filter(File file, @Nullable String uri, ElementType type) {
		Bson filter = Filters.and(filterFile(file.getId()), filterType(type));
		if(uri != null) filter = Filters.and(filter, filterUri(uri));
		return filter;
	}
	
	public static Bson filterTlg(ElementKey tlgKey) {
		return Filters.and(filterUser(tlgKey.user), filterFile(File.toID(tlgKey.file)), filterTlg(tlgKey.name));
	}
	
	public static Bson filterWithin(User user, Bson geoJsonGeometry) {
		return Filters.geoWithin(GEOFIELD, geoJsonGeometry);
	}
	public static Bson filterWithin(User user, JSONObject geoJsonGeometry) {
		return Filters.geoWithin(GEOFIELD, Document.parse(geoJsonGeometry.toString()));
	}
	public static Bson filterWithinSphere(User user, double north, double east, double radius) {
		return Filters.geoWithinCenterSphere(GEOFIELD, east, north, radius/6378137.);
	}
	
	public static Bson filterIntersects(User user, Bson geoJsonGeometry) {
		return Filters.geoIntersects(GEOFIELD, geoJsonGeometry);
	}
	public static Bson filterIntersects(User user, JSONObject geoJsonGeometry) {
		return Filters.geoIntersects(GEOFIELD, Document.parse(geoJsonGeometry.toString()));
	}
	public static Bson filterIntersects(User user, double north, double east) {
		return Filters.geoIntersects(GEOFIELD, new Point(new Position(east, north)));
	}
	
	public static Bson filterNear(User user, Bson geoJsonGeometry, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, geoJsonGeometry, maxDistance, minDistance);
	}
	public static Bson filterNear(User user, JSONObject geoJsonGeometry, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, Document.parse(geoJsonGeometry.toString()), maxDistance, minDistance);
	}
	public static Bson filterNear(User user, double north, double east, double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, east, north, maxDistance/6378137., minDistance/6378137.);
	}
	
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
	
	public static Document createTlg(File file, String uri, String tlgName, String geoJsonFeature) {
		return create(file, uri, ElementType.TimeLog, geoJsonFeature, Double.NaN, tlgName + " Boundary", null)
				.append(TLG, tlgName);
	}
	
	public static Bson update(String geoJsonFeature, double area) {
		return Updates.combine(
				Updates.set(FEATURE, Document.parse(geoJsonFeature)),
				Double.isFinite(area) ? Updates.set(AREA, area) : Updates.unset(AREA));
	}
	public static Bson update(String geoJsonFeature, double area, String label) {
		return Updates.combine(
				Updates.set(FEATURE, Document.parse(geoJsonFeature)), 
				Updates.set(LABEL, label),
				Double.isFinite(area) ? Updates.set(AREA, area) : Updates.unset(AREA));
	}
	
	private final ObjectId id;
	private final Optional<String> tlg;
	private final String user, uri;
	private final ObjectId file;
	private final ElementType type;
	private final Document feature;
	private final String label;
	private final double area;
	@CheckForNull
	private final MinMaxValues values;

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
	
	public Bson filter() {
		return Filters.eq(id);
	}
	
	public ObjectId getId() {
		return id;
	}

	public String getUri() {
		return uri;
	}
	
	public boolean isTlg() {
		return type == ElementType.TimeLog;
	}
	
	public Optional<String> getTlgName() {
		return tlg;
	}
	
	public Optional<ElementKey> getTlgKey() {
		return tlg.isPresent() ? Optional.of(new ElementKey(user, File.toURI(file), tlg.get())) : Optional.empty();
	}

	public String getUser() {
		return user;
	}

	public ObjectId getFile() {
		return file;
	}
	
	public static ElementType type(@Nullable String type) throws IllegalArgumentException {
		if(type == null) return ElementType.Other;
		return ElementType.valueOf(type);
	}
	
	public static enum ElementType {
		Other, TimeLog, Field, TreatmentZone, GuidancePattern, FieldAccess
	}
	
	public ElementType getType() {
		return type;
	}
	
	public String getLabel() {
		return label;
	}
	
	public double getArea() {
		return area;
	}
	
	@CheckForNull
	public MinMaxValues getValues() {
		return values;
	}
	
	public String getFullLabel() {
		if(isTlg()) return String.format("Simplified %s %s", type.name(), label != null ? label : getTlgName());
		else return String.format("%s %s %s", type.name(), getGeoType().name(), label != null ? label : "");
	}

	public Document getFeature() {
		return feature;
	}
	
	public JSONObject getFeatureJson() {
		return new JSONObject(feature.toJson());
	}
	
	public Document getGeometry() {
		return feature.get(GEOMETRY, Document.class);
	}
	
	public JSONObject getGeometryJson() {
		return new JSONObject(getGeometry().toJson());
	}
	
	public static enum GeoType {
		Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection
	}
	public GeoType getGeoType() {
		return GeoType.valueOf(feature.getEmbedded(Arrays.asList("geometry", "type"), String.class));
	}
	
	public Bson filterWithin() {
		return Filters.geoWithin(GEOFIELD, getGeometry());
	}
	
	public Bson filterIntersects() {
		return Filters.geoIntersects(GEOFIELD, getGeometry());
	}
	
	public Bson filterNear(double maxDistance, double minDistance) {
		return Filters.nearSphere(GEOFIELD, getGeometry(), maxDistance, minDistance);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

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
	
	public static class MinMax {
		public static final String MIN = "min", MAX = "max";
		private double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		
		public MinMax() {
			this.min = Double.MAX_VALUE;
			this.max = Double.MIN_VALUE;
		}
		
		@CheckForNull
		static MinMax read(Document doc) {
			return doc != null ? new MinMax(doc) : null;
		}
		private MinMax(Document doc) {
			this.min = doc.getDouble(MIN);
			this.max = doc.getDouble(MAX);
		}
		
		private Document toDoc() {
			return new Document()
					.append(MIN, min)
					.append(MAX, max);
		}
		
		public void addValue(double val) {
			if(val < min) min = val;
			if(val > max) max = val;
		}
		
		public double rel(double val) {
			return (val - min) / (max - min);
		}
		
		public boolean isRange() {
			return min < max;
		}
	}
	
	public static class MinMaxValues implements Iterable<Map.Entry<String, MinMax>> {
		private final Document doc;
		
		@CheckForNull
		static MinMaxValues read(Document doc) {
			return doc != null ? new MinMaxValues(doc) : null;
		}
		private MinMaxValues(Document doc) {
			this.doc = doc;
		}
		
		public MinMaxValues() {
			this.doc = new Document();
		}
		
		private Document doc() {
			return this.doc;
		}

		public int size() {
			return doc.size();
		}

		public boolean isEmpty() {
			return doc.isEmpty();
		}

		public MinMax get(String key) {
			return MinMax.read(doc.get(key, Document.class));
		}
		
		public void put(String key, MinMax minmax) {
			if(minmax.isRange())
				doc.put(key, minmax.toDoc());
		}

		public Set<String> keySet() {
			return doc.keySet();
		}

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
		
		public JSONObject toJson() {
			return new JSONObject(doc.toJson());
		}

	}

}
