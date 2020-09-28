package de.sdsd.projekt.parser.isoxml;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;

import agrirouter.technicalmessagetype.Gps.GPSList.GPSEntry;
import de.sdsd.projekt.parser.isoxml.Attribute.ByteAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DDIAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.DoubleAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.HEXAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.IDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.IntAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.OIDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.UShortAttr;
import de.sdsd.projekt.parser.isoxml.RefAttr.OIDRef;
import de.sdsd.projekt.parser.isoxml.TimeLog.ValueDescription;
import efdi.GrpcEfdi;
import efdi.GrpcEfdi.DataLogValue;
import efdi.GrpcEfdi.Device;
import efdi.GrpcEfdi.DeviceElement;
import efdi.GrpcEfdi.DeviceObjectReference;
import efdi.GrpcEfdi.DeviceProcessData;
import efdi.GrpcEfdi.DeviceProperty;
import efdi.GrpcEfdi.DeviceValuePresentation;
import efdi.GrpcEfdi.ISO11783_TaskData;
import efdi.GrpcEfdi.Time;
import efdi.GrpcEfdi.Time.TimeType;
import efdi.GrpcEfdi.UID;

public class TelemetryConverter {
	public final TimeLog timelog;
	
	private final int start, positionNorth, positionEast, positionUp, positionStatus, 
			pdop, hdop, numberOfSatellites, gpsUtcTime, gpsUtcDate;

	public TelemetryConverter(TimeLog timelog) {
		this.timelog = timelog;
		List<String> h = timelog.getHeaderNames();
		int start = -1, positionNorth = -1, positionEast = -1, positionUp = -1, positionStatus = -1, 
				pdop = -1, hdop = -1, numberOfSatellites = -1, gpsUtcTime = -1, gpsUtcDate = -1;
		for(int i = 0; i < h.size(); ++i) {
			switch(h.get(i)) {
			case "start": 
				start = i;
				break;
			case "positionNorth": 
				positionNorth = i;
				break;
			case "positionEast": 
				positionEast = i;
				break;
			case "positionUp": 
				positionUp = i;
				break;
			case "positionStatus": 
				positionStatus = i;
				break;
			case "pdop": 
				pdop = i;
				break;
			case "hdop": 
				hdop = i;
				break;
			case "numberOfSatellites": 
				numberOfSatellites = i;
				break;
			case "gpsUtcTime": 
				gpsUtcTime = i;
				break;
			case "gpsUtcDate": 
				gpsUtcDate = i;
				break;
			}
		}
		this.start = start;
		this.positionNorth = positionNorth;
		this.positionEast = positionEast;
		this.positionUp = positionUp;
		this.positionStatus = positionStatus;
		this.pdop = pdop;
		this.hdop = hdop;
		this.numberOfSatellites = numberOfSatellites;
		this.gpsUtcTime = gpsUtcTime;
		this.gpsUtcDate = gpsUtcDate;
	}
	
	public Instant readTimeStamp(int index) {
		return timelog.get(index).getHead(start, Instant.class);
	}
	
	public ISO11783_TaskData readDeviceDescription(IsoXmlElement taskdata) throws SAXException {
		return readDeviceDescription(taskdata, timelog);
	}
	
	public static ISO11783_TaskData readDeviceDescription(IsoXmlElement taskdata, TimeLog timelog) throws SAXException {
		Map<String, IsoXmlElement> devices = new HashMap<>();
		for(ValueDescription desc : timelog.getValueDescriptions()) {
			IsoXmlElement dvc = desc.getDeviceElement().getParent();
			if(dvc != null)
				devices.put(dvc.uri, dvc);
		}
		
		return devices.isEmpty() ? readAllDevicesDescription(taskdata) : createDeviceDescription(taskdata, devices.values());
	}
	
	public static ISO11783_TaskData readAllDevicesDescription(IsoXmlElement taskdata) throws SAXException {
		return createDeviceDescription(taskdata, taskdata.findChildren("DVC"));
	}
	
	private static ISO11783_TaskData createDeviceDescription(IsoXmlElement taskdata, Collection<IsoXmlElement> devices) throws SAXException {
		ISO11783_TaskData.Builder deviceDescription = ISO11783_TaskData.newBuilder()
				.setVersionMajorValue(taskdata.getAttribute("versionMajor", EnumAttr.class).number())
				.setVersionMinor(taskdata.getAttribute("versionMinor", ByteAttr.class).getValue())
				.setManagementSoftwareManufacturer(taskdata.getAttribute("managementSoftwareManufacturer", StringAttr.class).getValue())
				.setManagementSoftwareVersion(taskdata.getAttribute("managementSoftwareVersion", StringAttr.class).getValue())
				.setTaskControllerManufacturer(taskdata.getAttribute("taskControllerManufacturer", StringAttr.class).getValue())
				.setTaskControllerVersion(taskdata.getAttribute("taskControllerVersion", StringAttr.class).getValue())
				.setDataTransferOriginValue(taskdata.getAttribute("dataTransferOrigin", EnumAttr.class).number())
				.setDataTransferLanguage(taskdata.getAttribute("dataTransferLanguage", StringAttr.class).getValue());
		
		for(IsoXmlElement dvc : devices) {
			deviceDescription.addDevice(readDevice(dvc));
		}
		return deviceDescription.build();
	}
	
