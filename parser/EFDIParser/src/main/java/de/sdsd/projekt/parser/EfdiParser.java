package de.sdsd.projekt.parser;

import static de.sdsd.projekt.api.Util.lit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONObject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.ValueInfo;
import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;
import de.sdsd.projekt.api.ServiceResult.WikiInstance;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;
import efdi.GrpcEfdi.DataLogValue;
import efdi.GrpcEfdi.Device;
import efdi.GrpcEfdi.DeviceElement;
import efdi.GrpcEfdi.DeviceObjectReference;
import efdi.GrpcEfdi.DeviceProcessData;
import efdi.GrpcEfdi.DeviceProperty;
import efdi.GrpcEfdi.DeviceValuePresentation;
import efdi.GrpcEfdi.ISO11783_TaskData;
import efdi.GrpcEfdi.Time;
import efdi.GrpcEfdi.TimeLog;
import efdi.GrpcEfdi.UID;

public class EfdiParser {
	private final ISO11783_TaskData deviceDescription;
	private final List<EfdiTimeLog> timelogs = new ArrayList<>();
	private final List<String> initErrors = new ArrayList<>();
	
	private final Map<ValueKey, ValueInfo> infos = new HashMap<>();
	
	private static Map<String, WikiInstance> ddiMap = null;
	private static final Resource DDI = Util.UNKNOWN.res("ddi");
	public static void initDdiMap() {
		try {
			ddiMap = ParserAPI.getWikinormiaInstances(DDI, true); //TODO: local
		} catch (JsonRpcException e) {
			ddiMap = Collections.emptyMap();
			System.err.println(e.getMessage());
		}
	}
	
	public EfdiParser(InputStream efdizip) throws IOException {		
		ISO11783_TaskData deviceDescription = null;
		JSONObject jsoninfos = null;
		try(ZipInputStream stream = new ZipInputStream(efdizip, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = stream.getNextEntry()) != null) {
				try {
					if(entry.isDirectory()) continue;
					String name = new File(entry.getName()).getName();
					if(name.toLowerCase().equals("devicedescription.bin"))
						deviceDescription = ISO11783_TaskData.parseFrom(stream);
					else if(name.toLowerCase().equals("tlginfo.json"))
						jsoninfos = new JSONObject(IOUtils.toString(stream, StandardCharsets.UTF_8));
					else if(name.endsWith(".bin"))
						timelogs.add(new EfdiTimeLog(Util.createRandomUri(), name.substring(0, name.length() - 4), TimeLog.parseFrom(stream)));
				} catch (InvalidProtocolBufferException e) {
					initErrors.add(entry.getName() + ": " + e.getMessage());
				}
			}
		}
		this.deviceDescription = deviceDescription;
		
		if(deviceDescription != null)
			deviceDescription.getDeviceList().forEach(this::findValueInfos);
		if(jsoninfos != null) {
			findValueInfos(jsoninfos.getJSONObject("vuris"));
			JSONObject tlguris = jsoninfos.getJSONObject("tlguris");
			for(EfdiTimeLog tlg : timelogs) {
				tlg.uri = tlguris.getString(tlg.name);
			}
		}
			
