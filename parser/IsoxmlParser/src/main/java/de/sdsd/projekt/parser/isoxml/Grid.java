package de.sdsd.projekt.parser.isoxml;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import de.sdsd.projekt.api.ParserAPI;
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

public class Grid extends AbstractList<GridEntry> implements RandomAccess {
	
	private final ParserAPI.Grid grid;
	
	private final int rowCount, columnCount;
	
	private final int type;
	private final Map<Integer, TreatmentZone> refs;
	private final GridEntry[][] treatment;

	public Grid(IsoXmlElement grd, byte[] content) {
		grid = new ParserAPI.Grid(grd.getUris().get(0), 
				grd.getAttribute("filename", StringAttr.class).getValue(),
				grd.getAttribute("gridMinimumNorthPosition", DoubleAttr.class).getValue(),
				grd.getAttribute("gridMinimumEastPosition", DoubleAttr.class).getValue(),
				grd.getAttribute("gridCellNorthSize", DoubleAttr.class).getValue(),
				grd.getAttribute("gridCellEastSize", DoubleAttr.class).getValue());
		
		rowCount = grd.getAttribute("gridMaximumRow", ULongAttr.class).getValue().intValue();
		columnCount = grd.getAttribute("gridMaximumColumn", ULongAttr.class).getValue().intValue();
		treatment = new GridEntry[rowCount][columnCount];
		
		ByteBuffer data = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		type = grd.getAttribute("gridType", EnumAttr.class).number();
		if(type == 1) {
			refs = new HashMap<>();
			IsoXmlElement tsk = grd.getParent();
			if(tsk != null) {
				for(IsoXmlElement e : tsk.getChildren()) {
					if(e.getTag().equals("TZN"))
						refs.put(e.getAttribute("treatmentZoneCode", OIDAttr.class).getValue(), new TreatmentZone(e, false));
				}
			}
			if(refs.isEmpty())
				throw new IllegalArgumentException("No TreatmentZones for GridType 1");
			
			for(int row = 0; row < rowCount; ++row) {
				for(int col = 0; col < columnCount; ++col) {
					treatment[row][col] = new GridEntry(grid.northMin + row * grid.northCellSize, grid.eastMin + col * grid.eastCellSize, 
							grid.northCellSize, grid.eastCellSize, refs, data);
				}
			}
		}
		else if(type == 2) {
			IsoXmlElement tzn = grd.getAttribute("treatmentZoneCode", OIDRef.class).getRef();
			if(tzn == null) throw new IllegalArgumentException("GridType 2 without valid TreatmentZone");
			TreatmentZone tr = new TreatmentZone(tzn, true);
			refs = Collections.singletonMap(tzn.getAttribute("treatmentZoneCode", OIDAttr.class).getValue(), tr);
			
			for(int row = 0; row < rowCount; ++row) {
				for(int col = 0; col < columnCount; ++col) {
					treatment[row][col] = new GridEntry(grid.northMin + row * grid.northCellSize, grid.eastMin + col * grid.eastCellSize, 
							grid.northCellSize, grid.eastCellSize, tr, data);
				}
			}
		}
		else
			throw new IllegalArgumentException("Unknown GridType " + type);
	}
	
	public ParserAPI.Grid getGrid() {
		return grid;
	}

	public int getRowCount() {
		return rowCount;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public int getType() {
		return type;
	}
	
	public Collection<TreatmentZone> getTreatmentZones() {
		return Collections.unmodifiableCollection(refs.values());
	}
	
	public GridEntry get(int row, int col) {
		return treatment[row][col];
	}

	@Override
	public GridEntry get(int index) {
		return treatment[index / columnCount][index % columnCount];
	}

	@Override
	public int size() {
		return rowCount * columnCount;
	}
	
	public List<String> getAllErrors() {
		List<String> errors = new ArrayList<>();
		for(int row = 0; row < rowCount; ++row) {
			for(int col = 0; col < columnCount; ++col) {
				GridEntry ge = treatment[row][col];
				if(ge.hasErrors())
					errors.add(String.format("%s [%d,%d]: %s", grid.name, row, col, ge.getError()));
			}
		}
		return errors;
	}
	
	public class TreatmentZone extends AbstractList<ValueInfo> {
		public final IsoXmlElement tzn;
		private final ValueInfo[] valueInfos;
		
		TreatmentZone(IsoXmlElement tzn, boolean onlyEmpty) {
			this.tzn = tzn;
			this.valueInfos = tzn.findChildren("PDV").stream()
					.map(ValueInfo::new)
					.filter(v -> !onlyEmpty || v.value == 0)
					.toArray(ValueInfo[]::new);
		}
		
		public IsoXmlElement getTZN() {
			return tzn;
		}

		@Override
		public ValueInfo get(int index) {
			return valueInfos[index];
		}

		@Override
		public int size() {
			return valueInfos.length;
		}
	}
	
	public class ValueInfo extends ParserAPI.ValueInfo {
		public final IsoXmlElement processDataVariable;
		public final int ddi, value;
		
		ValueInfo(IsoXmlElement pdv) {
			super(pdv.getUris().get(0));
			addGrid(grid);
			
			this.processDataVariable = pdv;
			this.ddi = pdv.getAttribute("processDataDdi", DDIAttr.class).getValue();
			this.value = pdv.getAttribute("processDataValue", IntAttr.class).getValue();
			IsoXmlElement vpn = pdv.getAttribute("valuePresentationIdRef", IDRef.class).getRef();
			if(vpn != null) {
				setOffset(vpn.getAttribute("offset", IntAttr.class).getValue());
				setScale(vpn.getAttribute("scale", DoubleAttr.class).getValue());
				setNumberOfDecimals(vpn.getAttribute("numberOfDecimals", ByteAttr.class).getValue());
				setUnit(vpn.getAttribute("unitDesignator", StringAttr.class).getValue());
			}
		}

		public IsoXmlElement getProcessDataVariable() {
			return processDataVariable;
		}

		public int getDDI() {
			return ddi;
		}
		
		public int getValue() {
			return value;
		}
	}

}
