package de.sdsd.projekt.parser.isoxml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.sdsd.projekt.parser.isoxml.Link.LinkEntry;

public class LinkList extends HashMap<String, List<LinkEntry>> {
	private static final long serialVersionUID = 360329094401452166L;

	public LinkList(IsoXmlElement linklist) {
		if(!"ISO11783LinkList".equals(linklist.getTag()))
			throw new IllegalArgumentException("Given element is no LINKLIST");
		linklist.findChildren("LGP").stream()
				.flatMap(lgp -> lgp.findChildren("LNK").stream())
				.filter(lnk -> !lnk.hasErrors())
				.map(LinkEntry::new)
				.forEachOrdered(this::put);
	}
	
	private void put(LinkEntry link) {
		List<LinkEntry> links = get(link.getId());
		if(links == null) links = new ArrayList<>();
		links.add(link);
		put(link.getId(), links);
	}

}
