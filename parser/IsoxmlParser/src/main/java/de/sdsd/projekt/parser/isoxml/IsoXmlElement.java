package de.sdsd.projekt.parser.isoxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.parser.isoxml.Attribute.IDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.OIDAttr;
import de.sdsd.projekt.parser.isoxml.Attribute.UnknownAttr;

/**
 * Represents an isoxml tag element.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class IsoXmlElement {
	
	/** The format. */
	private final JSONObject format;
	
	/** The root. */
	private final ISOXMLParser root;
	
	/** The tag. */
	public final String tag;
	
	/** The name. */
	public final String name;
	
	/** The uri. */
	public final String uri;
	
	/** The label. */
	@CheckForNull
	private String label;
	
	/** The id. */
	@CheckForNull
	public final IDAttr id;
	
	/** The oid. */
	@CheckForNull
	public final OIDAttr oid;
	
	/** The parent. */
	@CheckForNull
	private IsoXmlElement parent;
	
	/** The prefix. */
	private final String prefix;
	
	/** The attributes. */
	private final Map<String, Attribute<?>> attributes;
	
	/** The children. */
	private final List<IsoXmlElement> children = new ArrayList<>();
	
	/** The oidref. */
	@CheckForNull
	private Map<Integer, IsoXmlElement> oidref = null;
	
	/** The contenterrors. */
	private final Validation contenterrors = new Validation();
	
	/** The links. */
	private List<? extends Link> links;
	

	/**
	 * Instantiates a new iso xml element.
	 *
	 * @param root the root
	 * @param formats the formats
	 * @param parent the parent
	 * @param element the element
	 */
	public IsoXmlElement(ISOXMLParser root, JSONObject formats, @Nullable IsoXmlElement parent, Element element) {
		this.root = root;
		this.tag = element.getTagName();
		this.format = formats.optJSONObject(tag);
		this.parent = parent;
		this.uri = Util.createRandomUri();

		String name = null;
		IDAttr id = null;
		OIDAttr oid = null;

		if(format != null) {
			name = format.getString("name");

			JSONObject attrib = format.optJSONObject("attrib");
			if(attrib != null) {
				attributes = new LinkedHashMap<>(attrib.length());
				for(String k : attrib.keySet()) {
					JSONObject f = attrib.getJSONObject(k);
					Attribute<?> attr = Attribute.parseAttribute(this, k, f, element.hasAttribute(k) ? element.getAttribute(k) : null);
					attributes.put(attr.getName(), attr);
					if(attr.hasValue()) {
						switch(attr.getType()) {
						case ID:
							if(!attr.getStringValue().isEmpty())
								id = (IDAttr) attr;
							break;
						case OID:
							if(!attr.hasError())
								oid = (OIDAttr) attr;
							break;
						case STRING:
							if(label == null && !attr.getStringValue().isEmpty()) {
								for(String l : Arrays.asList("designator", "label", "name")) {
									if(attr.getName().toLowerCase().endsWith(l)) {
										label = attr.getStringValue();
										break;
									}
								}
							}
							break;
						default:
							break;
						}
					}
				}
				NamedNodeMap attrMap = element.getAttributes();
				for (int i = 0; i < attrMap.getLength(); ++i) {
					Attr attr = (Attr) attrMap.item(i);
					if(!attrib.has(attr.getName()))
						attributes.put(attr.getName(), Attribute.createUnknownAttribute(this, attr.getName(), attr.getValue()));
				}
			}
			else
				attributes = Collections.emptyMap();
		}
		else {
			NamedNodeMap attrMap = element.getAttributes();
			attributes = new LinkedHashMap<>(attrMap.getLength());
			for (int i = 0; i < attrMap.getLength(); ++i) {
				Attr attr = (Attr) attrMap.item(i);
				attributes.put(attr.getName(), Attribute.createUnknownAttribute(this, attr.getName(), attr.getValue()));
			}
		}

		if(label == null) {
			if(id != null) label = id.getValue();
			if(oid != null) label = tag + '#' + oid.getValue();
			if(name != null) label = name;
		}
		this.name = name != null ? name : "";
		this.id = id;
		this.oid = oid;
		if(id != null)
			root.setNodeId(this);
		if(oid != null && parent != null) {
			if(parent.oidref == null)
				parent.oidref = new HashMap<>();
			parent.oidref.put(oid.getValue(), this);
		}
		String prefix = id != null ? String.format("%s(%s)", tag, id.getValue())
				: oid != null ? String.format("%s(%d)", tag, oid.getValue()) 
						: String.format("%s(%s)", tag, label);
		this.prefix = parent != null ? parent.prefix + '.' + prefix : prefix;

		for(Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
			if(child instanceof Element) {
				children.add(new IsoXmlElement(root, formats, this, (Element) child));
			}
		}
		checkContent();
	}

	/**
	 * Removes the from parent.
	 */
	public void removeFromParent() {
		if(parent == null) return;
		if(oid != null && parent.oidref != null)
			parent.oidref.remove(oid.getValue());
		parent.children.remove(this);
		parent.checkContent();
		parent = null;
	}

	/**
	 * Adds the children.
	 *
	 * @param childs the childs
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public void addChildren(List<IsoXmlElement> childs) throws IllegalArgumentException {
		for (IsoXmlElement child : childs) {
			if(child.root != root) 
				throw new IllegalArgumentException("Trying to add children from another root");
			children.add(child);
			if(child.oid != null) {
				if(oidref == null)
					oidref = new HashMap<>();
				oidref.put(child.oid.getValue(), child);
			}
		}
		checkContent();
	}

	/**
	 * Check content.
	 */
	private void checkContent() {
		contenterrors.clear();
		if(format != null && format.has("content")) {
			JSONObject content = format.getJSONObject("content");
			HashMap<String, MutableInt> tagCount = new HashMap<>();
			for(IsoXmlElement child : children) {
				MutableInt count = tagCount.get(child.getTag());
				if(count == null) tagCount.put(child.getTag(), new MutableInt(1));
				else count.increment();
			}
			for(String k : content.keySet()) {
				int min = content.getJSONObject(k).optInt("min", 0);
				if(min > 0) {
					MutableInt count = tagCount.get(k);
					if(count == null || count.intValue() < min)
						contenterrors.warn(String.format("Contains less than %d elements of %s", min, k));
				}
			}
			for (Entry<String, MutableInt> entry : tagCount.entrySet()) {
				JSONObject f = content.optJSONObject(entry.getKey());
				if(f == null) contenterrors.warn("Unknown element: " + entry.getKey());
				else if(entry.getValue().intValue() > f.optInt("max", Integer.MAX_VALUE))
					contenterrors.warn(String.format("Must not contain more than %d elements of %s", f.getInt("max"), entry.getKey()));
			}
		}
	}

	/**
	 * Gets the root.
	 *
	 * @return the root
	 */
	@Nonnull
	public ISOXMLParser getRoot() {
		return root;
	}

	/**
	 * Gets the tag.
	 *
	 * @return the tag
	 */
	@Nonnull
	public String getTag() {
		return tag;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Nonnull
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	@CheckForNull
	public String getLabel() {
		return label;
	}
	
	/**
	 * Sets the label.
	 *
	 * @param label the label
	 * @return the iso xml element
	 */
	public IsoXmlElement setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@CheckForNull
	public IDAttr getId() {
		return id;
	}

	/**
	 * Gets the oid.
	 *
	 * @return the oid
	 */
	@CheckForNull
	public OIDAttr getOid() {
		return oid;
	}

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	@CheckForNull
	public IsoXmlElement getParent() {
		return parent;
	}

	//@Nonnull
	//public Resource toResource(Model model) {
	//	return model.createResource(uri);
	//}
	
	/**
	 * To resources.
	 *
	 * @param model the model
	 * @return the list
	 */
	public List<Resource> toResources(Model model) {
		List<Resource> resources = new ArrayList<>();
		for(String uri : getUris()) {
			resources.add(model.createResource(uri));
		}
		return resources;
	}
	
	/**
	 * Gets the uris.
	 *
	 * @return the uris
	 */
	public List<String> getUris() {
		//no links so we use our generated uri
		if(links == null || links.isEmpty()) {
			return Arrays.asList(uri);
		}
		List<String> resources = new ArrayList<>();
		for(Link link : links) {
			resources.add(link.getEscapedUri());
		}
		return resources;
	}

	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	@Nonnull
	public Map<String, Attribute<?>> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * Gets the attribute.
	 *
	 * @param name the name
	 * @return the attribute
	 */
	@Nonnull
	public Attribute<?> getAttribute(String name) {
		Attribute<?> attr = attributes.get(name);
		return attr != null ? attr : new UnknownAttr(this, name, null, null);
	}

	/**
	 * Gets the attribute.
	 *
	 * @param <T> the generic type
	 * @param name the name
	 * @param cls the cls
	 * @return the attribute
	 */
	@Nonnull
	public <T extends Attribute<?>> T getAttribute(String name, Class<T> cls) {
		try {
			Attribute<?> attr = attributes.get(name);
			return cls.isInstance(attr) ? cls.cast(attr) 
					: cls.getConstructor(IsoXmlElement.class, String.class, JSONObject.class, String.class).newInstance(this, name, null, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the children.
	 *
	 * @return the children
	 */
	@Nonnull
	public List<IsoXmlElement> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * Find children.
	 *
	 * @param tag the tag
	 * @return the list
	 */
	@Nonnull
	public List<IsoXmlElement> findChildren(String tag) {
		ArrayList<IsoXmlElement> out = new ArrayList<>();
		for (IsoXmlElement child : children) {
			if(child.getTag().equalsIgnoreCase(tag))
				out.add(child);
		}
		return out;
	}

	/**
	 * Find child.
	 *
	 * @param tag the tag
	 * @return the iso xml element
	 */
	@CheckForNull
	public IsoXmlElement findChild(String tag) {
		for (IsoXmlElement child : children) {
			if(child.getTag().equalsIgnoreCase(tag))
				return child;
		}
		return null;
	}

	/**
	 * Gets the node by O id.
	 *
	 * @param oid the oid
	 * @return the node by O id
	 */
	@CheckForNull
	public IsoXmlElement getNodeByOId(Integer oid) {
		return oidref != null ? oidref.get(oid) : null;
	}

	/**
	 * Prefix end.
	 *
	 * @param msg the msg
	 * @return the string
	 */
	@Nonnull
	public String prefixEnd(String msg) {
		return prefix + ": " + msg;
	}

	/**
	 * Prefix mid.
	 *
	 * @param msg the msg
	 * @return the string
	 */
	@Nonnull
	public String prefixMid(String msg) {
		return prefix + '.' + msg;
	}

	/**
	 * Checks for errors.
	 *
	 * @return true, if successful
	 */
	@Nonnull
	public boolean hasErrors() {
		if(format == null) return true;
		if(!contenterrors.isEmpty()) return true;
		for (Attribute<?> attr : attributes.values()) {
			if(attr.hasError()) return true;
			if(attr.hasValue() && attr instanceof RefAttr && !((RefAttr) attr).tryDeRef()) return true;
		}
		return false;
	}

	/**
	 * Checks for errors all.
	 *
	 * @return true, if successful
	 */
	@Nonnull
	public boolean hasErrorsAll() {
		if(hasErrors()) return true;
		for (IsoXmlElement child : children) {
			if(child.hasErrorsAll()) return true;
		}
		return false;
	}

	/**
	 * Gets the errors.
	 *
	 * @return the errors
	 */
	@Nonnull
	public Validation getErrors() {
		Validation out = new Validation();
		if(format == null) 
			out.warn(prefixEnd("Unknown element"));
		for(Attribute<?> attr : attributes.values()) {
			if(attr.hasError())
				out.error(prefixMid(attr.getError()));
			if(attr.hasWarning())
				out.warn(prefixMid(attr.getWarning()));
			if(attr.hasValue() && attr instanceof RefAttr) {
				RefAttr ref = (RefAttr) attr;
				if(!ref.tryDeRef()) {
					String refError = ref.getRefError();
					if(refError != null) out.error(prefixMid(refError));
				}
			}
		}
		out.addAll(contenterrors, prefix + ": ");
		return out;
	}

	/**
	 * Gets the all errors.
	 *
	 * @return the all errors
	 */
	@Nonnull
	public Validation getAllErrors() {
		Validation out = getErrors();
		children.stream()
				.map(IsoXmlElement::getErrors)
				.forEachOrdered(out::addAll);
		return out;
	}

	/**
	 * Sets the links.
	 *
	 * @param links the new links
	 */
	public void setLinks(List<? extends Link> links) {
		this.links = links;
	}
	
}