	private static Device readDevice(IsoXmlElement dvc) throws SAXException {
		Device.Builder device = Device.newBuilder()
				.setDeviceId(toUID(dvc))
				.setDeviceDesignator(dvc.getAttribute("deviceDesignator", StringAttr.class).getValue())
				.setDeviceSoftwareVersion(dvc.getAttribute("deviceSoftwareVersion", StringAttr.class).getValue())
				.setClientName(ByteString.copyFrom(dvc.getAttribute("clientName", HEXAttr.class).getValue()))
				.setDeviceSerialNumber(dvc.getAttribute("deviceSerialNumber", StringAttr.class).getValue())
				.setDeviceStructureLabel(ByteString.copyFrom(dvc.getAttribute("deviceStructureLabel", HEXAttr.class).getValue()))
				.setDeviceLocalizationLabel(ByteString.copyFrom(dvc.getAttribute("deviceLocalizationLabel", HEXAttr.class).getValue()));
		
		for(IsoXmlElement child : dvc.getChildren()) {
			switch(child.getTag()) {
			case "DET":
				device.addDeviceElement(readDeviceElement(child));
				break;
			case "DPD":
				device.addDeviceProcessData(readDeviceProcessData(child));
				break;
			case "DPT":
				device.addDeviceProperty(readDeviceProperty(child));
				break;
			case "DVP":
				device.addDeviceValuePresentation(readDeviceValuePresentation(child));
				break;
			}
		}
		return device.build();
	}
	
	private static DeviceElement readDeviceElement(IsoXmlElement det) throws SAXException {
		DeviceElement.Builder deviceElement = DeviceElement.newBuilder()
				.setDeviceElementId(toUID(det))
				.setDeviceElementObjectId(det.getAttribute("deviceElementObjectId", OIDAttr.class).getValue())
				.setDeviceElementTypeValue(det.getAttribute("deviceElementType", EnumAttr.class).number())
				.setDeviceElementDesignator(det.getAttribute("deviceElementDesignator", StringAttr.class).getValue())
				.setDeviceElementNumber(det.getAttribute("deviceElementNumber", UShortAttr.class).getValue())
				.setParentObjectId(det.getAttribute("parentObjectId", OIDRef.class).getValue());
		
		for(IsoXmlElement child : det.getChildren()) {
			if(child.getTag().equals("DOR"))
				deviceElement.addDeviceObjectReference(readDeviceObjectReference(child));
		}
		return deviceElement.build();
	}
	
	private static DeviceProcessData readDeviceProcessData(IsoXmlElement dpd) throws SAXException {
		return DeviceProcessData.newBuilder()
				.setDeviceProcessDataObjectId(dpd.getAttribute("deviceProcessDataObjectId", OIDAttr.class).getValue())
				.setDeviceProcessDataDdi(dpd.getAttribute("deviceProcessDataDdi", DDIAttr.class).getValue())
				.setDeviceProcessDataProperty(dpd.getAttribute("deviceProcessDataProperty", IntAttr.class).getValue())
				.setDeviceProcessDataTriggerMethods(dpd.getAttribute("deviceProcessDataTriggerMethods", ByteAttr.class).getValue())
				.setDeviceProcessDataDesignator(dpd.getAttribute("deviceProcessDataDesignator", StringAttr.class).getValue())
				.setDeviceValuePresentationObjectId(dpd.getAttribute("deviceValuePresentationObjectId", OIDRef.class).getValue())
				.build();
	}
	
	private static DeviceProperty readDeviceProperty(IsoXmlElement dpt) throws SAXException {
		return DeviceProperty.newBuilder()
				.setDevicePropertyObjectId(dpt.getAttribute("devicePropertyObjectId", OIDAttr.class).getValue())
				.setDevicePropertyDdi(dpt.getAttribute("devicePropertyDdi", DDIAttr.class).getValue())
				.setDevicePropertyValue(dpt.getAttribute("devicePropertyValue", IntAttr.class).getValue())
				.setDevicePropertyDesignator(dpt.getAttribute("devicePropertyDesignator", StringAttr.class).getValue())
				.setDeviceValuePresentationObjectId(dpt.getAttribute("deviceValuePresentationObjectId", OIDRef.class).getValue())
				.build();
	}
	
