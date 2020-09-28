package de.sdsd.projekt.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import com.google.protobuf.Timestamp;

import de.sdsd.projekt.api.ServiceResult.TimedPosition;
import de.sdsd.projekt.api.isoxml.Device;
import de.sdsd.projekt.api.isoxml.Device.DeviceElement;
import de.sdsd.projekt.api.isoxml.Device.DeviceElementType;
import de.sdsd.projekt.api.isoxml.Device.DeviceProcessData;
import de.sdsd.projekt.api.isoxml.Device.DeviceProperty;
import de.sdsd.projekt.api.isoxml.Device.DeviceValuePresentation;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.Task;
import de.sdsd.projekt.api.isoxml.Task.PositionStatus;
import de.sdsd.projekt.api.isoxml.Task.TaskStatus;
import de.sdsd.projekt.api.isoxml.TimeLog;
import de.sdsd.projekt.api.isoxml.TimeLog.ValueDeclaration;
import efdi.GrpcEfdi;
import efdi.GrpcEfdi.Position;

public class EFDItoISOXML {
	private final IsoxmlCreator creator;
	private final String deviceName;
	
	public EFDItoISOXML(GrpcEfdi.ISO11783_TaskData deviceDescription) throws ParserConfigurationException {
		this.creator = new IsoxmlCreator(deviceDescription.getManagementSoftwareManufacturer());
		setDeviceDescription(deviceDescription);
		StringBuilder deviceName = new StringBuilder();
		for(GrpcEfdi.Device dvc : deviceDescription.getDeviceList()) {
			deviceName.append(dvc.getDeviceDesignator()).append('_');
		}
		this.deviceName = deviceName.toString();
	}
	
	private void setDeviceDescription(GrpcEfdi.ISO11783_TaskData dd) {
		if(!dd.getManagementSoftwareVersion().isEmpty())
			creator.root.setManagementSoftwareVersion(dd.getManagementSoftwareVersion());
		if(!dd.getTaskControllerManufacturer().isEmpty())
			creator.root.setTaskControllerManufacturer(dd.getTaskControllerManufacturer());
		if(!dd.getTaskControllerVersion().isEmpty())
			creator.root.setTaskControllerVersion(dd.getTaskControllerVersion());
		if(!dd.getDataTransferLanguage().isEmpty())
			creator.root.setDataTransferLanguage(dd.getDataTransferLanguage());
		dd.getDeviceList().forEach(DVC::new);
	}
	
	private final Map<String, Elem> refs = new HashMap<>();
	private static String uri(String code, GrpcEfdi.UID uid) {
		return code + '-' + uid.getNumber();
	}
	private void setRef(String code, GrpcEfdi.UID uid, Elem el) {
		refs.put(uri(code, uid), el);
	}
	@SuppressWarnings("unchecked")
	private <T extends Elem> T getRef(String code, GrpcEfdi.UID uid) throws NoSuchElementException {
		if(uid.getNumber() == 0L) throw new NoSuchElementException();
		Elem e = refs.get(uri(code, uid));
		if(e == null) throw new NoSuchElementException();
		return (T) e;
	}
	
	private class DVC {
		private final Map<Integer, Elem> deviceObjects = new HashMap<>();
		public final Device dvc;
		
		@SuppressWarnings("unchecked")
		private <T extends Elem> void putORef(int oid, Consumer<T> setter) {
			if(oid != 0) {
				Elem e = deviceObjects.get(oid);
				if(e != null) setter.accept((T) e);
			}
		}
		
		public DVC(GrpcEfdi.Device efdi) {
			dvc = creator.root.addDevice(
					efdi.getClientName().toByteArray(), 
					efdi.getDeviceStructureLabel().toByteArray(), 
					efdi.getDeviceLocalizationLabel().toByteArray());
			if(!efdi.getDeviceDesignator().isEmpty())
				dvc.setDesignator(efdi.getDeviceDesignator());
			if(!efdi.getDeviceSoftwareVersion().isEmpty())
				dvc.setSoftwareVersion(efdi.getDeviceSoftwareVersion());
			if(!efdi.getDeviceSerialNumber().isEmpty())
				dvc.setSerialNumber(efdi.getDeviceSerialNumber());
			setRef("DVC", efdi.getDeviceId(), dvc);
			
			efdi.getDeviceValuePresentationList().forEach(this::addDeviceValuePresentation);
			efdi.getDeviceProcessDataList().forEach(this::addDeviceProcessData);
			efdi.getDevicePropertyList().forEach(this::addDeviceProperty);
			efdi.getDeviceElementList().forEach(this::addDeviceElement);
		}
		
		private void addDeviceValuePresentation(GrpcEfdi.DeviceValuePresentation efdi) {
			DeviceValuePresentation dvp = dvc.addDeviceValuePresentation(
					(int) efdi.getOffset(), efdi.getScale(), efdi.getNumberOfDecimals());
			if(!efdi.getUnitDesignator().isEmpty())
				dvp.setUnitDesignator(efdi.getUnitDesignator());
			deviceObjects.put(efdi.getDeviceValuePresentationObjectId(), dvp);
		}
		
