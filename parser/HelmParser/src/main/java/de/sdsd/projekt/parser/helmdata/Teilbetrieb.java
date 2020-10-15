package de.sdsd.projekt.parser.helmdata;

import com.opencsv.bean.CsvBindByName;

import de.sdsd.projekt.parser.annotations.HelmIdReference;
import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.interfaces.Geographic;
import de.sdsd.projekt.parser.interfaces.Identifiable;

/**
 * CSV mapping class for RAX entity "Teilbetrieb".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Teilbetrieb implements Identifiable, Geographic {
	
	/** The teil betriebs id. */
	@CsvBindByName
	@HelmTransient
	private Long teilBetriebsId;
	
	/** The betriebs id. */
	@CsvBindByName
	@HelmTransient
	@HelmIdReference(Betrieb.class)
	private Long betriebsId;
	
	/** The teilbetriebsname. */
	@CsvBindByName
	@HelmLabel
	private String teilbetriebsname;
	
	/** The strasse. */
	@CsvBindByName
	private String strasse;
	
	/** The plz. */
	@CsvBindByName
	private String plz;
	
	/** The ort. */
	@CsvBindByName
	private String ort;
	
	/** The telefon. */
	@CsvBindByName
	private String telefon;
	
	/** The eu betriebs nr. */
	@CsvBindByName
	private String euBetriebsNr;
	
	/** The kunden nr. */
	@CsvBindByName
	private String kundenNr;
	
	/** The wgs 84 n. */
	@CsvBindByName
	@HelmTransient
	private Double wgs84n;
	
	/** The wgs 84 e. */
	@CsvBindByName
	@HelmTransient
	private Double wgs84e;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return teilBetriebsId;
	}

	/**
	 * Gets the geo label.
	 *
	 * @return the geo label
	 */
	@Override
	public String getGeoLabel() {
		return this.teilbetriebsname;
	}

	/**
	 * Gets the longitude.
	 *
	 * @return the longitude
	 */
	@Override
	public Double getLongitude() {
		return this.wgs84e;
	}

	/**
	 * Gets the latitude.
	 *
	 * @return the latitude
	 */
	@Override
	public Double getLatitude() {
		return this.wgs84n;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Teilbetrieb [teilBetriebsId=" + teilBetriebsId + ", betriebsId=" + betriebsId + ", teilbetriebsname="
				+ teilbetriebsname + ", strasse=" + strasse + ", plz=" + plz + ", ort=" + ort + ", telefon=" + telefon
				+ ", euBetriebsNr=" + euBetriebsNr + ", kundenNr=" + kundenNr + ", wgs84n=" + wgs84n + ", wgs84e="
				+ wgs84e + "]";
	}
}