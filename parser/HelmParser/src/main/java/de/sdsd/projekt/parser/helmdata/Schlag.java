package de.sdsd.projekt.parser.helmdata;

import java.util.Date;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import de.sdsd.projekt.parser.annotations.HelmIdReference;
import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.interfaces.Geographic;
import de.sdsd.projekt.parser.interfaces.Identifiable;

/**
 * CSV mapping class for RAX entity "Schlag".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Schlag implements Identifiable, Geographic {
	
	/** The schlag id. */
	@CsvBindByName
	@HelmTransient
	private Long schlagId;
	
	/** The teil betriebs id. */
	@CsvBindByName
	@HelmIdReference(Teilbetrieb.class)
	@HelmTransient
	private Long teilBetriebsId;
	
	/** The erntejahr. */
	@CsvBindByName
	private Long erntejahr;
	
	/** The schlagbezeichnung. */
	@CsvBindByName
	@HelmLabel
	private String schlagbezeichnung;
	
	/** The schlag nr. */
	@CsvBindByName
	private Long schlagNr;
	
	/** The satz nr. */
	@CsvBindByName
	private String satzNr;
	
	/** The ha. */
	@CsvBindByName
	private Double ha;
	
	/** The flik. */
	@CsvBindByName
	private String flik;
	
	/** The wgs 84 n. */
	@CsvBindByName
	@HelmTransient
	private Double wgs84n;
	
	/** The wgs 84 e. */
	@CsvBindByName
	@HelmTransient
	private Double wgs84e;
	
	/** The kultur. */
	@CsvBindByName
	private String kultur;
	
	/** The sorte. */
	@CsvBindByName
	private String sorte;
	
	/** The vorfrucht. */
	@CsvBindByName
	private String vorfrucht;
	
	/** The vorfrucht sorte. */
	@CsvBindByName
	private String vorfruchtSorte;
	
	/** The zwischenfrucht. */
	@CsvBindByName
	private String zwischenfrucht;
	
	/** The saatdatum. */
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date saatdatum;
	
	/** The oeko lkm. */
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date oekoLkm;
	
	/** The oeko as. */
	@CsvBindByName
	private String oekoAs;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return schlagId;
	}

	/**
	 * Gets the geo label.
	 *
	 * @return the geo label
	 */
	@Override
	public String getGeoLabel() {
		return this.schlagbezeichnung;
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
		return "Schlag [schlagId=" + schlagId + ", teilBetriebsId=" + teilBetriebsId + ", erntejahr=" + erntejahr
				+ ", schlagbezeichnung=" + schlagbezeichnung + ", schlagNr=" + schlagNr + ", satzNr=" + satzNr + ", ha="
				+ ha + ", flik=" + flik + ", wgs84n=" + wgs84n + ", wgs84e=" + wgs84e + ", kultur=" + kultur
				+ ", sorte=" + sorte + ", vorfrucht=" + vorfrucht + ", vorfruchtSorte=" + vorfruchtSorte
				+ ", zwischenfrucht=" + zwischenfrucht + ", saatdatum=" + saatdatum + ", oekoLkm=" + oekoLkm
				+ ", oekoAs=" + oekoAs + "]";
	}
}