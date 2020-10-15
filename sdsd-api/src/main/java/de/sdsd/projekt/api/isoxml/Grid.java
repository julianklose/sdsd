package de.sdsd.projekt.api.isoxml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.OIDElem;

/**
 * The Class Grid.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public abstract class Grid extends Elem {

	/**
	 * Instantiates a new grid.
	 *
	 * @param parent        the parent
	 * @param minNorth      the min north
	 * @param minEast       the min east
	 * @param cellSizeNorth the cell size north
	 * @param cellSizeEast  the cell size east
	 * @param cols          the cols
	 * @param rows          the rows
	 * @param type          the type
	 */
	public Grid(Task parent, double minNorth, double minEast, double cellSizeNorth, double cellSizeEast, int cols,
			int rows, int type) {
		super(parent, "GRD");
		e.setAttribute("A", north(minNorth));
		e.setAttribute("B", east(minEast));
		if (cellSizeNorth < 0 || cellSizeNorth > 1 || cellSizeEast < 0 || cellSizeEast > 1)
			throw new IllegalArgumentException("invalid cell size");
		e.setAttribute("C", floating(cellSizeNorth));
		e.setAttribute("D", floating(cellSizeEast));
		if (cols < 0 || rows < 0)
			throw new IllegalArgumentException("invalid row count");
		e.setAttribute("E", Integer.toString(cols));
		e.setAttribute("F", Integer.toString(rows));
		e.setAttribute("G", createFile());
		e.setAttribute("I", Integer.toString(type));
	}

	/**
	 * Overwrite file name.
	 *
	 * @param filename the filename
	 * @return the grid
	 */
	public Grid overwriteFileName(String filename) {
		e.setAttribute("G", filename);
		return this;
	}

	/**
	 * Sets the file length.
	 *
	 * @param fileLength the file length
	 * @return the grid
	 */
	public Grid setFileLength(long fileLength) {
		if (fileLength < 0 || fileLength > 4294967294L)
			throw new IllegalArgumentException("invalid file length");
		e.setAttribute("H", Long.toString(fileLength));
		return this;
	}

	/**
	 * The Class GridType1.
	 */
	public static class GridType1 extends Grid {

		/** The cols. */
		private final int cols;

		/** The buf. */
		private final byte[] buf;

		/**
		 * Instantiates a new grid type 1.
		 *
		 * @param parent        the parent
		 * @param minNorth      the min north
		 * @param minEast       the min east
		 * @param cellSizeNorth the cell size north
		 * @param cellSizeEast  the cell size east
		 * @param cols          the cols
		 * @param rows          the rows
		 */
		public GridType1(Task parent, double minNorth, double minEast, double cellSizeNorth, double cellSizeEast,
				int cols, int rows) {
			super(parent, minNorth, minEast, cellSizeNorth, cellSizeEast, cols, rows, 1);
			this.cols = cols;
			this.buf = new byte[cols * rows];
		}

		/**
		 * Sets the.
		 *
		 * @param row the row
		 * @param col the col
		 * @param tzn the tzn
		 * @return the grid type 1
		 */
		public GridType1 set(int row, int col, TreatmentZone tzn) {
			buf[row * cols + col] = (byte) Integer.parseInt(tzn.oid);
			return this;
		}

		/**
		 * Write.
		 *
		 * @param out the out
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public void write(OutputStream out) throws IOException {
			out.write(buf);
		}
	}

	/**
	 * The Class GridType2.
	 */
	public static class GridType2 extends Grid {

		/** The pdvs. */
		private final List<ProcessDataVariable> pdvs;

		/** The cols. */
		private final int cols;

		/** The buf. */
		private final ByteBuffer buf;

		/**
		 * Instantiates a new grid type 2.
		 *
		 * @param parent        the parent
		 * @param minNorth      the min north
		 * @param minEast       the min east
		 * @param cellSizeNorth the cell size north
		 * @param cellSizeEast  the cell size east
		 * @param cols          the cols
		 * @param rows          the rows
		 * @param tzn           the tzn
		 */
		public GridType2(Task parent, double minNorth, double minEast, double cellSizeNorth, double cellSizeEast,
				int cols, int rows, TreatmentZone tzn) {
			super(parent, minNorth, minEast, cellSizeNorth, cellSizeEast, cols, rows, 2);
			e.setAttribute("J", tzn.oid());

			pdvs = tzn.pdvs;
			this.cols = cols;
			this.buf = ByteBuffer.allocate(cols * rows * pdvs.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
		}

		/**
		 * Sets the.
		 *
		 * @param row    the row
		 * @param col    the col
		 * @param values the values
		 * @return the grid type 2
		 */
		public GridType2 set(int row, int col, int... values) {
			if (values.length != pdvs.size())
				throw new IllegalArgumentException("value count doesn't match PDV count (" + pdvs.size() + ")");
			buf.position((row * cols + col) * pdvs.size() * 4);
			for (int v : values) {
				buf.putInt(v);
			}
			return this;
		}

		/**
		 * Sets the.
		 *
		 * @param row    the row
		 * @param col    the col
		 * @param values the values
		 * @return the grid type 2
		 */
		public GridType2 set(int row, int col, double... values) {
			if (values.length != pdvs.size())
				throw new IllegalArgumentException("value count doesn't match PDV count (" + pdvs.size() + ")");
			buf.position((row * cols + col) * pdvs.size() * 4);
			for (int i = 0; i < values.length; ++i) {
				Device.ValuePresentation vpn = pdvs.get(i).vpn;
				buf.putInt(vpn != null ? vpn.convert(values[i]) : (int) Math.round(values[i]));
			}
			return this;
		}

		/**
		 * Write.
		 *
		 * @param out the out
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public void write(OutputStream out) throws IOException {
			out.write(buf.array());
		}
	}

	/**
	 * The Class TreatmentZone.
	 */
	public static class TreatmentZone extends OIDElem {

		/** The pdvs. */
		private final List<ProcessDataVariable> pdvs = new ArrayList<>();

		/**
		 * Instantiates a new treatment zone.
		 *
		 * @param parent the parent
		 */
		public TreatmentZone(Task parent) {
			super(parent, "TZN");
			e.setAttribute("A", oid);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the treatment zone
		 */
		public TreatmentZone setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the colour.
		 *
		 * @param color the color
		 * @return the treatment zone
		 */
		public TreatmentZone setColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("C", Integer.toString(color));
			return this;
		}

		/**
		 * Adds the polygon.
		 *
		 * @param type the type
		 * @return the partfield. polygon
		 */
		public Partfield.Polygon addPolygon(Partfield.PolygonType type) {
			return new Partfield.Polygon(this, type);
		}

		/**
		 * Adds the process data variable.
		 *
		 * @param ddi   the ddi
		 * @param value the value
		 * @return the process data variable
		 */
		public ProcessDataVariable addProcessDataVariable(int ddi, int value) {
			return new ProcessDataVariable(this, ddi, value);
		}
	}

	/**
	 * The Class ProcessDataVariable.
	 */
	public static class ProcessDataVariable extends Elem {

		/** The vpn. */
		private Device.ValuePresentation vpn = null;

		/**
		 * Instantiates a new process data variable.
		 *
		 * @param parent the parent
		 * @param ddi    the ddi
		 * @param value  the value
		 */
		private ProcessDataVariable(Elem parent, int ddi, int value) {
			super(parent, "PDV");
			e.setAttribute("A", ddi(ddi));
			e.setAttribute("B", Integer.toString(value));
		}

		/**
		 * Instantiates a new process data variable.
		 *
		 * @param parent the parent
		 * @param ddi    the ddi
		 * @param value  the value
		 */
		public ProcessDataVariable(Grid.TreatmentZone parent, int ddi, int value) {
			this((Elem) parent, ddi, value);
			parent.pdvs.add(this);
		}

		/**
		 * Instantiates a new process data variable.
		 *
		 * @param parent the parent
		 * @param ddi    the ddi
		 * @param value  the value
		 */
		public ProcessDataVariable(ProcessDataVariable parent, int ddi, int value) {
			this((Elem) parent, ddi, value);
		}

		/**
		 * Sets the product.
		 *
		 * @param pdt the pdt
		 * @return the process data variable
		 */
		public ProcessDataVariable setProduct(Partfield.Product pdt) {
			e.setAttribute("C", pdt.id);
			return this;
		}

		/**
		 * Sets the device element.
		 *
		 * @param det the det
		 * @return the process data variable
		 */
		public ProcessDataVariable setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("D", det.id);
			return this;
		}

		/**
		 * Sets the value presentation.
		 *
		 * @param vpn the vpn
		 * @return the process data variable
		 */
		public ProcessDataVariable setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("E", vpn.id);
			this.vpn = vpn;
			return this;
		}

		/**
		 * Sets the actual cultural practice value.
		 *
		 * @param value the value
		 * @return the process data variable
		 */
		public ProcessDataVariable setActualCulturalPracticeValue(int value) {
			e.setAttribute("F", Integer.toString(value));
			return this;
		}

		/**
		 * Sets the element type instance value.
		 *
		 * @param value the value
		 * @return the process data variable
		 */
		public ProcessDataVariable setElementTypeInstanceValue(int value) {
			e.setAttribute("G", Integer.toString(value));
			return this;
		}

		/**
		 * Adds the process data variable.
		 *
		 * @param ddi   the ddi
		 * @param value the value
		 * @return the process data variable
		 */
		public ProcessDataVariable addProcessDataVariable(int ddi, int value) {
			return new ProcessDataVariable(this, ddi, value);
		}
	}

}