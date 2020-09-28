package de.sdsd.projekt.api.isoxml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;

public abstract class Point extends Elem {
	public static enum PointType {
		FLAG(1), OTHER(2), FIELD_ACCESS(3), STORAGE(4), OBSTACLE(5), GUID_REF_A(6), 
		GUID_REF_B(7), GUID_REF_CENTER(8), GUID_POINT(9), PARTFIELD_REF_POINT(10), HOMEBASE(11);
		
		public final int number;
		private PointType(int number) {
			this.number = number;
		}
		
		public static Optional<PointType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<PointType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	protected Point(Elem parent) {
		super(parent, "PNT");
	}
	
	public static class XmlPoint extends Point {
		private XmlPoint(Elem parent, PointType type, double north, double east) {
			super(parent);
			e.setAttribute("A", Integer.toString(type.number));
			e.setAttribute("C", north(north));
			e.setAttribute("D", east(east));
		}
		public XmlPoint(Partfield parent, PointType type, double north, double east) {
			this((Elem)parent, type, north, east);
		}
		public XmlPoint(Partfield.LineString parent, PointType type, double north, double east) {
			this((Elem)parent, type, north, east);
		}
		
		public XmlPoint setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public XmlPoint setUp(double up) {
			e.setAttribute("E", Long.toString(Math.round(up * 1e3)));
			return this;
		}
		
		public XmlPoint setColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("F", Integer.toString(color));
			return this;
		}
		
		public XmlPoint setID() {
			e.setAttribute("G", id);
			return this;
		}
		
		public XmlPoint setHorizontalAccuracy(double horizontalAccuracy) {
			if(horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("H", floating(horizontalAccuracy));
			return this;
		}
		
		public XmlPoint setVerticalAccuracy(double verticalAccuracy) {
			if(verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("I", floating(verticalAccuracy));
			return this;
		}
	}
	
	public static class BinPoint extends Point {
		private boolean varType, varNorth, varEast,
				varUp = false, varColour = false, 
				varHorizontalAccuracy = false, varVerticalAccuracy = false;
		private final ByteBuffer buf;
		
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
		public BinPoint(Partfield parent) {
			this((Elem)parent);
		}
		public BinPoint(Partfield.LineString parent) {
			this((Elem)parent);
		}
		
		public BinPoint overwriteFilename(String filename) {
			e.setAttribute("J", filename);
			return this;
		}
		
		public BinPoint setType(PointType type) {
			e.setAttribute("A", Integer.toString(type.number));
			varType = false;
			return this;
		}
		public BinPoint setTypeVar() {
			e.setAttribute("A", "");
			varType = true;
			return this;
		}
		
		public BinPoint setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public BinPoint setNorth(double north) {
			e.setAttribute("C", north(north));
			varNorth = false;
			return this;
		}
		public BinPoint setNorthVar() {
			e.setAttribute("C", "");
			varNorth = true;
			return this;
		}
		
		public BinPoint setEast(double east) {
			e.setAttribute("D", east(east));
			varEast = false;
			return this;
		}
		public BinPoint setEastVar() {
			e.setAttribute("D", "");
			varEast = true;
			return this;
		}
		
		public BinPoint setUp(double up) {
			e.setAttribute("E", Long.toString(Math.round(up * 1e3)));
			varUp = false;
			return this;
		}
		public BinPoint setUpVar() {
			e.setAttribute("E", "");
			varUp = true;
			return this;
		}
		
		public BinPoint setColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("F", Integer.toString(color));
			varColour = false;
			return this;
		}
		public BinPoint setColourVar() {
			e.setAttribute("F", "");
			varColour = true;
			return this;
		}
		
		public BinPoint setID() {
			e.setAttribute("G", id);
			return this;
		}
		
		public BinPoint setHorizontalAccuracy(double horizontalAccuracy) {
			if(horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("H", floating(horizontalAccuracy));
			varHorizontalAccuracy = false;
			return this;
		}
		public BinPoint setHorizontalAccuracyVar() {
			e.setAttribute("H", "");
			varHorizontalAccuracy = true;
			return this;
		}
		
		public BinPoint setVerticalAccuracy(double verticalAccuracy) {
			if(verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("I", floating(verticalAccuracy));
			varVerticalAccuracy = false;
			return this;
		}
		public BinPoint setVerticalAccuracyVar() {
			e.setAttribute("I", "");
			varVerticalAccuracy = true;
			return this;
		}
		
		public BinPoint setFileLength(long fileLength) {
			if(fileLength < 0 || fileLength > 4294967294L)
				throw new IllegalArgumentException("invalid file length");
			e.setAttribute("K", Long.toString(fileLength));
			return this;
		}

		public VarPoint writePoint() {
			return new VarPoint();
		}
		
		public class VarPoint {
			private Byte type = null;
			private Long north = null, east = null;
			private Integer up = null;
			private Byte colour = null;
			private Short horizontalAccuracy = null, verticalAccuracy = null;
			
			public void setType(PointType type) {
				if(!varType) throw new IllegalStateException("type is not variable");
				this.type = (byte) type.number;
			}
			public void setNorth(double north) {
				if(!varNorth) throw new IllegalStateException("north is not variable");
				this.north = Math.round(north * 1e16);
			}
			public void setEast(double east) {
				if(!varEast) throw new IllegalStateException("east is not variable");
				this.east = Math.round(east * 1e16);
			}
			public void setUp(double up) {
				if(!varUp) throw new IllegalStateException("up is not variable");
				this.up = (int) Math.round(up * 1e3);
			}
			public void setColour(int colour) {
				if(!varColour) throw new IllegalStateException("colour is not variable");
				this.colour = (byte) colour;
			}
			public void setHorizontalAccuracy(int horizontalAccuracy) {
				if(!varHorizontalAccuracy) throw new IllegalStateException("horizontal accuracy is not variable");
				this.horizontalAccuracy = (short) horizontalAccuracy;
			}
			public void setVerticalAccuracy(int verticalAccuracy) {
				if(!varVerticalAccuracy) throw new IllegalStateException("vertical accuracy is not variable");
				this.verticalAccuracy = (short) verticalAccuracy;
			}
			
			public boolean check() {
				if(varType && type == null) return false;
				if(varNorth && north == null) return false;
				if(varEast && east == null) return false;
				if(varUp && up == null) return false;
				if(varColour && colour == null) return false;
				if(varHorizontalAccuracy && horizontalAccuracy == null) return false;
				if(varVerticalAccuracy && verticalAccuracy == null) return false;
				return true;
			}
			
			public int write(OutputStream out) throws IOException {
				buf.rewind();
				if(varType) buf.put(type);
				if(varNorth) buf.putLong(north);
				if(varEast) buf.putLong(east);
				if(varUp) buf.putInt(up);
				if(varColour) buf.put(colour);
				if(varHorizontalAccuracy) buf.putShort(horizontalAccuracy);
				if(varVerticalAccuracy) buf.putShort(verticalAccuracy);
				out.write(buf.array(), 0, buf.position());
				return buf.position();
			}
		}
	}
	
}