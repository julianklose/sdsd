package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;

import org.xml.sax.SAXException;

import de.sdsd.projekt.parser.isoxml.Grid.ValueInfo;

/**
 * The Class GridValue.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class GridValue {

	/** The value. */
	public final int value;

	/** The info. */
	public final ValueInfo info;

	/**
	 * Instantiates a new grid value.
	 *
	 * @param value the value
	 * @param info  the info
	 */
	GridValue(int value, ValueInfo info) {
		this.value = value;
		this.info = info;
	}

	/**
	 * Instantiates a new grid value.
	 *
	 * @param info the info
	 */
	GridValue(ValueInfo info) {
		this(info.value, info);
	}

	/**
	 * Gets the info.
	 *
	 * @return the info
	 */
	public ValueInfo getInfo() {
		return info;
	}

	/**
	 * Gets the ddi.
	 *
	 * @return the ddi
	 */
	public int getDDI() {
		return info.ddi;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Gets the scaled value.
	 *
	 * @param index the index
	 * @return the scaled value
	 * @throws IOException  Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 */
	public double getScaledValue(int index) throws IOException, SAXException {
		return info.translateValue(value);
	}

	/**
	 * Gets the formatted value.
	 *
	 * @param index the index
	 * @return the formatted value
	 * @throws IOException  Signals that an I/O exception has occurred.
	 * @throws SAXException the SAX exception
	 */
	public String getFormattedValue(int index) throws IOException, SAXException {
		return info.formatValue(getScaledValue(index)) + ' ' + info.getUnit();
	}
}
