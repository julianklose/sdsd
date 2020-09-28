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
	private Double longitude;
	private Double latitude;
	private Resource reference;
	private String label;

	public HelmCoordinate() {
	}

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

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Resource getReference() {
		return reference;
	}

	public void setReference(Resource reference) {
		this.reference = reference;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return "HelmCoordinate [longitude=" + longitude + ", latitude=" + latitude + ", reference=" + reference
				+ ", label=" + label + "]";
	}
}