package de.sdsd.projekt.parser.isoxml;

import org.apache.jena.util.URIref;

import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.RefAttr.IDRef;

public class Link {
	public final String value;
	
	public Link(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

	public String getEscapedUri() {
		if(value.startsWith("{") && value.endsWith("}")) {
			return URIref.encode(value.substring(1, value.length()-1));
		}
		return URIref.encode(getValue());
	}
	
	public static class LinkEntry extends Link {
		public final String id;
		public final String designator;
	
		public LinkEntry(IsoXmlElement lnk) {
			super(lnk.getAttribute("linkValue", StringAttr.class).getValue());
			if(lnk.hasErrors()) 
				throw new IllegalArgumentException("Given link contains errors");
			this.id = lnk.getAttribute("objectIdRef", IDRef.class).getValue();
			this.designator = lnk.getAttribute("linkDesignator", StringAttr.class).getValue();
		}
	
		public String getId() {
			return id;
		}
	
		public String getDesignator() {
			return designator;
		}
	}
}
