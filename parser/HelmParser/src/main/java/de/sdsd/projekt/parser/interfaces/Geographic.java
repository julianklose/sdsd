package de.sdsd.projekt.parser.interfaces;

/**
 * Interface to be implemented by CSV mapping classed containing a single GPS
 * coordinate (lon, lat) with a corresponding label attribute.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public interface Geographic {
	
	/**
	 * Gets the geo label.
	 *
	 * @return the geo label
	 */
	public String getGeoLabel();

	/**
	 * Gets the longitude.
	 *
	 * @return the longitude
	 */
	public Double getLongitude();

	/**
	 * Gets the latitude.
	 *
	 * @return the latitude
	 */
	public Double getLatitude();
}
