package de.sdsd.projekt.parser.wrapper;

import java.util.Arrays;

import org.apache.jena.rdf.model.Resource;
import org.json.JSONObject;

/**
 * Wrapper class for a GPS coordinate which references an entry of a Jena RDF
 * model.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class HelmCoordinate {
	
	/** The longitude. */
	private Double longitude;
	
	/** The latitude. */
	private Double latitude;
	
	/** The reference. */
	private Resource reference;
	
	/** The label. */
	private String label;

	/**
	 * Instantiates a new helm coordinate.
	 */
	public HelmCoordinate() {
	}

	/**
	 * Instantiates a new helm coordinate.
	 *
	 * @param longitude the longitude
	 * @param latitude the latitude
	 * @param reference the reference
	 * @param label the label
	 */
	public HelmCoordinate(Double longitude, Double latitude, Resource reference, String label) {
		this.longitude = longitude;
		this.latitude = latitude;
		this.reference = reference;
		this.label = label;
	}

	/**
	 * Converts the GPS coordinates of this instance into valid Geo-JSON.
	 * 
	 * @return Geo-JSON representation of the GPS coordinates.
	 */
	public JSONObject toGeoJson() {
		JSONObject point = new JSONObject();
		point.put("type", "Point");
		point.put("coordinates", Arrays.asList(this.longitude, this.latitude));

		JSONObject feature = new JSONObject();
		feature.put("type", "Feature");
		feature.put("properties", new JSONObject());
		feature.put("geometry", point);

		return feature;
	}

	/**
	 * Gets the longitude.
	 *
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * Sets the longitude.
	 *
	 * @param longitude the new longitude
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Gets the latitude.
	 *
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * Sets the latitude.
	 *
	 * @param latitude the new latitude
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Gets the reference.
	 *
	 * @return the reference
	 */
	public Resource getReference() {
		return reference;
	}

	/**
	 * Sets the reference.
	 *
	 * @param reference the new reference
	 */
	public void setReference(Resource reference) {
		this.reference = reference;
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
	 * @param label the new label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "HelmCoordinate [longitude=" + longitude + ", latitude=" + latitude + ", reference=" + reference
				+ ", label=" + label + "]";
	}
}