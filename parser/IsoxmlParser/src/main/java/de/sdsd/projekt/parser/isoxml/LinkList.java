package de.sdsd.projekt.parser.isoxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.sdsd.projekt.parser.isoxml.Link.LinkEntry;

/**
 * The Class LinkList.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class LinkList extends HashMap<String, List<LinkEntry>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 360329094401452166L;

	/**
	 * Instantiates a new link list.
	 *
	 * @param linklist the linklist
	 */
	public LinkList(IsoXmlElement linklist) {
		if (!"ISO11783LinkList".equals(linklist.getTag()))
			throw new IllegalArgumentException("Given element is no LINKLIST");
		linklist.findChildren("LGP").stream().flatMap(lgp -> lgp.findChildren("LNK").stream())
				.filter(lnk -> !lnk.hasErrors()).map(LinkEntry::new).forEachOrdered(this::put);
	}

	/**
	 * Put.
	 *
	 * @param link the link
	 */
	private void put(LinkEntry link) {
		List<LinkEntry> links = get(link.getId());
		if (links == null)
			links = new ArrayList<>();
		links.add(link);
		put(link.getId(), links);
	}

}
