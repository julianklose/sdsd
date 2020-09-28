package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.parser.isoxml.Attribute.ByteAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DoubleAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.ULongAttr;

public abstract class Geo {

	public static class GeoException extends Exception {
		private static final long serialVersionUID = -117904243081186238L;

		public GeoException(String message) {
			super(message);
		}
	}
	
	public static interface GeoType {}

	public final IsoXmlElement element;

	public Geo(IsoXmlElement e) {
		this.element = e;
	}
	
	public IsoXmlElement getElement() {
		return element;
	}
	
	public abstract GeoType getType();
	public abstract String getDesignator();
	public abstract byte getColor();

	public abstract List<String> getErrors();

	abstract JSONArray toCoordinates() throws GeoException;
	public abstract JSONObject toGeoJson() throws GeoException;

	public static enum PointType implements GeoType {
		FLAG, OTHER, FIELD_ACCESS, STORAGE, OBSTACLE, GUID_REF_A, GUID_REF_B, GUID_REF_CENTER, GUID_POINT,
		PARTFIELD_REF_POINT, HOMEBASE;
		private static final PointType[] VALUES = { null, FLAG, OTHER, FIELD_ACCESS, STORAGE, OBSTACLE, GUID_REF_A,
				GUID_REF_B, GUID_REF_CENTER, GUID_POINT, PARTFIELD_REF_POINT, HOMEBASE };

