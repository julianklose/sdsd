package de.sdsd.projekt.parser.isoxml;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Map;
import java.util.RandomAccess;

import javax.annotation.CheckForNull;

import de.sdsd.projekt.parser.isoxml.Grid.TreatmentZone;

/**
 * The Class GridEntry.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class GridEntry extends AbstractList<GridValue> implements RandomAccess {

	/** The east size. */
	private final double northMin, eastMin, northSize, eastSize;

	/** The treatment zone. */
	private final IsoXmlElement treatmentZone;

	/** The values. */
	private final GridValue[] values;

	/** The error. */
	private String error = null;

	/**
	 * Instantiates a new grid entry.
	 *
	 * @param northMin  the north min
	 * @param eastMin   the east min
	 * @param northSize the north size
	 * @param eastSize  the east size
	 * @param refs      the refs
	 * @param data      the data
	 */
	// grid type 1
	GridEntry(double northMin, double eastMin, double northSize, double eastSize, Map<Integer, TreatmentZone> refs,
			ByteBuffer data) {
		this.northMin = northMin;
		this.eastMin = eastMin;
		this.northSize = northSize;
		this.eastSize = eastSize;

		TreatmentZone treatment = null;
		GridValue[] values = null;
		try {
			treatment = refs.get(Byte.toUnsignedInt(data.get()));
			if (treatment == null)
				error = "Input data invalid";
			else {
				values = treatment.stream().map(GridValue::new).toArray(GridValue[]::new);
			}
		} catch (BufferUnderflowException e) {
			error = "Input data incomplete";
		}
		this.treatmentZone = treatment != null ? treatment.tzn : null;
		this.values = values != null ? values : new GridValue[0];
	}

	/**
	 * Instantiates a new grid entry.
	 *
	 * @param northMin  the north min
	 * @param eastMin   the east min
	 * @param northSize the north size
	 * @param eastSize  the east size
	 * @param treatment the treatment
	 * @param data      the data
	 */
	// grid type 2
	GridEntry(double northMin, double eastMin, double northSize, double eastSize, TreatmentZone treatment,
			ByteBuffer data) {
		this.northMin = northMin;
		this.eastMin = eastMin;
		this.northSize = northSize;
		this.eastSize = eastSize;
		this.treatmentZone = treatment.tzn;
		this.values = new GridValue[treatment.size()];

		try {
			for (int i = 0; i < values.length; ++i) {
				values[i] = new GridValue(data.getInt(), treatment.get(i));
			}
		} catch (BufferUnderflowException e) {
			error = "Input data incomplete";
		}
	}

	/**
	 * Gets the north min.
	 *
	 * @return the north min
	 */
	public double getNorthMin() {
		return northMin;
	}

	/**
	 * Gets the east min.
	 *
	 * @return the east min
	 */
	public double getEastMin() {
		return eastMin;
	}

	/**
	 * Gets the north max.
	 *
	 * @return the north max
	 */
	public double getNorthMax() {
		return northMin + northSize;
	}

	/**
	 * Gets the east max.
	 *
	 * @return the east max
	 */
	public double getEastMax() {
		return eastMin + eastSize;
	}

	/**
	 * Gets the north size.
	 *
	 * @return the north size
	 */
	public double getNorthSize() {
		return northSize;
	}

	/**
	 * Gets the east size.
	 *
	 * @return the east size
	 */
	public double getEastSize() {
		return eastSize;
	}

	/**
	 * Gets the treatment zone.
	 *
	 * @return the treatment zone
	 */
	public IsoXmlElement getTreatmentZone() {
		return treatmentZone;
	}

	/**
	 * Checks for errors.
	 *
	 * @return true, if successful
	 */
	public boolean hasErrors() {
		return error != null;
	}

	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * Checks for value.
	 *
	 * @return true, if successful
	 */
	public boolean hasValue() {
		for (int i = 0; i < values.length; ++i) {
			if (values[i].getValue() != 0)
				return true;
		}
		return false;
	}

	/**
	 * Gets the from ddi.
	 *
	 * @param ddi the ddi
	 * @return the from ddi
	 */
	@CheckForNull
	public GridValue getFromDdi(int ddi) {
		for (GridValue v : values) {
			if (v.info.ddi == ddi)
				return v;
		}
		return null;
	}

	/**
	 * Gets the from uri.
	 *
	 * @param valueUri the value uri
	 * @return the from uri
	 */
	@CheckForNull
	public GridValue getFromUri(String valueUri) {
		for (GridValue v : values) {
			if (v.info.valueUri.equals(valueUri))
				return v;
		}
		return null;
	}

	/**
	 * Gets the.
	 *
	 * @param index the index
	 * @return the grid value
	 */
	@Override
	public GridValue get(int index) {
		return values[index];
	}

	/**
	 * Size.
	 *
	 * @return the int
	 */
	@Override
	public int size() {
		return values.length;
	}

}
