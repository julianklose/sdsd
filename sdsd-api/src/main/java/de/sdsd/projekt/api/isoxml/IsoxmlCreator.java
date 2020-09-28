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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class IsoxmlCreator {
	final Document document;
	private final Map<String, MutableInt> ids = new HashMap<>();
	
	/** The ISOXML root element. */
	public final ISO11783_TaskData root;
	
	/**
	 * Instantiates a new ISOXML creator.
	 *
	 * @param managementSoftwareManufacturer the management software manufacturer
	 * @throws ParserConfigurationException Indicates a serious configuration error of the XML creator.
	 */
	public IsoxmlCreator(String managementSoftwareManufacturer) throws ParserConfigurationException {
		this.document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
        		.newDocument();
		this.root = new ISO11783_TaskData(VersionMajor.E2_FDIS, 2, DataTransferOrigin.FMIS)
				.setManagementSoftwareManufacturer(managementSoftwareManufacturer);
	}
	
	/**
	 * Write the main TASKDATA.XML file.
	 *
	 * @param out the stream to write to
	 * @throws TransformerFactoryConfigurationError the transformer factory configuration error
	 * @throws TransformerException the transformer exception
	 */
	public void writeTaskData(OutputStream out) 
			throws TransformerFactoryConfigurationError, TransformerException {
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
		if(num == null) ids.put(tag, num = new MutableInt());
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
		if(num == null) ids.put(parent.id, num = new MutableInt());
		return Integer.toString(num.incrementAndGet());
	}
	

	/**
	 * The base class for ISOXML elements.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static abstract class Elem {
		protected final IsoxmlCreator isoxmlCreator;
		protected final Element e;
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
			if(ddi < 0 || ddi > 0xFFFF)
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
			if(north < -90 || north > 90)
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
			if(east < -180 || east > 180)
				throw new IllegalArgumentException("invalid east position");
			return floating(east);
		}
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
			if(!Double.isFinite(val))
				throw new IllegalArgumentException("invalid floating point value");
			return df.format(val);
		}
		
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
		 * @param tag the tag
		 */
		Elem(Elem parent, String tag) {
			this.isoxmlCreator = parent.isoxmlCreator;
			this.e = isoxmlCreator.document.createElement(tag);
			parent.e.appendChild(e);
			this.id = isoxmlCreator.createID(tag);
			this.parent = parent;
		}
		
		/**
		 * Returns the ISOXML ID (TAG1)
		 *
		 * @return ISOXML ID
		 */
		public String id() {
			return id;
		}
		
		/**
		 * Returns the parent element.
		 * Might only be null for the root element.
		 *
		 * @return the parent element
		 */
		public Elem parent() {
			return parent;
		}
		
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
			if(parent != null)
				parent.removeChild(e);
		}
	}
	
	/**
	 * Base class for elements with an object ID.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	static abstract class OIDElem extends Elem {
		protected final String oid;
		
		/**
		 * Instantiates a new OID elem.
		 *
		 * @param parent the parent
		 * @param tag the tag
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
	
	public static enum VersionMajor {
		DIS(0), FDIS_1(1), FDIS_2(2), E2_DIS(3), E2_FDIS(4);
		
		public final int number;
		private VersionMajor(int number) {
			this.number = number;
		}
		
		public static Optional<VersionMajor> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		
		public static Optional<VersionMajor> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static enum DataTransferOrigin {
		FMIS(1), MICS(2);
		
		public final int number;
		private DataTransferOrigin(int number) {
			this.number = number;
		}
		
		public static Optional<DataTransferOrigin> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<DataTransferOrigin> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public class ISO11783_TaskData extends Elem {
		public ISO11783_TaskData(VersionMajor versionMajor, int versionMinor, DataTransferOrigin origin) {
			super(IsoxmlCreator.this, "ISO11783_TaskData");
			e.setAttribute("VersionMajor", Integer.toString(versionMajor.number));
			if(versionMinor < 0 || versionMinor > 99)
				throw new IllegalArgumentException("invalid version minor");
	        e.setAttribute("VersionMinor", Integer.toString(versionMinor));
	        e.setAttribute("DataTransferOrigin", Integer.toString(origin.number));
		}
		
		public ISO11783_TaskData setManagementSoftwareManufacturer(String managementSoftwareManufacturer) {
			e.setAttribute("ManagementSoftwareManufacturer", managementSoftwareManufacturer);
			return this;
		}
		
		public ISO11783_TaskData setManagementSoftwareVersion(String managementSoftwareVersion) {
			e.setAttribute("ManagementSoftwareVersion", managementSoftwareVersion);
			return this;
		}
		
		public ISO11783_TaskData setTaskControllerManufacturer(String taskControllerManufacturer) {
			e.setAttribute("TaskControllerManufacturer", taskControllerManufacturer);
			return this;
		}
		
		public ISO11783_TaskData setTaskControllerVersion(String taskControllerVersion) {
			e.setAttribute("TaskControllerVersion", taskControllerVersion);
			return this;
		}
		
		public ISO11783_TaskData setDataTransferLanguage(String lang) {
			e.setAttribute("lang", lang);
			return this;
		}
		
		public AttachedFile addAttachedFile(String filenameWithExtension, 
				FilePreservation preserve, String manufacturerGln, int fileType) {
			return new AttachedFile(this, filenameWithExtension, preserve, manufacturerGln, fileType);
		}
		
		public Partfield.BaseStation addBaseStation(String designator, 
				double north, double east, int up) {
			return new Partfield.BaseStation(this, designator, north, east, up);
		}
		
		public Task.CodedComment addCodedComment(String designator, Task.CodedCommentScope scope) {
			return new Task.CodedComment(this, designator, scope);
		}
		
		public Task.CodedCommentGroup addCodedCommentGroup(String designator) {
			return new Task.CodedCommentGroup(this, designator);
		}
		
		public Device.ColourLegend addColourLegend() {
			return new Device.ColourLegend(this);
		}
		
		public Partfield.CropType addCropType(String designator) {
			return new Partfield.CropType(this, designator);
		}
		
		public Task.CulturalPractice addCulturalPractice(String designator) {
			return new Task.CulturalPractice(this, designator);
		}
		
		public Task.Customer addCustomer(String lastName) {
			return new Task.Customer(this, lastName);
		}
		
		public Device addDevice(byte[] clientName, byte[] deviceStructureLabel, byte[] deviceLocalizationLabel) {
			return new Device(this, clientName, deviceStructureLabel, deviceLocalizationLabel);
		}
		public Device addDevice(long clientName, byte[] deviceStructureLabel, long deviceLocalizationLabel) {
			return new Device(this, clientName, deviceStructureLabel, deviceLocalizationLabel);
		}
		
		public Task.Farm addFarm(String designator) {
			return new Task.Farm(this, designator);
		}
		
		public Task.OperationTechnique addOperationTechnique(String designator) {
			return new Task.OperationTechnique(this, designator);
		}
		
		public Partfield addPartfield(String designator, long area) {
			return new Partfield(this, designator, area);
		}
		
		public Partfield.Product addProduct(String designator) {
			return new Partfield.Product(this, designator);
		}
		
		public Partfield.ProductGroup addProductGroup(String designator) {
			return new Partfield.ProductGroup(this, designator);
		}
		
		public Task addTask(String designator, Task.TaskStatus status) {
			return new Task(this, designator, status);
		}
		
		public TaskControllerCapabilities addTaskControllerCapabilities(long taskControllerControlFunctionName, 
				String taskControllerDesignator, VersionMajor versionNumber, int providedCapabilities, 
				int numberOfBoomsSectionControl, int numberOfSectionsSectionControl, int numberOfControlChannels) {
			return new TaskControllerCapabilities(this, taskControllerControlFunctionName, 
					taskControllerDesignator, versionNumber, providedCapabilities, 
					numberOfBoomsSectionControl, numberOfSectionsSectionControl, numberOfControlChannels);
		}
		
		public Device.ValuePresentation addValuePresentation(int offset, double scale, int numberOfDecimals) {
			return new Device.ValuePresentation(this, offset, scale, numberOfDecimals);
		}
		
		public Task.Worker addWorker(String lastName) {
			return new Task.Worker(this, lastName);
		}
	}
	
	public static class TaskControllerCapabilities extends Elem {
		public TaskControllerCapabilities(ISO11783_TaskData parent, long taskControllerControlFunctionName, 
				String taskControllerDesignator, VersionMajor versionNumber, int providedCapabilities, 
				int numberOfBoomsSectionControl, int numberOfSectionsSectionControl, int numberOfControlChannels) {
			super(parent, "TCC");
			e.setAttribute("A", Long.toHexString(taskControllerControlFunctionName).toUpperCase());
			e.setAttribute("B", taskControllerDesignator);
			e.setAttribute("C", Integer.toString(versionNumber.number));
			if(providedCapabilities < 0 || providedCapabilities > 63)
				throw new IllegalArgumentException("invalid provided capabilities");
			e.setAttribute("D", Integer.toString(providedCapabilities));
			if(numberOfBoomsSectionControl < 0 || numberOfBoomsSectionControl > 255)
				throw new IllegalArgumentException("invalid number of booms");
			e.setAttribute("E", Integer.toString(numberOfBoomsSectionControl));
			if(numberOfSectionsSectionControl < 0 || numberOfSectionsSectionControl > 255)
				throw new IllegalArgumentException("invalid number of sections");
			e.setAttribute("F", Integer.toString(numberOfSectionsSectionControl));
			if(numberOfControlChannels < 0 || numberOfControlChannels > 255)
				throw new IllegalArgumentException("invalid number of control channels");
			e.setAttribute("G", Integer.toString(numberOfControlChannels));
		}
	}
	
	public static enum FilePreservation {
		PRESERVE_NO(1), PRESERVE_YES(2);
		
		public final int number;
		private FilePreservation(int number) {
			this.number = number;
		}
		
		public static Optional<FilePreservation> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}
		public static Optional<FilePreservation> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_')+1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public static class AttachedFile extends Elem {
		public AttachedFile(ISO11783_TaskData parent, String filenameWithExtension, 
				FilePreservation preserve, String manufacturerGln, int fileType) {
			super(parent, "AFE");
			e.setAttribute("A", filenameWithExtension);
			e.setAttribute("B", Integer.toString(preserve.number));
			e.setAttribute("C", manufacturerGln);
			if(fileType < 1 || fileType > 255)
				throw new IllegalArgumentException("invalid file type");
			e.setAttribute("D", Integer.toString(fileType));
		}
		
		public AttachedFile setFileVersion(String fileVersion) {
			e.setAttribute("E", fileVersion);
			return this;
		}
		
		public AttachedFile setFileLength(long fileLength) {
			if(fileLength < 0)
				throw new IllegalArgumentException("invalid file length");
			e.setAttribute("F", Long.toString(fileLength));
			return this;
		}
	}
	
}