	private static DeviceValuePresentation readDeviceValuePresentation(IsoXmlElement dvp) throws SAXException {
		return DeviceValuePresentation.newBuilder()
				.setDeviceValuePresentationObjectId(dvp.getAttribute("deviceValuePresentationObjectId", OIDAttr.class).getValue())
				.setOffset(dvp.getAttribute("offset", IntAttr.class).getValue())
				.setScale(dvp.getAttribute("scale", DoubleAttr.class).getValue())
				.setNumberOfDecimals(dvp.getAttribute("numberOfDecimals", ByteAttr.class).getValue())
				.setUnitDesignator(dvp.getAttribute("unitDesignator", StringAttr.class).getValue())
				.build();
	}
	
	private static DeviceObjectReference readDeviceObjectReference(IsoXmlElement dor) throws SAXException {
		return DeviceObjectReference.newBuilder()
				.setDeviceObjectId(dor.getAttribute("deviceObjectId", OIDRef.class).getValue())
				.build();
	}
	
	private static final Pattern ID_REGEX = Pattern.compile("[A-Z]{3}-?(\\d{1,18})");
	private static UID toUID(IsoXmlElement elem) throws SAXException {
		IDAttr id = elem.getId();
		if(id == null || !id.hasValue())
			throw new SAXException(elem.getTag() + " has no ID");
		Matcher matcher = ID_REGEX.matcher(id.getValue());
		if(!matcher.find())
			throw new SAXException(elem.getTag() + " has invalid ID");
		return UID.newBuilder()
				.setNumber(Long.parseLong(matcher.group(1)))
				.addUri(matcher.group(0))
				.build();
	}
	
	public Time readTime(int index) throws SAXException {
		TimeLogEntry tle = timelog.get(index);
		Time.Builder time = Time.newBuilder();
		if(start >= 0) time.setStart(Timestamps.fromMillis(tle.getHead(start, Instant.class).toEpochMilli()));
		time.setType(TimeType.D_EFFECTIVE);
		
		GrpcEfdi.Position.Builder pos = GrpcEfdi.Position.newBuilder();
		if(positionNorth >= 0) pos.setPositionNorth(tle.getHead(positionNorth, Double.class));
		if(positionEast >= 0) pos.setPositionEast(tle.getHead(positionEast, Double.class));
		if(positionUp >= 0) pos.setPositionUp(tle.getHead(positionUp, Integer.class));
		if(positionStatus >= 0) pos.setPositionStatusValue(tle.getHead(positionStatus, Byte.class));
		if(pdop >= 0) pos.setPdop(tle.getHead(pdop, Float.class));
		if(hdop >= 0) pos.setHdop(tle.getHead(hdop, Float.class));
		if(numberOfSatellites >= 0) pos.setNumberOfSatellites(tle.getHead(numberOfSatellites, Byte.class));
		if(gpsUtcTime >= 0 && gpsUtcDate >= 0) {
			Instant gpsUtc = new TimeLog.GpsTime(tle.getHead(gpsUtcTime, Integer.class), 
					tle.getHead(gpsUtcDate, Integer.class)).toInstant(ZoneOffset.UTC);
			pos.setGpsUtcTimestamp(Timestamps.fromMillis(gpsUtc.toEpochMilli()));
		}
		time.setPositionStart(pos.build());
		
		for(int i = 0; i < tle.size(); ++i) {
			if(tle.hasValue(i)) {
				ValueDescription desc = tle.getValueDescription(i);
				time.addDataLogValue(DataLogValue.newBuilder()
						.setProcessDataDdi(desc.ddi)
						.setProcessDataValue(tle.getValue(i))
						.setDeviceElementIdRef(toUID(desc.deviceElement))
						.build());
			}
		}
		
		return time.build();
	}
	
	public GPSEntry readGpsEntry(int index) throws SAXException {
		TimeLogEntry tle = timelog.get(index);
		GPSEntry.Builder pos = GPSEntry.newBuilder();

		if(positionNorth >= 0) pos.setPositionNorth(tle.getHead(positionNorth, Double.class));
		if(positionEast >= 0) pos.setPositionEast(tle.getHead(positionEast, Double.class));
		if(positionUp >= 0) pos.setPositionUp(tle.getHead(positionUp, Integer.class));
		if(positionStatus >= 0) pos.setPositionStatusValue(tle.getHead(positionStatus, Byte.class));
		if(pdop >= 0) pos.setPdop(tle.getHead(pdop, Float.class));
		if(hdop >= 0) pos.setHdop(tle.getHead(hdop, Float.class));
		if(numberOfSatellites >= 0) pos.setNumberOfSatellites(tle.getHead(numberOfSatellites, Byte.class));
		if(gpsUtcTime >= 0 && gpsUtcDate >= 0) {
			Instant gpsUtc = new TimeLog.GpsTime(tle.getHead(gpsUtcTime, Integer.class), 
					tle.getHead(gpsUtcDate, Integer.class)).toInstant(ZoneOffset.UTC);
			pos.setGpsUtcTimestamp(Timestamps.fromMillis(gpsUtc.toEpochMilli()));
		} else {
			pos.setGpsUtcTimestamp(Timestamps.fromMillis(tle.getHead(start, Instant.class).toEpochMilli()));
		}
		
		return pos.build();
	}
}
