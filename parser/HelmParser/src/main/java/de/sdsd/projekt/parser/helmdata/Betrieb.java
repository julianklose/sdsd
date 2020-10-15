package de.sdsd.projekt.parser.helmdata;

import com.opencsv.bean.CsvBindByName;

import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.interfaces.Identifiable;

/**
 * CSV mapping class for RAX entity "Betrieb".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Betrieb implements Identifiable {
	
	/** The betriebs id. */
	@CsvBindByName
	@HelmTransient
	private Long betriebsId;
	
	/** The nachname. */
	@CsvBindByName
	@HelmLabel
	private String nachname;
	
	/** The vorname. */
	@CsvBindByName
	private String vorname;
	
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
	
	/** The fax. */
	@CsvBindByName
	private String fax;
	
	/** The mobil. */
	@CsvBindByName
	private String mobil;
	
	/** The email. */
	@CsvBindByName
	private String email;
	
	/** The eu betriebsnummer. */
	@CsvBindByName
	private String euBetriebsnummer;
	
	/** The oeko verband. */
	@CsvBindByName
	private String oekoVerband;
	
	/** The oeko verband nr. */
	@CsvBindByName
	private String oekoVerbandNr;
	
	/** The kontrollstelle. */
	@CsvBindByName
	private String kontrollstelle;
	
	/** The eu kontroll nr. */
	@CsvBindByName
	private String euKontrollNr;
	
	/** The ezg. */
	@CsvBindByName
	private String ezg;
	
	/** The ezg zert nr. */
	@CsvBindByName
	private String ezgZertNr;
	
	/** The farmbox id. */
	@CsvBindByName
	private String farmboxId;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return betriebsId;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Betrieb [betriebsId=" + betriebsId + ", nachname=" + nachname + ", vorname=" + vorname + ", strasse="
				+ strasse + ", plz=" + plz + ", ort=" + ort + ", telefon=" + telefon + ", fax=" + fax + ", mobil="
				+ mobil + ", email=" + email + ", euBetriebsnummer=" + euBetriebsnummer + ", oekoVerband=" + oekoVerband
				+ ", oekoVerbandNr=" + oekoVerbandNr + ", kontrollstelle=" + kontrollstelle + ", euKontrollNr="
				+ euKontrollNr + ", ezg=" + ezg + ", ezgZertNr=" + ezgZertNr + ", farmboxId=" + farmboxId + "]";
	}
}