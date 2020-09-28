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
	@CsvBindByName
	@HelmTransient
	private Long artikelId;
	@CsvBindByName
	@HelmIdReference(Artikeltyp.class)
	@HelmTransient
	private Long artikelTypId;
	@CsvBindByName
	@HelmLabel
	private String artikelname;
	@CsvBindByName
	private String einheit;
	@CsvBindByName
	private String zulassungsNrCharge;
	@CsvBindByName
	private String wirkstoffeBeize;
	@CsvBindByName
	private Double nKg;
	@CsvBindByName
	private Double nh4Kg;
	@CsvBindByName
	private Double p2o5Kg;
	@CsvBindByName
	private Double k2oKg;
	@CsvBindByName
	private Double mgoKg;
	@CsvBindByName
	private Double sKg;
	@CsvBindByName
	private Double borKg;

	@Override
	public Long getId() {
		return artikelId;
	}

	@Override
	public String toString() {
		return "Artikel [artikelId=" + artikelId + ", artikelTypId=" + artikelTypId + ", artikelname=" + artikelname
				+ ", einheit=" + einheit + ", zulassungsNrCharge=" + zulassungsNrCharge + ", wirkstoffeBeize="
				+ wirkstoffeBeize + ", nKg=" + nKg + ", nh4Kg=" + nh4Kg + ", p2o5Kg=" + p2o5Kg + ", k2oKg=" + k2oKg
				+ ", mgoKg=" + mgoKg + ", sKg=" + sKg + ", borKg=" + borKg + "]";
	}

}