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

public class TimeLog extends Elem {
	public final String filename;
	private boolean isPositionUp = false;
	private int dlvcount = 0;
	private ByteBuffer buf = null;
	
	public TimeLog(Task parent, String filename) {
		super(parent, "TLG");
		this.filename = filename;
		e.setAttribute("A", filename);
		e.setAttribute("C", "1");
	}
	
	public TimeLog(Task parent) {
		super(parent, "TLG");
		this.filename = createFile();
		e.setAttribute("A", filename);
		e.setAttribute("C", "1");
	}
	
	public TimeLog setFileLength(long fileLength) {
		if(fileLength < 0 || fileLength > 4294967294L)
			throw new IllegalArgumentException("invalid file length");
		e.setAttribute("B", Long.toString(fileLength));
		return this;
	}
	
	public static class ValueDeclaration {
		public final int ddi;
		public final Device.DeviceElement det;
		
		public ValueDeclaration(int ddi, Device.DeviceElement det) {
			this.ddi = ddi;
			this.det = det;
		}

		@Override
		public int hashCode() {
			return Objects.hash(ddi, det.id);
		}

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
	
	public void writeXML(OutputStream out, boolean isPositionUp, Task.PositionStatus status, ValueDeclaration[] dlvs) 
			throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		if(dlvs.length > 255)
			throw new IllegalArgumentException("max 255 dlvs");
		int capacity = 0;
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
        		.newDocument();
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
		if(isPositionUp) {
			ptn.setAttribute("C", "");
			capacity += 4;
		}
		ptn.setAttribute("D", Integer.toString(status.number));
		
