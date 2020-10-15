package de.sdsd.projekt.parser.helmdata;

import java.util.Date;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import de.sdsd.projekt.parser.annotations.HelmIdReference;
import de.sdsd.projekt.parser.annotations.HelmTransient;

/**
 * CSV mapping class for RAX entity "Tagebuch".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Tagebuch {
	
	/** The schlag id. */
	@CsvBindByName
	@HelmIdReference(Schlag.class)
	@HelmTransient
	private Long schlagId;
	
	/** The buch nr. */
	@CsvBindByName
	private Long buchNr;
	
	/** The datum. */
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date datum;
	
	/** The ec. */
	@CsvBindByName
	private Long ec;
	
	/** The artikel id. */
	@CsvBindByName
	@HelmIdReference(Artikel.class)
	@HelmTransient
	private Long artikelId;
	
	/** The menge ha. */
	@CsvBindByName
	private Double mengeHa;
	
	/** The wartezeit. */
	@CsvBindByName
	private String wartezeit;
	
	/** The bearb flaeche ha. */
	@CsvBindByName
	private Double bearbFlaecheHa;
	
	/** The indikation bemerkung. */
	@CsvBindByName
	private String indikationBemerkung;

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Tagebuch [schlagId=" + schlagId + ", buchNr=" + buchNr + ", datum=" + datum + ", ec=" + ec
				+ ", artikelId=" + artikelId + ", mengeHa=" + mengeHa + ", wartezeit=" + wartezeit + ", bearbFlaecheHa="
				+ bearbFlaecheHa + ", indikationBemerkung=" + indikationBemerkung + "]";
	}
}