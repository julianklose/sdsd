package de.sdsd.projekt.api.isoxml;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.codec.binary.Hex;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.ISO11783_TaskData;

/**
 * The Class Task.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class Task extends Elem {

	/**
	 * The Enum TaskStatus.
	 */
	public static enum TaskStatus {

		/** The planned. */
		PLANNED(1),
		/** The running. */
		RUNNING(2),
		/** The paused. */
		PAUSED(3),
		/** The completed. */
		COMPLETED(4),
		/** The template. */
		TEMPLATE(5),
		/** The canceled. */
		CANCELED(6);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new task status.
		 *
		 * @param number the number
		 */
		private TaskStatus(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<TaskStatus> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<TaskStatus> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * Instantiates a new task.
	 *
	 * @param parent     the parent
	 * @param designator the designator
	 * @param status     the status
	 */
	public Task(ISO11783_TaskData parent, String designator, TaskStatus status) {
		super(parent, "TSK");
		e.setAttribute("A", id);
		e.setAttribute("B", designator);
		e.setAttribute("G", Integer.toString(status.number));
	}

	/**
	 * Sets the customer.
	 *
	 * @param ctr the ctr
	 * @return the task
	 */
	public Task setCustomer(Customer ctr) {
		e.setAttribute("C", ctr.id);
		return this;
	}

	/**
	 * Sets the farm.
	 *
	 * @param frm the frm
	 * @return the task
	 */
	public Task setFarm(Farm frm) {
		e.setAttribute("D", frm.id);
		return this;
	}

	/**
	 * Sets the partfield.
	 *
	 * @param pfd the pfd
	 * @return the task
	 */
	public Task setPartfield(Partfield pfd) {
		e.setAttribute("E", pfd.id);
		return this;
	}

	/**
	 * Sets the responsible worker.
	 *
	 * @param wkr the wkr
	 * @return the task
	 */
	public Task setResponsibleWorker(Worker wkr) {
		e.setAttribute("F", wkr.id);
		return this;
	}

	/**
	 * Sets the default treatment zone code.
	 *
	 * @param tzn the tzn
	 * @return the task
	 */
	public Task setDefaultTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("H", tzn.oid());
		return this;
	}

	/**
	 * Sets the position lost treatment zone code.
	 *
	 * @param tzn the tzn
	 * @return the task
	 */
	public Task setPositionLostTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("I", tzn.oid());
		return this;
	}

	/**
	 * Sets the out of field treatment zone code.
	 *
	 * @param tzn the tzn
	 * @return the task
	 */
	public Task setOutOfFieldTreatmentZoneCode(Grid.TreatmentZone tzn) {
		e.setAttribute("J", tzn.oid());
		return this;
	}

	/**
	 * Adds the oper tech practice.
	 *
	 * @param cpc the cpc
	 * @return the oper tech practice
	 */
	public OperTechPractice addOperTechPractice(CulturalPractice cpc) {
		return new OperTechPractice(this, cpc);
	}

	/**
	 * Adds the connection.
	 *
	 * @param dvc0 the dvc 0
	 * @param det0 the det 0
	 * @param dvc1 the dvc 1
	 * @param det1 the det 1
	 * @return the connection
	 */
	public Connection addConnection(Device dvc0, Device.DeviceElement det0, Device dvc1, Device.DeviceElement det1) {
		return new Connection(this, dvc0, det0, dvc1, det1);
	}

	/**
	 * Adds the control assignment.
	 *
	 * @param sourceClientName           the source client name
	 * @param userClientName             the user client name
	 * @param sourceDeviceStructureLabel the source device structure label
	 * @param userDeviceStructureLabel   the user device structure label
	 * @param sourceDeviceElementNumber  the source device element number
	 * @param userDeviceElementNumber    the user device element number
	 * @param processDataDDI             the process data DDI
	 * @return the control assignment
	 */
	public ControlAssignment addControlAssignment(long sourceClientName, long userClientName,
			byte[] sourceDeviceStructureLabel, byte[] userDeviceStructureLabel, int sourceDeviceElementNumber,
			int userDeviceElementNumber, int processDataDDI) {
		return new ControlAssignment(this, sourceClientName, userClientName, sourceDeviceStructureLabel,
				userDeviceStructureLabel, sourceDeviceElementNumber, userDeviceElementNumber, processDataDDI);
	}

	/**
	 * Adds the product allocation.
	 *
	 * @param pdt the pdt
	 * @return the product allocation
	 */
	public ProductAllocation addProductAllocation(Partfield.Product pdt) {
		return new ProductAllocation(this, pdt);
	}

	/**
	 * Adds the comment allocation.
	 *
	 * @return the comment allocation
	 */
	public CommentAllocation addCommentAllocation() {
		return new CommentAllocation(this);
	}

	/**
	 * Adds the worker allocation.
	 *
	 * @param wkr the wkr
	 * @return the worker allocation
	 */
	public WorkerAllocation addWorkerAllocation(Worker wkr) {
		return new WorkerAllocation(this, wkr);
	}

	/**
	 * Adds the device allocation.
	 *
	 * @param clientNameValue the client name value
	 * @return the device allocation
	 */
	public DeviceAllocation addDeviceAllocation(long clientNameValue) {
		return new DeviceAllocation(this, clientNameValue);
	}

	/**
	 * Adds the guidance allocation.
	 *
	 * @param ggp the ggp
	 * @return the guidance allocation
	 */
	public GuidanceAllocation addGuidanceAllocation(Partfield.GuidanceGroup ggp) {
		return new GuidanceAllocation(this, ggp);
	}

	/**
	 * Adds the data log trigger.
	 *
	 * @param ddi    the ddi
	 * @param method the method
	 * @return the data log trigger
	 */
	public DataLogTrigger addDataLogTrigger(int ddi, int method) {
		return new DataLogTrigger(this, ddi, method);
	}

	/**
	 * Adds the time.
	 *
	 * @param start the start
	 * @param type  the type
	 * @return the time
	 */
	public Time addTime(OffsetDateTime start, TimeType type) {
		return new Time(this, start, type);
	}

	/**
	 * Adds the time log.
	 *
	 * @return the time log
	 */
	public TimeLog addTimeLog() {
		return new TimeLog(this);
	}

	/**
	 * Adds the grid type 1.
	 *
	 * @param minNorth      the min north
	 * @param minEast       the min east
	 * @param cellSizeNorth the cell size north
	 * @param cellSizeEast  the cell size east
	 * @param maxCol        the max col
	 * @param maxRow        the max row
	 * @return the grid
	 */
	public Grid addGridType1(double minNorth, double minEast, double cellSizeNorth, double cellSizeEast, int maxCol,
			int maxRow) {
		return new Grid.GridType1(this, minNorth, minEast, cellSizeNorth, cellSizeEast, maxCol, maxRow);
	}

	/**
	 * Adds the grid type 2.
	 *
	 * @param minNorth      the min north
	 * @param minEast       the min east
	 * @param cellSizeNorth the cell size north
	 * @param cellSizeEast  the cell size east
	 * @param maxCol        the max col
	 * @param maxRow        the max row
	 * @param tzn           the tzn
	 * @return the grid
	 */
	public Grid addGridType2(double minNorth, double minEast, double cellSizeNorth, double cellSizeEast, int maxCol,
			int maxRow, Grid.TreatmentZone tzn) {
		return new Grid.GridType2(this, minNorth, minEast, cellSizeNorth, cellSizeEast, maxCol, maxRow, tzn);
	}

	/**
	 * Adds the treatment zone.
	 *
	 * @return the grid. treatment zone
	 */
	public Grid.TreatmentZone addTreatmentZone() {
		return new Grid.TreatmentZone(this);
	}

	/**
	 * The Class Customer.
	 */
	public static class Customer extends Elem {

		/**
		 * Instantiates a new customer.
		 *
		 * @param parent   the parent
		 * @param lastName the last name
		 */
		public Customer(ISO11783_TaskData parent, String lastName) {
			super(parent, "CTR");
			e.setAttribute("A", id);
			e.setAttribute("B", lastName);
		}

		/**
		 * Sets the first name.
		 *
		 * @param firstName the first name
		 * @return the customer
		 */
		public Customer setFirstName(String firstName) {
			e.setAttribute("C", firstName);
			return this;
		}

		/**
		 * Sets the street.
		 *
		 * @param street the street
		 * @return the customer
		 */
		public Customer setStreet(String street) {
			e.setAttribute("D", street);
			return this;
		}

		/**
		 * Sets the po box.
		 *
		 * @param pobox the pobox
		 * @return the customer
		 */
		public Customer setPoBox(String pobox) {
			e.setAttribute("E", pobox);
			return this;
		}

		/**
		 * Sets the postal code.
		 *
		 * @param postalCode the postal code
		 * @return the customer
		 */
		public Customer setPostalCode(String postalCode) {
			e.setAttribute("F", postalCode);
			return this;
		}

		/**
		 * Sets the city.
		 *
		 * @param city the city
		 * @return the customer
		 */
		public Customer setCity(String city) {
			e.setAttribute("G", city);
			return this;
		}

		/**
		 * Sets the state.
		 *
		 * @param state the state
		 * @return the customer
		 */
		public Customer setState(String state) {
			e.setAttribute("H", state);
			return this;
		}

		/**
		 * Sets the country.
		 *
		 * @param country the country
		 * @return the customer
		 */
		public Customer setCountry(String country) {
			e.setAttribute("I", country);
			return this;
		}

		/**
		 * Sets the phone.
		 *
		 * @param phone the phone
		 * @return the customer
		 */
		public Customer setPhone(String phone) {
			e.setAttribute("J", phone);
			return this;
		}

		/**
		 * Sets the mobile.
		 *
		 * @param mobile the mobile
		 * @return the customer
		 */
		public Customer setMobile(String mobile) {
			e.setAttribute("K", mobile);
			return this;
		}

		/**
		 * Sets the fax.
		 *
		 * @param fax the fax
		 * @return the customer
		 */
		public Customer setFax(String fax) {
			e.setAttribute("L", fax);
			return this;
		}

		/**
		 * Sets the email.
		 *
		 * @param email the email
		 * @return the customer
		 */
		public Customer setEmail(String email) {
			e.setAttribute("M", email);
			return this;
		}
	}

	/**
	 * The Class Farm.
	 */
	public static class Farm extends Elem {

		/**
		 * Instantiates a new farm.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public Farm(ISO11783_TaskData parent, String designator) {
			super(parent, "FRM");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Sets the street.
		 *
		 * @param street the street
		 * @return the farm
		 */
		public Farm setStreet(String street) {
			e.setAttribute("C", street);
			return this;
		}

		/**
		 * Sets the po box.
		 *
		 * @param pobox the pobox
		 * @return the farm
		 */
		public Farm setPoBox(String pobox) {
			e.setAttribute("D", pobox);
			return this;
		}

		/**
		 * Sets the postal code.
		 *
		 * @param postalCode the postal code
		 * @return the farm
		 */
		public Farm setPostalCode(String postalCode) {
			e.setAttribute("E", postalCode);
			return this;
		}

		/**
		 * Sets the city.
		 *
		 * @param city the city
		 * @return the farm
		 */
		public Farm setCity(String city) {
			e.setAttribute("F", city);
			return this;
		}

		/**
		 * Sets the state.
		 *
		 * @param state the state
		 * @return the farm
		 */
		public Farm setState(String state) {
			e.setAttribute("G", state);
			return this;
		}

		/**
		 * Sets the country.
		 *
		 * @param country the country
		 * @return the farm
		 */
		public Farm setCountry(String country) {
			e.setAttribute("H", country);
			return this;
		}

		/**
		 * Sets the customer.
		 *
		 * @param customer the customer
		 * @return the farm
		 */
		public Farm setCustomer(Customer customer) {
			e.setAttribute("I", customer.id);
			return this;
		}
	}

	/**
	 * The Class Worker.
	 */
	public static class Worker extends Elem {

		/**
		 * Instantiates a new worker.
		 *
		 * @param parent   the parent
		 * @param lastName the last name
		 */
		public Worker(ISO11783_TaskData parent, String lastName) {
			super(parent, "WKR");
			e.setAttribute("A", id);
			e.setAttribute("B", lastName);
		}

		/**
		 * Sets the first name.
		 *
		 * @param firstName the first name
		 * @return the worker
		 */
		public Worker setFirstName(String firstName) {
			e.setAttribute("C", firstName);
			return this;
		}

		/**
		 * Sets the street.
		 *
		 * @param street the street
		 * @return the worker
		 */
		public Worker setStreet(String street) {
			e.setAttribute("D", street);
			return this;
		}

		/**
		 * Sets the po box.
		 *
		 * @param pobox the pobox
		 * @return the worker
		 */
		public Worker setPoBox(String pobox) {
			e.setAttribute("E", pobox);
			return this;
		}

		/**
		 * Sets the postal code.
		 *
		 * @param postalCode the postal code
		 * @return the worker
		 */
		public Worker setPostalCode(String postalCode) {
			e.setAttribute("F", postalCode);
			return this;
		}

		/**
		 * Sets the city.
		 *
		 * @param city the city
		 * @return the worker
		 */
		public Worker setCity(String city) {
			e.setAttribute("G", city);
			return this;
		}

		/**
		 * Sets the state.
		 *
		 * @param state the state
		 * @return the worker
		 */
		public Worker setState(String state) {
			e.setAttribute("H", state);
			return this;
		}

		/**
		 * Sets the country.
		 *
		 * @param country the country
		 * @return the worker
		 */
		public Worker setCountry(String country) {
			e.setAttribute("I", country);
			return this;
		}

		/**
		 * Sets the phone.
		 *
		 * @param phone the phone
		 * @return the worker
		 */
		public Worker setPhone(String phone) {
			e.setAttribute("J", phone);
			return this;
		}

		/**
		 * Sets the mobile.
		 *
		 * @param mobile the mobile
		 * @return the worker
		 */
		public Worker setMobile(String mobile) {
			e.setAttribute("K", mobile);
			return this;
		}

		/**
		 * Sets the license number.
		 *
		 * @param licenseNumber the license number
		 * @return the worker
		 */
		public Worker setLicenseNumber(String licenseNumber) {
			e.setAttribute("L", licenseNumber);
			return this;
		}

		/**
		 * Sets the email.
		 *
		 * @param email the email
		 * @return the worker
		 */
		public Worker setEmail(String email) {
			e.setAttribute("M", email);
			return this;
		}
	}

	/**
	 * The Class CulturalPractice.
	 */
	public static class CulturalPractice extends Elem {

		/**
		 * Instantiates a new cultural practice.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public CulturalPractice(ISO11783_TaskData parent, String designator) {
			super(parent, "CPC");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Adds the operation technique reference.
		 *
		 * @param otq the otq
		 * @return the operation technique reference
		 */
		public OperationTechniqueReference addOperationTechniqueReference(OperationTechnique otq) {
			return new OperationTechniqueReference(this, otq);
		}
	}

	/**
	 * The Class OperationTechniqueReference.
	 */
	public static class OperationTechniqueReference extends Elem {

		/**
		 * Instantiates a new operation technique reference.
		 *
		 * @param parent the parent
		 * @param otq    the otq
		 */
		public OperationTechniqueReference(CulturalPractice parent, OperationTechnique otq) {
			super(parent, "OTR");
			e.setAttribute("A", otq.id);
		}
	}

	/**
	 * The Class OperationTechnique.
	 */
	public static class OperationTechnique extends Elem {

		/**
		 * Instantiates a new operation technique.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public OperationTechnique(ISO11783_TaskData parent, String designator) {
			super(parent, "OTQ");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}

	/**
	 * The Class OperTechPractice.
	 */
	public static class OperTechPractice extends Elem {

		/**
		 * Instantiates a new oper tech practice.
		 *
		 * @param parent the parent
		 * @param cpc    the cpc
		 */
		public OperTechPractice(Task parent, CulturalPractice cpc) {
			super(parent, "OTP");
			e.setAttribute("A", cpc.id);
		}

		/**
		 * Sets the operation technique.
		 *
		 * @param otq the otq
		 * @return the oper tech practice
		 */
		public OperTechPractice setOperationTechnique(OperationTechnique otq) {
			e.setAttribute("B", otq.id);
			return this;
		}
	}

	/**
	 * The Class Connection.
	 */
	public static class Connection extends Elem {

		/**
		 * Instantiates a new connection.
		 *
		 * @param parent the parent
		 * @param dvc0   the dvc 0
		 * @param det0   the det 0
		 * @param dvc1   the dvc 1
		 * @param det1   the det 1
		 */
		public Connection(Task parent, Device dvc0, Device.DeviceElement det0, Device dvc1, Device.DeviceElement det1) {
			super(parent, "CNN");
			e.setAttribute("A", dvc0.id);
			e.setAttribute("B", det0.id);
			e.setAttribute("C", dvc1.id);
			e.setAttribute("D", det1.id);
		}
	}

	/**
	 * The Enum AllocationStampType.
	 */
	public static enum AllocationStampType {

		/** The planned. */
		PLANNED(1),
		/** The effective. */
		EFFECTIVE(4);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new allocation stamp type.
		 *
		 * @param number the number
		 */
		private AllocationStampType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<AllocationStampType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<AllocationStampType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class AllocationStamp.
	 */
	public static class AllocationStamp extends Elem {

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		private AllocationStamp(Elem parent, OffsetDateTime start, AllocationStampType type) {
			super(parent, "ASP");
			e.setAttribute("A", start.toString());
			e.setAttribute("D", Integer.toString(type.number));
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(CommentAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(ControlAssignment parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(DeviceAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(GuidanceAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(GuidanceShift parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(ProductAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Instantiates a new allocation stamp.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public AllocationStamp(WorkerAllocation parent, OffsetDateTime start, AllocationStampType type) {
			this((Elem) parent, start, type);
		}

		/**
		 * Sets the stop.
		 *
		 * @param stop the stop
		 * @return the allocation stamp
		 */
		public AllocationStamp setStop(OffsetDateTime stop) {
			e.setAttribute("B", stop.toString());
			return this;
		}

		/**
		 * Sets the duration.
		 *
		 * @param duration the duration
		 * @return the allocation stamp
		 */
		public AllocationStamp setDuration(Duration duration) {
			e.setAttribute("C", Long.toString(duration.getSeconds()));
			return this;
		}

		/**
		 * Adds the position.
		 *
		 * @param north  the north
		 * @param east   the east
		 * @param status the status
		 * @return the position
		 */
		public Position addPosition(double north, double east, PositionStatus status) {
			return new Position(this, north, east, status);
		}
	}

	/**
	 * The Class ControlAssignment.
	 */
	public static class ControlAssignment extends Elem {

		/**
		 * Instantiates a new control assignment.
		 *
		 * @param parent                     the parent
		 * @param sourceClientName           the source client name
		 * @param userClientName             the user client name
		 * @param sourceDeviceStructureLabel the source device structure label
		 * @param userDeviceStructureLabel   the user device structure label
		 * @param sourceDeviceElementNumber  the source device element number
		 * @param userDeviceElementNumber    the user device element number
		 * @param processDataDDI             the process data DDI
		 */
		public ControlAssignment(Task parent, long sourceClientName, long userClientName,
				byte[] sourceDeviceStructureLabel, byte[] userDeviceStructureLabel, int sourceDeviceElementNumber,
				int userDeviceElementNumber, int processDataDDI) {
			super(parent, "CAT");
			if (sourceDeviceStructureLabel.length > 39 || userDeviceStructureLabel.length > 39)
				throw new IllegalArgumentException("invalid device structure label");
			if (sourceDeviceElementNumber < 0 || sourceDeviceElementNumber > 4095 || userDeviceElementNumber < 0
					|| userDeviceElementNumber > 4095)
				throw new IllegalArgumentException("invalid device element number");
			e.setAttribute("A", Long.toHexString(sourceClientName).toUpperCase());
			e.setAttribute("B", Long.toHexString(userClientName).toUpperCase());
			e.setAttribute("C", Hex.encodeHexString(sourceDeviceStructureLabel, false));
			e.setAttribute("D", Hex.encodeHexString(userDeviceStructureLabel, false));
			e.setAttribute("E", Integer.toString(sourceDeviceElementNumber));
			e.setAttribute("F", Integer.toString(userDeviceElementNumber));
			e.setAttribute("G", ddi(processDataDDI));
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Enum TransferMode.
	 */
	public static enum TransferMode {

		/** The filling. */
		FILLING(1),
		/** The emptying. */
		EMPTYING(2),
		/** The remainder. */
		REMAINDER(3);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new transfer mode.
		 *
		 * @param number the number
		 */
		private TransferMode(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<TransferMode> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<TransferMode> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class ProductAllocation.
	 */
	public static class ProductAllocation extends Elem {

		/**
		 * Instantiates a new product allocation.
		 *
		 * @param parent the parent
		 * @param pdt    the pdt
		 */
		public ProductAllocation(Task parent, Partfield.Product pdt) {
			super(parent, "PAN");
			e.setAttribute("A", pdt.id);
		}

		/**
		 * Sets the quantity DDI.
		 *
		 * @param ddi the ddi
		 * @return the product allocation
		 */
		public ProductAllocation setQuantityDDI(int ddi) {
			e.setAttribute("B", ddi(ddi));
			return this;
		}

		/**
		 * Sets the quantity value.
		 *
		 * @param value the value
		 * @return the product allocation
		 */
		public ProductAllocation setQuantityValue(int value) {
			if (value < 0)
				throw new IllegalArgumentException("invalid quantity value");
			e.setAttribute("C", Integer.toString(value));
			return this;
		}

		/**
		 * Sets the transfer mode.
		 *
		 * @param transferMode the transfer mode
		 * @return the product allocation
		 */
		public ProductAllocation setTransferMode(TransferMode transferMode) {
			e.setAttribute("D", Integer.toString(transferMode.number));
			return this;
		}

		/**
		 * Sets the device element.
		 *
		 * @param det the det
		 * @return the product allocation
		 */
		public ProductAllocation setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("E", det.id);
			return this;
		}

		/**
		 * Sets the value presentation.
		 *
		 * @param vpn the vpn
		 * @return the product allocation
		 */
		public ProductAllocation setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("F", vpn.id);
			return this;
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Enum CodedCommentScope.
	 */
	public static enum CodedCommentScope {

		/** The point. */
		POINT(1),
		/** The global. */
		GLOBAL(2),
		/** The continuous. */
		CONTINUOUS(3);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new coded comment scope.
		 *
		 * @param number the number
		 */
		private CodedCommentScope(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<CodedCommentScope> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<CodedCommentScope> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class CodedComment.
	 */
	public static class CodedComment extends Elem {

		/**
		 * Instantiates a new coded comment.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 * @param scope      the scope
		 */
		public CodedComment(ISO11783_TaskData parent, String designator, CodedCommentScope scope) {
			super(parent, "CCT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
			e.setAttribute("C", Integer.toString(scope.number));
		}

		/**
		 * Sets the group.
		 *
		 * @param ccg the ccg
		 * @return the coded comment
		 */
		public CodedComment setGroup(CodedCommentGroup ccg) {
			e.setAttribute("D", ccg.id);
			return this;
		}

		/**
		 * Adds the list value.
		 *
		 * @param designator the designator
		 * @return the coded comment list value
		 */
		public CodedCommentListValue addListValue(String designator) {
			return new CodedCommentListValue(this, designator);
		}
	}

	/**
	 * The Class CodedCommentGroup.
	 */
	public static class CodedCommentGroup extends Elem {

		/**
		 * Instantiates a new coded comment group.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public CodedCommentGroup(ISO11783_TaskData parent, String designator) {
			super(parent, "CCG");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}

	/**
	 * The Class CodedCommentListValue.
	 */
	public static class CodedCommentListValue extends Elem {

		/**
		 * Instantiates a new coded comment list value.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public CodedCommentListValue(CodedComment parent, String designator) {
			super(parent, "CCL");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}
	}

	/**
	 * The Class CommentAllocation.
	 */
	public static class CommentAllocation extends Elem {

		/**
		 * Instantiates a new comment allocation.
		 *
		 * @param parent the parent
		 */
		public CommentAllocation(Task parent) {
			super(parent, "CAN");
		}

		/**
		 * Sets the coded comment.
		 *
		 * @param cct the cct
		 * @return the comment allocation
		 */
		public CommentAllocation setCodedComment(CodedComment cct) {
			e.setAttribute("A", cct.id);
			return this;
		}

		/**
		 * Sets the coded comment list value.
		 *
		 * @param ccl the ccl
		 * @return the comment allocation
		 */
		public CommentAllocation setCodedCommentListValue(CodedCommentListValue ccl) {
			e.setAttribute("B", ccl.id);
			return this;
		}

		/**
		 * Sets the free comment text.
		 *
		 * @param text the text
		 * @return the comment allocation
		 */
		public CommentAllocation setFreeCommentText(String text) {
			e.setAttribute("C", text);
			return this;
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Class WorkerAllocation.
	 */
	public static class WorkerAllocation extends Elem {

		/**
		 * Instantiates a new worker allocation.
		 *
		 * @param parent the parent
		 * @param wkr    the wkr
		 */
		public WorkerAllocation(Task parent, Worker wkr) {
			super(parent, "WAN");
			e.setAttribute("A", wkr.id);
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Class DeviceAllocation.
	 */
	public static class DeviceAllocation extends Elem {

		/**
		 * Instantiates a new device allocation.
		 *
		 * @param parent          the parent
		 * @param clientNameValue the client name value
		 */
		public DeviceAllocation(Task parent, long clientNameValue) {
			super(parent, "DAN");
			e.setAttribute("A", Long.toHexString(clientNameValue));
		}

		/**
		 * Sets the client name mask.
		 *
		 * @param clientNameMask the client name mask
		 * @return the device allocation
		 */
		public DeviceAllocation setClientNameMask(long clientNameMask) {
			e.setAttribute("B", Long.toHexString(clientNameMask));
			return this;
		}

		/**
		 * Sets the device.
		 *
		 * @param dvc the dvc
		 * @return the device allocation
		 */
		public DeviceAllocation setDevice(Device dvc) {
			e.setAttribute("C", dvc.id);
			return this;
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Class GuidanceShift.
	 */
	public static class GuidanceShift extends Elem {

		/**
		 * Instantiates a new guidance shift.
		 *
		 * @param parent the parent
		 * @param ggp    the ggp
		 * @param gpn    the gpn
		 */
		public GuidanceShift(GuidanceAllocation parent, Partfield.GuidanceGroup ggp, Partfield.GuidancePattern gpn) {
			super(parent, "GST");
			e.setAttribute("A", ggp.id);
			e.setAttribute("B", gpn.id);
		}

		/**
		 * Sets the east shift.
		 *
		 * @param east the east
		 * @return the guidance shift
		 */
		public GuidanceShift setEastShift(int east) {
			e.setAttribute("C", Integer.toString(east));
			return this;
		}

		/**
		 * Sets the north shift.
		 *
		 * @param north the north
		 * @return the guidance shift
		 */
		public GuidanceShift setNorthShift(int north) {
			e.setAttribute("D", Integer.toString(north));
			return this;
		}

		/**
		 * Sets the propagation offset.
		 *
		 * @param offset the offset
		 * @return the guidance shift
		 */
		public GuidanceShift setPropagationOffset(int offset) {
			e.setAttribute("E", Integer.toString(offset));
			return this;
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}
	}

	/**
	 * The Class GuidanceAllocation.
	 */
	public static class GuidanceAllocation extends Elem {

		/**
		 * Instantiates a new guidance allocation.
		 *
		 * @param parent the parent
		 * @param ggp    the ggp
		 */
		public GuidanceAllocation(Task parent, Partfield.GuidanceGroup ggp) {
			super(parent, "GAN");
			e.setAttribute("A", ggp.id);
		}

		/**
		 * Adds the allocation stamp.
		 *
		 * @param start the start
		 * @param type  the type
		 * @return the allocation stamp
		 */
		public AllocationStamp addAllocationStamp(OffsetDateTime start, AllocationStampType type) {
			return new AllocationStamp(this, start, type);
		}

		/**
		 * Adds the guidance shift.
		 *
		 * @param ggp the ggp
		 * @param gpn the gpn
		 * @return the guidance shift
		 */
		public GuidanceShift addGuidanceShift(Partfield.GuidanceGroup ggp, Partfield.GuidancePattern gpn) {
			return new GuidanceShift(this, ggp, gpn);
		}
	}

	/**
	 * The Class DataLogTrigger.
	 */
	public static class DataLogTrigger extends Elem {

		/**
		 * Instantiates a new data log trigger.
		 *
		 * @param parent the parent
		 * @param ddi    the ddi
		 * @param method the method
		 */
		public DataLogTrigger(Task parent, int ddi, int method) {
			super(parent, "DLT");
			e.setAttribute("A", ddi(ddi));
			if (method < 1 || method > 31)
				throw new IllegalArgumentException("invalid method");
			e.setAttribute("B", Integer.toString(method));
		}

		/**
		 * Sets the distance interval.
		 *
		 * @param distanceInterval the distance interval
		 * @return the data log trigger
		 */
		public DataLogTrigger setDistanceInterval(long distanceInterval) {
			if (distanceInterval < 0 || distanceInterval > 1000000)
				throw new IllegalArgumentException("invalid distance interval");
			e.setAttribute("C", Long.toString(distanceInterval));
			return this;
		}

		/**
		 * Sets the time interval.
		 *
		 * @param timeInterval the time interval
		 * @return the data log trigger
		 */
		public DataLogTrigger setTimeInterval(long timeInterval) {
			if (timeInterval < 0 || timeInterval > 60000)
				throw new IllegalArgumentException("invalid time interval");
			e.setAttribute("D", Long.toString(timeInterval));
			return this;
		}

		/**
		 * Sets the threshold minimum.
		 *
		 * @param thresholdMinimum the threshold minimum
		 * @return the data log trigger
		 */
		public DataLogTrigger setThresholdMinimum(int thresholdMinimum) {
			e.setAttribute("E", Integer.toString(thresholdMinimum));
			return this;
		}

		/**
		 * Sets the threshold maximum.
		 *
		 * @param thresholdMaximum the threshold maximum
		 * @return the data log trigger
		 */
		public DataLogTrigger setThresholdMaximum(int thresholdMaximum) {
			e.setAttribute("F", Integer.toString(thresholdMaximum));
			return this;
		}

		/**
		 * Sets the threshold change.
		 *
		 * @param thresholdChange the threshold change
		 * @return the data log trigger
		 */
		public DataLogTrigger setThresholdChange(int thresholdChange) {
			e.setAttribute("G", Integer.toString(thresholdChange));
			return this;
		}

		/**
		 * Sets the device element.
		 *
		 * @param det the det
		 * @return the data log trigger
		 */
		public DataLogTrigger setDeviceElement(Device.DeviceElement det) {
			e.setAttribute("H", det.id);
			return this;
		}

		/**
		 * Sets the value presentation.
		 *
		 * @param vpn the vpn
		 * @return the data log trigger
		 */
		public DataLogTrigger setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("I", vpn.id);
			return this;
		}

		/**
		 * Sets the PGN.
		 *
		 * @param pgn the pgn
		 * @return the data log trigger
		 */
		public DataLogTrigger setPGN(long pgn) {
			if (pgn < 0 || pgn > 262143)
				throw new IllegalArgumentException("invalid PGN");
			e.setAttribute("J", Long.toString(pgn));
			return this;
		}

		/**
		 * Sets the pgn start bit.
		 *
		 * @param pgnStartBit the pgn start bit
		 * @return the data log trigger
		 */
		public DataLogTrigger setPgnStartBit(int pgnStartBit) {
			if (pgnStartBit < 0 || pgnStartBit > 63)
				throw new IllegalArgumentException("invalid PGN start bit");
			e.setAttribute("K", Long.toString(pgnStartBit));
			return this;
		}

		/**
		 * Sets the pgn stop bit.
		 *
		 * @param pgnStopBit the pgn stop bit
		 * @return the data log trigger
		 */
		public DataLogTrigger setPgnStopBit(int pgnStopBit) {
			if (pgnStopBit < 0 || pgnStopBit > 63)
				throw new IllegalArgumentException("invalid PGN stop bit");
			e.setAttribute("L", Long.toString(pgnStopBit));
			return this;
		}
	}

	/**
	 * The Enum PositionStatus.
	 */
	public static enum PositionStatus {

		/** The no gps. */
		NO_GPS(0),
		/** The gnss. */
		GNSS(1),
		/** The dgnss. */
		DGNSS(2),
		/** The precise gnss. */
		PRECISE_GNSS(3),
		/** The rtk finteger. */
		RTK_FINTEGER(4),
		/** The rtk float. */
		RTK_FLOAT(5),

		/** The est dr mode. */
		EST_DR_MODE(6),
		/** The manual input. */
		MANUAL_INPUT(7),
		/** The simulate mode. */
		SIMULATE_MODE(8),
		/** The error. */
		ERROR(14),
		/** The not available. */
		NOT_AVAILABLE(15);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new position status.
		 *
		 * @param number the number
		 */
		private PositionStatus(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<PositionStatus> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<PositionStatus> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class GpsTime.
	 */
	public static class GpsTime {

		/** The Constant gpsTimeStart. */
		private static final Instant gpsTimeStart = Instant.ofEpochSecond(315532800); // 1980-01-01

		/** The time. */
		public final long date, time;

		/**
		 * Instantiates a new gps time.
		 *
		 * @param timestamp the timestamp
		 */
		public GpsTime(Instant timestamp) {
			Duration duration = Duration.between(gpsTimeStart, timestamp);
			date = duration.toDays();
			time = duration.minusDays(date).toMillis();
		}

		/**
		 * Gets the date.
		 *
		 * @return the date
		 */
		public long getDate() {
			return date;
		}

		/**
		 * Gets the time.
		 *
		 * @return the time
		 */
		public long getTime() {
			return time;
		}
	}

	/**
	 * The Class Position.
	 */
	public static class Position extends Elem {

		/**
		 * Instantiates a new position.
		 *
		 * @param parent the parent
		 * @param north  the north
		 * @param east   the east
		 * @param status the status
		 */
		private Position(Elem parent, double north, double east, PositionStatus status) {
			super(parent, "PTN");
			e.setAttribute("A", north(north));
			e.setAttribute("B", east(east));
			e.setAttribute("D", Integer.toString(status.number));
		}

		/**
		 * Instantiates a new position.
		 *
		 * @param parent the parent
		 * @param north  the north
		 * @param east   the east
		 * @param status the status
		 */
		public Position(AllocationStamp parent, double north, double east, PositionStatus status) {
			this((Elem) parent, north, east, status);
		}

		/**
		 * Instantiates a new position.
		 *
		 * @param parent the parent
		 * @param north  the north
		 * @param east   the east
		 * @param status the status
		 */
		public Position(Time parent, double north, double east, PositionStatus status) {
			this((Elem) parent, north, east, status);
		}

		/**
		 * Sets the up.
		 *
		 * @param up the up
		 * @return the position
		 */
		public Position setUp(double up) {
			e.setAttribute("C", Long.toString(Math.round(up * 1e3)));
			return this;
		}

		/**
		 * Sets the PDOP.
		 *
		 * @param pdop the pdop
		 * @return the position
		 */
		public Position setPDOP(float pdop) {
			if (pdop < 0 || pdop > 99.9)
				throw new IllegalArgumentException("invalid PDOP");
			e.setAttribute("E", floating(pdop));
			return this;
		}

		/**
		 * Sets the HDOP.
		 *
		 * @param hdop the hdop
		 * @return the position
		 */
		public Position setHDOP(float hdop) {
			if (hdop < 0 || hdop > 99.9)
				throw new IllegalArgumentException("invalid HDOP");
			e.setAttribute("F", floating(hdop));
			return this;
		}

		/**
		 * Sets the number of satellites.
		 *
		 * @param numberOfSatellites the number of satellites
		 * @return the position
		 */
		public Position setNumberOfSatellites(int numberOfSatellites) {
			if (numberOfSatellites < 0 || numberOfSatellites > 255)
				throw new IllegalArgumentException("invalid number of satellites");
			e.setAttribute("G", Integer.toString(numberOfSatellites));
			return this;
		}

		/**
		 * Sets the gps utc.
		 *
		 * @param timestamp the timestamp
		 * @return the position
		 */
		public Position setGpsUtc(Instant timestamp) {
			GpsTime gpsTime = new GpsTime(timestamp);
			e.setAttribute("H", Long.toString(gpsTime.time));
			e.setAttribute("I", Long.toString(gpsTime.date));
			return this;
		}
	}

	/**
	 * The Enum TimeType.
	 */
	public static enum TimeType {

		/** The planned. */
		PLANNED(1),
		/** The preliminary. */
		PRELIMINARY(2),
		/** The effective. */
		EFFECTIVE(4),
		/** The ineffective. */
		INEFFECTIVE(5),
		/** The repair. */
		REPAIR(6),
		/** The clearing. */
		CLEARING(7),
		/** The powered down. */
		POWERED_DOWN(8);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new time type.
		 *
		 * @param number the number
		 */
		private TimeType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<TimeType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<TimeType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class Time.
	 */
	public static class Time extends Elem {

		/**
		 * Instantiates a new time.
		 *
		 * @param parent the parent
		 * @param start  the start
		 * @param type   the type
		 */
		public Time(Task parent, OffsetDateTime start, TimeType type) {
			super(parent, "TIM");
			e.setAttribute("A", start.toString());
			e.setAttribute("D", Integer.toString(type.number));
		}

		/**
		 * Sets the stop.
		 *
		 * @param stop the stop
		 * @return the time
		 */
		public Time setStop(OffsetDateTime stop) {
			e.setAttribute("B", stop.toString());
			return this;
		}

		/**
		 * Sets the duration.
		 *
		 * @param duration the duration
		 * @return the time
		 */
		public Time setDuration(Duration duration) {
			e.setAttribute("C", Long.toString(duration.getSeconds()));
			return this;
		}

		/**
		 * Adds the position.
		 *
		 * @param north  the north
		 * @param east   the east
		 * @param status the status
		 * @return the position
		 */
		public Position addPosition(double north, double east, PositionStatus status) {
			return new Position(this, north, east, status);
		}

		/**
		 * Adds the data log value.
		 *
		 * @param processDataDDI   the process data DDI
		 * @param processDataValue the process data value
		 * @param det              the det
		 * @return the data log value
		 */
		public DataLogValue addDataLogValue(int processDataDDI, int processDataValue, Device.DeviceElement det) {
			return new DataLogValue(this, processDataDDI, processDataValue, det);
		}
	}

	/**
	 * The Class DataLogValue.
	 */
	public static class DataLogValue extends Elem {

		/**
		 * Instantiates a new data log value.
		 *
		 * @param parent           the parent
		 * @param processDataDDI   the process data DDI
		 * @param processDataValue the process data value
		 * @param det              the det
		 */
		public DataLogValue(Time parent, int processDataDDI, int processDataValue, Device.DeviceElement det) {
			super(parent, "DLV");
			e.setAttribute("A", ddi(processDataDDI));
			e.setAttribute("B", Integer.toString(processDataValue));
			e.setAttribute("C", det.id);
		}

		/**
		 * Sets the PGN.
		 *
		 * @param pgn the pgn
		 * @return the data log value
		 */
		public DataLogValue setPGN(long pgn) {
			if (pgn < 0 || pgn > 262143)
				throw new IllegalArgumentException("invalid PGN");
			e.setAttribute("D", Long.toString(pgn));
			return this;
		}

		/**
		 * Sets the pgn start bit.
		 *
		 * @param pgnStartBit the pgn start bit
		 * @return the data log value
		 */
		public DataLogValue setPgnStartBit(int pgnStartBit) {
			if (pgnStartBit < 0 || pgnStartBit > 63)
				throw new IllegalArgumentException("invalid PGN start bit");
			e.setAttribute("E", Long.toString(pgnStartBit));
			return this;
		}

		/**
		 * Sets the pgn stop bit.
		 *
		 * @param pgnStopBit the pgn stop bit
		 * @return the data log value
		 */
		public DataLogValue setPgnStopBit(int pgnStopBit) {
			if (pgnStopBit < 0 || pgnStopBit > 63)
				throw new IllegalArgumentException("invalid PGN stop bit");
			e.setAttribute("F", Long.toString(pgnStopBit));
			return this;
		}
	}

}