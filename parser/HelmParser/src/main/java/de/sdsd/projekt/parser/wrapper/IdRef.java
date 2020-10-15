package de.sdsd.projekt.parser.wrapper;

import org.apache.jena.rdf.model.Resource;

/**
 * Wrapper class for referencing ID attributes associated with their
 * corresponding, randomly generated UriResource identifier
 * {@code referncingResource}.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class IdRef {
	
	/** The id. */
	private Id id;
	
	/** The referencing resource. */
	private Resource referencingResource;

	/**
	 * Instantiates a new id ref.
	 *
	 * @param id the id
	 * @param referencingResource the referencing resource
	 */
	public IdRef(Id id, Resource referencingResource) {
		this.id = id;
		this.referencingResource = referencingResource;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Id getId() {
		return id;
	}

	/**
	 * Gets the referencing resource.
	 *
	 * @return the referencing resource
	 */
	public Resource getReferencingResource() {
		return referencingResource;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "IdRef [id=" + id + ", referencingResource=" + referencingResource + "]";
	}
}