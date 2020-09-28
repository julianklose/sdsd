package de.sdsd.projekt.api.isoxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.math3.util.Precision;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.ISO11783_TaskData;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.OIDElem;

public class Device extends Elem {
	private final List<DeviceProcessData> dpds = new ArrayList<>();
	private final List<DeviceValuePresentation> dvps = new ArrayList<>();

	public Device(ISO11783_TaskData parent, 
			long clientName, byte[] deviceStructureLabel, long deviceLocalizationLabel) {
		super(parent, "DVC");
		e.setAttribute("A", id);
		e.setAttribute("D", Long.toHexString(clientName).toUpperCase());
		if(deviceStructureLabel.length > 39)
			throw new IllegalArgumentException("invalid device structure label");
		e.setAttribute("F", Hex.encodeHexString(deviceStructureLabel, false));
		if(deviceLocalizationLabel > 0xFFFFFFFFFFFFFFL)
			throw new IllegalArgumentException("invalid device localization label");
		e.setAttribute("G", Long.toHexString(deviceLocalizationLabel).toUpperCase());
	}
	
	public Device(ISO11783_TaskData parent, 
			byte[] clientName, byte[] deviceStructureLabel, byte[] deviceLocalizationLabel) {
		super(parent, "DVC");
		e.setAttribute("A", id);
		if(clientName.length > 8)
			throw new IllegalArgumentException("invalid client name");
		e.setAttribute("D", Hex.encodeHexString(clientName, false));
		if(deviceStructureLabel.length > 39)
			throw new IllegalArgumentException("invalid device structure label");
		e.setAttribute("F", Hex.encodeHexString(deviceStructureLabel, false));
		if(deviceLocalizationLabel.length != 7)
			throw new IllegalArgumentException("invalid device localization label");
		e.setAttribute("G", Hex.encodeHexString(deviceLocalizationLabel, false));
	}
	
	public Device setDesignator(String designator) {
		e.setAttribute("B", designator);
		return this;
	}
	
	public Device setSoftwareVersion(String softwareVersion) {
		e.setAttribute("C", softwareVersion);
		return this;
	}
	
	public Device setSerialNumber(String serialNumber) {
		e.setAttribute("E", serialNumber);
		return this;
	}

	public DeviceElement addDeviceElement(DeviceElementType type, 
			int deviceElementNumber, @Nullable DeviceElement parentObject) {
		return new DeviceElement(this, type, deviceElementNumber, parentObject);
	}
	
	public DeviceProcessData addOrGetDeviceProcessData(int ddi, int property, int triggerMethods, String designator, DeviceValuePresentation dvp) {
		int hash = Objects.hash(property, triggerMethods, designator, dvp);
		for(DeviceProcessData dpd : dpds) {
			if(dpd.hashCode() == hash)
				return dpd;
		}
		return new DeviceProcessData(this, ddi, property, triggerMethods)
				.setDesignator(designator)
				.setDeviceValuePresentation(dvp);
	}
	
	public DeviceProcessData addDeviceProcessData(int ddi, int property, int triggerMethods) {
		return new DeviceProcessData(this, ddi, property, triggerMethods);
	}
	
	public DeviceProperty addDeviceProperty(int ddi, int value) {
		return new DeviceProperty(this, ddi, value);
	}
	
	public DeviceValuePresentation addOrGetDeviceValuePresentation(int offset, double scale, int numberOfDecimals, String unit) {
		int hash = Objects.hash(numberOfDecimals, offset, scale, unit);
		for(DeviceValuePresentation dvp : dvps) {
			if(dvp.hashCode() == hash)
				return dvp;
		}
		return new DeviceValuePresentation(this, offset, scale, numberOfDecimals)
				.setUnitDesignator(unit);
	}
	
	public DeviceValuePresentation addDeviceValuePresentation(int offset, double scale, int numberOfDecimals) {
		return new DeviceValuePresentation(this, offset, scale, numberOfDecimals);
	}
	
