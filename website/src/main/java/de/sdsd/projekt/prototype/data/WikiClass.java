package de.sdsd.projekt.prototype.data;

import static de.sdsd.projekt.prototype.data.Util.lit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.sdsd.projekt.prototype.applogic.TripleFunctions;

/**
 * Represents a wikinormia class/type.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikiClass extends WikiEntry {
	
	/** The comment. */
	private Optional<String> comment;
	
	/** The format. */
	private final Optional<WikiEntry> format;
	
	/** The base. */
	private final Map<String, WikiEntry> base = new HashMap<>();
	
	/** The subtypes. */
	private final Map<String, WikiEntry> subtypes = new HashMap<>();
	
	/** The part of. */
	private final Map<String, WikiEntry> partOf = new HashMap<>();
	
	/** The parts. */
	private final Map<String, WikiEntry> parts = new HashMap<>();
	
	/** The attributes. */
	private final Map<String, WikiAttribute> attributes = new HashMap<>();
	
	/**
	 * Instantiates a new wiki class.
	 *
	 * @param model the model
	 * @param uri the uri
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiClass(Model model, String uri) throws NoSuchElementException {
		super(model, uri);
		Resource res = model.getResource(uri);
		Statement stmt = res.getProperty(RDFS.comment);
		this.comment = stmt != null ? Optional.of(stmt.getString()) : Optional.empty();
		
		stmt = res.getProperty(DCTerms.format);
		this.format = stmt != null ? Optional.of(new WikiEntry(stmt.getResource())) : Optional.empty();
		
		StmtIterator sit = res.listProperties(RDFS.subClassOf);
		try {
			while(sit.hasNext()) {
				Resource r = sit.next().getResource();
				this.base.put(r.getURI(), new WikiEntry(r));
			} 
		} finally {
			sit.close();
		}
		
		ResIterator rit = model.listResourcesWithProperty(RDFS.subClassOf, res);
		try {
			while(rit.hasNext()) {
				Resource r = rit.next();
				this.subtypes.put(r.getURI(), new WikiEntry(r));
			}
		} finally {
			rit.close();
		}
		
		sit = res.listProperties(DCTerms.isPartOf);
		try {
			while(sit.hasNext()) {
				Resource r = sit.next().getResource();
				this.partOf.put(r.getURI(), new WikiEntry(r));
			} 
		} finally {
			sit.close();
		}
		
		rit = model.listResourcesWithProperty(DCTerms.isPartOf, res);
		try {
			while(rit.hasNext()) {
				Resource r = rit.next();
				this.parts.put(r.getURI(), new WikiEntry(r));
			}
		} finally {
			rit.close();
		}
		
		rit = model.listResourcesWithProperty(RDFS.domain, res);
		try {
			while(rit.hasNext()) {
				new WikiAttribute(this, rit.next());
			}
		} finally {
			rit.close();
		}
	}
	
	/**
	 * Instantiates a new wiki class.
	 *
	 * @param format the format
	 * @param identifier the identifier
	 * @param label the label
	 */
	public WikiClass(@Nullable WikiEntry format, String identifier, String label) {
		this((format != null && !format.uri.equals(TripleFunctions.FORMAT_UNKNOWN.getURI())) ? 
				TripleFunctions.createWikiResourceUri(format.uri, identifier) : 
				TripleFunctions.createWikiResourceUri(identifier), format, identifier, label);
	}
	
	/**
	 * Instantiates a new wiki class.
	 *
	 * @param uri the uri
	 * @param format the format
	 * @param identifier the identifier
	 * @param label the label
	 */
	public WikiClass(String uri, @Nullable WikiEntry format, String identifier, String label) {
		super(uri, new WikiEntry(RDFS.Class.getURI(), null, "class", "Class"), identifier, label);
		this.format = Optional.ofNullable(format);
		this.comment = Optional.empty();
	}
	
	/**
	 * Creates the sub type.
	 *
	 * @param identifier the identifier
	 * @param label the label
	 * @return the wiki class
	 */
	public WikiClass createSubType(String identifier, String label) {
		return new WikiClass(format.orElse(null), identifier, label)
				.addBase(this);
	}
	
	/**
	 * Creates the part.
	 *
	 * @param identifier the identifier
	 * @param label the label
	 * @return the wiki class
	 */
	public WikiClass createPart(String identifier, String label) {
		return new WikiClass(format.orElse(null), identifier, label)
				.addPartOf(this);
	}
	
	/**
	 * Creates the attribute.
	 *
	 * @param identifier the identifier
	 * @param label the label
	 * @param range the range
	 * @return the wiki attribute
	 */
	public WikiAttribute createAttribute(String identifier, String label, WikiEntry range) {
		return new WikiAttribute(this, identifier, label, range);
	}
	
	/**
	 * Adds the attribute.
	 *
	 * @param identifier the identifier
	 * @param label the label
	 * @param range the range
	 * @return the wiki class
	 */
	public WikiClass addAttribute(String identifier, String label, WikiEntry range) {
		new WikiAttribute(this, identifier, label, range);
		return this;
	}
	
	/**
	 * Creates the instance.
	 *
	 * @param identifier the identifier
	 * @param label the label
	 * @return the wiki instance
	 */
	public WikiInstance createInstance(String identifier, String label) {
		return new WikiInstance(this, identifier, label);
	}
	
	/**
	 * Sets the label.
	 *
	 * @param label the label
	 * @return the wiki class
	 */
	@Override
	public WikiClass setLabel(String label) {
		this.label = label;
		return this;
	}
	
	/**
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	public Optional<String> getComment() {
		return comment;
	}

	/**
	 * Sets the comment.
	 *
	 * @param comment the comment
	 * @return the wiki class
	 */
	public WikiClass setComment(@Nullable String comment) {
		this.comment = Optional.ofNullable(comment);
		return this;
	}

	/**
	 * Gets the format.
	 *
	 * @return the format
	 */
	public Optional<WikiEntry> getFormat() {
		return format;
	}

	/**
	 * Gets the base.
	 *
	 * @return the base
	 */
	public Collection<WikiEntry> getBase() {
		return Collections.unmodifiableCollection(base.values());
	}

	/**
	 * Adds the base.
	 *
	 * @param base the base
	 * @return the wiki class
	 */
	public WikiClass addBase(WikiEntry base) {
		this.base.put(base.uri, base);
		if(base instanceof WikiClass)
			((WikiClass)base).subtypes.put(this.uri, this);
		return this;
	}
	
	/**
	 * Removes the base.
	 *
	 * @param uri the uri
	 * @return the wiki class
	 */
	public WikiClass removeBase(String uri) {
		WikiEntry base = this.base.remove(uri);
		if(base instanceof WikiClass)
			((WikiClass)base).subtypes.remove(this.uri);
		return this;
	}
	
	/**
	 * Gets the sub types.
	 *
	 * @return the sub types
	 */
	public Collection<WikiEntry> getSubTypes() {
		return Collections.unmodifiableCollection(subtypes.values());
	}

	/**
	 * Gets the part of.
	 *
	 * @return the part of
	 */
	public Collection<WikiEntry> getPartOf() {
		return Collections.unmodifiableCollection(partOf.values());
	}
	
	/**
	 * Adds the part of.
	 *
	 * @param parent the parent
	 * @return the wiki class
	 */
	public WikiClass addPartOf(WikiEntry parent) {
		this.partOf.put(parent.uri, parent);
		if(parent instanceof WikiClass)
			((WikiClass)parent).parts.put(this.uri, this);
		return this;
	}
	
	/**
	 * Removes the part of.
	 *
	 * @param uri the uri
	 * @return the wiki class
	 */
	public WikiClass removePartOf(String uri) {
		WikiEntry parent = this.partOf.remove(uri);
		if(parent instanceof WikiClass)
			((WikiClass)parent).parts.remove(this.uri);
		return this;
	}

	/**
	 * Gets the parts.
	 *
	 * @return the parts
	 */
	public Collection<WikiEntry> getParts() {
		return Collections.unmodifiableCollection(parts.values());
	}

	/**
	 * Gets the attributes.
	 *
	 * @return the attributes
	 */
	public Collection<WikiAttribute> getAttributes() {
		return Collections.unmodifiableCollection(attributes.values());
	}
	
	/**
	 * Adds the attribute.
	 *
	 * @param attr the attr
	 * @return the wiki class
	 */
	WikiClass addAttribute(WikiAttribute attr) {
		this.attributes.put(attr.getUri(), attr);
		return this;
	}
	
	/**
	 * Removes the attribute.
	 *
	 * @param uri the uri
	 * @return the wiki class
	 */
	public WikiClass removeAttribute(String uri) {
		this.attributes.remove(uri);
		return this;
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
		if(comment.isPresent()) res.addLiteral(RDFS.comment, lit(comment.get()));
		if(format.isPresent()) res.addProperty(DCTerms.format, format.get().res(model));
		for(WikiEntry we : base.values()) res.addProperty(RDFS.subClassOf, we.res(model));
		for(WikiEntry we : partOf.values()) res.addProperty(DCTerms.isPartOf, we.res(model));
		for(WikiAttribute wa : attributes.values()) wa.writeTo(model);
		return res;
	}
	
}
