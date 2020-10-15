package de.sdsd.projekt.parser.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Wrapper class for a (loosely connected) Jena RDF model, together with all the
 * referencing ID attributes and GPS coordinates.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class RdfModel {
	
	/** The model. */
	private Model model;
	
	/** The refs. */
	private List<IdRef> refs;
	
	/** The coords. */
	private List<HelmCoordinate> coords;

	/**
	 * Instantiates a new rdf model.
	 */
	public RdfModel() {
		this.model = ModelFactory.createDefaultModel();
		this.refs = new ArrayList<IdRef>();
		this.coords = new ArrayList<HelmCoordinate>();
	}

	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		return this.model == null || this.model.isEmpty();
	}

	/**
	 * Adds the part of reference.
	 *
	 * @param ref the ref
	 */
	public void addPartOfReference(IdRef ref) {
		this.refs.add(ref);
	}

	/**
	 * Adds the coordinate.
	 *
	 * @param coord the coord
	 */
	public void addCoordinate(HelmCoordinate coord) {
		this.coords.add(coord);
	}

	/**
	 * Gets the model.
	 *
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Sets the model.
	 *
	 * @param model the new model
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	/**
	 * Gets the refs.
	 *
	 * @return the refs
	 */
	public List<IdRef> getRefs() {
		return refs;
	}

	/**
	 * Sets the refs.
	 *
	 * @param refs the new refs
	 */
	public void setRefs(List<IdRef> refs) {
		this.refs = refs;
	}

	/**
	 * Gets the coords.
	 *
	 * @return the coords
	 */
	public List<HelmCoordinate> getCoords() {
		return coords;
	}

	/**
	 * Sets the coords.
	 *
	 * @param coords the new coords
	 */
	public void setCoords(List<HelmCoordinate> coords) {
		this.coords = coords;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "RdfModel [model=" + model + ", refs=" + refs + ", coords=" + coords + "]";
	}
}