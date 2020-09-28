package de.sdsd.projekt.parser.isoxml;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xml.sax.SAXException;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.ValueInfo;
import de.sdsd.projekt.parser.isoxml.Attribute.AttrType;
import de.sdsd.projekt.parser.isoxml.Attribute.ByteAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DDIAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DoubleAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.IntAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.UShortAttr;
import de.sdsd.projekt.parser.isoxml.RefAttr.IDRef;
import de.sdsd.projekt.parser.isoxml.RefAttr.OIDRef;

/**
 * Class for reading binary timelogs.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class TimeLog extends AbstractList<TimeLogEntry> implements RandomAccess {
	private final ArrayList<TimeLogEntry> entries = new ArrayList<>();
	private final TimeLogConfig config;
	private final IsoXmlElement tlg;
	private final ParserAPI.TimeLog timelog;
	
	static boolean isHeaderRead(Attribute<?> attr) {
		return attr.hasValue() && attr.getStringValue().isEmpty();
	}
	
	static int headerByteCount(Attribute<?> attr) {
		switch(attr.getType()) {
		case BYTE:
		case ENUM:
			return 1;
		case INT:
			return 4;
		case ULONG:
			return 4;
		case USHORT:
		case DDI:
			return 2;
		case DOUBLE:
			return 4;
		case DECIMAL:
			return 2;
		case DATETIME:
			return 6;
		default:
			throw new IllegalArgumentException("No specific length for the given attribute");
		}
	}
	
	
	static Object headerRead(Attribute<?> attr, ByteBuffer head) throws BufferUnderflowException, IllegalArgumentException {
		if(isHeaderRead(attr)) {
			switch(attr.getType()) {
			case BYTE:
			case ENUM:
				return head.get();
			case INT:
				return head.getInt();
			case ULONG:
				return Integer.toUnsignedLong(head.getInt());
			case USHORT:
			case DDI:
				return Short.toUnsignedInt(head.getShort());
			case DOUBLE:
				return head.getInt() * 1e-7;
			case DECIMAL:
				return head.getShort() * 1e-1F;
			case DATETIME: {
				int ms = head.getInt();
				int days = Short.toUnsignedInt(head.getShort());
				return new GpsTime(ms, days).toLocalInstant();
			}
			default:
				throw new IllegalArgumentException("No specific length for the given attribute");
			}
		}
		else
			return attr.getType() == AttrType.ENUM ? ((EnumAttr)attr).number() : attr.getValue();
	}
	
	public static class GpsTime {
		private static final LocalDateTime gpsTimeStart = LocalDateTime.of(1980, 1, 1, 0, 0, 0, 0);
		
		public final int days, ms;
		public GpsTime(int ms, int days) {
			this.days = days;
			this.ms = ms;
		}
		
		public Instant toInstant(ZoneId zoneId) {
			return gpsTimeStart.plusDays(days).plusNanos(ms * 1000000L).atZone(zoneId).toInstant();
		}
		
		public Instant toLocalInstant() {
			return toInstant(ZoneId.systemDefault());
		}
	}
	
	public static GpsTime findNextValidTime(ByteBuffer data, @Nullable GpsTime last) throws BufferUnderflowException {
		if(last == null) {
			int ms = data.getInt();
			int days = Short.toUnsignedInt(data.getShort());
			return new GpsTime(ms, days);
		}
		
		for(int index = data.position(); index < data.limit() - 5; ++index) {
			int days = Short.toUnsignedInt(data.getShort(index + 4));
			if(days < last.days || days > last.days + 3) continue;
			int ms = data.getInt(index);
			if(days == last.days && ms < last.ms - 3600000) continue;
			data.position(index + 6);
			return new GpsTime(ms, days);
		}
		
		throw new BufferUnderflowException();
	}
	
	public TimeLog(IsoXmlElement tlg, String name, IsoXmlElement tim, byte[] content) throws IOException {
		this.tlg = tlg;
		this.config = new TimeLogConfig(tim, this::getTimeLog);
		
		TimeLogEntry entry = null;
		ByteBuffer data = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
		while (data.hasRemaining()) {
			entry = new TimeLogEntry(config, data, entry);
			entries.add(entry);
		}
		
		this.timelog = new ParserAPI.TimeLog(tlg.getUris().get(0), name, 
				entries.get(0).getHead(0, Instant.class), 
				getLastValid().getHead(0, Instant.class), 
				entries.size());
	}
	
	public TimeLog findReferences() throws SAXException {
		config.findReferences();
		return this;
	}
	
	public IsoXmlElement getTlg() {
		return tlg;
	}
	
	public ParserAPI.TimeLog getTimeLog() {
		return timelog;
	}
	
	public List<String> getHeaderNames() {
		return config.getHeader().stream()
				.map(Attribute::getName)
				.collect(Collectors.toList());
	}
	
	public static class ValueDescription extends ValueInfo {
		@Nonnull
		public final int ddi;
		@Nonnull
		public final IsoXmlElement dataLogValue, deviceElement;
		@CheckForNull
		public final IsoXmlElement deviceProcessData, deviceValuePresentation;
		
		public static ValueDescription create(IsoXmlElement dlv, @Nullable ParserAPI.TimeLog timelog, boolean counted) throws SAXException {
			DDIAttr ddiattr = dlv.getAttribute("processDataDdi", DDIAttr.class);
			IDRef detref = dlv.getAttribute("deviceElementIdRef", IDRef.class);
			if(!ddiattr.hasValue()) 
				throw new SAXException("Missing DDI in DataLogValue");
			int ddi = ddiattr.getValue();
			if(!detref.hasValue())
				throw new SAXException("Missing Device Element ID in DataLogValue");
			if(!detref.tryDeRef())
				throw new SAXException("Couldn't find Device Element " + detref.getValue());
			IsoXmlElement det = detref.getRef();
			if(det == null)
				throw new SAXException("Referenced Device Element is a missing");
			if(!det.getTag().equals("DET"))
				throw new SAXException("Referenced Device Element is a " + det.getName());

			return new ValueDescription(dlv, ddi, det, timelog, counted);
		}
		
		public static String createValueUri(IsoXmlElement det, int ddi) {
			return String.format("sdsd:%s-%s:%d:%d", 
					det.getParent().getAttribute("clientName").getStringValue().toLowerCase(), 
					det.getParent().getAttribute("deviceStructureLabel").getStringValue().toLowerCase(), 
					det.getAttribute("deviceElementNumber", UShortAttr.class).getValue(), 
					ddi);
		}
		private static ConcurrentHashMap<String, Integer> COUNTED_VALUE_URIS = new ConcurrentHashMap<>();
		public static String createCountedValueUri(IsoXmlElement det, int ddi) {
			String vuri = createValueUri(det, ddi);
			Integer num = COUNTED_VALUE_URIS.compute(vuri, (k,v) -> v != null ? v + 1 : 1);
			return vuri + ':' + num.toString();
		}
		
		private ValueDescription(IsoXmlElement dlv, int ddi, IsoXmlElement det, @Nullable ParserAPI.TimeLog timelog, boolean counted) throws SAXException {
			super(counted ? createCountedValueUri(det, ddi) : createValueUri(det, ddi));
			dlv.setLinks(Arrays.asList(new Link(valueUri)));
			if(timelog != null) addTimeLog(timelog);
			
			this.dataLogValue = dlv;
			this.deviceElement = det;
			this.ddi = ddi;
			
			Optional<IsoXmlElement> optdpd = det.getChildren().stream()
					.filter(ele -> ele.getTag().equals("DOR"))
					.map(dor -> dor.getAttribute("deviceObjectId", OIDRef.class).getRef())
					.filter(ele -> ele != null && ele.getTag().equals("DPD"))
					.filter(dpd -> dpd.getAttribute("deviceProcessDataDdi", DDIAttr.class).getValue().intValue() == ddi)
					.findAny();
			
			if(optdpd.isPresent()) {
				this.deviceProcessData = optdpd.get();
				setDesignator(deviceProcessData.getAttribute("deviceProcessDataDesignator", StringAttr.class).getValue());
				this.deviceValuePresentation = deviceProcessData.getAttribute("deviceValuePresentationObjectId", OIDRef.class).getRef();
				if(deviceValuePresentation != null) {
					setOffset(deviceValuePresentation.getAttribute("offset", IntAttr.class).getValue());
					DoubleAttr scale = deviceValuePresentation.getAttribute("scale", DoubleAttr.class);
					setScale(scale.hasValue() ? scale.getValue() : 1.);
					setNumberOfDecimals(deviceValuePresentation.getAttribute("numberOfDecimals", ByteAttr.class).getValue());
					setUnit(deviceValuePresentation.getAttribute("unitDesignator", StringAttr.class).getValue());
				}
			} else {
				this.deviceProcessData = null;
				this.deviceValuePresentation = null;
			}
		}
		
		@Nonnull
		public IsoXmlElement getDataLogValue() {
			return dataLogValue;
		}

		public int getDdi() {
			return ddi;
		}

		@Nonnull
		public IsoXmlElement getDeviceElement() {
			return deviceElement;
		}

		@CheckForNull
		public IsoXmlElement getDeviceProcessData() {
			return deviceProcessData;
		}
		
		@CheckForNull
		public IsoXmlElement getDeviceValuePresentation() {
			return deviceValuePresentation;
		}
	}
	
	public List<ValueDescription> getValueDescriptions() throws SAXException {
		return config.getValueDescriptions();
	}
	
	public List<String> getAllErrors() {
		List<String> errors = new ArrayList<>();
		
		try {
			config.findReferences();
			List<ValueDescription> vds = config.getValueDescriptions();
			if(vds == null) 
				errors.add(timelog.name + ": Couldn't find value descriptions");
			else {
				for(int i = 0; i < vds.size(); ++i) {
					ValueDescription vd = vds.get(i);
					String prefix = String.format("%s (%04X): ", timelog.name, vd.getDdi());
					
					IsoXmlElement dvp = vd.getDeviceValuePresentation();
					if(dvp == null) {
						errors.add(prefix + "Device Value Presentation is missing");
						continue;
					}
					
					IntAttr offset = dvp.getAttribute("offset", IntAttr.class);
					DoubleAttr scale = dvp.getAttribute("scale", DoubleAttr.class);
					ByteAttr numberOfDecimals = dvp.getAttribute("numberOfDecimals", ByteAttr.class);
					StringAttr unit = dvp.getAttribute("unitDesignator", StringAttr.class);

					if(!offset.hasValue())
						errors.add(prefix + "Required attribute 'offset' is missing");
					if(!scale.hasValue())
						errors.add(prefix + "Required attribute 'scale' is missing");
					if(offset.hasError())
						errors.add(prefix + offset.getError());
					if(scale.hasError())
						errors.add(prefix + scale.getError());
					if(numberOfDecimals.hasError())
						errors.add(prefix + numberOfDecimals.getError());
					if(unit.hasError())
						errors.add(prefix + unit.getError());
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			errors.add(timelog.name + ": " + e.getMessage());
		}
		
		for(int i = 0; i < entries.size(); ++i) {
			TimeLogEntry tle = entries.get(i);
			if(tle.hasError())
				errors.add(String.format("%s [%d]: %s", timelog.name, i, tle.getError()));
		}
		
		return errors;
	}

	@Override
	public TimeLogEntry get(int index) {
		return entries.get(index);
	}
	
	public TimeLogEntry getLastValid() {
		for(int i = entries.size()-1; i >= 0; --i) {
			TimeLogEntry entry = entries.get(i);
			if(entry.getHead(0, Instant.class) != null)
				return entry;
		}
		throw new IndexOutOfBoundsException("No valid entry found");
	}

	@Override
	public int size() {
		return entries.size();
	}

}