		timelogs.forEach(EfdiTimeLog::init);
	}
	
	private static String dvcIdentifier(Device dvc) {
		return Hex.encodeHexString(dvc.getClientName().toByteArray(), true) + '-' + 
				Hex.encodeHexString(dvc.getDeviceStructureLabel().toByteArray(), true);
	}
	private static String createValueUri(Device dvc, DeviceElement det, DeviceProcessData dpd) {
		return Util.createUri(String.format("sdsd:%s:%d:%d", dvcIdentifier(dvc), det.getDeviceElementNumber(), dpd.getDeviceProcessDataDdi()));
	}
	
	
	private void findValueInfos(Device dvc) {
		Map<Integer, DeviceProcessData> dpdMap = dvc.getDeviceProcessDataList().stream()
				.collect(Collectors.toMap(DeviceProcessData::getDeviceProcessDataObjectId, Function.identity()));
		Map<Integer, DeviceValuePresentation> dvpMap = dvc.getDeviceValuePresentationList().stream()
				.collect(Collectors.toMap(DeviceValuePresentation::getDeviceValuePresentationObjectId, Function.identity()));
		
		for(DeviceElement det : dvc.getDeviceElementList()) {
			for(DeviceObjectReference dor : det.getDeviceObjectReferenceList()) {
				DeviceProcessData dpd = dpdMap.get(dor.getDeviceObjectId());
				if(dpd != null) {
					ValueKey key = new ValueKey(det.getDeviceElementId(), dpd.getDeviceProcessDataDdi());
					DeviceValuePresentation dvp = dvpMap.get(dpd.getDeviceValuePresentationObjectId());
					ValueInfo info = new ValueInfo(createValueUri(dvc, det, dpd))
							.setDesignator(dpd.getDeviceProcessDataDesignator());
					if(dvp != null)
						info.setOffset(dvp.getOffset())
								.setScale(dvp.getScale())
								.setNumberOfDecimals(dvp.getNumberOfDecimals())
								.setUnit(dvp.getUnitDesignator());
					infos.put(key, info);
				}
			}
		}
	}
	private void findValueInfos(JSONObject valueUris) {
		for(String det : valueUris.keySet()) {
			long detnum = Long.parseLong(det);
			JSONObject vuris = valueUris.getJSONObject(det);
			for(String ddi : vuris.keySet()) {
				infos.put(new ValueKey(detnum, Integer.parseInt(ddi)), 
						new ValueInfo(vuris.getString(ddi)));
			}
		}
	}
	
	public static WikiInstance ddi(int ddi) {
		if(ddiMap == null) initDdiMap();
		return ddiMap.get(Integer.toString(ddi));
	}
	
	public List<String> getErrors() {
		return Collections.unmodifiableList(initErrors);
	}
	
	public ISO11783_TaskData getDeviceDescription() {
		return deviceDescription;
	}

	public List<EfdiTimeLog> getTimelogs() {
		return timelogs;
	}
	
	public Value getValue(DataLogValue dlv) {
		return new Value(dlv);
	}

	public List<String> validate() {
		List<String> errors = new ArrayList<>(initErrors);
		if(deviceDescription != null) {
			if(deviceDescription.getVersionMajorValue() < 0 || deviceDescription.getVersionMajorValue() > 4)
				errors.add("VersionMajor out of range (0-4): " + deviceDescription.getVersionMajorValue());
			if(deviceDescription.getVersionMinor() < 0 || deviceDescription.getVersionMinor() > 99)
				errors.add("VersionMinor out of range (0-99): " + deviceDescription.getVersionMinor());
			if(deviceDescription.getDataTransferOriginValue() < 0 || deviceDescription.getDataTransferOriginValue() > 2)
				errors.add("DataTransferOrigin out of range (0-2): " + deviceDescription.getDataTransferOriginValue());

			for(Device dvc : deviceDescription.getDeviceList()) {
				String prefixDVC = "DVC-" + dvc.getDeviceId().getNumber();
				if(dvc.getClientName().isEmpty())
					errors.add(prefixDVC + ": ClientName missing");
				if(dvc.getClientName().size() > 8)
					errors.add(prefixDVC + ": ClientName too long (max 8 bytes)");
				if(dvc.getDeviceStructureLabel().isEmpty())
					errors.add(prefixDVC + ": DeviceStructureLabel missing");
				if(dvc.getDeviceStructureLabel().size() > 39)
					errors.add(prefixDVC + ": DeviceStructureLabel too long (max 39 bytes)");
				if(dvc.getDeviceLocalizationLabel().isEmpty())
					errors.add(prefixDVC + ": DeviceLocalizationLabel missing");
				if(dvc.getDeviceLocalizationLabel().size() > 7)
					errors.add(prefixDVC + ": DeviceLocalizationLabel too long (max 7 bytes)");
				
				Map<Integer, Message> deviceObjects = new HashMap<>();
				for(DeviceValuePresentation dvp : dvc.getDeviceValuePresentationList()) {
					String prefix = prefixDVC + ".DVP-" + dvp.getDeviceValuePresentationObjectId();
					deviceObjects.put(dvp.getDeviceValuePresentationObjectId(), dvp);
					
					if(dvp.getScale() <= 0)
						errors.add(prefix + ": Scale must be more than 0");
				}
				
				for(DeviceProperty dpt : dvc.getDevicePropertyList()) {
					String prefix = prefixDVC + ".DPT-" + dpt.getDevicePropertyObjectId();
					deviceObjects.put(dpt.getDevicePropertyObjectId(), dpt);
					
					if(dpt.getDevicePropertyDdi() < 0 || dpt.getDevicePropertyDdi() >= 65535)
						errors.add(prefix + ": DDI out of range (0 - 65534): " + dpt.getDevicePropertyDdi());
					if(dpt.getDeviceValuePresentationObjectId() != 0 && !deviceObjects.containsKey(dpt.getDeviceValuePresentationObjectId()))
						errors.add(prefix + ": Unknown DVP " + dpt.getDeviceValuePresentationObjectId());
				}
				
				for(DeviceProcessData dpd : dvc.getDeviceProcessDataList()) {
					String prefix = prefixDVC + ".DPD-" + dpd.getDeviceProcessDataObjectId();
					deviceObjects.put(dpd.getDeviceProcessDataObjectId(), dpd);
					
					if(dpd.getDeviceProcessDataDdi() < 0 || dpd.getDeviceProcessDataDdi() >= 65535)
						errors.add(prefix + ": DDI out of range (0 - 65534): " + dpd.getDeviceProcessDataDdi());
					if(dpd.getDeviceProcessDataProperty() < 0 || dpd.getDeviceProcessDataProperty() > 7)
						errors.add(prefix + ": DeviceProcessDataProperty out of range (0 - 7): " + dpd.getDeviceProcessDataProperty());
					if(dpd.getDeviceProcessDataTriggerMethods() < 0 || dpd.getDeviceProcessDataTriggerMethods() > 31)
						errors.add(prefix + ": DeviceProcessDataTriggerMethods out of range (0 - 31): " + dpd.getDeviceProcessDataTriggerMethods());
					if(dpd.getDeviceValuePresentationObjectId() != 0 && !deviceObjects.containsKey(dpd.getDeviceValuePresentationObjectId()))
						errors.add(prefix + ": Unknown DVP " + dpd.getDeviceValuePresentationObjectId());
				}
				
				for(DeviceElement det : dvc.getDeviceElementList()) {
					deviceObjects.put(det.getDeviceElementObjectId(), det);
				}
				for(DeviceElement det : dvc.getDeviceElementList()) {
					String prefix = prefixDVC + ".DET-" + det.getDeviceElementId().getNumber();
					
					if(det.getDeviceElementTypeValue() < 0 || det.getDeviceElementTypeValue() > 7)
						errors.add(prefix + ": DeviceElementType out of range (0 - 7): " + det.getDeviceElementTypeValue());
					if(det.getDeviceElementNumber() < 0 || det.getDeviceElementNumber() > 4095)
						errors.add(prefix + ": DeviceElementType out of range (0 - 4095): " + det.getDeviceElementNumber());
					if(det.getParentObjectId() != 0 && !deviceObjects.containsKey(det.getParentObjectId()))
						errors.add(prefix + ": Unknown parent " + det.getParentObjectId());
					for(DeviceObjectReference dor : det.getDeviceObjectReferenceList()) {
						if(!deviceObjects.containsKey(dor.getDeviceObjectId()))
							errors.add(prefix + ": Unknown object reference " + dor.getDeviceObjectId());
					}
				}
			}
		}
		
		return errors;
	}
	
	private static final WikiFormat ISOXML = Util.format("isoxml"), EFDI = Util.format("efdiTimelog");
	
	public Model toRDF(List<String> errors) {
		if(deviceDescription == null) return null;
		Model model = ModelFactory.createDefaultModel();
		
		int hashcode = Arrays.hashCode(deviceDescription.getDeviceList().stream()
				.flatMap(dvc -> Stream.of(dvc.getClientName(), dvc.getDeviceLocalizationLabel()))
				.toArray());
		Resource base = model.createResource(Util.createUri(Integer.toString(hashcode, 16)), EFDI.res("ISO11783TaskData"));
		WikiType taskdata = ISOXML.res("ISO11783TaskData");
		base.addProperty(taskdata.prop("A"), ISOXML.res("VersionMajor").inst(deviceDescription.getVersionMajorValue()));
		base.addProperty(taskdata.prop("B"), lit(deviceDescription.getVersionMinor()));
		base.addProperty(taskdata.prop("C"), lit(deviceDescription.getManagementSoftwareManufacturer()));
		base.addProperty(taskdata.prop("D"), lit(deviceDescription.getManagementSoftwareVersion()));
		base.addProperty(taskdata.prop("E"), lit(deviceDescription.getTaskControllerManufacturer()));
		base.addProperty(taskdata.prop("F"), lit(deviceDescription.getTaskControllerVersion()));
		base.addProperty(taskdata.prop("G"), ISOXML.res("DataTransferOrigin").inst(deviceDescription.getDataTransferOriginValue()));
		base.addProperty(taskdata.prop("H"), lit(deviceDescription.getDataTransferLanguage()));
		base.addProperty(RDFS.label, "ISO11783TaskData");
		
		WikiType isodvc = ISOXML.res("DVC"),
				isodvp = ISOXML.res("DVP"),
				isodpt = ISOXML.res("DPT"),
				isodpd = ISOXML.res("DPD"),
				isodet = ISOXML.res("DET"),
				isodor = ISOXML.res("DOR");
		WikiAttr doID = ISOXML.res("DO").prop("id");
		
		Map<Long, Resource> detMap = new HashMap<>();
		for(Device dvc : deviceDescription.getDeviceList()) {
			String dvcid = dvcIdentifier(dvc);
			Resource device = Util.createResource(model, dvcid, EFDI.res("DVC"), base);
			device.addProperty(isodvc.prop("A"), lit(dvc.getDeviceId().getNumber()));
			device.addProperty(isodvc.prop("B"), lit(dvc.getDeviceDesignator()));
			device.addProperty(isodvc.prop("C"), lit(dvc.getDeviceSoftwareVersion()));
			device.addProperty(isodvc.prop("D"), lit(dvc.getClientName().toByteArray()));
			device.addProperty(isodvc.prop("E"), lit(dvc.getDeviceSerialNumber()));
			device.addProperty(isodvc.prop("F"), lit(dvc.getDeviceStructureLabel().toByteArray()));
			device.addProperty(isodvc.prop("G"), lit(dvc.getDeviceLocalizationLabel().toByteArray()));
			device.addProperty(RDFS.label, dvc.getDeviceDesignator().isEmpty() ? ("DVC-" + dvc.getDeviceId().getNumber()) : dvc.getDeviceDesignator());
			
			Map<Integer, Resource> deviceObjects = new HashMap<>();
			for(DeviceValuePresentation dvp : dvc.getDeviceValuePresentationList()) {
				Resource res = Util.createResource(model, dvcid + ':' + dvp.getDeviceValuePresentationObjectId(), EFDI.res("DVP"), device);
				res.addProperty(doID, lit(dvp.getDeviceValuePresentationObjectId()));
				res.addProperty(isodvp.prop("B"), lit(dvp.getOffset()));
				res.addProperty(isodvp.prop("C"), lit(dvp.getScale()));
				res.addProperty(isodvp.prop("D"), lit(dvp.getNumberOfDecimals()));
				res.addProperty(isodvp.prop("E"), lit(dvp.getUnitDesignator()));
				res.addProperty(RDFS.label, dvp.getUnitDesignator().isEmpty() ? ("DVP#" + dvp.getDeviceValuePresentationObjectId()) : dvp.getUnitDesignator());
				deviceObjects.put(dvp.getDeviceValuePresentationObjectId(), res);
			}
			
			Map<Integer, Integer> deviceDdis = new HashMap<>();
			for(DeviceProperty dpt : dvc.getDevicePropertyList()) {
				Resource res = Util.createResource(model, dvcid + ':' + dpt.getDevicePropertyObjectId(), EFDI.res("DPT"), device);
				res.addProperty(doID, lit(dpt.getDevicePropertyObjectId()));
				
				deviceDdis.put(dpt.getDevicePropertyObjectId(), dpt.getDevicePropertyDdi());
				WikiInstance ddi = ddi(dpt.getDevicePropertyDdi());
				if(ddi != null) res.addProperty(isodpt.prop("B"), ddi.res);
				else errors.add(String.format("DVC-%d.DPT-%d: Unknown DDI %d", dvc.getDeviceId().getNumber(), 
						dpt.getDevicePropertyObjectId(), dpt.getDevicePropertyDdi()));
				
				res.addProperty(isodpt.prop("C"), lit(dpt.getDevicePropertyValue()));
				res.addProperty(isodpt.prop("D"), lit(dpt.getDevicePropertyDesignator()));
				
				if(dpt.getDeviceValuePresentationObjectId() != 0) {
					Resource dvp = deviceObjects.get(dpt.getDeviceValuePresentationObjectId());
					if(dvp != null) res.addProperty(isodpt.prop("E"), dvp);
					else errors.add(String.format("DVC-%d.DPT-%d: Unknown DVP %d", dvc.getDeviceId().getNumber(), 
							dpt.getDevicePropertyObjectId(), dpt.getDeviceValuePresentationObjectId()));
				}
				
				if(dpt.getDevicePropertyDesignator().isEmpty())
					res.addProperty(RDFS.label, ddi != null ? ddi.label : ("DDI " + dpt.getDevicePropertyDdi()));
				else
					res.addProperty(RDFS.label, dpt.getDevicePropertyDesignator());
				deviceObjects.put(dpt.getDevicePropertyObjectId(), res);
			}
			
			for(DeviceProcessData dpd : dvc.getDeviceProcessDataList()) {
				Resource res = Util.createResource(model, dvcid + ':' + dpd.getDeviceProcessDataObjectId(), EFDI.res("DPD"), device);
				res.addProperty(doID, lit(dpd.getDeviceProcessDataObjectId()));
				
				deviceDdis.put(dpd.getDeviceProcessDataObjectId(), dpd.getDeviceProcessDataDdi());
				WikiInstance ddi = ddi(dpd.getDeviceProcessDataDdi());
				if(ddi != null) res.addProperty(isodpd.prop("B"), ddi.res);
				else errors.add(String.format("DVC-%d.DPD-%d: Unknown DDI %d", dvc.getDeviceId().getNumber(), 
						dpd.getDeviceProcessDataObjectId(), dpd.getDeviceProcessDataDdi()));
				
				res.addProperty(isodpd.prop("C"), lit(dpd.getDeviceProcessDataProperty()));
				res.addProperty(isodpd.prop("D"), lit(dpd.getDeviceProcessDataTriggerMethods()));
				res.addProperty(isodpd.prop("E"), lit(dpd.getDeviceProcessDataDesignator()));
				
				if(dpd.getDeviceValuePresentationObjectId() != 0) {
					Resource dvp = deviceObjects.get(dpd.getDeviceValuePresentationObjectId());
					if(dvp != null) res.addProperty(isodpd.prop("F"), dvp);
					else errors.add(String.format("DVC-%d.DPD-%d: Unknown DVP %d", dvc.getDeviceId().getNumber(), 
							dpd.getDeviceProcessDataObjectId(), dpd.getDeviceValuePresentationObjectId()));
				}
				
				if(dpd.getDeviceProcessDataDesignator().isEmpty())
					res.addProperty(RDFS.label, ddi != null ? ddi.label : ("DDI " + dpd.getDeviceProcessDataDdi()));
				else
					res.addProperty(RDFS.label, dpd.getDeviceProcessDataDesignator());
				deviceObjects.put(dpd.getDeviceProcessDataObjectId(), res);
			}
			
			for(DeviceElement det : dvc.getDeviceElementList()) {
				Resource res = Util.createResource(model, dvcid + ':' + det.getDeviceElementObjectId(), EFDI.res("DET"), device);
				detMap.put(det.getDeviceElementId().getNumber(), res);
				deviceObjects.put(det.getDeviceElementObjectId(), res);
			}
			for(DeviceElement det : dvc.getDeviceElementList()) {
				Resource res = deviceObjects.get(det.getDeviceElementObjectId());
				res.addProperty(isodet.prop("A"), lit(det.getDeviceElementId().getNumber()));
				res.addProperty(doID, lit(det.getDeviceElementObjectId()));
				res.addProperty(isodet.prop("C"), ISOXML.res("DeviceElementType").inst(det.getDeviceElementTypeValue()));
				res.addProperty(isodet.prop("D"), lit(det.getDeviceElementDesignator()));
				res.addProperty(isodet.prop("E"), lit(det.getDeviceElementNumber()));
				
				if(det.getParentObjectId() != 0) {
					Resource parent = deviceObjects.get(det.getParentObjectId());
					if(parent != null) res.addProperty(isodet.prop("F"), parent);
					else errors.add(String.format("DVC-%d.DET-%d: Unknown parent %d", dvc.getDeviceId().getNumber(), 
							det.getDeviceElementId().getNumber(), det.getParentObjectId()));
				}
				
				for(DeviceObjectReference dor : det.getDeviceObjectReferenceList()) {
					Resource obj = deviceObjects.get(dor.getDeviceObjectId());
					if(obj != null) {
						String identifier = String.format("%s:%d:%d", dvcid, det.getDeviceElementObjectId(), deviceDdis.get(dor.getDeviceObjectId()));
						Resource dorres = Util.createResource(model, identifier, EFDI.res("DOR"),  res);
						dorres.addProperty(isodor.prop("A"),  obj);
					}
					else errors.add(String.format("DVC-%d.DET-%d: Unknown object reference %d", dvc.getDeviceId().getNumber(), 
							det.getDeviceElementId().getNumber(), dor.getDeviceObjectId()));
				}
				
				res.addProperty(RDFS.label, det.getDeviceElementDesignator().isEmpty() ? 
						("DET-" + det.getDeviceElementId().getNumber()) : det.getDeviceElementDesignator());
			}
		}
		
		Map<ValueInfo, DataLogValue> dlvMap = new HashMap<>(infos.size());
		for(EfdiTimeLog tlg : timelogs) {
			ParserAPI.TimeLog timelog = tlg.createLog();
			timelog.writeTo(model, EFDI.res("TLG"))
					.addLiteral(RDFS.label, Util.lit(tlg.name));
			
			for(ValueInfo info : infos.values()) {
				info.addTimeLog(timelog);
			}
			
			for(Time tim : tlg.tlg.getTimeList()) {
				for(DataLogValue dlv : tim.getDataLogValueList()) {
					ValueInfo info = infos.get(new ValueKey(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi()));
					if(info == null) continue; // shouldn't be possible
					dlvMap.put(info, dlv);
				}
			}
		}
		WikiType efdidlv = EFDI.res("DLV"), isodlv = ISOXML.res("DLV");
		for(Map.Entry<ValueKey, ValueInfo> e : infos.entrySet()) {
			Resource res = e.getValue().writeTo(model, efdidlv);
			
			WikiInstance ddi = ddi(e.getKey().ddi);
			if(ddi != null) res.addProperty(isodlv.prop("A"), ddi.res);
			else errors.add("DLV: Unknown DDI " + e.getKey().ddi);
			Resource det = detMap.get(e.getKey().detnum);
			if(det != null) res.addProperty(isodlv.prop("C"), det);
			else errors.add("DLV: Unknown DET-" + e.getKey().detnum);
			
			DataLogValue dlv = dlvMap.get(e.getValue());
			if(dlv != null && dlv.getDataLogPgn() != 0) {
				res.addProperty(isodlv.prop("D"), lit(dlv.getDataLogPgn()));
				res.addProperty(isodlv.prop("E"), lit(dlv.getDataLogPgnStartBit()));
				res.addProperty(isodlv.prop("F"), lit(dlv.getDataLogPgnStopBit()));
			}
			
			StringBuilder label = new StringBuilder();
			if(det != null && det.hasProperty(RDFS.label)) label.append(det.getProperty(RDFS.label).getString());
			else label.append("DET-" + e.getKey().detnum);
			label.append(": ");
			if(ddi != null) label.append(ddi.label);
			else label.append("DDI " + e.getKey().ddi);
			res.addProperty(RDFS.label, label.toString());
		}
		
		return model;
	}
	
	public class EfdiTimeLog extends AbstractList<EfdiTimeLog.Entry> {
		public final TimeLog tlg;
		private String uri, name;
		private final LinkedHashMap<ValueKey, Integer> indexes = new LinkedHashMap<>();
		
		public EfdiTimeLog(String uri, String name, TimeLog tlg) {
			this.tlg = tlg;
			this.uri = uri;
			this.name = name;
		}
		
		private void init() {
			Set<ValueKey> missing = new HashSet<>();
			for(Time tim : tlg.getTimeList()) {
				for(DataLogValue dlv : tim.getDataLogValueList()) {
					ValueKey key = new ValueKey(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi());
					if(infos.containsKey(key))
						indexes.putIfAbsent(key, indexes.size());
					else
						missing.add(key);
				}
			}
			for(ValueKey key : missing) {
				initErrors.add("The reference DET-" + key.detnum 
						+ " DDI:" + key.ddi + " doesn't exist in the device description.");
			}
		}
		
		public ParserAPI.TimeLog createLog() {
			return new ParserAPI.TimeLog(uri, name, get(0).getTime(), get(size()-1).getTime(), size());
		}
		
		public List<Time> getEntries() {
			return tlg.getTimeList();
		}
		
		public List<ValueInfo> getValueInfos() {
			List<ValueInfo> valueInfos = new ArrayList<>(indexes.size());
			for(ValueKey key : indexes.keySet()) {
				valueInfos.add(infos.get(key));
			}
			return valueInfos;
		}
		
		@Override
		public Entry get(int index) {
			return new Entry(tlg.getTime(index));
		}

		@Override
		public int size() {
			return tlg.getTimeCount();
		}
		
		public class Entry extends AbstractList<Value> {
			public final Time tim;
			
			public Entry(Time tim) {
				this.tim = tim;
			}
			
			public Instant getTime() {
				return Instant.ofEpochSecond(tim.getStart().getSeconds(), tim.getStart().getNanos());
			}
			
			public double getLatitude() {
				return tim.getPositionStart().getPositionNorth();
			}
			
			public double getLongitude() {
				return tim.getPositionStart().getPositionEast();
			}
			
			public double getAltitude() {
				return tim.getPositionStart().getPositionUp() == 0 ? Double.NaN : tim.getPositionStart().getPositionUp()/1000.;
			}
			
			public Long[] getValueRow() {
				Long[] vals = new Long[indexes.size()];
				for(DataLogValue dlv : tim.getDataLogValueList()) {
					ValueKey key = new ValueKey(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi());
					Integer index = indexes.get(key);
					if(index == null) continue;
					vals[index] = dlv.getProcessDataValue();
				}
				return vals;
			}

			@Override
			public Value get(int index) throws IllegalArgumentException, IndexOutOfBoundsException {
				return new Value(tim.getDataLogValue(index));
			}

			@Override
			public int size() {
				return tim.getDataLogValueCount();
			}
		}
	}
	
	public class Value {
		private final ValueKey key;
		public final long val;
		
		public Value(DataLogValue dlv) throws IllegalArgumentException {
			key = new ValueKey(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi());
			if(!infos.containsKey(key))
				throw new IllegalArgumentException("The reference DET-" + dlv.getDeviceElementIdRef().getNumber() 
							+ " DDI:" + dlv.getProcessDataDdi() + " doesn't exist in the device description.");
			this.val = dlv.getProcessDataValue();
		}
		
		public ValueInfo info() {
			return infos.get(key);
		}
	}
	
	private static class ValueKey {
		public final long detnum;
		public final int ddi;
		
		public ValueKey(long detnum, int ddi) {
			this.detnum = detnum;
			this.ddi = ddi;
		}
		public ValueKey(UID det, int ddi) {
			this(det.getNumber(), ddi);
		}

		@Override
		public int hashCode() {
			return Objects.hash(ddi, detnum);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ValueKey))
				return false;
			ValueKey other = (ValueKey) obj;
			return ddi == other.ddi && detnum == other.detnum;
		}
	}
	
}
