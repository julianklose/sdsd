package de.sdsd.projekt.parser.helmdata;

import com.opencsv.bean.CsvBindByName;

import de.sdsd.projekt.parser.annotations.HelmIdReference;
import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.interfaces.Identifiable;

/**
 * CSV mapping class for RAX entity "Artikel".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Artikel implements Identifiable {
	
	/** The artikel id. */
	@CsvBindByName
	@HelmTransient
	private Long artikelId;
	
	/** The artikel typ id. */
	@CsvBindByName
	@HelmIdReference(Artikeltyp.class)
	@HelmTransient
	private Long artikelTypId;
	
	/** The artikelname. */
	@CsvBindByName
	@HelmLabel
	private String artikelname;
	
	/** The einheit. */
	@CsvBindByName
	private String einheit;
	
	/** The zulassungs nr charge. */
	@CsvBindByName
	private String zulassungsNrCharge;
	
	/** The wirkstoffe beize. */
	@CsvBindByName
	private String wirkstoffeBeize;
	
	/** The n kg. */
	@CsvBindByName
	private Double nKg;
	
	/** The nh 4 kg. */
	@CsvBindByName
	private Double nh4Kg;
	
	/** The p 2 o 5 kg. */
	@CsvBindByName
	private Double p2o5Kg;
	
	/** The k 2 o kg. */
	@CsvBindByName
	private Double k2oKg;
	
	/** The mgo kg. */
	@CsvBindByName
	private Double mgoKg;
	
	/** The s kg. */
	@CsvBindByName
	private Double sKg;
	
	/** The bor kg. */
	@CsvBindByName
	private Double borKg;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return artikelId;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Artikel [artikelId=" + artikelId + ", artikelTypId=" + artikelTypId + ", artikelname=" + artikelname
				+ ", einheit=" + einheit + ", zulassungsNrCharge=" + zulassungsNrCharge + ", wirkstoffeBeize="
				+ wirkstoffeBeize + ", nKg=" + nKg + ", nh4Kg=" + nh4Kg + ", p2o5Kg=" + p2o5Kg + ", k2oKg=" + k2oKg
				+ ", mgoKg=" + mgoKg + ", sKg=" + sKg + ", borKg=" + borKg + "]";
	}

}