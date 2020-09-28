package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;

import org.xml.sax.SAXException;

import de.sdsd.projekt.parser.isoxml.Grid.ValueInfo;

public class GridValue {
	public final int value;
	public final ValueInfo info;
	
	GridValue(int value, ValueInfo info) {
		this.value = value;
		this.info = info;
	}
	GridValue(ValueInfo info) {
		this(info.value, info);
	}
	
	public ValueInfo getInfo() {
		return info;
	}
	
	public int getDDI() {
		return info.ddi;
	}
	
	public int getValue() {
		return value;
	}
	
	public double getScaledValue(int index) throws IOException, SAXException {
		return info.translateValue(value);
	}
	
	public String getFormattedValue(int index) throws IOException, SAXException {
		return info.formatValue(getScaledValue(index)) + ' ' + info.getUnit();
	}
}