	public static class DeviceObjectReference extends Elem {
		private final DeviceProcessData dpd;
		private DeviceObjectReference(DeviceElement parent, OIDElem deviceObject) {
			super(parent, "DOR");
			e.setAttribute("A", deviceObject.oid);
			this.dpd = (deviceObject instanceof DeviceProcessData) ? (DeviceProcessData)deviceObject : null;
		}
		public DeviceObjectReference(DeviceElement parent, DeviceProcessData dpd) {
			this(parent, (OIDElem)dpd);
			parent.dors.add(this);
		}
		public DeviceObjectReference(DeviceElement parent, DeviceProperty dpt) {
			this(parent, (OIDElem)dpt);
		}
		
		int hash() {
			return dpd != null ? dpd.hashCode() : 0;
		}
	}
	
	public static enum DeviceElementType {
		DEVICE(1), FUNCTION(2), BIN(3), SECTION(4), UNIT(5), CONNECTOR(6), NAVIGATION(7);
		
		public final int number;
		private DeviceElementType(int number) {
			this.number = number;
		}
		
		public static Optional<DeviceElementType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<DeviceElementType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	public static class DeviceElement extends OIDElem {
		private final List<DeviceObjectReference> dors = new ArrayList<>();
		
		public DeviceElement(Device parent, DeviceElementType type, 
				int deviceElementNumber, @Nullable DeviceElement parentObject) {
			super(parent, "DET");
			e.setAttribute("A", id);
			e.setAttribute("B", oid);
			e.setAttribute("C", Integer.toString(type.number));
			if(deviceElementNumber < 0 || deviceElementNumber > 4095)
				throw new IllegalArgumentException("invalid device element number");
			e.setAttribute("E", Integer.toString(deviceElementNumber));
			e.setAttribute("F", parentObject != null ? parentObject.oid : "0");
		}
		
		public DeviceElement setDesignator(String designator) {
			e.setAttribute("D", designator);
			return this;
		}
		
		public DeviceObjectReference addDeviceProcessDataReference(DeviceProcessData dpd) {
			int hash = dpd.hashCode();
			for(DeviceObjectReference dor : dors) {
				if(dor.hash() == hash)
					return dor;
			}
			return new DeviceObjectReference(this, dpd);
		}
		
		public DeviceObjectReference addDevicePropertyReference(DeviceProperty dpt) {
			return new DeviceObjectReference(this, dpt);
		}
	}
	
	public static class DeviceProcessData extends OIDElem {
		public static int PROPERTY_DEFAULT = 1, PROPERTY_SETTABLE = 2, PROPERTY_CONTROL_SOURCE = 4;
		public static int TRIGGER_TIME = 1, TRIGGER_DISTANCE = 2, TRIGGER_THRESHOLD = 4, TRIGGER_CHANGE = 8, TRIGGER_TOTAL = 16;
		
		private final int property, triggerMethods;
		private String designator = null;
		private DeviceValuePresentation dvp = null;
		
		public DeviceProcessData(Device parent, 
				int ddi, int property, int triggerMethods) {
			super(parent, "DPD");
			e.setAttribute("A", oid);
			e.setAttribute("B", ddi(ddi));
			if(property < 0 || property > 7)
				throw new IllegalArgumentException("invalid property");
			if((property & PROPERTY_SETTABLE) != 0 && (property & PROPERTY_CONTROL_SOURCE) != 0)
				throw new IllegalArgumentException("PROPERTY_SETTABLE and PROPERTY_CONTROL_SOURCE are mutally exclusive");
			e.setAttribute("C", Integer.toString(property));
			if(triggerMethods < 0 || triggerMethods > 31)
				throw new IllegalArgumentException("invalid triggerMethods");
			e.setAttribute("D", Integer.toString(triggerMethods));
			this.property = property;
			this.triggerMethods = triggerMethods;
			parent.dpds.add(this);
		}
		
		public DeviceProcessData setDesignator(String designator) {
			e.setAttribute("E", designator);
			this.designator = designator;
			return this;
		}
		
