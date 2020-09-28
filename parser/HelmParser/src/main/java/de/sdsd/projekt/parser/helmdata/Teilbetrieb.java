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
	@CsvBindByName
	@HelmTransient
	private Long teilBetriebsId;
	@CsvBindByName
	@HelmTransient
	@HelmIdReference(Betrieb.class)
	private Long betriebsId;
	@CsvBindByName
	@HelmLabel
	private String teilbetriebsname;
	@CsvBindByName
	private String strasse;
	@CsvBindByName
	private String plz;
	@CsvBindByName
	private String ort;
	@CsvBindByName
	private String telefon;
	@CsvBindByName
	private String euBetriebsNr;
	@CsvBindByName
	private String kundenNr;
	@CsvBindByName
	@HelmTransient
	private Double wgs84n;
	@CsvBindByName
	@HelmTransient
	private Double wgs84e;

	@Override
	public Long getId() {
		return teilBetriebsId;
	}

	@Override
	public String getGeoLabel() {
		return this.teilbetriebsname;
	}

	@Override
	public Double getLongitude() {
		return this.wgs84e;
	}

	@Override
	public Double getLatitude() {
		return this.wgs84n;
	}

	@Override
	public String toString() {
		return "Teilbetrieb [teilBetriebsId=" + teilBetriebsId + ", betriebsId=" + betriebsId + ", teilbetriebsname="
				+ teilbetriebsname + ", strasse=" + strasse + ", plz=" + plz + ", ort=" + ort + ", telefon=" + telefon
				+ ", euBetriebsNr=" + euBetriebsNr + ", kundenNr=" + kundenNr + ", wgs84n=" + wgs84n + ", wgs84e="
				+ wgs84e + "]";
	}
}