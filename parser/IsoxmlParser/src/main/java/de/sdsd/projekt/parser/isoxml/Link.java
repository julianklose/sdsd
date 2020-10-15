package de.sdsd.projekt.parser.isoxml;

import org.apache.jena.util.URIref;

import de.sdsd.projekt.parser.isoxml.Attribute.StringAttr;
import de.sdsd.projekt.parser.isoxml.RefAttr.IDRef;

/**
 * The Class Link.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class Link {

	/** The value. */
	public final String value;

	/**
	 * Instantiates a new link.
	 *
	 * @param value the value
	 */
	public Link(String value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Gets the escaped uri.
	 *
	 * @return the escaped uri
	 */
	public String getEscapedUri() {
		if (value.startsWith("{") && value.endsWith("}")) {
			return URIref.encode(value.substring(1, value.length() - 1));
		}
		return URIref.encode(getValue());
	}

	/**
	 * The Class LinkEntry.
	 */
	public static class LinkEntry extends Link {

		/** The id. */
		public final String id;

		/** The designator. */
		public final String designator;

		/**
		 * Instantiates a new link entry.
		 *
		 * @param lnk the lnk
		 */
		public LinkEntry(IsoXmlElement lnk) {
			super(lnk.getAttribute("linkValue", StringAttr.class).getValue());
			if (lnk.hasErrors())
				throw new IllegalArgumentException("Given link contains errors");
			this.id = lnk.getAttribute("objectIdRef", IDRef.class).getValue();
			this.designator = lnk.getAttribute("linkDesignator", StringAttr.class).getValue();
		}

		/**
		 * Gets the id.
		 *
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public String getDesignator() {
			return designator;
		}
	}
}