		public DeviceProcessData setDeviceValuePresentation(DeviceValuePresentation dvp) {
			e.setAttribute("F", dvp.oid());
			this.dvp = dvp;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(property, triggerMethods, designator, dvp);
		}
	}
	
	public static class DeviceProperty extends OIDElem {
		public DeviceProperty(Device parent, int ddi, int value) {
			super(parent, "DPT");
			e.setAttribute("A", oid);
			e.setAttribute("B", ddi(ddi));
			e.setAttribute("C", Integer.toString(value));
		}
		
		public DeviceProperty setDesignator(String designator) {
			e.setAttribute("D", designator);
			return this;
		}
		
		public DeviceProperty setDeviceValuePresentation(DeviceValuePresentation dvp) {
			e.setAttribute("E", dvp.oid());
			return this;
		}
	}
	
	public static class DeviceValuePresentation extends OIDElem {
		private final int offset, numberOfDecimals;
		private final double scale;
		private String unit = null;
		
		public DeviceValuePresentation(Device parent, int offset, double scale, int numberOfDecimals) {
			super(parent, "DVP");
			e.setAttribute("A", oid);
			e.setAttribute("B", Integer.toString(offset));
			e.setAttribute("C", floating(scale));
			if(numberOfDecimals < 0 || numberOfDecimals > 7)
				throw new IllegalArgumentException("invalid number of decimals");
			e.setAttribute("D", Integer.toString(numberOfDecimals));
			this.offset = offset;
			this.scale = scale;
			this.numberOfDecimals = numberOfDecimals;
			parent.dvps.add(this);
		}
		
		public DeviceValuePresentation setUnitDesignator(String unit) {
			e.setAttribute("E", unit);
			this.unit = unit;
			return this;
		}
		
		public int convert(double value) {
			return (int) Math.round(((Precision.round(value, numberOfDecimals) / scale) - offset));
		}

		@Override
		public int hashCode() {
			return Objects.hash(numberOfDecimals, offset, scale, unit);
		}
	}
	
	public static class ValuePresentation extends Elem {
		private final int offset, numberOfDecimals;
		private final double scale;
		
		public ValuePresentation(ISO11783_TaskData parent, int offset, double scale, int numberOfDecimals) {
			super(parent, "VPN");
			e.setAttribute("A", id);
			e.setAttribute("B", Integer.toString(offset));
			e.setAttribute("C", floating(scale));
			if(numberOfDecimals < 0 || numberOfDecimals > 7)
				throw new IllegalArgumentException("invalid number of decimals");
			e.setAttribute("D", Integer.toString(numberOfDecimals));
			this.offset = offset;
			this.scale = scale;
			this.numberOfDecimals = numberOfDecimals;
		}
		
		public ValuePresentation setUnitDesignator(String unit) {
			e.setAttribute("E", unit);
			return this;
		}
		
		public ValuePresentation setColourLegend(ColourLegend cld) {
			e.setAttribute("F", cld.id);
			return this;
		}
		
		public int convert(double value) {
			return (int) Math.round(((Precision.round(value, numberOfDecimals) / scale) - offset));
		}
	}
	
	public static class ColourLegend extends Elem {
		public ColourLegend(ISO11783_TaskData parent) {
			super(parent, "CLD");
			e.setAttribute("A", id);
		}
		
		public ColourLegend setDefaultColour(int color) {
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("B", Integer.toString(color));
			return this;
		}
		
		public ColourRange addColourRange(int minValue, int maxValue, int color) {
			return new ColourRange(this, minValue, maxValue, color);
		}
	}
	
	public static class ColourRange extends Elem {
		public ColourRange(ColourLegend parent, int minValue, int maxValue, int color) {
			super(parent, "CRG");
			e.setAttribute("A", Integer.toString(minValue));
			e.setAttribute("B", Integer.toString(maxValue));
			if(color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("C", Integer.toString(color));
		}
	}
}