		public static PointType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : OTHER;
		}
	}

	public static class Point extends Geo { // PNT
		public final PointType type;
		public final String designator;
		public final double north, east, up;
		public final byte color;
		public final double horizontalAccuracy, verticalAccuracy;

		Point(IsoXmlElement pnt, @Nullable ByteBuffer data) {
			super(pnt);
			this.designator = pnt.getAttribute("pointDesignator", StringAttr.class).getValue();

			EnumAttr type = pnt.getAttribute("pointType", EnumAttr.class);
			DoubleAttr north = pnt.getAttribute("pointNorth", DoubleAttr.class);
			DoubleAttr east = pnt.getAttribute("pointEast", DoubleAttr.class);
			DoubleAttr up = pnt.getAttribute("pointUp", DoubleAttr.class);
			ByteAttr color = pnt.getAttribute("pointColour", ByteAttr.class);
			DoubleAttr horizontalAccuracy = pnt.getAttribute("pointHorizontalAccuracy", DoubleAttr.class);
			DoubleAttr verticalAccuracy = pnt.getAttribute("pointVerticalAccuracy", DoubleAttr.class);

			this.type = PointType.fromNum(
					data != null && type.hasValue() && type.getStringValue().isEmpty() ? Byte.toUnsignedInt(data.get())
							: type.number());
			this.north = data != null && north.hasValue() && north.getStringValue().isEmpty()
					? BigDecimal.valueOf(data.getLong()).movePointLeft(16).doubleValue()
					: north.getValue();
			this.east = data != null && east.hasValue() && east.getStringValue().isEmpty()
					? BigDecimal.valueOf(data.getLong()).movePointLeft(16).doubleValue()
					: east.getValue();
			this.up = data != null && up.hasValue() && up.getStringValue().isEmpty() ? data.getInt()
					: up.hasValue() ? up.getValue() : Double.NaN;
			this.color = data != null && color.hasValue() && color.getStringValue().isEmpty() ? data.get()
					: color.getValue().byteValue();
			this.horizontalAccuracy = data != null && horizontalAccuracy.hasValue()
					&& horizontalAccuracy.getStringValue().isEmpty() ? Short.toUnsignedInt(data.getShort()) / 1000.
							: horizontalAccuracy.getValue();
			this.verticalAccuracy = data != null && verticalAccuracy.hasValue()
					&& verticalAccuracy.getStringValue().isEmpty() ? Short.toUnsignedInt(data.getShort()) / 1000.
							: verticalAccuracy.getValue();
		}

		public PointType getType() {
			return type;
		}

		public String getDesignator() {
			return designator;
		}

		public double getNorth() {
			return north;
		}

		public double getEast() {
			return east;
		}

		public double getUp() {
			return up;
		}

		public byte getColor() {
			return color;
		}

		public double getHorizontalAccuracy() {
			return horizontalAccuracy;
		}

		public double getVerticalAccuracy() {
			return verticalAccuracy;
		}

		public static List<Point> read(IsoXmlElement pnt) throws IllegalArgumentException, IOException {
			if (!pnt.getTag().equals("PNT"))
				throw new IllegalArgumentException("Given element is no PNT");
			if (pnt.getAttribute("filename").hasValue())
				return pnt.getRoot().getPoints(pnt);
			return Collections.singletonList(new Point(pnt, null));
		}

		public List<String> getErrors() {
			if (north == 0. && east == 0.)
				return Arrays.asList(element.prefixEnd("Position is [0,0]"));
			if (Math.abs(north) >= 90 || Math.abs(east) > 180)
				return Arrays.asList(element.prefixEnd("Coordinates out of range [-90,+90][-180,+180]"));
			return Collections.emptyList();
		}
		
		@Override
		JSONArray toCoordinates() throws GeoException {
			if (Math.abs(north) >= 90 || Math.abs(east) > 180)
				throw new GeoException("Coordinates out of range [-90,+90][-180,+180]");
			JSONArray coords = new JSONArray().put(east).put(north);
			if(Double.isFinite(up)) coords.put(up);
			return coords;
		}
		
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONObject geo = new JSONObject()
					.put("type", "Point")
					.put("coordinates", toCoordinates());
			
			JSONObject prop = new JSONObject();
			if (!designator.isEmpty())
				prop.put("designator", designator);
			prop.put("type", type.toString());
			if (color != 0)
				prop.put("color", color);
			if (horizontalAccuracy != 0)
				prop.put("horizontalAccuracy", horizontalAccuracy);
			if (verticalAccuracy != 0)
				prop.put("verticalAccuracy", verticalAccuracy);
			
			return new JSONObject()
					.put("type", "Feature")
					.put("geometry", geo)
					.put("properties", prop);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(east);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(north);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			Point other = (Point) obj;
			if (Double.doubleToLongBits(east) != Double.doubleToLongBits(other.east))
				return false;
			if (Double.doubleToLongBits(north) != Double.doubleToLongBits(other.north))
				return false;
			return true;
		}
	}

	public static enum LineStringType implements GeoType {
		POLYGON_EXTERIOR, POLYGON_INTERIOR, TRAM_LINE, SAMPLING_ROUTE, GUIDANCE_PATTERN, DRAINAGE, FENCE, FLAG,
		OBSTACLE;
		private static final LineStringType[] VALUES = { null, POLYGON_EXTERIOR, POLYGON_INTERIOR, TRAM_LINE,
				SAMPLING_ROUTE, GUIDANCE_PATTERN, DRAINAGE, FENCE, FLAG, OBSTACLE };

		public static LineStringType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : POLYGON_EXTERIOR;
		}
	}

	public static class LineString extends Geo { // LSG
		public final LineStringType type;
		public final String designator;
		public final long width, length;
		public final byte color;
		public final List<Point> points;

		public LineString(IsoXmlElement lsg) {
			super(lsg);
			if (!lsg.getTag().equals("LSG"))
				throw new IllegalArgumentException("Given element is no PNT");
			this.type = LineStringType.fromNum(lsg.getAttribute("lineStringType", EnumAttr.class).number());
			this.designator = lsg.getAttribute("lineStringDesignator", StringAttr.class).getValue();
			this.width = lsg.getAttribute("lineStringWidth", ULongAttr.class).getValue();
			this.length = lsg.getAttribute("lineStringLength", ULongAttr.class).getValue();
			this.color = lsg.getAttribute("lineStringColour", ByteAttr.class).getValue().byteValue();

			List<Point> points = new ArrayList<>();
			for (IsoXmlElement pnt : lsg.getChildren()) {
				if (!pnt.getTag().equals("PNT"))
					continue;
				try {
					points.addAll(Point.read(pnt));
				} catch (IOException e) {
				}
			}
			this.points = Collections.unmodifiableList(points);
		}

		public LineStringType getType() {
			return type;
		}

		public String getDesignator() {
			return designator;
		}

		public long getWidth() {
			return width;
		}

		public long getLength() {
			return length;
		}

		public byte getColor() {
			return color;
		}

		public List<Point> getPoints() {
			return points;
		}

		public List<String> getErrors() {
			List<String> errors = new ArrayList<>();
			for(Point pnt : points) {
				errors.addAll(pnt.getErrors());
			}
			if(new HashSet<>(points).size() < 2)
				errors.add("LineString must have at least 2 distinct points");
			return errors;
		}
		
		@Override
		JSONArray toCoordinates() throws GeoException {
			JSONArray coords = new JSONArray();
			for (int i = 0; i < points.size(); ++i) {
				if(i <= 0 || !points.get(i).equals(points.get(i-1)))
					coords.put(points.get(i).toCoordinates());
			}
			return coords;
		}
		
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONArray coords = toCoordinates();
			if(coords.length() < 2)
				throw new GeoException("LineString must have at least 2 distinct points");
			
			JSONObject geo = coords.length() > 1 
					? new JSONObject()
							.put("type", "LineString")
							.put("coordinates", coords)
					: new JSONObject()
							.put("type", "Point")
							.put("coordinates", coords.getJSONArray(0));
			
			JSONObject prop = new JSONObject();
			if (!designator.isEmpty())
				prop.put("designator", designator);
			prop.put("type", type.toString());
			if (width != 0)
				prop.put("width", width);
			if (length != 0)
				prop.put("length", length);
			if (color != 0)
				prop.put("color", color);
			
			return new JSONObject()
					.put("type", "Feature")
					.put("geometry", geo)
					.put("properties", prop);
		}
	}

	public static enum PolygonType implements GeoType {
		PARTFIELD_BOUNDARY, TREATMENT_ZONE, WATER_SURFACE, BUILDING, ROAD, OBSTACLE, FLAG, OTHER, MAINFIELD, HEADLAND,
		BUFFER_ZONE, WINDBREAK;
		private static final PolygonType[] VALUES = { null, PARTFIELD_BOUNDARY, TREATMENT_ZONE, WATER_SURFACE, BUILDING,
				ROAD, OBSTACLE, FLAG, OTHER, MAINFIELD, HEADLAND, BUFFER_ZONE, WINDBREAK };

		public static PolygonType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : OTHER;
		}
	}

	public static class Polygon extends Geo { // PLN
		public final PolygonType type;
		public final String designator;
		public final long area;
		public final byte color;
		public final List<LineString> rings;

		public Polygon(IsoXmlElement pln) {
			super(pln);
			if (!pln.getTag().equals("PLN"))
				throw new IllegalArgumentException("Given element is no PNT");
			this.type = PolygonType.fromNum(pln.getAttribute("polygonType", EnumAttr.class).number());
			this.designator = pln.getAttribute("polygonDesignator", StringAttr.class).getValue();
			this.area = pln.getAttribute("polygonArea", ULongAttr.class).getValue();
			this.color = pln.getAttribute("polygonColour", ByteAttr.class).getValue().byteValue();

			List<LineString> rings = new ArrayList<>();
			for (IsoXmlElement lsg : pln.getChildren()) {
				if (!lsg.getTag().equals("LSG"))
					continue;
				rings.add(new LineString(lsg));
			}
			this.rings = Collections.unmodifiableList(rings);
		}

		public PolygonType getType() {
			return type;
		}

		public String getDesignator() {
			return designator;
		}

		public long getArea() {
			return area;
		}

		public byte getColor() {
			return color;
		}

		public List<LineString> getRings() {
			return rings;
		}

		public List<String> getErrors() {
			List<String> errors = new ArrayList<>();
			if (rings.isEmpty())
				errors.add(element.prefixEnd("Polygon has no rings"));
			int outerrings = 0;
			for (LineString lsg : rings) {
				errors.addAll(lsg.getErrors());
				if (lsg.type == LineStringType.POLYGON_EXTERIOR)
					++outerrings;
				if (lsg.points.size() > 1) {
					if (!lsg.points.get(0).equals(lsg.points.get(lsg.points.size() - 1)))
						errors.add(lsg.element.prefixEnd("Polygon ring is not closed"));
					if (new HashSet<>(lsg.points).size() < 3)
						errors.add(lsg.element.prefixEnd("Polygon ring must have at least 3 distinct points"));
				}
			}
			if (outerrings > 1)
				errors.add(element.prefixEnd("Polygon has more than one POLYGON_EXTERIOR ring"));
			return errors;
		}

		private static JSONArray closedRing(LineString ring) throws GeoException {
			JSONArray coords = ring.toCoordinates();
			if(!ring.points.get(0).equals(ring.points.get(ring.points.size() - 1)))
				coords.put(ring.points.get(0).toCoordinates());
			if(coords.length() < 4)
				throw new GeoException("Polygon ring must have at least 3 distinct points");
			return coords;
		}
		
		@Override
		JSONArray toCoordinates() throws GeoException {
			if (rings.isEmpty())
				throw new GeoException("Polygon has no rings");
			int outerring = -1;
			for (int i = 0; i < rings.size(); ++i) {
				if (rings.get(i).type == LineStringType.POLYGON_EXTERIOR) {
					if (outerring >= 0)
						throw new GeoException("Polygon has more than one POLYGON_EXTERIOR ring");
					outerring = i;
				}
			}
			if (outerring < 0) outerring = 0;
			
			JSONArray coords = new JSONArray();
			coords.put(closedRing(rings.get(outerring)));
			for (int i = 0; i < rings.size(); ++i) {
				if (i != outerring)
					coords.put(closedRing(rings.get(i)));
			}
			return coords;
		}
		
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONObject geo = new JSONObject()
					.put("type", "Polygon")
					.put("coordinates", toCoordinates());
			
			JSONObject prop = new JSONObject();
			if (!designator.isEmpty())
				prop.put("designator", designator);
			prop.put("type", type.toString());
			if (area != 0)
				prop.put("area", area);
			if (color != 0)
				prop.put("color", color);
			
			return new JSONObject()
					.put("type", "Feature")
					.put("geometry", geo)
					.put("properties", prop);
		}
	}

	public static List<Point> readBinaryPoints(IsoXmlElement pnt, byte[] content) {
		ByteBuffer data = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		List<Point> points = new ArrayList<>();
		try {
			while (data.hasRemaining()) {
				points.add(new Point(pnt, data));
			}
		} catch (BufferUnderflowException e) {
		}
		return points;
	}

}
