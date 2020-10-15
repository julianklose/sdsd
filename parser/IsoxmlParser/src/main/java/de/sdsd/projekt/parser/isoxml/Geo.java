package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.parser.isoxml.Attribute.ByteAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DoubleAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.ULongAttr;

/**
 * The Class Geo.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public abstract class Geo {

	/**
	 * The Class GeoException.
	 */
	public static class GeoException extends Exception {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -117904243081186238L;

		/**
		 * Instantiates a new geo exception.
		 *
		 * @param message the message
		 */
		public GeoException(String message) {
			super(message);
		}
	}

	/**
	 * The Interface GeoType.
	 */
	public static interface GeoType {
	}

	/** The element. */
	public final IsoXmlElement element;

	/**
	 * Instantiates a new geo.
	 *
	 * @param e the e
	 */
	public Geo(IsoXmlElement e) {
		this.element = e;
	}

	/**
	 * Gets the element.
	 *
	 * @return the element
	 */
	public IsoXmlElement getElement() {
		return element;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public abstract GeoType getType();

	/**
	 * Gets the designator.
	 *
	 * @return the designator
	 */
	public abstract String getDesignator();

	/**
	 * Gets the color.
	 *
	 * @return the color
	 */
	public abstract byte getColor();

	/**
	 * Gets the errors.
	 *
	 * @return the errors
	 */
	public abstract Validation getErrors();

	/**
	 * To coordinates.
	 *
	 * @return the JSON array
	 * @throws GeoException the geo exception
	 */
	abstract JSONArray toCoordinates() throws GeoException;

	/**
	 * To geo json.
	 *
	 * @return the JSON object
	 * @throws GeoException the geo exception
	 */
	public abstract JSONObject toGeoJson() throws GeoException;

	/**
	 * The Enum PointType.
	 */
	public static enum PointType implements GeoType {

		/** The flag. */
		FLAG,
		/** The other. */
		OTHER,
		/** The field access. */
		FIELD_ACCESS,
		/** The storage. */
		STORAGE,
		/** The obstacle. */
		OBSTACLE,
		/** The guid ref a. */
		GUID_REF_A,
		/** The guid ref b. */
		GUID_REF_B,
		/** The guid ref center. */
		GUID_REF_CENTER,
		/** The guid point. */
		GUID_POINT,

		/** The partfield ref point. */
		PARTFIELD_REF_POINT,
		/** The homebase. */
		HOMEBASE;

		/** The Constant VALUES. */
		private static final PointType[] VALUES = { null, FLAG, OTHER, FIELD_ACCESS, STORAGE, OBSTACLE, GUID_REF_A,
				GUID_REF_B, GUID_REF_CENTER, GUID_POINT, PARTFIELD_REF_POINT, HOMEBASE };

		/**
		 * From num.
		 *
		 * @param num the num
		 * @return the point type
		 */
		public static PointType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : OTHER;
		}
	}

	/**
	 * The Class Point.
	 */
	public static class Point extends Geo {
		/** The type. */
		// PNT
		public final PointType type;

		/** The designator. */
		public final String designator;

		/** The up. */
		public final double north, east, up;

		/** The color. */
		public final byte color;

		/** The vertical accuracy. */
		public final double horizontalAccuracy, verticalAccuracy;

		/**
		 * Instantiates a new point.
		 *
		 * @param pnt  the pnt
		 * @param data the data
		 */
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

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		@Override
		public PointType getType() {
			return type;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		@Override
		public String getDesignator() {
			return designator;
		}

		/**
		 * Gets the north.
		 *
		 * @return the north
		 */
		public double getNorth() {
			return north;
		}

		/**
		 * Gets the east.
		 *
		 * @return the east
		 */
		public double getEast() {
			return east;
		}

		/**
		 * Gets the up.
		 *
		 * @return the up
		 */
		public double getUp() {
			return up;
		}

		/**
		 * Gets the color.
		 *
		 * @return the color
		 */
		@Override
		public byte getColor() {
			return color;
		}

		/**
		 * Gets the horizontal accuracy.
		 *
		 * @return the horizontal accuracy
		 */
		public double getHorizontalAccuracy() {
			return horizontalAccuracy;
		}

		/**
		 * Gets the vertical accuracy.
		 *
		 * @return the vertical accuracy
		 */
		public double getVerticalAccuracy() {
			return verticalAccuracy;
		}

		/**
		 * Read.
		 *
		 * @param pnt the pnt
		 * @return the list
		 * @throws IllegalArgumentException the illegal argument exception
		 * @throws IOException              Signals that an I/O exception has occurred.
		 */
		public static List<Point> read(IsoXmlElement pnt) throws IllegalArgumentException, IOException {
			if (!pnt.getTag().equals("PNT"))
				throw new IllegalArgumentException("Given element is no PNT");
			if (pnt.getAttribute("filename").hasValue())
				return pnt.getRoot().getPoints(pnt);
			return Collections.singletonList(new Point(pnt, null));
		}

		/**
		 * Gets the errors.
		 *
		 * @return the errors
		 */
		@Override
		public Validation getErrors() {
			Validation errors = new Validation();
			if (north == 0. && east == 0.)
				errors.warn(element.prefixEnd("Position is [0,0]"));
			else if (Math.abs(north) >= 90 || Math.abs(east) > 180)
				errors.error(element.prefixEnd("Coordinates out of range [-90,+90][-180,+180]"));
			return errors;
		}

		/**
		 * To coordinates.
		 *
		 * @return the JSON array
		 * @throws GeoException the geo exception
		 */
		@Override
		JSONArray toCoordinates() throws GeoException {
			if (Math.abs(north) >= 90 || Math.abs(east) > 180)
				throw new GeoException("Coordinates out of range [-90,+90][-180,+180]");
			JSONArray coords = new JSONArray().put(east).put(north);
			if (Double.isFinite(up))
				coords.put(up);
			return coords;
		}

		/**
		 * To geo json.
		 *
		 * @return the JSON object
		 * @throws GeoException the geo exception
		 */
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONObject geo = new JSONObject().put("type", "Point").put("coordinates", toCoordinates());

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

			return new JSONObject().put("type", "Feature").put("geometry", geo).put("properties", prop);
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
			long temp;
			temp = Double.doubleToLongBits(east);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(north);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			Point other = (Point) obj;
			if (Double.doubleToLongBits(east) != Double.doubleToLongBits(other.east))
				return false;
			if (Double.doubleToLongBits(north) != Double.doubleToLongBits(other.north))
				return false;
			return true;
		}
	}

	/**
	 * The Enum LineStringType.
	 */
	public static enum LineStringType implements GeoType {

		/** The polygon exterior. */
		POLYGON_EXTERIOR,
		/** The polygon interior. */
		POLYGON_INTERIOR,
		/** The tram line. */
		TRAM_LINE,
		/** The sampling route. */
		SAMPLING_ROUTE,
		/** The guidance pattern. */
		GUIDANCE_PATTERN,
		/** The drainage. */
		DRAINAGE,
		/** The fence. */
		FENCE,
		/** The flag. */
		FLAG,

		/** The obstacle. */
		OBSTACLE;

		/** The Constant VALUES. */
		private static final LineStringType[] VALUES = { null, POLYGON_EXTERIOR, POLYGON_INTERIOR, TRAM_LINE,
				SAMPLING_ROUTE, GUIDANCE_PATTERN, DRAINAGE, FENCE, FLAG, OBSTACLE };

		/**
		 * From num.
		 *
		 * @param num the num
		 * @return the line string type
		 */
		public static LineStringType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : POLYGON_EXTERIOR;
		}
	}

	/**
	 * The Class LineString.
	 */
	public static class LineString extends Geo {
		/** The type. */
		// LSG
		public final LineStringType type;

		/** The designator. */
		public final String designator;

		/** The length. */
		public final long width, length;

		/** The color. */
		public final byte color;

		/** The points. */
		public final List<Point> points;

		/**
		 * Instantiates a new line string.
		 *
		 * @param lsg the lsg
		 */
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

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		@Override
		public LineStringType getType() {
			return type;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		@Override
		public String getDesignator() {
			return designator;
		}

		/**
		 * Gets the width.
		 *
		 * @return the width
		 */
		public long getWidth() {
			return width;
		}

		/**
		 * Gets the length.
		 *
		 * @return the length
		 */
		public long getLength() {
			return length;
		}

		/**
		 * Gets the color.
		 *
		 * @return the color
		 */
		@Override
		public byte getColor() {
			return color;
		}

		/**
		 * Gets the points.
		 *
		 * @return the points
		 */
		public List<Point> getPoints() {
			return points;
		}

		/**
		 * Gets the errors.
		 *
		 * @return the errors
		 */
		@Override
		public Validation getErrors() {
			Validation errors = new Validation();
			for (Point pnt : points) {
				errors.addAll(pnt.getErrors());
			}
			if (new HashSet<>(points).size() < 2)
				errors.error("LineString must have at least 2 distinct points");
			return errors;
		}

		/**
		 * To coordinates.
		 *
		 * @return the JSON array
		 * @throws GeoException the geo exception
		 */
		@Override
		JSONArray toCoordinates() throws GeoException {
			JSONArray coords = new JSONArray();
			for (int i = 0; i < points.size(); ++i) {
				if (i <= 0 || !points.get(i).equals(points.get(i - 1)))
					coords.put(points.get(i).toCoordinates());
			}
			return coords;
		}

		/**
		 * To geo json.
		 *
		 * @return the JSON object
		 * @throws GeoException the geo exception
		 */
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONArray coords = toCoordinates();
			if (coords.length() < 2)
				throw new GeoException("LineString must have at least 2 distinct points");

			JSONObject geo = coords.length() > 1 ? new JSONObject().put("type", "LineString").put("coordinates", coords)
					: new JSONObject().put("type", "Point").put("coordinates", coords.getJSONArray(0));

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

			return new JSONObject().put("type", "Feature").put("geometry", geo).put("properties", prop);
		}
	}

	/**
	 * The Enum PolygonType.
	 */
	public static enum PolygonType implements GeoType {

		/** The partfield boundary. */
		PARTFIELD_BOUNDARY,
		/** The treatment zone. */
		TREATMENT_ZONE,
		/** The water surface. */
		WATER_SURFACE,
		/** The building. */
		BUILDING,
		/** The road. */
		ROAD,
		/** The obstacle. */
		OBSTACLE,
		/** The flag. */
		FLAG,
		/** The other. */
		OTHER,
		/** The mainfield. */
		MAINFIELD,
		/** The headland. */
		HEADLAND,

		/** The buffer zone. */
		BUFFER_ZONE,
		/** The windbreak. */
		WINDBREAK;

		/** The Constant VALUES. */
		private static final PolygonType[] VALUES = { null, PARTFIELD_BOUNDARY, TREATMENT_ZONE, WATER_SURFACE, BUILDING,
				ROAD, OBSTACLE, FLAG, OTHER, MAINFIELD, HEADLAND, BUFFER_ZONE, WINDBREAK };

		/**
		 * From num.
		 *
		 * @param num the num
		 * @return the polygon type
		 */
		public static PolygonType fromNum(int num) {
			return (num > 0 && num < VALUES.length) ? VALUES[num] : OTHER;
		}
	}

	/**
	 * The Class Polygon.
	 */
	public static class Polygon extends Geo {
		/** The type. */
		// PLN
		public final PolygonType type;

		/** The designator. */
		public final String designator;

		/** The area. */
		public final long area;

		/** The color. */
		public final byte color;

		/** The rings. */
		public final List<LineString> rings;

		/**
		 * Instantiates a new polygon.
		 *
		 * @param pln the pln
		 */
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

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		@Override
		public PolygonType getType() {
			return type;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		@Override
		public String getDesignator() {
			return designator;
		}

		/**
		 * Gets the area.
		 *
		 * @return the area
		 */
		public long getArea() {
			return area;
		}

		/**
		 * Gets the color.
		 *
		 * @return the color
		 */
		@Override
		public byte getColor() {
			return color;
		}

		/**
		 * Gets the rings.
		 *
		 * @return the rings
		 */
		public List<LineString> getRings() {
			return rings;
		}

		/**
		 * Gets the errors.
		 *
		 * @return the errors
		 */
		@Override
		public Validation getErrors() {
			Validation errors = new Validation();
			if (rings.isEmpty())
				errors.warn(element.prefixEnd("Polygon has no rings"));
			int outerrings = 0;
			for (LineString lsg : rings) {
				errors.addAll(lsg.getErrors());
				if (lsg.type == LineStringType.POLYGON_EXTERIOR)
					++outerrings;
				if (lsg.points.size() > 1) {
					if (!lsg.points.get(0).equals(lsg.points.get(lsg.points.size() - 1)))
						errors.warn(lsg.element.prefixEnd("Polygon ring is not closed"));
					if (new HashSet<>(lsg.points).size() < 3)
						errors.error(lsg.element.prefixEnd("Polygon ring must have at least 3 distinct points"));
				}
			}
			if (outerrings > 1)
				errors.error(element.prefixEnd("Polygon has more than one POLYGON_EXTERIOR ring"));
			return errors;
		}

		/**
		 * Closed ring.
		 *
		 * @param ring the ring
		 * @return the JSON array
		 * @throws GeoException the geo exception
		 */
		private static JSONArray closedRing(LineString ring) throws GeoException {
			JSONArray coords = ring.toCoordinates();
			if (!ring.points.get(0).equals(ring.points.get(ring.points.size() - 1)))
				coords.put(ring.points.get(0).toCoordinates());
			if (coords.length() < 4)
				throw new GeoException("Polygon ring must have at least 3 distinct points");
			return coords;
		}

		/**
		 * To coordinates.
		 *
		 * @return the JSON array
		 * @throws GeoException the geo exception
		 */
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
			if (outerring < 0)
				outerring = 0;

			JSONArray coords = new JSONArray();
			coords.put(closedRing(rings.get(outerring)));
			for (int i = 0; i < rings.size(); ++i) {
				if (i != outerring)
					coords.put(closedRing(rings.get(i)));
			}
			return coords;
		}

		/**
		 * To geo json.
		 *
		 * @return the JSON object
		 * @throws GeoException the geo exception
		 */
		@Override
		public JSONObject toGeoJson() throws GeoException {
			JSONObject geo = new JSONObject().put("type", "Polygon").put("coordinates", toCoordinates());

			JSONObject prop = new JSONObject();
			if (!designator.isEmpty())
				prop.put("designator", designator);
			prop.put("type", type.toString());
			if (area != 0)
				prop.put("area", area);
			if (color != 0)
				prop.put("color", color);

			return new JSONObject().put("type", "Feature").put("geometry", geo).put("properties", prop);
		}
	}

	/**
	 * Read binary points.
	 *
	 * @param pnt     the pnt
	 * @param content the content
	 * @return the list
	 */
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
