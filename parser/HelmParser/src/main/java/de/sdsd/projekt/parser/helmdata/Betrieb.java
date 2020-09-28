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
	@CsvBindByName
	@HelmTransient
	private Long betriebsId;
	@CsvBindByName
	@HelmLabel
	private String nachname;
	@CsvBindByName
	private String vorname;
	@CsvBindByName
	private String strasse;
	@CsvBindByName
	private String plz;
	@CsvBindByName
	private String ort;
	@CsvBindByName
	private String telefon;
	@CsvBindByName
	private String fax;
	@CsvBindByName
	private String mobil;
	@CsvBindByName
	private String email;
	@CsvBindByName
	private String euBetriebsnummer;
	@CsvBindByName
	private String oekoVerband;
	@CsvBindByName
	private String oekoVerbandNr;
	@CsvBindByName
	private String kontrollstelle;
	@CsvBindByName
	private String euKontrollNr;
	@CsvBindByName
	private String ezg;
	@CsvBindByName
	private String ezgZertNr;
	@CsvBindByName
	private String farmboxId;

	@Override
	public Long getId() {
		return betriebsId;
	}

	@Override
	public String toString() {
		return "Betrieb [betriebsId=" + betriebsId + ", nachname=" + nachname + ", vorname=" + vorname + ", strasse="
				+ strasse + ", plz=" + plz + ", ort=" + ort + ", telefon=" + telefon + ", fax=" + fax + ", mobil="
				+ mobil + ", email=" + email + ", euBetriebsnummer=" + euBetriebsnummer + ", oekoVerband=" + oekoVerband
				+ ", oekoVerbandNr=" + oekoVerbandNr + ", kontrollstelle=" + kontrollstelle + ", euKontrollNr="
				+ euKontrollNr + ", ezg=" + ezg + ", ezgZertNr=" + ezgZertNr + ", farmboxId=" + farmboxId + "]";
	}
}