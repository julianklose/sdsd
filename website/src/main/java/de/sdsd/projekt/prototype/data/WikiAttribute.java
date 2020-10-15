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
	
	/** The domain. */
	public final WikiEntry domain;
	
	/** The range. */
	public final WikiEntry range;
	
	/** The unit. */
	private String unit = null;

	/**
	 * Instantiates a new wiki attribute.
	 *
	 * @param type the type
	 * @param res the res
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiAttribute(WikiEntry type, Resource res) throws NoSuchElementException {
		super(res);
		this.domain = type;
		this.range = new WikiEntry(res.getRequiredProperty(RDFS.range).getResource());
		Statement unit = res.getProperty(TripleFunctions.UNIT);
		if(unit != null) this.unit = unit.getString();
		if(type instanceof WikiClass)
			((WikiClass)type).addAttribute(this);
	}
	
	/**
	 * Instantiates a new wiki attribute.
	 *
	 * @param type the type
	 * @param identifier the identifier
	 * @param label the label
	 * @param range the range
	 */
	public WikiAttribute(WikiEntry type, String identifier, String label, WikiEntry range) {
		this(type, TripleFunctions.createWikiPropertyUri(type.uri, identifier), identifier, label, range);
	}
	
	/**
	 * Instantiates a new wiki attribute.
	 *
	 * @param type the type
	 * @param uri the uri
	 * @param identifier the identifier
	 * @param label the label
	 * @param range the range
	 */
	public WikiAttribute(WikiEntry type, String uri, String identifier, String label, WikiEntry range) {
		super(uri, new WikiEntry(RDF.Property.getURI(), null, "property", "Property"), identifier, label);
		this.domain = type;
		this.range = range;
		if(type instanceof WikiClass)
			((WikiClass)type).addAttribute(this);
	}
	
	/**
	 * Prop.
	 *
	 * @param model the model
	 * @return the property
	 */
	public Property prop(Model model) {
		return model.createProperty(uri);
	}
	
	/**
	 * Sets the label.
	 *
	 * @param label the label
	 * @return the wiki attribute
	 */
	@Override
	public WikiAttribute setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Gets the domain.
	 *
	 * @return the domain
	 */
	public WikiEntry getDomain() {
		return domain;
	}
	
	/**
	 * Gets the range.
	 *
	 * @return the range
	 */
	public WikiEntry getRange() {
		return range;
	}
	
	/**
	 * Gets the unit.
	 *
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Sets the unit.
	 *
	 * @param unit the new unit
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Write to.
	 *
	 * @param model the model
	 * @return the resource
	 */
	@Override
	public Resource writeTo(Model model) {
		Resource res = super.writeTo(model);
		res.addProperty(RDFS.domain, domain.res(model));
		res.addProperty(RDFS.range, range.res(model));
		if(unit != null) res.addLiteral(TripleFunctions.UNIT, Util.lit(unit));
		return res;
	}
	
}