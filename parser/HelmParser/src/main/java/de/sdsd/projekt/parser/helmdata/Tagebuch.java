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
	@CsvBindByName
	@HelmIdReference(Schlag.class)
	@HelmTransient
	private Long schlagId;
	@CsvBindByName
	private Long buchNr;
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date datum;
	@CsvBindByName
	private Long ec;
	@CsvBindByName
	@HelmIdReference(Artikel.class)
	@HelmTransient
	private Long artikelId;
	@CsvBindByName
	private Double mengeHa;
	@CsvBindByName
	private String wartezeit;
	@CsvBindByName
	private Double bearbFlaecheHa;
	@CsvBindByName
	private String indikationBemerkung;

	@Override
	public String toString() {
		return "Tagebuch [schlagId=" + schlagId + ", buchNr=" + buchNr + ", datum=" + datum + ", ec=" + ec
				+ ", artikelId=" + artikelId + ", mengeHa=" + mengeHa + ", wartezeit=" + wartezeit + ", bearbFlaecheHa="
				+ bearbFlaecheHa + ", indikationBemerkung=" + indikationBemerkung + "]";
	}
}