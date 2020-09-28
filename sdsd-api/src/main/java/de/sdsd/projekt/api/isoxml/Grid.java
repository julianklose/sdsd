package de.sdsd.projekt.api.isoxml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.OIDElem;

public abstract class Grid extends Elem {

	public Grid(Task parent, 
			double minNorth, double minEast, 
			double cellSizeNorth, double cellSizeEast, 
			int cols, int rows, int type) {
		super(parent, "GRD");
		e.setAttribute("A", north(minNorth));
		e.setAttribute("B", east(minEast));
		if(cellSizeNorth < 0 || cellSizeNorth > 1 || cellSizeEast < 0 || cellSizeEast > 1)
			throw new IllegalArgumentException("invalid cell size");
		e.setAttribute("C", floating(cellSizeNorth));
		e.setAttribute("D", floating(cellSizeEast));
		if(cols < 0 || rows < 0)
			throw new IllegalArgumentException("invalid row count");
		e.setAttribute("E", Integer.toString(cols));
		e.setAttribute("F", Integer.toString(rows));
		e.setAttribute("G", createFile());
		e.setAttribute("I", Integer.toString(type));
	}
	
	public Grid overwriteFileName(String filename) {
		e.setAttribute("G", filename);
		return this;
	}
	
	public Grid setFileLength(long fileLength) {
		if(fileLength < 0 || fileLength > 4294967294L)
			throw new IllegalArgumentException("invalid file length");
		e.setAttribute("H", Long.toString(fileLength));
		return this;
	}
	
	public static class GridType1 extends Grid {

		private final int cols;
		private final byte[] buf;
		
		public GridType1(Task parent, 
				double minNorth, double minEast, 
				double cellSizeNorth, double cellSizeEast,
				int cols, int rows) {
			super(parent, minNorth, minEast, cellSizeNorth, cellSizeEast, cols, rows, 1);
			this.cols = cols;
			this.buf = new byte[cols * rows];
		}
		
		public GridType1 set(int row, int col, TreatmentZone tzn) {
			buf[row * cols + col] = (byte)Integer.parseInt(tzn.oid);
			return this;
		}
		
		public void write(OutputStream out) throws IOException {
			out.write(buf);
		}
	}
	
	public static class GridType2 extends Grid {
		
		private final List<ProcessDataVariable> pdvs;
		private final int cols;
		private final ByteBuffer buf;
		
		public GridType2(Task parent, 
				double minNorth, double minEast, 
				double cellSizeNorth, double cellSizeEast,
				int cols, int rows, TreatmentZone tzn) {
			super(parent, minNorth, minEast, cellSizeNorth, cellSizeEast, cols, rows, 2);
			e.setAttribute("J", tzn.oid());
			
			pdvs = tzn.pdvs;
			this.cols = cols;
			this.buf = ByteBuffer.allocate(cols * rows * pdvs.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
		}
		
		public GridType2 set(int row, int col, int...values) {
			if(values.length != pdvs.size())
				throw new IllegalArgumentException("value count doesn't match PDV count (" + pdvs.size() + ")");
			buf.position((row * cols + col) * pdvs.size() * 4);
			for(int v : values) {
				buf.putInt(v);
			}
			return this;
		}
		
		public GridType2 set(int row, int col, double...values) {
			if(values.length != pdvs.size())
				throw new IllegalArgumentException("value count doesn't match PDV count (" + pdvs.size() + ")");
			buf.position((row * cols + col) * pdvs.size() * 4);
			for(int i = 0; i < values.length; ++i) {
				Device.ValuePresentation vpn = pdvs.get(i).vpn;
				buf.putInt(vpn != null ? vpn.convert(values[i]) : (int)Math.round(values[i]));
			}
			return this;
		}
		
		public void write(OutputStream out) throws IOException {
			out.write(buf.array());
		}
	}
	
	public static class TreatmentZone extends OIDElem {
		private final List<ProcessDataVariable> pdvs = new ArrayList<>();
		
		public TreatmentZone(Task parent) {
			super(parent, "TZN");
			e.setAttribute("A", oid);
		}
		
		public TreatmentZone setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}
		
		public TreatmentZone setColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("C", Integer.toString(color));
			return this;
		}
		
		public Partfield.Polygon addPolygon(Partfield.PolygonType type) {
			return new Partfield.Polygon(this, type);
		}
		
		public ProcessDataVariable addProcessDataVariable(int ddi, int value) {
			return new ProcessDataVariable(this, ddi, value);
		}
	}
	
	public static class ProcessDataVariable extends Elem {
		private Device.ValuePresentation vpn = null;
		
		private ProcessDataVariable(Elem parent, int ddi, int value) {
			super(parent, "PDV");
			e.setAttribute("A", ddi(ddi));
			e.setAttribute("B", Integer.toString(value));
		}
		public ProcessDataVariable(Grid.TreatmentZone parent, int ddi, int value) {
			this((Elem)parent, ddi, value);
			parent.pdvs.add(this);
		}
		public ProcessDataVariable(ProcessDataVariable parent, int ddi, int value) {
			this((Elem)parent, ddi, value);
		}

		public ProcessDataVariable setProduct(Partfield.Product pdt) {
			e.setAttribute("C", pdt.id);
			return this;
		}

		public ProcessDataVariable setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("D", det.id);
			return this;
		}

		public ProcessDataVariable setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("E", vpn.id);
			this.vpn = vpn;
			return this;
		}

		public ProcessDataVariable setActualCulturalPracticeValue(int value) {
			e.setAttribute("F", Integer.toString(value));
			return this;
		}

		public ProcessDataVariable setElementTypeInstanceValue(int value) {
			e.setAttribute("G", Integer.toString(value));
			return this;
		}
		
		public ProcessDataVariable addProcessDataVariable(int ddi, int value) {
			return new ProcessDataVariable(this, ddi, value);
		}
	}
	
}