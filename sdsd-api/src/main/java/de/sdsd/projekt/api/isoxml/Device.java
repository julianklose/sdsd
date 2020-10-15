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

/**
 * The Class Device.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class Device extends Elem {

	/** The dpds. */
	private final List<DeviceProcessData> dpds = new ArrayList<>();

	/** The dvps. */
	private final List<DeviceValuePresentation> dvps = new ArrayList<>();

	/**
	 * Instantiates a new device.
	 *
	 * @param parent                  the parent
	 * @param clientName              the client name
	 * @param deviceStructureLabel    the device structure label
	 * @param deviceLocalizationLabel the device localization label
	 */
	public Device(ISO11783_TaskData parent, long clientName, byte[] deviceStructureLabel,
			long deviceLocalizationLabel) {
		super(parent, "DVC");
		e.setAttribute("A", id);
		e.setAttribute("D", Long.toHexString(clientName).toUpperCase());
		if (deviceStructureLabel.length > 39)
			throw new IllegalArgumentException("invalid device structure label");
		e.setAttribute("F", Hex.encodeHexString(deviceStructureLabel, false));
		if (deviceLocalizationLabel > 0xFFFFFFFFFFFFFFL)
			throw new IllegalArgumentException("invalid device localization label");
		e.setAttribute("G", Long.toHexString(deviceLocalizationLabel).toUpperCase());
	}

	/**
	 * Instantiates a new device.
	 *
	 * @param parent                  the parent
	 * @param clientName              the client name
	 * @param deviceStructureLabel    the device structure label
	 * @param deviceLocalizationLabel the device localization label
	 */
	public Device(ISO11783_TaskData parent, byte[] clientName, byte[] deviceStructureLabel,
			byte[] deviceLocalizationLabel) {
		super(parent, "DVC");
		e.setAttribute("A", id);
		if (clientName.length > 8)
			throw new IllegalArgumentException("invalid client name");
		e.setAttribute("D", Hex.encodeHexString(clientName, false));
		if (deviceStructureLabel.length > 39)
			throw new IllegalArgumentException("invalid device structure label");
		e.setAttribute("F", Hex.encodeHexString(deviceStructureLabel, false));
		if (deviceLocalizationLabel.length != 7)
			throw new IllegalArgumentException("invalid device localization label");
		e.setAttribute("G", Hex.encodeHexString(deviceLocalizationLabel, false));
	}

	/**
	 * Sets the designator.
	 *
	 * @param designator the designator
	 * @return the device
	 */
	public Device setDesignator(String designator) {
		e.setAttribute("B", designator);
		return this;
	}

	/**
	 * Sets the software version.
	 *
	 * @param softwareVersion the software version
	 * @return the device
	 */
	public Device setSoftwareVersion(String softwareVersion) {
		e.setAttribute("C", softwareVersion);
		return this;
	}

	/**
	 * Sets the serial number.
	 *
	 * @param serialNumber the serial number
	 * @return the device
	 */
	public Device setSerialNumber(String serialNumber) {
		e.setAttribute("E", serialNumber);
		return this;
	}

	/**
	 * Adds the device element.
	 *
	 * @param type                the type
	 * @param deviceElementNumber the device element number
	 * @param parentObject        the parent object
	 * @return the device element
	 */
	public DeviceElement addDeviceElement(DeviceElementType type, int deviceElementNumber,
			@Nullable DeviceElement parentObject) {
		return new DeviceElement(this, type, deviceElementNumber, parentObject);
	}

	/**
	 * Adds the or get device process data.
	 *
	 * @param ddi            the ddi
	 * @param property       the property
	 * @param triggerMethods the trigger methods
	 * @param designator     the designator
	 * @param dvp            the dvp
	 * @return the device process data
	 */
	public DeviceProcessData addOrGetDeviceProcessData(int ddi, int property, int triggerMethods, String designator,
			DeviceValuePresentation dvp) {
		int hash = Objects.hash(property, triggerMethods, designator, dvp);
		for (DeviceProcessData dpd : dpds) {
			if (dpd.hashCode() == hash)
				return dpd;
		}
		return new DeviceProcessData(this, ddi, property, triggerMethods).setDesignator(designator)
				.setDeviceValuePresentation(dvp);
	}

	/**
	 * Adds the device process data.
	 *
	 * @param ddi            the ddi
	 * @param property       the property
	 * @param triggerMethods the trigger methods
	 * @return the device process data
	 */
	public DeviceProcessData addDeviceProcessData(int ddi, int property, int triggerMethods) {
		return new DeviceProcessData(this, ddi, property, triggerMethods);
	}

	/**
	 * Adds the device property.
	 *
	 * @param ddi   the ddi
	 * @param value the value
	 * @return the device property
	 */
	public DeviceProperty addDeviceProperty(int ddi, int value) {
		return new DeviceProperty(this, ddi, value);
	}

	/**
	 * Adds the or get device value presentation.
	 *
	 * @param offset           the offset
	 * @param scale            the scale
	 * @param numberOfDecimals the number of decimals
	 * @param unit             the unit
	 * @return the device value presentation
	 */
	public DeviceValuePresentation addOrGetDeviceValuePresentation(int offset, double scale, int numberOfDecimals,
			String unit) {
		int hash = Objects.hash(numberOfDecimals, offset, scale, unit);
		for (DeviceValuePresentation dvp : dvps) {
			if (dvp.hashCode() == hash)
				return dvp;
		}
		return new DeviceValuePresentation(this, offset, scale, numberOfDecimals).setUnitDesignator(unit);
	}

	/**
	 * Adds the device value presentation.
	 *
	 * @param offset           the offset
	 * @param scale            the scale
	 * @param numberOfDecimals the number of decimals
	 * @return the device value presentation
	 */
	public DeviceValuePresentation addDeviceValuePresentation(int offset, double scale, int numberOfDecimals) {
		return new DeviceValuePresentation(this, offset, scale, numberOfDecimals);
	}

	/**
	 * The Class DeviceObjectReference.
	 */
	public static class DeviceObjectReference extends Elem {

		/** The dpd. */
		private final DeviceProcessData dpd;

		/**
		 * Instantiates a new device object reference.
		 *
		 * @param parent       the parent
		 * @param deviceObject the device object
		 */
		private DeviceObjectReference(DeviceElement parent, OIDElem deviceObject) {
			super(parent, "DOR");
			e.setAttribute("A", deviceObject.oid);
			this.dpd = (deviceObject instanceof DeviceProcessData) ? (DeviceProcessData) deviceObject : null;
		}

		/**
		 * Instantiates a new device object reference.
		 *
		 * @param parent the parent
		 * @param dpd    the dpd
		 */
		public DeviceObjectReference(DeviceElement parent, DeviceProcessData dpd) {
			this(parent, (OIDElem) dpd);
			parent.dors.add(this);
		}

		/**
		 * Instantiates a new device object reference.
		 *
		 * @param parent the parent
		 * @param dpt    the dpt
		 */
		public DeviceObjectReference(DeviceElement parent, DeviceProperty dpt) {
			this(parent, (OIDElem) dpt);
		}

		/**
		 * Hash.
		 *
		 * @return the int
		 */
		int hash() {
			return dpd != null ? dpd.hashCode() : 0;
		}
	}

	/**
	 * The Enum DeviceElementType.
	 */
	public static enum DeviceElementType {

		/** The device. */
		DEVICE(1),
		/** The function. */
		FUNCTION(2),
		/** The bin. */
		BIN(3),
		/** The section. */
		SECTION(4),
		/** The unit. */
		UNIT(5),
		/** The connector. */
		CONNECTOR(6),
		/** The navigation. */
		NAVIGATION(7);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new device element type.
		 *
		 * @param number the number
		 */
		private DeviceElementType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<DeviceElementType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<DeviceElementType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class DeviceElement.
	 */
	public static class DeviceElement extends OIDElem {

		/** The dors. */
		private final List<DeviceObjectReference> dors = new ArrayList<>();

		/**
		 * Instantiates a new device element.
		 *
		 * @param parent              the parent
		 * @param type                the type
		 * @param deviceElementNumber the device element number
		 * @param parentObject        the parent object
		 */
		public DeviceElement(Device parent, DeviceElementType type, int deviceElementNumber,
				@Nullable DeviceElement parentObject) {
			super(parent, "DET");
			e.setAttribute("A", id);
			e.setAttribute("B", oid);
			e.setAttribute("C", Integer.toString(type.number));
			if (deviceElementNumber < 0 || deviceElementNumber > 4095)
				throw new IllegalArgumentException("invalid device element number");
			e.setAttribute("E", Integer.toString(deviceElementNumber));
			e.setAttribute("F", parentObject != null ? parentObject.oid : "0");
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the device element
		 */
		public DeviceElement setDesignator(String designator) {
			e.setAttribute("D", designator);
			return this;
		}

		/**
		 * Adds the device process data reference.
		 *
		 * @param dpd the dpd
		 * @return the device object reference
		 */
		public DeviceObjectReference addDeviceProcessDataReference(DeviceProcessData dpd) {
			int hash = dpd.hashCode();
			for (DeviceObjectReference dor : dors) {
				if (dor.hash() == hash)
					return dor;
			}
			return new DeviceObjectReference(this, dpd);
		}

		/**
		 * Adds the device property reference.
		 *
		 * @param dpt the dpt
		 * @return the device object reference
		 */
		public DeviceObjectReference addDevicePropertyReference(DeviceProperty dpt) {
			return new DeviceObjectReference(this, dpt);
		}
	}

	/**
	 * The Class DeviceProcessData.
	 */
	public static class DeviceProcessData extends OIDElem {

		/** The property control source. */
		public static int PROPERTY_DEFAULT = 1, PROPERTY_SETTABLE = 2, PROPERTY_CONTROL_SOURCE = 4;

		/** The trigger total. */
		public static int TRIGGER_TIME = 1, TRIGGER_DISTANCE = 2, TRIGGER_THRESHOLD = 4, TRIGGER_CHANGE = 8,
				TRIGGER_TOTAL = 16;

		/** The trigger methods. */
		private final int property, triggerMethods;

		/** The designator. */
		private String designator = null;

		/** The dvp. */
		private DeviceValuePresentation dvp = null;

		/**
		 * Instantiates a new device process data.
		 *
		 * @param parent         the parent
		 * @param ddi            the ddi
		 * @param property       the property
		 * @param triggerMethods the trigger methods
		 */
		public DeviceProcessData(Device parent, int ddi, int property, int triggerMethods) {
			super(parent, "DPD");
			e.setAttribute("A", oid);
			e.setAttribute("B", ddi(ddi));
			if (property < 0 || property > 7)
				throw new IllegalArgumentException("invalid property");
			if ((property & PROPERTY_SETTABLE) != 0 && (property & PROPERTY_CONTROL_SOURCE) != 0)
				throw new IllegalArgumentException(
						"PROPERTY_SETTABLE and PROPERTY_CONTROL_SOURCE are mutally exclusive");
			e.setAttribute("C", Integer.toString(property));
			if (triggerMethods < 0 || triggerMethods > 31)
				throw new IllegalArgumentException("invalid triggerMethods");
			e.setAttribute("D", Integer.toString(triggerMethods));
			this.property = property;
			this.triggerMethods = triggerMethods;
			parent.dpds.add(this);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the device process data
		 */
		public DeviceProcessData setDesignator(String designator) {
			e.setAttribute("E", designator);
			this.designator = designator;
			return this;
		}

		/**
		 * Sets the device value presentation.
		 *
		 * @param dvp the dvp
		 * @return the device process data
		 */
		public DeviceProcessData setDeviceValuePresentation(DeviceValuePresentation dvp) {
			e.setAttribute("F", dvp.oid());
			this.dvp = dvp;
			return this;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(property, triggerMethods, designator, dvp);
		}
	}

	/**
	 * The Class DeviceProperty.
	 */
	public static class DeviceProperty extends OIDElem {

		/**
		 * Instantiates a new device property.
		 *
		 * @param parent the parent
		 * @param ddi    the ddi
		 * @param value  the value
		 */
		public DeviceProperty(Device parent, int ddi, int value) {
			super(parent, "DPT");
			e.setAttribute("A", oid);
			e.setAttribute("B", ddi(ddi));
			e.setAttribute("C", Integer.toString(value));
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the device property
		 */
		public DeviceProperty setDesignator(String designator) {
			e.setAttribute("D", designator);
			return this;
		}

		/**
		 * Sets the device value presentation.
		 *
		 * @param dvp the dvp
		 * @return the device property
		 */
		public DeviceProperty setDeviceValuePresentation(DeviceValuePresentation dvp) {
			e.setAttribute("E", dvp.oid());
			return this;
		}
	}

	/**
	 * The Class DeviceValuePresentation.
	 */
	public static class DeviceValuePresentation extends OIDElem {

		/** The number of decimals. */
		private final int offset, numberOfDecimals;

		/** The scale. */
		private final double scale;

		/** The unit. */
		private String unit = null;

		/**
		 * Instantiates a new device value presentation.
		 *
		 * @param parent           the parent
		 * @param offset           the offset
		 * @param scale            the scale
		 * @param numberOfDecimals the number of decimals
		 */
		public DeviceValuePresentation(Device parent, int offset, double scale, int numberOfDecimals) {
			super(parent, "DVP");
			e.setAttribute("A", oid);
			e.setAttribute("B", Integer.toString(offset));
			e.setAttribute("C", floating(scale));
			if (numberOfDecimals < 0 || numberOfDecimals > 7)
				throw new IllegalArgumentException("invalid number of decimals");
			e.setAttribute("D", Integer.toString(numberOfDecimals));
			this.offset = offset;
			this.scale = scale;
			this.numberOfDecimals = numberOfDecimals;
			parent.dvps.add(this);
		}

		/**
		 * Sets the unit designator.
		 *
		 * @param unit the unit
		 * @return the device value presentation
		 */
		public DeviceValuePresentation setUnitDesignator(String unit) {
			e.setAttribute("E", unit);
			this.unit = unit;
			return this;
		}

		/**
		 * Convert.
		 *
		 * @param value the value
		 * @return the int
		 */
		public int convert(double value) {
			return (int) Math.round(((Precision.round(value, numberOfDecimals) / scale) - offset));
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(numberOfDecimals, offset, scale, unit);
		}
	}

	/**
	 * The Class ValuePresentation.
	 */
	public static class ValuePresentation extends Elem {

		/** The number of decimals. */
		private final int offset, numberOfDecimals;

		/** The scale. */
		private final double scale;

		/**
		 * Instantiates a new value presentation.
		 *
		 * @param parent           the parent
		 * @param offset           the offset
		 * @param scale            the scale
		 * @param numberOfDecimals the number of decimals
		 */
		public ValuePresentation(ISO11783_TaskData parent, int offset, double scale, int numberOfDecimals) {
			super(parent, "VPN");
			e.setAttribute("A", id);
			e.setAttribute("B", Integer.toString(offset));
			e.setAttribute("C", floating(scale));
			if (numberOfDecimals < 0 || numberOfDecimals > 7)
				throw new IllegalArgumentException("invalid number of decimals");
			e.setAttribute("D", Integer.toString(numberOfDecimals));
			this.offset = offset;
			this.scale = scale;
			this.numberOfDecimals = numberOfDecimals;
		}

		/**
		 * Sets the unit designator.
		 *
		 * @param unit the unit
		 * @return the value presentation
		 */
		public ValuePresentation setUnitDesignator(String unit) {
			e.setAttribute("E", unit);
			return this;
		}

		/**
		 * Sets the colour legend.
		 *
		 * @param cld the cld
		 * @return the value presentation
		 */
		public ValuePresentation setColourLegend(ColourLegend cld) {
			e.setAttribute("F", cld.id);
			return this;
		}

		/**
		 * Convert.
		 *
		 * @param value the value
		 * @return the int
		 */
		public int convert(double value) {
			return (int) Math.round(((Precision.round(value, numberOfDecimals) / scale) - offset));
		}
	}

	/**
	 * The Class ColourLegend.
	 */
	public static class ColourLegend extends Elem {

		/**
		 * Instantiates a new colour legend.
		 *
		 * @param parent the parent
		 */
		public ColourLegend(ISO11783_TaskData parent) {
			super(parent, "CLD");
			e.setAttribute("A", id);
		}

		/**
		 * Sets the default colour.
		 *
		 * @param color the color
		 * @return the colour legend
		 */
		public ColourLegend setDefaultColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("B", Integer.toString(color));
			return this;
		}

		/**
		 * Adds the colour range.
		 *
		 * @param minValue the min value
		 * @param maxValue the max value
		 * @param color    the color
		 * @return the colour range
		 */
		public ColourRange addColourRange(int minValue, int maxValue, int color) {
			return new ColourRange(this, minValue, maxValue, color);
		}
	}

	/**
	 * The Class ColourRange.
	 */
	public static class ColourRange extends Elem {

		/**
		 * Instantiates a new colour range.
		 *
		 * @param parent   the parent
		 * @param minValue the min value
		 * @param maxValue the max value
		 * @param color    the color
		 */
		public ColourRange(ColourLegend parent, int minValue, int maxValue, int color) {
			super(parent, "CRG");
			e.setAttribute("A", Integer.toString(minValue));
			e.setAttribute("B", Integer.toString(maxValue));
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("C", Integer.toString(color));
		}
	}
}
