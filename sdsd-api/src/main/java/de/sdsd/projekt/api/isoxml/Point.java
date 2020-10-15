package de.sdsd.projekt.api.isoxml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;

/**
 * The Class Point.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public abstract class Point extends Elem {

	/**
	 * The Enum PointType.
	 */
	public static enum PointType {

		/** The flag. */
		FLAG(1),
		/** The other. */
		OTHER(2),
		/** The field access. */
		FIELD_ACCESS(3),
		/** The storage. */
		STORAGE(4),
		/** The obstacle. */
		OBSTACLE(5),
		/** The guid ref a. */
		GUID_REF_A(6),

		/** The guid ref b. */
		GUID_REF_B(7),
		/** The guid ref center. */
		GUID_REF_CENTER(8),
		/** The guid point. */
		GUID_POINT(9),
		/** The partfield ref point. */
		PARTFIELD_REF_POINT(10),
		/** The homebase. */
		HOMEBASE(11);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new point type.
		 *
		 * @param number the number
		 */
		private PointType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<PointType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<PointType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * Instantiates a new point.
	 *
	 * @param parent the parent
	 */
	protected Point(Elem parent) {
		super(parent, "PNT");
	}

	/**
	 * The Class XmlPoint.
	 */
	public static class XmlPoint extends Point {

		/**
		 * Instantiates a new xml point.
		 *
		 * @param parent the parent
		 * @param type   the type
		 * @param north  the north
		 * @param east   the east
		 */
		private XmlPoint(Elem parent, PointType type, double north, double east) {
			super(parent);
			e.setAttribute("A", Integer.toString(type.number));
			e.setAttribute("C", north(north));
			e.setAttribute("D", east(east));
		}

		/**
		 * Instantiates a new xml point.
		 *
		 * @param parent the parent
		 * @param type   the type
		 * @param north  the north
		 * @param east   the east
		 */
		public XmlPoint(Partfield parent, PointType type, double north, double east) {
			this((Elem) parent, type, north, east);
		}

		/**
		 * Instantiates a new xml point.
		 *
		 * @param parent the parent
		 * @param type   the type
		 * @param north  the north
		 * @param east   the east
		 */
		public XmlPoint(Partfield.LineString parent, PointType type, double north, double east) {
			this((Elem) parent, type, north, east);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the xml point
		 */
		public XmlPoint setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the up.
		 *
		 * @param up the up
		 * @return the xml point
		 */
		public XmlPoint setUp(double up) {
			e.setAttribute("E", Long.toString(Math.round(up * 1e3)));
			return this;
		}

		/**
		 * Sets the colour.
		 *
		 * @param color the color
		 * @return the xml point
		 */
		public XmlPoint setColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("F", Integer.toString(color));
			return this;
		}

		/**
		 * Sets the ID.
		 *
		 * @return the xml point
		 */
		public XmlPoint setID() {
			e.setAttribute("G", id);
			return this;
		}

		/**
		 * Sets the horizontal accuracy.
		 *
		 * @param horizontalAccuracy the horizontal accuracy
		 * @return the xml point
		 */
		public XmlPoint setHorizontalAccuracy(double horizontalAccuracy) {
			if (horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("H", floating(horizontalAccuracy));
			return this;
		}

		/**
		 * Sets the vertical accuracy.
		 *
		 * @param verticalAccuracy the vertical accuracy
		 * @return the xml point
		 */
		public XmlPoint setVerticalAccuracy(double verticalAccuracy) {
			if (verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("I", floating(verticalAccuracy));
			return this;
		}
	}

	/**
	 * The Class BinPoint.
	 */
	public static class BinPoint extends Point {

		/** The var vertical accuracy. */
		private boolean varType, varNorth, varEast, varUp = false, varColour = false, varHorizontalAccuracy = false,
				varVerticalAccuracy = false;

		/** The buf. */
		private final ByteBuffer buf;

		/**
		 * Instantiates a new bin point.
		 *
		 * @param parent the parent
		 */
		private BinPoint(Elem parent) {
			super(parent);
			e.setAttribute("A", "");
			varType = true;
			e.setAttribute("C", "");
			varNorth = true;
			e.setAttribute("D", "");
			varEast = true;
			e.setAttribute("J", createFile());
			this.buf = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);
		}

		/**
		 * Instantiates a new bin point.
		 *
		 * @param parent the parent
		 */
		public BinPoint(Partfield parent) {
			this((Elem) parent);
		}

		/**
		 * Instantiates a new bin point.
		 *
		 * @param parent the parent
		 */
		public BinPoint(Partfield.LineString parent) {
			this((Elem) parent);
		}

		/**
		 * Overwrite filename.
		 *
		 * @param filename the filename
		 * @return the bin point
		 */
		public BinPoint overwriteFilename(String filename) {
			e.setAttribute("J", filename);
			return this;
		}

		/**
		 * Sets the type.
		 *
		 * @param type the type
		 * @return the bin point
		 */
		public BinPoint setType(PointType type) {
			e.setAttribute("A", Integer.toString(type.number));
			varType = false;
			return this;
		}

		/**
		 * Sets the type var.
		 *
		 * @return the bin point
		 */
		public BinPoint setTypeVar() {
			e.setAttribute("A", "");
			varType = true;
			return this;
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the bin point
		 */
		public BinPoint setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the north.
		 *
		 * @param north the north
		 * @return the bin point
		 */
		public BinPoint setNorth(double north) {
			e.setAttribute("C", north(north));
			varNorth = false;
			return this;
		}

		/**
		 * Sets the north var.
		 *
		 * @return the bin point
		 */
		public BinPoint setNorthVar() {
			e.setAttribute("C", "");
			varNorth = true;
			return this;
		}

		/**
		 * Sets the east.
		 *
		 * @param east the east
		 * @return the bin point
		 */
		public BinPoint setEast(double east) {
			e.setAttribute("D", east(east));
			varEast = false;
			return this;
		}

		/**
		 * Sets the east var.
		 *
		 * @return the bin point
		 */
		public BinPoint setEastVar() {
			e.setAttribute("D", "");
			varEast = true;
			return this;
		}

		/**
		 * Sets the up.
		 *
		 * @param up the up
		 * @return the bin point
		 */
		public BinPoint setUp(double up) {
			e.setAttribute("E", Long.toString(Math.round(up * 1e3)));
			varUp = false;
			return this;
		}

		/**
		 * Sets the up var.
		 *
		 * @return the bin point
		 */
		public BinPoint setUpVar() {
			e.setAttribute("E", "");
			varUp = true;
			return this;
		}

		/**
		 * Sets the colour.
		 *
		 * @param color the color
		 * @return the bin point
		 */
		public BinPoint setColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("F", Integer.toString(color));
			varColour = false;
			return this;
		}

		/**
		 * Sets the colour var.
		 *
		 * @return the bin point
		 */
		public BinPoint setColourVar() {
			e.setAttribute("F", "");
			varColour = true;
			return this;
		}

		/**
		 * Sets the ID.
		 *
		 * @return the bin point
		 */
		public BinPoint setID() {
			e.setAttribute("G", id);
			return this;
		}

		/**
		 * Sets the horizontal accuracy.
		 *
		 * @param horizontalAccuracy the horizontal accuracy
		 * @return the bin point
		 */
		public BinPoint setHorizontalAccuracy(double horizontalAccuracy) {
			if (horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("H", floating(horizontalAccuracy));
			varHorizontalAccuracy = false;
			return this;
		}

		/**
		 * Sets the horizontal accuracy var.
		 *
		 * @return the bin point
		 */
		public BinPoint setHorizontalAccuracyVar() {
			e.setAttribute("H", "");
			varHorizontalAccuracy = true;
			return this;
		}

		/**
		 * Sets the vertical accuracy.
		 *
		 * @param verticalAccuracy the vertical accuracy
		 * @return the bin point
		 */
		public BinPoint setVerticalAccuracy(double verticalAccuracy) {
			if (verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("I", floating(verticalAccuracy));
			varVerticalAccuracy = false;
			return this;
		}

		/**
		 * Sets the vertical accuracy var.
		 *
		 * @return the bin point
		 */
		public BinPoint setVerticalAccuracyVar() {
			e.setAttribute("I", "");
			varVerticalAccuracy = true;
			return this;
		}

		/**
		 * Sets the file length.
		 *
		 * @param fileLength the file length
		 * @return the bin point
		 */
		public BinPoint setFileLength(long fileLength) {
			if (fileLength < 0 || fileLength > 4294967294L)
				throw new IllegalArgumentException("invalid file length");
			e.setAttribute("K", Long.toString(fileLength));
			return this;
		}

		/**
		 * Write point.
		 *
		 * @return the var point
		 */
		public VarPoint writePoint() {
			return new VarPoint();
		}

		/**
		 * The Class VarPoint.
		 */
		public class VarPoint {

			/** The type. */
			private Byte type = null;

			/** The east. */
			private Long north = null, east = null;

			/** The up. */
			private Integer up = null;

			/** The colour. */
			private Byte colour = null;

			/** The vertical accuracy. */
			private Short horizontalAccuracy = null, verticalAccuracy = null;

			/**
			 * Sets the type.
			 *
			 * @param type the new type
			 */
			public void setType(PointType type) {
				if (!varType)
					throw new IllegalStateException("type is not variable");
				this.type = (byte) type.number;
			}

			/**
			 * Sets the north.
			 *
			 * @param north the new north
			 */
			public void setNorth(double north) {
				if (!varNorth)
					throw new IllegalStateException("north is not variable");
				this.north = Math.round(north * 1e16);
			}

			/**
			 * Sets the east.
			 *
			 * @param east the new east
			 */
			public void setEast(double east) {
				if (!varEast)
					throw new IllegalStateException("east is not variable");
				this.east = Math.round(east * 1e16);
			}

			/**
			 * Sets the up.
			 *
			 * @param up the new up
			 */
			public void setUp(double up) {
				if (!varUp)
					throw new IllegalStateException("up is not variable");
				this.up = (int) Math.round(up * 1e3);
			}

			/**
			 * Sets the colour.
			 *
			 * @param colour the new colour
			 */
			public void setColour(int colour) {
				if (!varColour)
					throw new IllegalStateException("colour is not variable");
				this.colour = (byte) colour;
			}

			/**
			 * Sets the horizontal accuracy.
			 *
			 * @param horizontalAccuracy the new horizontal accuracy
			 */
			public void setHorizontalAccuracy(int horizontalAccuracy) {
				if (!varHorizontalAccuracy)
					throw new IllegalStateException("horizontal accuracy is not variable");
				this.horizontalAccuracy = (short) horizontalAccuracy;
			}

			/**
			 * Sets the vertical accuracy.
			 *
			 * @param verticalAccuracy the new vertical accuracy
			 */
			public void setVerticalAccuracy(int verticalAccuracy) {
				if (!varVerticalAccuracy)
					throw new IllegalStateException("vertical accuracy is not variable");
				this.verticalAccuracy = (short) verticalAccuracy;
			}

			/**
			 * Check.
			 *
			 * @return true, if successful
			 */
			public boolean check() {
				if (varType && type == null)
					return false;
				if (varNorth && north == null)
					return false;
				if (varEast && east == null)
					return false;
				if (varUp && up == null)
					return false;
				if (varColour && colour == null)
					return false;
				if (varHorizontalAccuracy && horizontalAccuracy == null)
					return false;
				if (varVerticalAccuracy && verticalAccuracy == null)
					return false;
				return true;
			}

			/**
			 * Write.
			 *
			 * @param out the out
			 * @return the int
			 * @throws IOException Signals that an I/O exception has occurred.
			 */
			public int write(OutputStream out) throws IOException {
				buf.rewind();
				if (varType)
					buf.put(type);
				if (varNorth)
					buf.putLong(north);
				if (varEast)
					buf.putLong(east);
				if (varUp)
					buf.putInt(up);
				if (varColour)
					buf.put(colour);
				if (varHorizontalAccuracy)
					buf.putShort(horizontalAccuracy);
				if (varVerticalAccuracy)
					buf.putShort(verticalAccuracy);
				out.write(buf.array(), 0, buf.position());
				return buf.position();
			}
		}
	}

}