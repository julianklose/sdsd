package de.sdsd.projekt.api.isoxml;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.codec.binary.Hex;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.ISO11783_TaskData;

public class Task extends Elem {

	public static enum TaskStatus {
		PLANNED(1), RUNNING(2), PAUSED(3), COMPLETED(4), TEMPLATE(5), CANCELED(6);
		
		public final int number;
		private TaskStatus(int number) {
			this.number = number;
		}
		
		public static Optional<TaskStatus> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<TaskStatus> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public Task(ISO11783_TaskData parent, String designator, TaskStatus status) {
		super(parent, "TSK");
		e.setAttribute("A", id);
		e.setAttribute("B", designator);
		e.setAttribute("G", Integer.toString(status.number));
	}

	public Task setCustomer(Customer ctr) {
		e.setAttribute("C", ctr.id);
		return this;
	}

	public Task setFarm(Farm frm) {
		e.setAttribute("D", frm.id);
		return this;
	}

	public Task setPartfield(Partfield pfd) {
		e.setAttribute("E", pfd.id);
		return this;
	}

	public Task setResponsibleWorker(Worker wkr) {
		e.setAttribute("F", wkr.id);
		return this;
	}
	
	public Task setDefaultTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("H", tzn.oid());
		return this;
	}
	
	public Task setPositionLostTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("I", tzn.oid());
		return this;
	}
	
	public Task setOutOfFieldTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("J", tzn.oid());
		return this;
	}
	
	public OperTechPractice addOperTechPractice(CulturalPractice cpc) {
		return new OperTechPractice(this, cpc);
	}
	
	public Connection addConnection(Device dvc0, Device.DeviceElement det0, Device dvc1, Device.DeviceElement det1) {
		return new Connection(this, dvc0, det0, dvc1, det1);
	}
	
	public ControlAssignment addControlAssignment(long sourceClientName, long userClientName, 
			byte[] sourceDeviceStructureLabel, byte[] userDeviceStructureLabel,
			int sourceDeviceElementNumber, int userDeviceElementNumber,
			int processDataDDI) {
		return new ControlAssignment(this, 
				sourceClientName, userClientName, 
				sourceDeviceStructureLabel, userDeviceStructureLabel, 
				sourceDeviceElementNumber, userDeviceElementNumber, 
				processDataDDI);
	}
	
	public ProductAllocation addProductAllocation(Partfield.Product pdt) {
		return new ProductAllocation(this, pdt);
	}
	
	public CommentAllocation addCommentAllocation() {
		return new CommentAllocation(this);
	}
	
	public WorkerAllocation addWorkerAllocation(Worker wkr) {
		return new WorkerAllocation(this, wkr);
	}
	
	public DeviceAllocation addDeviceAllocation(long clientNameValue) {
		return new DeviceAllocation(this, clientNameValue);
	}
	
	public GuidanceAllocation addGuidanceAllocation(Partfield.GuidanceGroup ggp) {
		return new GuidanceAllocation(this, ggp);
	}
	
	public DataLogTrigger addDataLogTrigger(int ddi, int method) {
		return new DataLogTrigger(this, ddi, method);
	}
	
	public Time addTime(OffsetDateTime start, TimeType type) {
		return new Time(this, start, type);
	}
	
	public TimeLog addTimeLog() {
		return new TimeLog(this);
	}
	
	public Grid addGridType1(double minNorth, double minEast, 
			double cellSizeNorth, double cellSizeEast, 
			int maxCol, int maxRow) {
		return new Grid.GridType1(this, minNorth, minEast, 
				cellSizeNorth, cellSizeEast, 
				maxCol, maxRow);
	}
	
	public Grid addGridType2(double minNorth, double minEast, 
			double cellSizeNorth, double cellSizeEast, 
			int maxCol, int maxRow, Grid.TreatmentZone tzn) {
		return new Grid.GridType2(this, minNorth, minEast, 
				cellSizeNorth, cellSizeEast, 
				maxCol, maxRow, tzn);
	}
	
	public Grid.TreatmentZone addTreatmentZone() {
		return new Grid.TreatmentZone(this);
	}

	public static class Customer extends Elem {
		public Customer(ISO11783_TaskData parent, String lastName) {
			super(parent, "CTR");
			e.setAttribute("A", id);
			e.setAttribute("B", lastName);
		}
		
		public Customer setFirstName(String firstName) {
			e.setAttribute("C", firstName);
			return this;
		}
		
		public Customer setStreet(String street) {
			e.setAttribute("D", street);
			return this;
		}
		
		public Customer setPoBox(String pobox) {
			e.setAttribute("E", pobox);
			return this;
		}
		
		public Customer setPostalCode(String postalCode) {
			e.setAttribute("F", postalCode);
			return this;
		}
		
		public Customer setCity(String city) {
			e.setAttribute("G", city);
			return this;
		}
		
		public Customer setState(String state) {
			e.setAttribute("H", state);
			return this;
		}
		
		public Customer setCountry(String country) {
			e.setAttribute("I", country);
			return this;
		}
		
		public Customer setPhone(String phone) {
			e.setAttribute("J", phone);
			return this;
		}
		
		public Customer setMobile(String mobile) {
			e.setAttribute("K", mobile);
			return this;
		}
		
		public Customer setFax(String fax) {
			e.setAttribute("L", fax);
			return this;
		}
		
		public Customer setEmail(String email) {
			e.setAttribute("M", email);
			return this;
		}
	}

	public static class Farm extends Elem {
		public Farm(ISO11783_TaskData parent, String designator) {
			super(parent, "FRM");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public Farm setStreet(String street) {
			e.setAttribute("C", street);
			return this;
		}
		
		public Farm setPoBox(String pobox) {
			e.setAttribute("D", pobox);
			return this;
		}
		
		public Farm setPostalCode(String postalCode) {
			e.setAttribute("E", postalCode);
			return this;
		}
		
		public Farm setCity(String city) {
			e.setAttribute("F", city);
			return this;
		}
		
		public Farm setState(String state) {
			e.setAttribute("G", state);
			return this;
		}
		
		public Farm setCountry(String country) {
			e.setAttribute("H", country);
			return this;
		}
		
		public Farm setCustomer(Customer customer) {
			e.setAttribute("I", customer.id);
			return this;
		}
	}

	public static class Worker extends Elem {
		public Worker(ISO11783_TaskData parent, String lastName) {
			super(parent, "WKR");
			e.setAttribute("A", id);
			e.setAttribute("B", lastName);
		}
		
		public Worker setFirstName(String firstName) {
			e.setAttribute("C", firstName);
			return this;
		}
		
		public Worker setStreet(String street) {
			e.setAttribute("D", street);
			return this;
		}
		
		public Worker setPoBox(String pobox) {
			e.setAttribute("E", pobox);
			return this;
		}
		
		public Worker setPostalCode(String postalCode) {
			e.setAttribute("F", postalCode);
			return this;
		}
		
		public Worker setCity(String city) {
			e.setAttribute("G", city);
			return this;
		}
		
		public Worker setState(String state) {
			e.setAttribute("H", state);
			return this;
		}
		
		public Worker setCountry(String country) {
			e.setAttribute("I", country);
			return this;
		}
		
		public Worker setPhone(String phone) {
			e.setAttribute("J", phone);
			return this;
		}
		
		public Worker setMobile(String mobile) {
			e.setAttribute("K", mobile);
			return this;
		}
		
		public Worker setLicenseNumber(String licenseNumber) {
			e.setAttribute("L", licenseNumber);
			return this;
		}
		
		public Worker setEmail(String email) {
			e.setAttribute("M", email);
			return this;
		}
	}
	
	public static class CulturalPractice extends Elem {
		public CulturalPractice(ISO11783_TaskData parent, String designator) {
			super(parent, "CPC");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
		
		public OperationTechniqueReference addOperationTechniqueReference(OperationTechnique otq) {
			return new OperationTechniqueReference(this, otq);
		}
	}
	
	public static class OperationTechniqueReference extends Elem {
		public OperationTechniqueReference(CulturalPractice parent, OperationTechnique otq) {
			super(parent, "OTR");
			e.setAttribute("A", otq.id);
		}
	}
	
	public static class OperationTechnique extends Elem {
		public OperationTechnique(ISO11783_TaskData parent, String designator) {
			super(parent, "OTQ");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}
	
	public static class OperTechPractice extends Elem {
		public OperTechPractice(Task parent, CulturalPractice cpc) {
			super(parent, "OTP");
			e.setAttribute("A", cpc.id);
		}
		
		public OperTechPractice setOperationTechnique(OperationTechnique otq) {
			e.setAttribute("B", otq.id);
			return this;
		}
	}
	
	public static class Connection extends Elem {
		public Connection(Task parent, Device dvc0, Device.DeviceElement det0, Device dvc1, Device.DeviceElement det1) {
			super(parent, "CNN");
			e.setAttribute("A", dvc0.id);
			e.setAttribute("B", det0.id);
			e.setAttribute("C", dvc1.id);
			e.setAttribute("D", det1.id);
		}
	}
	
	public static enum AllocationStampType {
		PLANNED(1), EFFECTIVE(4);
		
		public final int number;
		private AllocationStampType(int number) {
			this.number = number;
		}
		
		public static Optional<AllocationStampType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<AllocationStampType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class AllocationStamp extends Elem {
		private AllocationStamp(Elem parent, OffsetDateTime start, AllocationStampType type) {
			super(parent, "ASP");
			e.setAttribute("A", start.toString());
			e.setAttribute("D", Integer.toString(type.number));
		}
		public AllocationStamp(CommentAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(ControlAssignment parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(DeviceAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(GuidanceAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(GuidanceShift parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(ProductAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		public AllocationStamp(WorkerAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem)parent, start, type);
		}
		
		public AllocationStamp setStop(OffsetDateTime stop) {
			e.setAttribute("B", stop.toString());
			return this;
		}
		
		public AllocationStamp setDuration(Duration duration) {
			e.setAttribute("C", Long.toString(duration.getSeconds()));
			return this;
		}
		
		public Position addPosition(double north, double east, PositionStatus status) {
			return new Position(this, north, east, status);
		}
	}
	
	public static class ControlAssignment extends Elem {
		public ControlAssignment(Task parent, 
				long sourceClientName, long userClientName, 
				byte[] sourceDeviceStructureLabel, byte[] userDeviceStructureLabel,
				int sourceDeviceElementNumber, int userDeviceElementNumber,
				int processDataDDI) {
			super(parent, "CAT");
			if(sourceDeviceStructureLabel.length > 39 || userDeviceStructureLabel.length > 39)
				throw new IllegalArgumentException("invalid device structure label");
			if(sourceDeviceElementNumber < 0 || sourceDeviceElementNumber > 4095
					|| userDeviceElementNumber < 0 || userDeviceElementNumber > 4095)
				throw new IllegalArgumentException("invalid device element number");
			e.setAttribute("A", Long.toHexString(sourceClientName).toUpperCase());
			e.setAttribute("B", Long.toHexString(userClientName).toUpperCase());
			e.setAttribute("C", Hex.encodeHexString(sourceDeviceStructureLabel, false));
			e.setAttribute("D", Hex.encodeHexString(userDeviceStructureLabel, false));
			e.setAttribute("E", Integer.toString(sourceDeviceElementNumber));
			e.setAttribute("F", Integer.toString(userDeviceElementNumber));
			e.setAttribute("G", ddi(processDataDDI));
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static enum TransferMode {
		FILLING(1), EMPTYING(2), REMAINDER(3);
		
		public final int number;
		private TransferMode(int number) {
			this.number = number;
		}
		
		public static Optional<TransferMode> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<TransferMode> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class ProductAllocation extends Elem {
		public ProductAllocation(Task parent, Partfield.Product pdt) {
			super(parent, "PAN");
			e.setAttribute("A", pdt.id);
		}
		
		public ProductAllocation setQuantityDDI(int ddi) {
			e.setAttribute("B", ddi(ddi));
			return this;
		}
		
		public ProductAllocation setQuantityValue(int value) {
			if(value < 0)
				throw new IllegalArgumentException("invalid quantity value");
			e.setAttribute("C", Integer.toString(value));
			return this;
		}
		
		public ProductAllocation setTransferMode(TransferMode transferMode) {
			e.setAttribute("D", Integer.toString(transferMode.number));
			return this;
		}
		
		public ProductAllocation setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("E", det.id);
			return this;
		}
		
		public ProductAllocation setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("F", vpn.id);
			return this;
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static enum CodedCommentScope {
		POINT(1), GLOBAL(2), CONTINUOUS(3);
		
		public final int number;
		private CodedCommentScope(int number) {
			this.number = number;
		}
		
		public static Optional<CodedCommentScope> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<CodedCommentScope> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class CodedComment extends Elem {
		public CodedComment(ISO11783_TaskData parent, String designator, CodedCommentScope scope) {
			super(parent, "CCT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
			e.setAttribute("C", Integer.toString(scope.number));
		}
		
		public CodedComment setGroup(CodedCommentGroup ccg) {
			e.setAttribute("D", ccg.id);
			return this;
		}
		
		public CodedCommentListValue addListValue(String designator) {
			return new CodedCommentListValue(this, designator);
		}
	}
	
	public static class CodedCommentGroup extends Elem {
		public CodedCommentGroup(ISO11783_TaskData parent, String designator) {
			super(parent, "CCG");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}
	
	public static class CodedCommentListValue extends Elem {
		public CodedCommentListValue(CodedComment parent, String designator) {
			super(parent, "CCL");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}
	
	public static class CommentAllocation extends Elem {
		public CommentAllocation(Task parent) {
			super(parent, "CAN");
		}
		
		public CommentAllocation setCodedComment(CodedComment cct) {
			e.setAttribute("A", cct.id);
			return this;
		}
		
		public CommentAllocation setCodedCommentListValue(CodedCommentListValue ccl) {
			e.setAttribute("B", ccl.id);
			return this;
		}
		
		public CommentAllocation setFreeCommentText(String text) {
			e.setAttribute("C", text);
			return this;
		}

		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static class WorkerAllocation extends Elem {
		public WorkerAllocation(Task parent, Worker wkr) {
			super(parent, "WAN");
			e.setAttribute("A", wkr.id);
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static class DeviceAllocation extends Elem {
		public DeviceAllocation(Task parent, long clientNameValue) {
			super(parent, "DAN");
			e.setAttribute("A", Long.toHexString(clientNameValue));
		}
		
		public DeviceAllocation setClientNameMask(long clientNameMask) {
			e.setAttribute("B", Long.toHexString(clientNameMask));
			return this;
		}
		
		public DeviceAllocation setDevice(Device dvc) {
			e.setAttribute("C", dvc.id);
			return this;
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static class GuidanceShift extends Elem {
		public GuidanceShift(GuidanceAllocation parent, Partfield.GuidanceGroup ggp, Partfield.GuidancePattern gpn) {
			super(parent, "GST");
			e.setAttribute("A", ggp.id);
			e.setAttribute("B", gpn.id);
		}
		
		public GuidanceShift setEastShift(int east) {
			e.setAttribute("C", Integer.toString(east));
			return this;
		}
		
		public GuidanceShift setNorthShift(int north) {
			e.setAttribute("D", Integer.toString(north));
			return this;
		}
		
		public GuidanceShift setPropagationOffset(int offset) {
			e.setAttribute("E", Integer.toString(offset));
			return this;
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}
	
	public static class GuidanceAllocation extends Elem {
		public GuidanceAllocation(Task parent, Partfield.GuidanceGroup ggp) {
			super(parent, "GAN");
			e.setAttribute("A", ggp.id);
		}
		
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
		
		public GuidanceShift addGuidanceShift(Partfield.GuidanceGroup ggp, Partfield.GuidancePattern gpn) {
			return new GuidanceShift(this, ggp, gpn);
		}
	}
	
	public static class DataLogTrigger extends Elem {
		public DataLogTrigger(Task parent, int ddi, int method) {
			super(parent, "DLT");
			e.setAttribute("A", ddi(ddi));
			if(method < 1 || method > 31)
				throw new IllegalArgumentException("invalid method");
			e.setAttribute("B", Integer.toString(method));
		}
		
		public DataLogTrigger setDistanceInterval(long distanceInterval) {
			if(distanceInterval < 0 || distanceInterval > 1000000)
				throw new IllegalArgumentException("invalid distance interval");
			e.setAttribute("C", Long.toString(distanceInterval));
			return this;
		}
		
		public DataLogTrigger setTimeInterval(long timeInterval) {
			if(timeInterval < 0 || timeInterval > 60000)
				throw new IllegalArgumentException("invalid time interval");
			e.setAttribute("D", Long.toString(timeInterval));
			return this;
		}
		
		public DataLogTrigger setThresholdMinimum(int thresholdMinimum) {
			e.setAttribute("E", Integer.toString(thresholdMinimum));
			return this;
		}
		
		public DataLogTrigger setThresholdMaximum(int thresholdMaximum) {
			e.setAttribute("F", Integer.toString(thresholdMaximum));
			return this;
		}
		
		public DataLogTrigger setThresholdChange(int thresholdChange) {
			e.setAttribute("G", Integer.toString(thresholdChange));
			return this;
		}
		
		public DataLogTrigger setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("H", det.id);
			return this;
		}
		
		public DataLogTrigger setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("I", vpn.id);
			return this;
		}
		
		public DataLogTrigger setPGN(long pgn) {
			if(pgn < 0 || pgn > 262143)
				throw new IllegalArgumentException("invalid PGN");
			e.setAttribute("J", Long.toString(pgn));
			return this;
		}
		
		public DataLogTrigger setPgnStartBit(int pgnStartBit) {
			if(pgnStartBit < 0 || pgnStartBit > 63)
				throw new IllegalArgumentException("invalid PGN start bit");
			e.setAttribute("K", Long.toString(pgnStartBit));
			return this;
		}
		
		public DataLogTrigger setPgnStopBit(int pgnStopBit) {
			if(pgnStopBit < 0 || pgnStopBit > 63)
				throw new IllegalArgumentException("invalid PGN stop bit");
			e.setAttribute("L", Long.toString(pgnStopBit));
			return this;
		}
	}
	
	public static enum PositionStatus {
		NO_GPS(0), GNSS(1), DGNSS(2), PRECISE_GNSS(3), RTK_FINTEGER(4), RTK_FLOAT(5), 
		EST_DR_MODE(6), MANUAL_INPUT(7), SIMULATE_MODE(8), ERROR(14), NOT_AVAILABLE(15);
		
		public final int number;
		private PositionStatus(int number) {
			this.number = number;
		}
		
		public static Optional<PositionStatus> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<PositionStatus> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class GpsTime {
		private static final Instant gpsTimeStart = Instant.ofEpochSecond(315532800); // 1980-01-01
		public final long date, time;
		
		public GpsTime(Instant timestamp) {
			Duration duration = Duration.between(gpsTimeStart, timestamp);
			date = duration.toDays();
			time = duration.minusDays(date).toMillis();
		}

		public long getDate() {
			return date;
		}

		public long getTime() {
			return time;
		}
	}
	
	public static class Position extends Elem {
		private Position(Elem parent, double north, double east, PositionStatus status) {
			super(parent, "PTN");
			e.setAttribute("A", north(north));
			e.setAttribute("B", east(east));
			e.setAttribute("D", Integer.toString(status.number));
		}
		public Position(AllocationStamp parent, double north, double east, PositionStatus status) {
			this((Elem)parent, north, east, status);
		}
		public Position(Time parent, double north, double east, PositionStatus status) {
			this((Elem)parent, north, east, status);
		}
		
		public Position setUp(double up) {
			e.setAttribute("C", Long.toString(Math.round(up * 1e3)));
			return this;
		}
		
		public Position setPDOP(float pdop) {
			if(pdop < 0 || pdop > 99.9)
				throw new IllegalArgumentException("invalid PDOP");
			e.setAttribute("E", floating(pdop));
			return this;
		}
		
		public Position setHDOP(float hdop) {
			if(hdop < 0 || hdop > 99.9)
				throw new IllegalArgumentException("invalid HDOP");
			e.setAttribute("F", floating(hdop));
			return this;
		}
		
		public Position setNumberOfSatellites(int numberOfSatellites) {
			if(numberOfSatellites < 0 || numberOfSatellites > 255)
				throw new IllegalArgumentException("invalid number of satellites");
			e.setAttribute("G", Integer.toString(numberOfSatellites));
			return this;
		}
		
		public Position setGpsUtc(Instant timestamp) {
			GpsTime gpsTime = new GpsTime(timestamp);
			e.setAttribute("H", Long.toString(gpsTime.time));
			e.setAttribute("I", Long.toString(gpsTime.date));
			return this;
		}
	}
	
	public static enum TimeType {
		PLANNED(1), PRELIMINARY(2), EFFECTIVE(4), INEFFECTIVE(5), REPAIR(6), CLEARING(7), POWERED_DOWN(8);
		
		public final int number;
		private TimeType(int number) {
			this.number = number;
		}
		
		public static Optional<TimeType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<TimeType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class Time extends Elem {
		public Time(Task parent, OffsetDateTime start, TimeType type) {
			super(parent, "TIM");
			e.setAttribute("A", start.toString());
			e.setAttribute("D", Integer.toString(type.number));
		}
		
		public Time setStop(OffsetDateTime stop) {
			e.setAttribute("B", stop.toString());
			return this;
		}
		
		public Time setDuration(Duration duration) {
			e.setAttribute("C", Long.toString(duration.getSeconds()));
			return this;
		}
		
		public Position addPosition(double north, double east, PositionStatus status) {
			return new Position(this, north, east, status);
		}
		
		public DataLogValue addDataLogValue(int processDataDDI, int processDataValue, Device.DeviceElement det) {
			return new DataLogValue(this, processDataDDI, processDataValue, det);
		}
	}
	
	public static class DataLogValue extends Elem {
		public DataLogValue(Time parent, int processDataDDI, int processDataValue, Device.DeviceElement det) {
			super(parent, "DLV");
			e.setAttribute("A", ddi(processDataDDI));
			e.setAttribute("B", Integer.toString(processDataValue));
			e.setAttribute("C", det.id);
		}
		
		public DataLogValue setPGN(long pgn) {
			if(pgn < 0 || pgn > 262143)
				throw new IllegalArgumentException("invalid PGN");
			e.setAttribute("D", Long.toString(pgn));
			return this;
		}
		
		public DataLogValue setPgnStartBit(int pgnStartBit) {
			if(pgnStartBit < 0 || pgnStartBit > 63)
				throw new IllegalArgumentException("invalid PGN start bit");
			e.setAttribute("E", Long.toString(pgnStartBit));
			return this;
		}
		
		public DataLogValue setPgnStopBit(int pgnStopBit) {
			if(pgnStopBit < 0 || pgnStopBit > 63)
				throw new IllegalArgumentException("invalid PGN stop bit");
			e.setAttribute("F", Long.toString(pgnStopBit));
			return this;
		}
	}
	
}