package de.sdsd.projekt.api.isoxml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.math3.util.Precision;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.sdsd.projekt.api.ServiceResult.TimedPosition;
import de.sdsd.projekt.api.isoxml.Device.DeviceProcessData;
import de.sdsd.projekt.api.isoxml.Device.DeviceValuePresentation;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.Task.GpsTime;

/**
 * The Class TimeLog.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class TimeLog extends Elem {

	/** The filename. */
	public final String filename;

	/** The is position up. */
	private boolean isPositionUp = false;

	/** The dlvcount. */
	private int dlvcount = 0;

	/** The buf. */
	private ByteBuffer buf = null;

	/**
	 * Instantiates a new time log.
	 *
	 * @param parent   the parent
	 * @param filename the filename
	 */
	public TimeLog(Task parent, String filename) {
		super(parent, "TLG");
		this.filename = filename;
		e.setAttribute("A", filename);
		e.setAttribute("C", "1");
	}

	/**
	 * Instantiates a new time log.
	 *
	 * @param parent the parent
	 */
	public TimeLog(Task parent) {
		super(parent, "TLG");
		this.filename = createFile();
		e.setAttribute("A", filename);
		e.setAttribute("C", "1");
	}

	/**
	 * Sets the file length.
	 *
	 * @param fileLength the file length
	 * @return the time log
	 */
	public TimeLog setFileLength(long fileLength) {
		if (fileLength < 0 || fileLength > 4294967294L)
			throw new IllegalArgumentException("invalid file length");
		e.setAttribute("B", Long.toString(fileLength));
		return this;
	}

	/**
	 * The Class ValueDeclaration.
	 */
	public static class ValueDeclaration {

		/** The ddi. */
		public final int ddi;

		/** The det. */
		public final Device.DeviceElement det;

		/**
		 * Instantiates a new value declaration.
		 *
		 * @param ddi the ddi
		 * @param det the det
		 */
		public ValueDeclaration(int ddi, Device.DeviceElement det) {
			this.ddi = ddi;
			this.det = det;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(ddi, det.id);
		}

		/**
		 * Equals.
		 *
		 * @param obj the obj
		 * @return true, if successful
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ValueDeclaration))
				return false;
			ValueDeclaration other = (ValueDeclaration) obj;
			return ddi == other.ddi && Objects.equals(det.id, other.det.id);
		}
	}

	/**
	 * Write XML.
	 *
	 * @param out          the out
	 * @param isPositionUp the is position up
	 * @param status       the status
	 * @param dlvs         the dlvs
	 * @throws ParserConfigurationException         the parser configuration
	 *                                              exception
	 * @throws TransformerFactoryConfigurationError the transformer factory
	 *                                              configuration error
	 * @throws TransformerException                 the transformer exception
	 */
	public void writeXML(OutputStream out, boolean isPositionUp, Task.PositionStatus status, ValueDeclaration[] dlvs)
			throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		if (dlvs.length > 255)
			throw new IllegalArgumentException("max 255 dlvs");
		int capacity = 0;
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element tim = document.createElement("TIM");
		document.appendChild(tim);
		tim.setAttribute("A", "");
		tim.setAttribute("D", Integer.toString(Task.TimeType.EFFECTIVE.number));
		capacity += 6;

		Element ptn = document.createElement("PTN");
		tim.appendChild(ptn);
		ptn.setAttribute("A", "");
		capacity += 4;
		ptn.setAttribute("B", "");
		capacity += 4;
		this.isPositionUp = isPositionUp;
		if (isPositionUp) {
			ptn.setAttribute("C", "");
			capacity += 4;
		}
		ptn.setAttribute("D", Integer.toString(status.number));

		this.dlvcount = dlvs.length;
		capacity += 1; // DLV count
		for (ValueDeclaration vd : dlvs) {
			Element dlv = document.createElement("DLV");
			dlv.setAttribute("A", ddi(vd.ddi));
			dlv.setAttribute("B", "");
			dlv.setAttribute("C", vd.det.id);
			tim.appendChild(dlv);
			capacity += 5;
		}

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.transform(new DOMSource(document), new StreamResult(out));

		this.buf = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Write time.
	 *
	 * @param out    the out
	 * @param tpos   the tpos
	 * @param values the values
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public int writeTime(OutputStream out, TimedPosition tpos, Integer[] values) throws IOException {
		if (buf == null)
			throw new IllegalStateException("Write XML before writing times!");
		if (values.length > dlvcount)
			throw new IllegalArgumentException("value count doesn't match the set DLV count");
		buf.rewind();

		GpsTime time = new Task.GpsTime(tpos.time);
		buf.putInt((int) time.time);
		buf.putShort((short) time.date);
		buf.putInt((int) (tpos.latitude * 1e7));
		buf.putInt((int) (tpos.longitude * 1e7));
		if (isPositionUp)
			buf.putInt(Double.isNaN(tpos.altitude) ? 0 : (int) (tpos.altitude * 1e3));

		buf.put((byte) Stream.of(values).filter(Objects::nonNull).count());
		for (int i = 0; i < values.length; ++i) {
			if (values[i] != null) {
				buf.put((byte) i).putInt(values[i]);
			}
		}
		out.write(buf.array(), buf.arrayOffset(), buf.position());
		return buf.position();
	}

	/**
	 * Helper.
	 *
	 * @return the helper
	 */
	public Helper helper() {
		return new Helper();
	}

	/**
	 * The Enum PropertyFlag.
	 */
	public static enum PropertyFlag {

		/** The default. */
		DEFAULT(1),
		/** The settable. */
		SETTABLE(2),
		/** The control source. */
		CONTROL_SOURCE(4);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new property flag.
		 *
		 * @param number the number
		 */
		private PropertyFlag(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<PropertyFlag> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<PropertyFlag> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum TriggerFlag.
	 */
	public static enum TriggerFlag {

		/** The time. */
		TIME(1),
		/** The distance. */
		DISTANCE(2),
		/** The threshold. */
		THRESHOLD(4),
		/** The change. */
		CHANGE(8),
		/** The total. */
		TOTAL(16);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new trigger flag.
		 *
		 * @param number the number
		 */
		private TriggerFlag(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<TriggerFlag> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<TriggerFlag> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class FullValueDeclaration.
	 */
	public static class FullValueDeclaration extends ValueDeclaration {

		/** The dvc. */
		public final Device dvc;

		/** The trigger methods. */
		private int property = 0, triggerMethods = 0;

		/** The designator. */
		private String designator = null;

		/** The number of decimals. */
		private int offset = 0, numberOfDecimals = 0;

		/** The scale. */
		private double scale = 1.;

		/** The unit. */
		private String unit = null;

		/**
		 * Instantiates a new full value declaration.
		 *
		 * @param dvc the dvc
		 * @param ddi the ddi
		 * @param det the det
		 */
		public FullValueDeclaration(Device dvc, int ddi, Device.DeviceElement det) {
			super(ddi, det);
			this.dvc = dvc;
		}

		/**
		 * Sets the property.
		 *
		 * @param flags the flags
		 * @return the full value declaration
		 */
		public FullValueDeclaration setProperty(PropertyFlag... flags) {
			int mask = 0;
			for (PropertyFlag f : flags) {
				mask |= f.number;
			}
			if ((mask & PropertyFlag.SETTABLE.number) != 0 && (mask & PropertyFlag.CONTROL_SOURCE.number) != 0)
				throw new IllegalArgumentException("SETTABLE and CONTROL_STATE are mutally exclusive");
			this.property = mask;
			return this;
		}

		/**
		 * Sets the trigger methods.
		 *
		 * @param flags the flags
		 * @return the full value declaration
		 */
		public FullValueDeclaration setTriggerMethods(TriggerFlag... flags) {
			int mask = 0;
			for (TriggerFlag f : flags) {
				mask |= f.number;
			}
			this.triggerMethods = mask;
			return this;
		}

		/** The Constant AVERAGES. */
		private static final int[] AVERAGES = { 98, 99, 100, 124, 125, 130, 140, 184, 191, 209, 212, 218, 219, 220, 262,
				308, 309, 310, 311, 312, 313, 314, 315, 349, 406, 407, 408, 415, 416, 417, 418, 419, 420, 423, 424, 448,
				449, 450, 451, 467, 488, 491, 501, 502, 503, 504, 516, 540, 550 };

		/**
		 * Checks if is task total.
		 *
		 * @return true, if is task total
		 */
		public boolean isTaskTotal() {
			return (property & PropertyFlag.SETTABLE.number) != 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0
					&& Arrays.binarySearch(AVERAGES, ddi) < 0;
		}

		/**
		 * Checks if is average.
		 *
		 * @return true, if is average
		 */
		public boolean isAverage() {
			return (property & PropertyFlag.SETTABLE.number) != 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0
					&& Arrays.binarySearch(AVERAGES, ddi) >= 0;
		}

		/**
		 * Checks if is lifetime total.
		 *
		 * @return true, if is lifetime total
		 */
		public boolean isLifetimeTotal() {
			return (property & PropertyFlag.SETTABLE.number) == 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0;
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the full value declaration
		 */
		public FullValueDeclaration setDesignator(String designator) {
			this.designator = designator;
			return this;
		}

		/**
		 * Sets the offset.
		 *
		 * @param offset the offset
		 * @return the full value declaration
		 */
		public FullValueDeclaration setOffset(int offset) {
			this.offset = offset;
			return this;
		}

		/**
		 * Sets the number of decimals.
		 *
		 * @param numberOfDecimals the number of decimals
		 * @return the full value declaration
		 */
		public FullValueDeclaration setNumberOfDecimals(int numberOfDecimals) {
			this.numberOfDecimals = numberOfDecimals;
			return this;
		}

		/**
		 * Sets the scale.
		 *
		 * @param scale the scale
		 * @return the full value declaration
		 */
		public FullValueDeclaration setScale(double scale) {
			this.scale = scale;
			return this;
		}

		/**
		 * Sets the unit.
		 *
		 * @param unit the unit
		 * @return the full value declaration
		 */
		public FullValueDeclaration setUnit(String unit) {
			this.unit = unit;
			return this;
		}

		/**
		 * Write DOM.
		 */
		void writeDOM() {
			DeviceValuePresentation dvp = dvc.addOrGetDeviceValuePresentation(offset, scale, numberOfDecimals, unit);
			DeviceProcessData dpd = dvc.addOrGetDeviceProcessData(ddi, property, triggerMethods, designator, dvp);
			det.addDeviceProcessDataReference(dpd);
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
	 * The Class Helper.
	 */
	public class Helper {

		/** The dlvs. */
		private List<FullValueDeclaration> dlvs = new ArrayList<>();

		/** The intvalues. */
		private Integer[] intvalues = null;

		/**
		 * Adds the value declaration.
		 *
		 * @param fvd the fvd
		 * @return the helper
		 */
		public Helper addValueDeclaration(FullValueDeclaration fvd) {
			dlvs.add(fvd);
			return this;
		}

		/**
		 * Write XML.
		 *
		 * @param out          the out
		 * @param isPositionUp the is position up
		 * @param status       the status
		 * @throws ParserConfigurationException         the parser configuration
		 *                                              exception
		 * @throws TransformerFactoryConfigurationError the transformer factory
		 *                                              configuration error
		 * @throws TransformerException                 the transformer exception
		 */
		public void writeXML(OutputStream out, boolean isPositionUp, Task.PositionStatus status)
				throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
			dlvs.forEach(fvd -> fvd.writeDOM());
			TimeLog.this.writeXML(out, isPositionUp, status, dlvs.toArray(new ValueDeclaration[dlvs.size()]));
			intvalues = new Integer[dlvs.size()];
		}

		/**
		 * Write time.
		 *
		 * @param out    the out
		 * @param tpos   the tpos
		 * @param values the values
		 * @return the int
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public int writeTime(OutputStream out, TimedPosition tpos, Double[] values) throws IOException {
			if (dlvs.size() != dlvcount)
				throw new IllegalStateException("Write XML before writing times!");
			if (values.length > dlvcount)
				throw new IllegalArgumentException("value count doesn't match the set DLV count");

			for (int i = 0; i < values.length; ++i) {
				intvalues[i] = values[i] != null ? dlvs.get(i).convert(values[i]) : null;
			}

			return TimeLog.this.writeTime(out, tpos, intvalues);
		}
	}
}