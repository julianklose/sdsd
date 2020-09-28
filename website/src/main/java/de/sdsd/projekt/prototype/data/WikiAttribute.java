package de.sdsd.projekt.prototype.data;

import java.util.NoSuchElementException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.sdsd.projekt.prototype.applogic.TripleFunctions;

/**
 * Represents a wikinormia attribute.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikiAttribute extends WikiEntry {
	
	public final WikiEntry domain;
	public final WikiEntry range;
	private String unit = null;

	public WikiAttribute(WikiEntry type, Resource res) throws NoSuchElementException {
		super(res);
		this.domain = type;
		this.range = new WikiEntry(res.getRequiredProperty(RDFS.range).getResource());
		Statement unit = res.getProperty(TripleFunctions.UNIT);
		if(unit != null) this.unit = unit.getString();
		if(type instanceof WikiClass)
			((WikiClass)type).addAttribute(this);
	}
	
	public WikiAttribute(WikiEntry type, String identifier, String label, WikiEntry range) {
		this(type, TripleFunctions.createWikiPropertyUri(type.uri, identifier), identifier, label, range);
	}
	
	public WikiAttribute(WikiEntry type, String uri, String identifier, String label, WikiEntry range) {
		super(uri, new WikiEntry(RDF.Property.getURI(), null, "property", "Property"), identifier, label);
		this.domain = type;
		this.range = range;
		if(type instanceof WikiClass)
			((WikiClass)type).addAttribute(this);
	}
	
	public Property prop(Model model) {
		return model.createProperty(uri);
	}
	
	@Override
	public WikiAttribute setLabel(String label) {
		this.label = label;
		return this;
	}

	public WikiEntry getDomain() {
		return domain;
	}
	
	public WikiEntry getRange() {
		return range;
	}
	
	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	@Override
	public Resource writeTo(Model model) {
		Resource res = super.writeTo(model);
		res.addProperty(RDFS.domain, domain.res(model));
		res.addProperty(RDFS.range, range.res(model));
		if(unit != null) res.addLiteral(TripleFunctions.UNIT, Util.lit(unit));
		return res;
	}
	
}