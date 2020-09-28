package de.sdsd.projekt.parser.isoxml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiAttr;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;
import de.sdsd.projekt.parser.isoxml.Attribute.DDIAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.EnumAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.IDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.OIDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.Link.LinkEntry;

/**
 * Base class of the isoxml parser. It represents a complete isoxml taskdata zip file.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ISOXMLParser {
	public static final WikiFormat FORMAT = Util.format("isoxml");
	
	@SuppressWarnings("unused")
	private static final JSONObject FORMATS, TASKDATA, LINKLIST, TIMELOG;
	static {
		try(InputStream in = ISOXMLParser.class.getResourceAsStream("/isoxml.json")) {
			JSONObject root = new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8));
			FORMATS = root.getJSONObject("typeFormats");
			TASKDATA = root.getJSONObject("taskdata");
			LINKLIST = root.getJSONObject("linklist");
			TIMELOG = root.getJSONObject("logdata");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private final HashMap<String, byte[]> content = new HashMap<>();
	private final DocumentBuilder builder;

	private final HashMap<String, IsoXmlElement> idref = new HashMap<>();

	/**
	 * Open an isoxml zip file.
	 * If the file is no zip file, an {@link ZipException} is thrown.
	 * 
	 * @param isoxmlzip stream of a isoxml zip file
	 * @throws IOException error while reading from the input stream
	 * @throws ParserConfigurationException error while creating the document builder
	 * @throws ZipException error while reading the zip file content
	 */
	public ISOXMLParser(InputStream isoxmlzip) throws IOException, ParserConfigurationException, ZipException {
		this.builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		try(ZipInputStream stream = new ZipInputStream(isoxmlzip, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while((entry = stream.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				content.put(new File(entry.getName()).getName().toLowerCase(), IOUtils.toByteArray(stream));
			}
		}
	}

	@Nonnull
	private Document getXml(String name) throws SAXException, IOException, FileNotFoundException {
		byte[] bin = content.get(name.toLowerCase());
		if(bin == null) throw new FileNotFoundException("Couldn't find " + name);
		InputSource is = new InputSource(new ByteArrayInputStream(bin));
		is.setEncoding("UTF-8");
		return builder.parse(is);
	}

	@Nonnull
	private byte[] getBin(String name) throws FileNotFoundException {
		byte[] bin = content.get(name.toLowerCase());
		if(bin == null) throw new FileNotFoundException("Couldn't find " + name);
		return bin;
	}

	private IsoXmlElement taskdata = null;
	
	/**
	 * Reads the main task data xml from the isoxml zip file.
	 * Call {@link #resolveAllXFR(IsoXmlElement)} to include all external xml tags.
	 * 
	 * @return root xml element of the isoxml
	 * @throws SAXException if the taskdata.xml is no valid xml
	 * @throws IOException if any IO errors occur
	 * @throws FileNotFoundException if there is no taskdata.xml in the isoxml zip file
	 */
	@Nonnull
	public IsoXmlElement readTaskData() throws SAXException, IOException, FileNotFoundException {
		if(taskdata == null)
			taskdata = new IsoXmlElement(this, TASKDATA, null, getXml("taskdata.xml").getDocumentElement());
		return taskdata;
	}

	@Nonnull
	public IsoXmlElement resolveXFR(IsoXmlElement xfr) throws IllegalArgumentException, SAXException, IOException {
		if(!xfr.getTag().equals("XFR"))
			throw new IllegalArgumentException("Given element is no XFR");
		StringAttr attr = xfr.getAttribute("filename", StringAttr.class);
		if(!attr.hasValue())
			throw new IllegalArgumentException("Given element doesn't have a filename attribute");
		if(attr.hasError())
			throw new IllegalArgumentException(attr.getError());
		Element xml = getXml(attr.getValue() + ".xml").getDocumentElement();
		return new IsoXmlElement(this, TASKDATA, null, xml);
	}

	public void resolveAllXFR(IsoXmlElement taskdata, @Nullable List<String> errors) {
		List<IsoXmlElement> xfrs = taskdata.findChildren("XFR");
		List<IsoXmlElement> add = new ArrayList<>(xfrs.size());
		for (IsoXmlElement xfr : xfrs) {
			try {
				xfr.removeFromParent();
				IsoXmlElement xfc = resolveXFR(xfr);
				add.addAll(xfc.getChildren());
			} catch (IllegalArgumentException | SAXException | IOException e) {
				String file = xfr.getAttribute("filename").getStringValue();
				System.err.println(file + ".xml: " + e.getMessage());
				if(errors != null) errors.add(file + ".xml: " + e.getMessage());
			}
		}
		taskdata.addChildren(add);
	}

	public TimeLog getTimeLog(IsoXmlElement tlg) 
			throws IllegalArgumentException, FileNotFoundException, SAXException, IOException {
		if(!tlg.getTag().equals("TLG"))
			throw new IllegalArgumentException("Given element is no TLG");
		StringAttr attr = tlg.getAttribute("filename", StringAttr.class);
		if(!attr.hasValue())
			throw new IllegalArgumentException("Given element doesn't have a filename attribute");
		if(attr.hasError())
			throw new IllegalArgumentException(attr.getError());
		IsoXmlElement tim = new IsoXmlElement(this, TIMELOG, null, getXml(attr.getValue() + ".xml").getDocumentElement());
		byte[] content = getBin(attr.getValue() + ".bin");
		return new TimeLog(tlg, attr.getValue(), tim, content);
	}
	
	public List<IsoXmlElement> getAllTimeLogs() throws FileNotFoundException, SAXException, IOException {
		return readTaskData().findChildren("TSK").stream()
				.flatMap(tsk -> tsk.findChildren("TLG").stream())
				.collect(Collectors.toList());
	}
	
	public Grid getGrid(IsoXmlElement grd) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		if(!grd.getTag().equals("GRD"))
			throw new IllegalArgumentException("Given element is no GRD");
		StringAttr attr = grd.getAttribute("filename", StringAttr.class);
		if(!attr.hasValue())
			throw new IllegalArgumentException("Given element doesn't have a filename attribute");
		if(attr.hasError())
			throw new IllegalArgumentException(attr.getError());
		byte[] content = getBin(attr.getValue() + ".bin");
		return new Grid(grd, content);
	}
	
	public List<IsoXmlElement> getAllGrids() throws FileNotFoundException, SAXException, IOException {
		return readTaskData().findChildren("TSK").stream()
				.flatMap(tsk -> tsk.findChildren("GRD").stream())
				.collect(Collectors.toList());
	}
	
	public List<Geo.Point> getPoints(IsoXmlElement pnt) 
			throws IllegalArgumentException, FileNotFoundException, IOException {
		if(!pnt.getTag().equals("PNT"))
			throw new IllegalArgumentException("Given element is no PNT");
		StringAttr attr = pnt.getAttribute("filename", StringAttr.class);
		if(!attr.hasValue())
			throw new IllegalArgumentException("Given element doesn't have a filename attribute");
		if(attr.hasError())
			throw new IllegalArgumentException(attr.getError());
		byte[] content = getBin(attr.getValue() + ".bin");
		return Geo.readBinaryPoints(pnt, content);
	}
	
	public List<Geo> getAllGeometries() throws FileNotFoundException, SAXException, IOException {
		List<Geo> list = new ArrayList<>();
		findGeometries(readTaskData(), list);
		return list;
	}
	
	private void findGeometries(IsoXmlElement e, List<Geo> list) {
		for(IsoXmlElement c : e.getChildren()) {
			try {
				switch(c.getTag()) {
				case "PFD":
				case "GGP":
				case "GPN":
				case "TSK":
				case "TZN":
				case "P339_WorkedArea":
					findGeometries(c, list);
					break;
				case "PNT":
					list.addAll(Geo.Point.read(c));
					break;
				case "LSG":
					list.add(new Geo.LineString(c));
					break;
				case "PLN":
					list.add(new Geo.Polygon(c));
					break;
				}
			} catch (IllegalArgumentException | IOException e1) {
			}
		}
	}
	
	//link list
	
	public static final String LinkListFilename = "LINKLIST.XML";
	public IsoXmlElement getLinkList(IsoXmlElement afe) 
			throws IllegalArgumentException, FileNotFoundException, SAXException, IOException {
		if(!LinkListFilename.equalsIgnoreCase(afe.getAttribute("filenameWithExtension", StringAttr.class).getValue()))
			throw new IllegalArgumentException("Given element doesn't describe an attached linklist");
		return new IsoXmlElement(this, LINKLIST, null, getXml(LinkListFilename).getDocumentElement());
	}
	
	public LinkList buildLinkList(IsoXmlElement linklist) throws IllegalArgumentException {
		return new LinkList(linklist);
	}
	
	public void integrateLinkList() {
		for(IsoXmlElement afe : taskdata.findChildren("AFE")) {
			try {
				IsoXmlElement linkList = getLinkList(afe);
				LinkList links = buildLinkList(linkList);
				
				for(Entry<String, List<LinkEntry>> e : links.entrySet()) {
					IsoXmlElement element = getNodeById(e.getKey());
					if(element != null)
						element.setLinks(e.getValue());
				}
				
			} catch(IllegalArgumentException e) { 
				// no linklist AFE
			} catch(IOException | SAXException e) { 
				// error while reading linklist
				System.err.println(e.getMessage());
			}
		}
	}

	@CheckForNull
	public IsoXmlElement getNodeById(String id) {
		return idref.get(id);
	}

	boolean setNodeId(IsoXmlElement node) {
		IDAttr id = node.getId();
		if(id != null && id.hasValue()) {
			idref.put(id.getValue(), node);
			return true;
		}
		return false;
	}
	

	private static final List<String> GEOMETRY_TAGS = Arrays.asList("PLN", "LSG", "PNT");
	public static final WikiType T_DDI = Util.UNKNOWN.res("ddi");
	
	public List<Resource> toRDF(Model model, IsoXmlElement element, boolean omitGeometries) {
		
		if(omitGeometries && element.getParent() != null && GEOMETRY_TAGS.contains(element.getParent().getTag()))
			return Collections.emptyList();

		List<Resource> resources = element.toResources(model);
		
		for(Resource resource : resources) {

			//class and type assertion
			WikiType clazz = FORMAT.res(element.getTag()); //model.createResource(wikinormia + element.getName());
			model.add(resource, RDF.type, clazz);
			
			if(element.getLabel() != null)
				model.add(resource, RDFS.label, element.getLabel());

			//this is the local id in ISOXML like "PNT806"
			//TODO maybe not correct to put that in XML page
			//Property propertyId = ResourceFactory.createProperty(wikinormiaXML + "#id");

			for(Attribute<?> attr : element.getAttributes().values()) {
				if(!attr.hasValue()) continue;

				//propery is always fragment identifier
				WikiAttr property = clazz.prop(attr.getKey());

				if(attr instanceof RefAttr) {
					//object property
					RefAttr refAttr = (RefAttr) attr;

					IsoXmlElement objectElement = refAttr.getRef();
					if(objectElement != null) {
						//assertion
						for(Resource object : objectElement.toResources(model)) {
							model.add(resource, property, object);
						}
					}

				} else if(attr instanceof EnumAttr) {
					WikiType enumType = clazz.res(attr.getKey());
					Resource enumVal = enumType.inst(Byte.toString(((EnumAttr)attr).number()));
					model.add(resource, property, enumVal);
				} else if(attr instanceof DDIAttr) {
					model.add(resource, property, T_DDI.inst(((DDIAttr)attr).getValue()));
				} else if(attr instanceof OIDAttr && element.getTag().startsWith("D")) { 
					try {
						model.addLiteral(resource, FORMAT.res("DO").prop("id"), attr.toLiteral());
					} catch(Exception e) {} // ignore
				} else {
					try {
						model.addLiteral(resource, property, attr.toLiteral());
					} catch(Exception e) {} // ignore

					//special id property
					//if(property.getLocalName().endsWith("Id")) {
					//	model.add(resource, propertyId, attr.getStringValue());
					//}
				}
			}
		}

		//recursive for children
		for(IsoXmlElement child : element.getChildren()) {

			//recursive call
			List<Resource> childResources = toRDF(model, child, omitGeometries);
			
			//Property propertyChild = ResourceFactory.createProperty(wikinormiaXML + "#child");
			//Property propertyParent = ResourceFactory.createProperty(wikinormiaXML + "#parent");
			
			//child - parent - relation
			for(Resource res : resources) {
				for(Resource ch : childResources) {
					//model.add(res, DCTerms.isPartOf, ch);
					model.add(ch, DCTerms.isPartOf, res);
				}
			}
		}
		
		return resources;
	}

}