		this.dlvcount = dlvs.length;
		capacity += 1; // DLV count
		for(ValueDeclaration vd : dlvs) {
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
	
	public int writeTime(OutputStream out, TimedPosition tpos, Integer[] values) throws IOException {
		if(buf == null)
			throw new IllegalStateException("Write XML before writing times!");
		if(values.length > dlvcount)
			throw new IllegalArgumentException("value count doesn't match the set DLV count");
		buf.rewind();
		
		GpsTime time = new Task.GpsTime(tpos.time);
		buf.putInt((int)time.time);
		buf.putShort((short)time.date);
		buf.putInt((int)(tpos.latitude * 1e7));
		buf.putInt((int)(tpos.longitude * 1e7));
		if(isPositionUp)
			buf.putInt(Double.isNaN(tpos.altitude) ? 0 : (int)(tpos.altitude * 1e3));
		
		buf.put((byte)Stream.of(values).filter(Objects::nonNull).count());
		for(int i = 0; i < values.length; ++i) {
			if(values[i] != null) {
				buf.put((byte)i).putInt(values[i]);
			}
		}
		out.write(buf.array(), buf.arrayOffset(), buf.position());
		return buf.position();
	}
	
	public Helper helper() {
		return new Helper();
	}
	
	public static enum PropertyFlag {
		DEFAULT(1), SETTABLE(2), CONTROL_SOURCE(4);
		
		public final int number;
		private PropertyFlag(int number) {
			this.number = number;
		}
		
		public static Optional<PropertyFlag> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<PropertyFlag> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	public static enum TriggerFlag {
		TIME(1), DISTANCE(2), THRESHOLD(4), CHANGE(8), TOTAL(16);
		
		public final int number;
		private TriggerFlag(int number) {
			this.number = number;
		}
		
		public static Optional<TriggerFlag> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<TriggerFlag> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class FullValueDeclaration extends ValueDeclaration {
		public final Device dvc;
		
		private int property = 0, triggerMethods = 0;
		private String designator = null;
		
		private int offset = 0, numberOfDecimals = 0;
		private double scale = 1.;
		private String unit = null;

		public FullValueDeclaration(Device dvc, int ddi, Device.DeviceElement det) {
			super(ddi, det);
			this.dvc = dvc;
		}
		
		public FullValueDeclaration setProperty(PropertyFlag...flags) {
			int mask = 0;
			for(PropertyFlag f : flags) {
				mask |= f.number;
			}
			if((mask & PropertyFlag.SETTABLE.number) != 0 && (mask & PropertyFlag.CONTROL_SOURCE.number) != 0)
				throw new IllegalArgumentException("SETTABLE and CONTROL_STATE are mutally exclusive");
			this.property = mask;
			return this;
		}

		public FullValueDeclaration setTriggerMethods(TriggerFlag...flags) {
			int mask = 0;
			for(TriggerFlag f : flags) {
				mask |= f.number;
			}
			this.triggerMethods = mask;
			return this;
		}
		
		private static final int[] AVERAGES = { 98, 99, 100, 124, 125, 130, 140, 184, 
				191, 209, 212, 218, 219, 220, 262, 308, 309, 310, 311, 312, 313, 314, 
				315, 349, 406, 407, 408, 415, 416, 417, 418, 419, 420, 423, 424, 448, 
				449, 450, 451, 467, 488, 491, 501, 502, 503, 504, 516, 540, 550 };
		public boolean isTaskTotal() {
			return (property & PropertyFlag.SETTABLE.number) != 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0
					&& Arrays.binarySearch(AVERAGES, ddi) < 0;
		}
		
		public boolean isAverage() {
			return (property & PropertyFlag.SETTABLE.number) != 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0
					&& Arrays.binarySearch(AVERAGES, ddi) >= 0;
		}

		public boolean isLifetimeTotal() {
			return (property & PropertyFlag.SETTABLE.number) == 0 && (triggerMethods & TriggerFlag.TOTAL.number) != 0;
		}
		
		public FullValueDeclaration setDesignator(String designator) {
			this.designator = designator;
			return this;
		}

		public FullValueDeclaration setOffset(int offset) {
			this.offset = offset;
			return this;
		}

		public FullValueDeclaration setNumberOfDecimals(int numberOfDecimals) {
			this.numberOfDecimals = numberOfDecimals;
			return this;
		}

		public FullValueDeclaration setScale(double scale) {
			this.scale = scale;
			return this;
		}
		
		public FullValueDeclaration setUnit(String unit) {
			this.unit = unit;
			return this;
		}
		
		void writeDOM() {
			DeviceValuePresentation dvp = dvc.addOrGetDeviceValuePresentation(offset, scale, numberOfDecimals, unit);
			DeviceProcessData dpd = dvc.addOrGetDeviceProcessData(ddi, property, triggerMethods, designator, dvp);
			det.addDeviceProcessDataReference(dpd);
		}

		public int convert(double value) {
			return (int) Math.round(((Precision.round(value, numberOfDecimals) / scale) - offset));
		}
	}
	
	public class Helper {
		private List<FullValueDeclaration> dlvs = new ArrayList<>();
		private Integer[] intvalues = null;
		
		public Helper addValueDeclaration(FullValueDeclaration fvd) {
			dlvs.add(fvd);
			return this;
		}
		
		public void writeXML(OutputStream out, boolean isPositionUp, Task.PositionStatus status) 
				throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
			dlvs.forEach(fvd -> fvd.writeDOM());
			TimeLog.this.writeXML(out, isPositionUp, status, dlvs.toArray(new ValueDeclaration[dlvs.size()]));
			intvalues = new Integer[dlvs.size()];
		}
		
		public int writeTime(OutputStream out, TimedPosition tpos, Double[] values) throws IOException {
			if(dlvs.size() != dlvcount)
				throw new IllegalStateException("Write XML before writing times!");
			if(values.length > dlvcount)
				throw new IllegalArgumentException("value count doesn't match the set DLV count");
			
			for(int i = 0; i < values.length; ++i) {
				intvalues[i] = values[i] != null ? dlvs.get(i).convert(values[i]) : null;
			}
			
			return TimeLog.this.writeTime(out, tpos, intvalues);
		}
	}
}