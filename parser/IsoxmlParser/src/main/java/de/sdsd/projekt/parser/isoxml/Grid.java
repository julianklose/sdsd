package de.sdsd.projekt.parser.isoxml;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.RandomAccess;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.parser.isoxml.Attribute.ByteAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DDIAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DoubleAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.IntAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.OIDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.ULongAttr;
import de.sdsd.projekt.parser.isoxml.RefAttr.IDRef;
import de.sdsd.projekt.parser.isoxml.RefAttr.OIDRef;

/**
 * The Class Grid.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class Grid extends AbstractList<GridEntry> implements RandomAccess {

	/** The grid. */
	private final ParserAPI.Grid grid;

	/** The column count. */
	private final int rowCount, columnCount;

	/** The type. */
	private final int type;

	/** The refs. */
	private final Map<Integer, TreatmentZone> refs;

	/** The treatment. */
	private final GridEntry[][] treatment;

	/**
	 * Instantiates a new grid.
	 *
	 * @param grd     the grd
	 * @param content the content
	 */
	public Grid(IsoXmlElement grd, byte[] content) {
		grid = new ParserAPI.Grid(grd.getUris().get(0), grd.getAttribute("filename", StringAttr.class).getValue(),
				grd.getAttribute("gridMinimumNorthPosition", DoubleAttr.class).getValue(),
				grd.getAttribute("gridMinimumEastPosition", DoubleAttr.class).getValue(),
				grd.getAttribute("gridCellNorthSize", DoubleAttr.class).getValue(),
				grd.getAttribute("gridCellEastSize", DoubleAttr.class).getValue());

		rowCount = grd.getAttribute("gridMaximumRow", ULongAttr.class).getValue().intValue();
		columnCount = grd.getAttribute("gridMaximumColumn", ULongAttr.class).getValue().intValue();
		treatment = new GridEntry[rowCount][columnCount];

		ByteBuffer data = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		type = grd.getAttribute("gridType", EnumAttr.class).number();
		if (type == 1) {
			refs = new HashMap<>();
			IsoXmlElement tsk = grd.getParent();
			if (tsk != null) {
				for (IsoXmlElement e : tsk.getChildren()) {
					if (e.getTag().equals("TZN"))
						refs.put(e.getAttribute("treatmentZoneCode", OIDAttr.class).getValue(),
								new TreatmentZone(e, false));
				}
			}
			if (refs.isEmpty())
				throw new IllegalArgumentException("No TreatmentZones for GridType 1");

			for (int row = 0; row < rowCount; ++row) {
				for (int col = 0; col < columnCount; ++col) {
					treatment[row][col] = new GridEntry(grid.northMin + row * grid.northCellSize,
							grid.eastMin + col * grid.eastCellSize, grid.northCellSize, grid.eastCellSize, refs, data);
				}
			}
		} else if (type == 2) {
			IsoXmlElement tzn = grd.getAttribute("treatmentZoneCode", OIDRef.class).getRef();
			if (tzn == null)
				throw new IllegalArgumentException("GridType 2 without valid TreatmentZone");
			TreatmentZone tr = new TreatmentZone(tzn, true);
			refs = Collections.singletonMap(tzn.getAttribute("treatmentZoneCode", OIDAttr.class).getValue(), tr);

			for (int row = 0; row < rowCount; ++row) {
				for (int col = 0; col < columnCount; ++col) {
					treatment[row][col] = new GridEntry(grid.northMin + row * grid.northCellSize,
							grid.eastMin + col * grid.eastCellSize, grid.northCellSize, grid.eastCellSize, tr, data);
				}
			}
		} else
			throw new IllegalArgumentException("Unknown GridType " + type);
	}

	/**
	 * Gets the grid.
	 *
	 * @return the grid
	 */
	public ParserAPI.Grid getGrid() {
		return grid;
	}

	/**
	 * Gets the row count.
	 *
	 * @return the row count
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * Gets the column count.
	 *
	 * @return the column count
	 */
	public int getColumnCount() {
		return columnCount;
	}

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Gets the treatment zones.
	 *
	 * @return the treatment zones
	 */
	public Collection<TreatmentZone> getTreatmentZones() {
		return Collections.unmodifiableCollection(refs.values());
	}

	/**
	 * Gets the.
	 *
	 * @param row the row
	 * @param col the col
	 * @return the grid entry
	 */
	public GridEntry get(int row, int col) {
		return treatment[row][col];
	}

	/**
	 * Gets the.
	 *
	 * @param index the index
	 * @return the grid entry
	 */
	@Override
	public GridEntry get(int index) {
		return treatment[index / columnCount][index % columnCount];
	}

	/**
	 * Size.
	 *
	 * @return the int
	 */
	@Override
	public int size() {
		return rowCount * columnCount;
	}

	/**
	 * Gets the all errors.
	 *
	 * @return the all errors
	 */
	public Validation getAllErrors() {
		Validation errors = new Validation();
		for (int row = 0; row < rowCount; ++row) {
			for (int col = 0; col < columnCount; ++col) {
				GridEntry ge = treatment[row][col];
				if (ge.hasErrors())
					errors.error("%s [%d,%d]: %s", grid.name, row, col, ge.getError());
			}
		}
		return errors;
	}

	/**
	 * The Class TreatmentZone.
	 */
	public class TreatmentZone extends AbstractList<ValueInfo> {

		/** The tzn. */
		public final IsoXmlElement tzn;

		/** The value infos. */
		private final ValueInfo[] valueInfos;

		/**
		 * Instantiates a new treatment zone.
		 *
		 * @param tzn       the tzn
		 * @param onlyEmpty the only empty
		 */
		TreatmentZone(IsoXmlElement tzn, boolean onlyEmpty) {
			this.tzn = tzn;
			this.valueInfos = tzn.findChildren("PDV").stream().map(ValueInfo::new)
					.filter(v -> !onlyEmpty || v.value == 0).toArray(ValueInfo[]::new);
		}

		/**
		 * Gets the tzn.
		 *
		 * @return the tzn
		 */
		public IsoXmlElement getTZN() {
			return tzn;
		}

		/**
		 * Gets the.
		 *
		 * @param index the index
		 * @return the value info
		 */
		@Override
		public ValueInfo get(int index) {
			return valueInfos[index];
		}

		/**
		 * Size.
		 *
		 * @return the int
		 */
		@Override
		public int size() {
			return valueInfos.length;
		}
	}

	/**
	 * The Class ValueInfo.
	 */
	public class ValueInfo extends ParserAPI.ValueInfo {

		/** The process data variable. */
		public final IsoXmlElement processDataVariable;

		/** The value. */
		public final int ddi, value;

		/**
		 * Instantiates a new value info.
		 *
		 * @param pdv the pdv
		 */
		ValueInfo(IsoXmlElement pdv) {
			super(pdv.getUris().get(0));
			addGrid(grid);

			this.processDataVariable = pdv;
			this.ddi = pdv.getAttribute("processDataDdi", DDIAttr.class).getValue();
			this.value = pdv.getAttribute("processDataValue", IntAttr.class).getValue();
			IsoXmlElement vpn = pdv.getAttribute("valuePresentationIdRef", IDRef.class).getRef();
			if (vpn != null) {
				setOffset(vpn.getAttribute("offset", IntAttr.class).getValue());
				setScale(vpn.getAttribute("scale", DoubleAttr.class).getValue());
				setNumberOfDecimals(vpn.getAttribute("numberOfDecimals", ByteAttr.class).getValue());
				setUnit(vpn.getAttribute("unitDesignator", StringAttr.class).getValue());
			}
		}

		/**
		 * Gets the process data variable.
		 *
		 * @return the process data variable
		 */
		public IsoXmlElement getProcessDataVariable() {
			return processDataVariable;
		}

		/**
		 * Gets the ddi.
		 *
		 * @return the ddi
		 */
		public int getDDI() {
			return ddi;
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		public int getValue() {
			return value;
		}
	}

}
