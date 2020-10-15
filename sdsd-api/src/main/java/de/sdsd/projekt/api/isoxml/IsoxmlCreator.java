package de.sdsd.projekt.api.isoxml;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.mutable.MutableInt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Main Helper class for creating ISOXML.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public class IsoxmlCreator {

	/** The document. */
	final Document document;

	/** The ids. */
	private final Map<String, MutableInt> ids = new HashMap<>();

	/** The ISOXML root element. */
	public final ISO11783_TaskData root;

	/**
	 * Instantiates a new ISOXML creator.
	 *
	 * @param managementSoftwareManufacturer the management software manufacturer
	 * @throws ParserConfigurationException Indicates a serious configuration error
	 *                                      of the XML creator.
	 */
	public IsoxmlCreator(String managementSoftwareManufacturer) throws ParserConfigurationException {
		this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		this.root = new ISO11783_TaskData(VersionMajor.E2_FDIS, 2, DataTransferOrigin.FMIS)
				.setManagementSoftwareManufacturer(managementSoftwareManufacturer);
	}

	/**
	 * Write the main TASKDATA.XML file.
	 *
	 * @param out the stream to write to
	 * @throws TransformerFactoryConfigurationError the transformer factory
	 *                                              configuration error
	 * @throws TransformerException                 the transformer exception
	 */
	public void writeTaskData(OutputStream out) throws TransformerFactoryConfigurationError, TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.transform(new DOMSource(document), new StreamResult(out));
	}

	/**
	 * Creates the a new unique ID for the tag.
	 *
	 * @param tag the tag name
	 * @return unique ID: e.g. TAG1
	 */
	final String createID(String tag) {
		tag = tag.toUpperCase();
		MutableInt num = ids.get(tag);
		if (num == null)
			ids.put(tag, num = new MutableInt());
		return tag + num.incrementAndGet();
	}

	/**
	 * Creates a new unique object ID.
	 *
	 * @param parent the parent element
	 * @return unique object ID
	 */
	final String createOID(Elem parent) {
		MutableInt num = ids.get(parent.id);
		if (num == null)
			ids.put(parent.id, num = new MutableInt());
		return Integer.toString(num.incrementAndGet());
	}

	/**
	 * The base class for ISOXML elements.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	public static abstract class Elem {

		/** The isoxml creator. */
		protected final IsoxmlCreator isoxmlCreator;

		/** The e. */
		protected final Element e;

		/** The id. */
		protected final String id;

		/** The parent element. */
		public final Elem parent;

		/**
		 * DDI checker and formatter.
		 *
		 * @param ddi the ddi
		 * @return formatted DDI: 005A
		 */
		protected static String ddi(int ddi) {
			if (ddi < 0 || ddi > 0xFFFF)
				throw new IllegalArgumentException("invalid DDI");
			return String.format("%04X", ddi);
		}

		/**
		 * Latitude checker and formatter.
		 *
		 * @param north the latitude [-90, +90]
		 * @return formatted latitude
		 */
		protected static String north(double north) {
			if (north < -90 || north > 90)
				throw new IllegalArgumentException("invalid north position");
			return floating(north);
		}

		/**
		 * Longitude checker and formatter.
		 *
		 * @param east the longitude [-180, +180]
		 * @return formatted longitude
		 */
		protected static String east(double east) {
			if (east < -180 || east > 180)
				throw new IllegalArgumentException("invalid east position");
			return floating(east);
		}

		/** The Constant df. */
		private static final DecimalFormat df;
		static {
			df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			df.setMaximumFractionDigits(340);
		}

		/**
		 * Checker and formatter for floating point numbers.
		 *
		 * @param val the value
		 * @return formatted string
		 */
		protected static String floating(double val) {
			if (!Double.isFinite(val))
				throw new IllegalArgumentException("invalid floating point value");
			return df.format(val);
		}

		/**
		 * Instantiates a new elem.
		 *
		 * @param isoxmlCreator the isoxml creator
		 * @param tag           the tag
		 */
		private Elem(IsoxmlCreator isoxmlCreator, String tag) {
			this.isoxmlCreator = isoxmlCreator;
			this.e = isoxmlCreator.document.createElement(tag);
			isoxmlCreator.document.appendChild(e);
			this.id = isoxmlCreator.createID(tag);
			this.parent = null;
		}

		/**
		 * Instantiates a new elem.
		 *
		 * @param parent the parent
		 * @param tag    the tag
		 */
		Elem(Elem parent, String tag) {
			this.isoxmlCreator = parent.isoxmlCreator;
			this.e = isoxmlCreator.document.createElement(tag);
			parent.e.appendChild(e);
			this.id = isoxmlCreator.createID(tag);
			this.parent = parent;
		}

		/**
		 * Returns the ISOXML ID (TAG1).
		 *
		 * @return ISOXML ID
		 */
		public String id() {
			return id;
		}

		/**
		 * Returns the parent element. Might only be null for the root element.
		 *
		 * @return the parent element
		 */
		public Elem parent() {
			return parent;
		}

		/** The Constant ID_REGEX. */
		private static final Pattern ID_REGEX = Pattern.compile("(\\w+)-(\\d+)");

		/**
		 * Creates a file name without extension based on the element ID.
		 *
		 * @return ISOXML file name (TAG00001)
		 */
		protected String createFile() {
			Matcher matcher = ID_REGEX.matcher(id);
			matcher.matches();
			String tag = matcher.group(1);
			int num = Integer.parseUnsignedInt(matcher.group(2));
			return String.format("%s%05d", tag, num);
		}

		/**
		 * Removes the element from its parent.
		 */
		public void remove() {
			Node parent = e.getParentNode();
			if (parent != null)
				parent.removeChild(e);
		}
	}

	/**
	 * Base class for elements with an object ID.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
	 *         Klose</a>
	 */
	static abstract class OIDElem extends Elem {

		/** The oid. */
		protected final String oid;

		/**
		 * Instantiates a new OID elem.
		 *
		 * @param parent the parent
		 * @param tag    the tag
		 */
		OIDElem(Elem parent, String tag) {
			super(parent, tag);
			this.oid = isoxmlCreator.createOID(parent);
		}

		/**
		 * Returns the object ID.
		 *
		 * @return the object ID
		 */
		public String oid() {
			return oid;
		}
	}

	/**
	 * The Enum VersionMajor.
	 */
	public static enum VersionMajor {

		/** The dis. */
		DIS(0),
		/** The fdis 1. */
		FDIS_1(1),
		/** The fdis 2. */
		FDIS_2(2),
		/** The e2 dis. */
		E2_DIS(3),
		/** The e2 fdis. */
		E2_FDIS(4);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new version major.
		 *
		 * @param number the number
		 */
		private VersionMajor(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<VersionMajor> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<VersionMajor> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum DataTransferOrigin.
	 */
	public static enum DataTransferOrigin {

		/** The fmis. */
		FMIS(1),
		/** The mics. */
		MICS(2);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new data transfer origin.
		 *
		 * @param number the number
		 */
		private DataTransferOrigin(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<DataTransferOrigin> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<DataTransferOrigin> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class ISO11783_TaskData.
	 */
	public class ISO11783_TaskData extends Elem {

		/**
		 * Instantiates a new ISO 11783 task data.
		 *
		 * @param versionMajor the version major
		 * @param versionMinor the version minor
		 * @param origin       the origin
		 */
		public ISO11783_TaskData(VersionMajor versionMajor, int versionMinor, DataTransferOrigin origin) {
			super(IsoxmlCreator.this, "ISO11783_TaskData");
			e.setAttribute("VersionMajor", Integer.toString(versionMajor.number));
			if (versionMinor < 0 || versionMinor > 99)
				throw new IllegalArgumentException("invalid version minor");
			e.setAttribute("VersionMinor", Integer.toString(versionMinor));
			e.setAttribute("DataTransferOrigin", Integer.toString(origin.number));
		}

		/**
		 * Sets the management software manufacturer.
		 *
		 * @param managementSoftwareManufacturer the management software manufacturer
		 * @return the ISO 11783 task data
		 */
		public ISO11783_TaskData setManagementSoftwareManufacturer(String managementSoftwareManufacturer) {
			e.setAttribute("ManagementSoftwareManufacturer", managementSoftwareManufacturer);
			return this;
		}

		/**
		 * Sets the management software version.
		 *
		 * @param managementSoftwareVersion the management software version
		 * @return the ISO 11783 task data
		 */
		public ISO11783_TaskData setManagementSoftwareVersion(String managementSoftwareVersion) {
			e.setAttribute("ManagementSoftwareVersion", managementSoftwareVersion);
			return this;
		}

		/**
		 * Sets the task controller manufacturer.
		 *
		 * @param taskControllerManufacturer the task controller manufacturer
		 * @return the ISO 11783 task data
		 */
		public ISO11783_TaskData setTaskControllerManufacturer(String taskControllerManufacturer) {
			e.setAttribute("TaskControllerManufacturer", taskControllerManufacturer);
			return this;
		}

		/**
		 * Sets the task controller version.
		 *
		 * @param taskControllerVersion the task controller version
		 * @return the ISO 11783 task data
		 */
		public ISO11783_TaskData setTaskControllerVersion(String taskControllerVersion) {
			e.setAttribute("TaskControllerVersion", taskControllerVersion);
			return this;
		}

		/**
		 * Sets the data transfer language.
		 *
		 * @param lang the lang
		 * @return the ISO 11783 task data
		 */
		public ISO11783_TaskData setDataTransferLanguage(String lang) {
			e.setAttribute("lang", lang);
			return this;
		}

		/**
		 * Adds the attached file.
		 *
		 * @param filenameWithExtension the filename with extension
		 * @param preserve              the preserve
		 * @param manufacturerGln       the manufacturer gln
		 * @param fileType              the file type
		 * @return the attached file
		 */
		public AttachedFile addAttachedFile(String filenameWithExtension, FilePreservation preserve,
				String manufacturerGln, int fileType) {
			return new AttachedFile(this, filenameWithExtension, preserve, manufacturerGln, fileType);
		}

		/**
		 * Adds the base station.
		 *
		 * @param designator the designator
		 * @param north      the north
		 * @param east       the east
		 * @param up         the up
		 * @return the partfield. base station
		 */
		public Partfield.BaseStation addBaseStation(String designator, double north, double east, int up) {
			return new Partfield.BaseStation(this, designator, north, east, up);
		}

		/**
		 * Adds the coded comment.
		 *
		 * @param designator the designator
		 * @param scope      the scope
		 * @return the task. coded comment
		 */
		public Task.CodedComment addCodedComment(String designator, Task.CodedCommentScope scope) {
			return new Task.CodedComment(this, designator, scope);
		}

		/**
		 * Adds the coded comment group.
		 *
		 * @param designator the designator
		 * @return the task. coded comment group
		 */
		public Task.CodedCommentGroup addCodedCommentGroup(String designator) {
			return new Task.CodedCommentGroup(this, designator);
		}

		/**
		 * Adds the colour legend.
		 *
		 * @return the device. colour legend
		 */
		public Device.ColourLegend addColourLegend() {
			return new Device.ColourLegend(this);
		}

		/**
		 * Adds the crop type.
		 *
		 * @param designator the designator
		 * @return the partfield. crop type
		 */
		public Partfield.CropType addCropType(String designator) {
			return new Partfield.CropType(this, designator);
		}

		/**
		 * Adds the cultural practice.
		 *
		 * @param designator the designator
		 * @return the task. cultural practice
		 */
		public Task.CulturalPractice addCulturalPractice(String designator) {
			return new Task.CulturalPractice(this, designator);
		}

		/**
		 * Adds the customer.
		 *
		 * @param lastName the last name
		 * @return the task. customer
		 */
		public Task.Customer addCustomer(String lastName) {
			return new Task.Customer(this, lastName);
		}

		/**
		 * Adds the device.
		 *
		 * @param clientName              the client name
		 * @param deviceStructureLabel    the device structure label
		 * @param deviceLocalizationLabel the device localization label
		 * @return the device
		 */
		public Device addDevice(byte[] clientName, byte[] deviceStructureLabel, byte[] deviceLocalizationLabel) {
			return new Device(this, clientName, deviceStructureLabel, deviceLocalizationLabel);
		}

		/**
		 * Adds the device.
		 *
		 * @param clientName              the client name
		 * @param deviceStructureLabel    the device structure label
		 * @param deviceLocalizationLabel the device localization label
		 * @return the device
		 */
		public Device addDevice(long clientName, byte[] deviceStructureLabel, long deviceLocalizationLabel) {
			return new Device(this, clientName, deviceStructureLabel, deviceLocalizationLabel);
		}

		/**
		 * Adds the farm.
		 *
		 * @param designator the designator
		 * @return the task. farm
		 */
		public Task.Farm addFarm(String designator) {
			return new Task.Farm(this, designator);
		}

		/**
		 * Adds the operation technique.
		 *
		 * @param designator the designator
		 * @return the task. operation technique
		 */
		public Task.OperationTechnique addOperationTechnique(String designator) {
			return new Task.OperationTechnique(this, designator);
		}

		/**
		 * Adds the partfield.
		 *
		 * @param designator the designator
		 * @param area       the area
		 * @return the partfield
		 */
		public Partfield addPartfield(String designator, long area) {
			return new Partfield(this, designator, area);
		}

		/**
		 * Adds the product.
		 *
		 * @param designator the designator
		 * @return the partfield. product
		 */
		public Partfield.Product addProduct(String designator) {
			return new Partfield.Product(this, designator);
		}

		/**
		 * Adds the product group.
		 *
		 * @param designator the designator
		 * @return the partfield. product group
		 */
		public Partfield.ProductGroup addProductGroup(String designator) {
			return new Partfield.ProductGroup(this, designator);
		}

		/**
		 * Adds the task.
		 *
		 * @param designator the designator
		 * @param status     the status
		 * @return the task
		 */
		public Task addTask(String designator, Task.TaskStatus status) {
			return new Task(this, designator, status);
		}

		/**
		 * Adds the task controller capabilities.
		 *
		 * @param taskControllerControlFunctionName the task controller control function
		 *                                          name
		 * @param taskControllerDesignator          the task controller designator
		 * @param versionNumber                     the version number
		 * @param providedCapabilities              the provided capabilities
		 * @param numberOfBoomsSectionControl       the number of booms section control
		 * @param numberOfSectionsSectionControl    the number of sections section
		 *                                          control
		 * @param numberOfControlChannels           the number of control channels
		 * @return the task controller capabilities
		 */
		public TaskControllerCapabilities addTaskControllerCapabilities(long taskControllerControlFunctionName,
				String taskControllerDesignator, VersionMajor versionNumber, int providedCapabilities,
				int numberOfBoomsSectionControl, int numberOfSectionsSectionControl, int numberOfControlChannels) {
			return new TaskControllerCapabilities(this, taskControllerControlFunctionName, taskControllerDesignator,
					versionNumber, providedCapabilities, numberOfBoomsSectionControl, numberOfSectionsSectionControl,
					numberOfControlChannels);
		}

		/**
		 * Adds the value presentation.
		 *
		 * @param offset           the offset
		 * @param scale            the scale
		 * @param numberOfDecimals the number of decimals
		 * @return the device. value presentation
		 */
		public Device.ValuePresentation addValuePresentation(int offset, double scale, int numberOfDecimals) {
			return new Device.ValuePresentation(this, offset, scale, numberOfDecimals);
		}

		/**
		 * Adds the worker.
		 *
		 * @param lastName the last name
		 * @return the task. worker
		 */
		public Task.Worker addWorker(String lastName) {
			return new Task.Worker(this, lastName);
		}
	}

	/**
	 * The Class TaskControllerCapabilities.
	 */
	public static class TaskControllerCapabilities extends Elem {

		/**
		 * Instantiates a new task controller capabilities.
		 *
		 * @param parent                            the parent
		 * @param taskControllerControlFunctionName the task controller control function
		 *                                          name
		 * @param taskControllerDesignator          the task controller designator
		 * @param versionNumber                     the version number
		 * @param providedCapabilities              the provided capabilities
		 * @param numberOfBoomsSectionControl       the number of booms section control
		 * @param numberOfSectionsSectionControl    the number of sections section
		 *                                          control
		 * @param numberOfControlChannels           the number of control channels
		 */
		public TaskControllerCapabilities(ISO11783_TaskData parent, long taskControllerControlFunctionName,
				String taskControllerDesignator, VersionMajor versionNumber, int providedCapabilities,
				int numberOfBoomsSectionControl, int numberOfSectionsSectionControl, int numberOfControlChannels) {
			super(parent, "TCC");
			e.setAttribute("A", Long.toHexString(taskControllerControlFunctionName).toUpperCase());
			e.setAttribute("B", taskControllerDesignator);
			e.setAttribute("C", Integer.toString(versionNumber.number));
			if (providedCapabilities < 0 || providedCapabilities > 63)
				throw new IllegalArgumentException("invalid provided capabilities");
			e.setAttribute("D", Integer.toString(providedCapabilities));
			if (numberOfBoomsSectionControl < 0 || numberOfBoomsSectionControl > 255)
				throw new IllegalArgumentException("invalid number of booms");
			e.setAttribute("E", Integer.toString(numberOfBoomsSectionControl));
			if (numberOfSectionsSectionControl < 0 || numberOfSectionsSectionControl > 255)
				throw new IllegalArgumentException("invalid number of sections");
			e.setAttribute("F", Integer.toString(numberOfSectionsSectionControl));
			if (numberOfControlChannels < 0 || numberOfControlChannels > 255)
				throw new IllegalArgumentException("invalid number of control channels");
			e.setAttribute("G", Integer.toString(numberOfControlChannels));
		}
	}

	/**
	 * The Enum FilePreservation.
	 */
	public static enum FilePreservation {

		/** The preserve no. */
		PRESERVE_NO(1),
		/** The preserve yes. */
		PRESERVE_YES(2);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new file preservation.
		 *
		 * @param number the number
		 */
		private FilePreservation(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<FilePreservation> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<FilePreservation> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class AttachedFile.
	 */
	public static class AttachedFile extends Elem {

		/**
		 * Instantiates a new attached file.
		 *
		 * @param parent                the parent
		 * @param filenameWithExtension the filename with extension
		 * @param preserve              the preserve
		 * @param manufacturerGln       the manufacturer gln
		 * @param fileType              the file type
		 */
		public AttachedFile(ISO11783_TaskData parent, String filenameWithExtension, FilePreservation preserve,
				String manufacturerGln, int fileType) {
			super(parent, "AFE");
			e.setAttribute("A", filenameWithExtension);
			e.setAttribute("B", Integer.toString(preserve.number));
			e.setAttribute("C", manufacturerGln);
			if (fileType < 1 || fileType > 255)
				throw new IllegalArgumentException("invalid file type");
			e.setAttribute("D", Integer.toString(fileType));
		}

		/**
		 * Sets the file version.
		 *
		 * @param fileVersion the file version
		 * @return the attached file
		 */
		public AttachedFile setFileVersion(String fileVersion) {
			e.setAttribute("E", fileVersion);
			return this;
		}

		/**
		 * Sets the file length.
		 *
		 * @param fileLength the file length
		 * @return the attached file
		 */
		public AttachedFile setFileLength(long fileLength) {
			if (fileLength < 0)
				throw new IllegalArgumentException("invalid file length");
			e.setAttribute("F", Long.toString(fileLength));
			return this;
		}
	}

}