		private void addDeviceProcessData(GrpcEfdi.DeviceProcessData efdi) {
			DeviceProcessData dpd = dvc.addDeviceProcessData(efdi.getDeviceProcessDataDdi(), 
					efdi.getDeviceProcessDataProperty(), efdi.getDeviceProcessDataTriggerMethods());
			if(!efdi.getDeviceProcessDataDesignator().isEmpty())
				dpd.setDesignator(efdi.getDeviceProcessDataDesignator());
			putORef(efdi.getDeviceValuePresentationObjectId(), dpd::setDeviceValuePresentation);
			deviceObjects.put(efdi.getDeviceProcessDataObjectId(), dpd);
		}
		
		private void addDeviceProperty(GrpcEfdi.DeviceProperty efdi) {
			DeviceProperty dpt = dvc.addDeviceProperty(efdi.getDevicePropertyDdi(), (int) efdi.getDevicePropertyValue());
			if(!efdi.getDevicePropertyDesignator().isEmpty())
				dpt.setDesignator(efdi.getDevicePropertyDesignator());
			putORef(efdi.getDeviceValuePresentationObjectId(), dpt::setDeviceValuePresentation);
			deviceObjects.put(efdi.getDevicePropertyObjectId(), dpt);
		}
		
		private void addDeviceElement(GrpcEfdi.DeviceElement efdi) {
			DeviceElement det = dvc.addDeviceElement(
					DeviceElementType.from(efdi.getDeviceElementTypeValue()).get(), 
					efdi.getDeviceElementNumber(), 
					(DeviceElement) deviceObjects.get(efdi.getParentObjectId()));
			if(!efdi.getDeviceElementDesignator().isEmpty())
				det.setDesignator(efdi.getDeviceElementDesignator());
			for(GrpcEfdi.DeviceObjectReference ref : efdi.getDeviceObjectReferenceList()) {
				Elem e = deviceObjects.get(ref.getDeviceObjectId());
				if(e instanceof DeviceProcessData)
					det.addDeviceProcessDataReference((DeviceProcessData) e);
				else if(e instanceof DeviceProperty)
					det.addDevicePropertyReference((DeviceProperty) e);
			}
			deviceObjects.put(efdi.getDeviceElementObjectId(), det);
			setRef("DET", efdi.getDeviceElementId(), det);
		}
	}
	
	private static TimedPosition tpos(GrpcEfdi.Time tim) {
		Timestamp ts = tim.getStart();
		Position pos = tim.getPositionStart();
		return new TimedPosition(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()), 
				pos.getPositionNorth(), pos.getPositionEast(), pos.getPositionUp());
	}
	
	public class TLG {
		public final GrpcEfdi.TimeLog tl;
		private final Task tsk;
		private final TimeLog tlg;
		
		private final Map<Integer, ValueDeclaration> vdmap = new LinkedHashMap<>();
		private final Map<Integer, Integer> vdindex = new HashMap<>();
		private final boolean isPositionUp;
		private final PositionStatus status;
		
		public TLG(GrpcEfdi.TimeLog tl) {
			if(tl.getTimeCount() == 0)
				throw new IllegalArgumentException("The given timelog is empty");
			this.tl = tl;
			LocalDate day = LocalDate.ofEpochDay(tl.getTime(0).getStart().getSeconds() / 86400);
			this.tsk = creator.root.addTask(deviceName + day.toString(), TaskStatus.PAUSED);
			this.tlg = tsk.addTimeLog();
			
			boolean isPositionUp = false;
			PositionStatus status = PositionStatus.SIMULATE_MODE;
			long dlvcount = 0;
			for(GrpcEfdi.Time tim : tl.getTimeList()) {
				isPositionUp |= tim.getPositionStart().getPositionUp() != 0;
				status = PositionStatus.from(tim.getPositionStart().getPositionStatusValue()).orElse(status);
				
				for(GrpcEfdi.DataLogValue dlv : tim.getDataLogValueList()) {
					++dlvcount;
					int hash = ((int) dlv.getDeviceElementIdRef().getNumber()) ^ dlv.getProcessDataDdi();
					if(vdmap.containsKey(hash)) continue;
					DeviceElement det = getRef("DET", dlv.getDeviceElementIdRef());
					vdmap.put(hash, new ValueDeclaration(dlv.getProcessDataDdi(), det));
					vdindex.put(hash, vdindex.size());
				}
			}
			tlg.setFileLength(tl.getTimeCount() * (isPositionUp ? 19 : 15) + dlvcount * 5);
			this.isPositionUp = isPositionUp;
			this.status = status;
		}
		
		public String getFilename() {
			return tlg.filename;
		}
		
		public void writeXML(OutputStream out) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
			tlg.writeXML(out, isPositionUp, status, vdmap.values().toArray(new ValueDeclaration[0]));
		}
		
		public void writeBIN(OutputStream out) throws IOException {
			Integer[] values = new Integer[vdmap.size()];
			for(GrpcEfdi.Time tim : tl.getTimeList()) {
				Arrays.fill(values, null);
				for(GrpcEfdi.DataLogValue dlv : tim.getDataLogValueList()) {
					int hash = ((int) dlv.getDeviceElementIdRef().getNumber()) ^ dlv.getProcessDataDdi();
					values[vdindex.get(hash)] = (int) dlv.getProcessDataValue();
				}
				tlg.writeTime(out, tpos(tim), values);
			}
		}
	}
	
	public TLG addTimeLog(GrpcEfdi.TimeLog tl) {
		return new TLG(tl);
	}
	
	public void writeTaskData(OutputStream out) throws TransformerFactoryConfigurationError, TransformerException {
		creator.writeTaskData(out);
	}
}
