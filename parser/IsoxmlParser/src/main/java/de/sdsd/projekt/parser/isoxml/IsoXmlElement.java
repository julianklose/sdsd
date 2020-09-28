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
	private final JSONObject format;
	private final ISOXMLParser root;
	public final String tag;
	public final String name;
	public final String uri;
	@CheckForNull
	private String label;
	@CheckForNull
	public final IDAttr id;
	@CheckForNull
	public final OIDAttr oid;
	@CheckForNull
	private IsoXmlElement parent;
	private final String prefix;
	private final Map<String, Attribute<?>> attributes;
	private final List<IsoXmlElement> children = new ArrayList<>();
	@CheckForNull
	private Map<Integer, IsoXmlElement> oidref = null;
	private final List<String> contenterrors = new ArrayList<>();
	private List<? extends Link> links;
	

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

	public void removeFromParent() {
		if(parent == null) return;
		if(oid != null && parent.oidref != null)
			parent.oidref.remove(oid.getValue());
		parent.children.remove(this);
		parent.checkContent();
		parent = null;
	}

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
						contenterrors.add(String.format("Contains less than %d elements of %s", min, k));
				}
			}
			for (Entry<String, MutableInt> entry : tagCount.entrySet()) {
				JSONObject f = content.optJSONObject(entry.getKey());
				if(f == null) contenterrors.add("Unknown element: " + entry.getKey());
				else if(entry.getValue().intValue() > f.optInt("max", Integer.MAX_VALUE))
					contenterrors.add(String.format("Must not contain more than %d elements of %s", f.getInt("max"), entry.getKey()));
			}
		}
	}

	@Nonnull
	public ISOXMLParser getRoot() {
		return root;
	}

	@Nonnull
	public String getTag() {
		return tag;
	}

	@Nonnull
	public String getName() {
		return name;
	}
	
	@CheckForNull
	public String getLabel() {
		return label;
	}
	public IsoXmlElement setLabel(String label) {
		this.label = label;
		return this;
	}

	@CheckForNull
	public IDAttr getId() {
		return id;
	}

	@CheckForNull
	public OIDAttr getOid() {
		return oid;
	}

	@CheckForNull
	public IsoXmlElement getParent() {
		return parent;
	}

	//@Nonnull
	//public Resource toResource(Model model) {
	//	return model.createResource(uri);
	//}
	
	public List<Resource> toResources(Model model) {
		List<Resource> resources = new ArrayList<>();
		for(String uri : getUris()) {
			resources.add(model.createResource(uri));
		}
		return resources;
	}
	
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

	@Nonnull
	public Map<String, Attribute<?>> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Nonnull
	public Attribute<?> getAttribute(String name) {
		Attribute<?> attr = attributes.get(name);
		return attr != null ? attr : new UnknownAttr(this, name, null, null);
	}

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

	@Nonnull
	public List<IsoXmlElement> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Nonnull
	public List<IsoXmlElement> findChildren(String tag) {
		ArrayList<IsoXmlElement> out = new ArrayList<>();
		for (IsoXmlElement child : children) {
			if(child.getTag().equalsIgnoreCase(tag))
				out.add(child);
		}
		return out;
	}

	@CheckForNull
	public IsoXmlElement findChild(String tag) {
		for (IsoXmlElement child : children) {
			if(child.getTag().equalsIgnoreCase(tag))
				return child;
		}
		return null;
	}

	@CheckForNull
	public IsoXmlElement getNodeByOId(Integer oid) {
		return oidref != null ? oidref.get(oid) : null;
	}

	@Nonnull
	public String prefixEnd(String msg) {
		return prefix + ": " + msg;
	}

	@Nonnull
	public String prefixMid(String msg) {
		return prefix + '.' + msg;
	}

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

	@Nonnull
	public boolean hasErrorsAll() {
		if(hasErrors()) return true;
		for (IsoXmlElement child : children) {
			if(child.hasErrorsAll()) return true;
		}
		return false;
	}

	@Nonnull
	public List<String> getErrors() {
		List<String> out = new ArrayList<>();
		if(format == null) 
			out.add(prefixEnd("Unknown element"));
		for(Attribute<?> attr : attributes.values()) {
			if(attr.hasError())
				out.add(prefixMid(attr.getError()));
			if(attr.hasValue() && attr instanceof RefAttr) {
				RefAttr ref = (RefAttr) attr;
				if(!ref.tryDeRef()) {
					String refError = ref.getRefError();
					if(refError != null) out.add(prefixMid(refError));
				}
			}
		}
		contenterrors.stream()
		.map(this::prefixEnd)
		.forEachOrdered(out::add);
		return out;
	}

	@Nonnull
	public List<String> getAllErrors() {
		List<String> out = getErrors();
		children.stream()
		.map(IsoXmlElement::getErrors)
		.forEachOrdered(out::addAll);
		return out;
	}

	public void setLinks(List<? extends Link> links) {
		this.links = links;
	}
	
}
