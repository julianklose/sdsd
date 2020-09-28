package de.sdsd.projekt.parser.isoxml;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Map;
import java.util.RandomAccess;

import javax.annotation.CheckForNull;

import de.sdsd.projekt.parser.isoxml.Grid.TreatmentZone;

public class GridEntry extends AbstractList<GridValue> implements RandomAccess {
	private final double northMin, eastMin, northSize, eastSize;
	private final IsoXmlElement treatmentZone;
	private final GridValue[] values;
	private String error = null;
	
	// grid type 1
	GridEntry(double northMin, double eastMin, double northSize, double eastSize,
			Map<Integer, TreatmentZone> refs, ByteBuffer data) {
		this.northMin = northMin;
		this.eastMin = eastMin;
		this.northSize = northSize;
		this.eastSize = eastSize;
		
		TreatmentZone treatment = null;
		GridValue[] values = null;
		try {
			treatment = refs.get(Byte.toUnsignedInt(data.get()));
			if(treatment == null)
				error = "Input data invalid";
			else {
				values = treatment.stream()
						.map(GridValue::new)
						.toArray(GridValue[]::new);
			}
		} catch (BufferUnderflowException e) {
			error = "Input data incomplete";
		}
		this.treatmentZone = treatment != null ? treatment.tzn : null;
		this.values = values != null ? values : new GridValue[0];
	}
	
	// grid type 2
	GridEntry(double northMin, double eastMin, double northSize, double eastSize,
			TreatmentZone treatment, ByteBuffer data) {
		this.northMin = northMin;
		this.eastMin = eastMin;
		this.northSize = northSize;
		this.eastSize = eastSize;
		this.treatmentZone = treatment.tzn;
		this.values = new GridValue[treatment.size()];
		
		try {
			for(int i = 0; i < values.length; ++i) {
				values[i] = new GridValue(data.getInt(), treatment.get(i));
			}
		} catch (BufferUnderflowException e) {
			error = "Input data incomplete";
		}
	}
	
	public double getNorthMin() {
		return northMin;
	}
	
	public double getEastMin() {
		return eastMin;
	}
	
	public double getNorthMax() {
		return northMin + northSize;
	}
	
	public double getEastMax() {
		return eastMin + eastSize;
	}
	
	public double getNorthSize() {
		return northSize;
	}
	
	public double getEastSize() {
		return eastSize;
	}
	
	public IsoXmlElement getTreatmentZone() {
		return treatmentZone;
	}
	
	public boolean hasErrors() {
		return error != null;
	}
	
	public String getError() {
		return error;
	}
	
	public boolean hasValue() {
		for(int i = 0; i < values.length; ++i) {
			if(values[i].getValue() != 0) return true;
		}
		return false;
	}
	
	@CheckForNull
	public GridValue getFromDdi(int ddi) {
		for(GridValue v : values) {
			if(v.info.ddi == ddi)
				return v;
		}
		return null;
	}
	
	@CheckForNull
	public GridValue getFromUri(String valueUri) {
		for(GridValue v : values) {
			if(v.info.valueUri.equals(valueUri))
				return v;
		}
		return null;
	}
	
	@Override
	public GridValue get(int index) {
		return values[index];
	}

	@Override
	public int size() {
		return values.length;
	}

}
