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
	public String getGeoLabel();

	public Double getLongitude();

	public Double getLatitude();
}
