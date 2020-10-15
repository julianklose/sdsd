package de.sdsd.projekt.prototype.data;

import static de.sdsd.projekt.prototype.data.Util.lit;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Represents a generic wikinormia entry.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WikiEntry implements Comparable<WikiEntry> {
	
	/**
	 * Gets the main resource.
	 *
	 * @param model the model
	 * @param uri the uri
	 * @return the main resource
	 * @throws NoSuchElementException the no such element exception
	 */
	private static Resource getMainResource(Model model, String uri) throws NoSuchElementException {
		Resource res = model.getResource(uri);
		if (!model.containsResource(res))
			throw new NoSuchElementException("The given URI doesn't exist in the model");
		return res;
	}

	/** The uri. */
	protected final String uri;
	
	/** The type. */
	protected final Optional<WikiEntry> type;
	
	/** The identifier. */
	protected final String identifier;
	
	/** The label. */
	protected String label;
	
	/**
	 * Instantiates a new wiki entry.
	 *
	 * @param model the model
	 * @param uri the uri
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiEntry(Model model, String uri) throws NoSuchElementException {
		this(getMainResource(model, uri));
	}
	
	/**
	 * Instantiates a new wiki entry.
	 *
	 * @param res the res
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiEntry(Resource res) throws NoSuchElementException {
		this.uri = res.getURI();
		Statement stmt = res.getProperty(RDF.type);
		this.type = stmt != null ? Optional.of(new WikiEntry(stmt.getResource())) : Optional.empty();
		stmt = res.getProperty(DCTerms.identifier);
		this.identifier = stmt != null ? stmt.getString() : res.getLocalName();
		stmt = res.getProperty(RDFS.label);
		this.label = stmt != null ? stmt.getString() : identifier;
	}
	
	/**
	 * Instantiates a new wiki entry.
	 *
	 * @param uri the uri
	 * @param type the type
	 * @param identifier the identifier
	 * @param label the label
	 * @throws NoSuchElementException the no such element exception
	 */
	public WikiEntry(String uri, @Nullable WikiEntry type, String identifier, String label) throws NoSuchElementException {
		this.uri = uri;
		this.type = Optional.ofNullable(type);
		this.identifier = identifier;
		this.label = label;
	}
	
	/**
	 * Res.
	 *
	 * @param model the model
	 * @return the resource
	 */
	public Resource res(@Nullable Model model) {
		return model != null ? model.createResource(uri) : ResourceFactory.createResource(uri);
	}

	/**
	 * Gets the uri.
	 *
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public Optional<WikiEntry> getType() {
		return type;
	}
	
	/**
	 * Gets the identifier.
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Sets the label.
	 *
	 * @param label the label
	 * @return the wiki entry
	 */
	public WikiEntry setLabel(String label) {
		this.label = label;
		return this;
	}
	
	/**
	 * Write to.
	 *
	 * @param model the model
	 * @return the resource
	 */
	public Resource writeTo(Model model) {
		Resource res = model.createResource(uri);
		if(type.isPresent()) res.addProperty(RDF.type, model.createResource(type.get().uri));
		res.addLiteral(DCTerms.identifier, lit(identifier));
		if(label != null) res.addLiteral(RDFS.label, lit(label));
		return res;
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return String.format("%s(%s)", label, identifier);
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(WikiEntry o) {
		return identifier.compareToIgnoreCase(o.identifier);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(uri);
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof WikiEntry))
			return false;
		WikiEntry other = (WikiEntry) obj;
		return Objects.equals(uri, other.uri);
	}
	
}
