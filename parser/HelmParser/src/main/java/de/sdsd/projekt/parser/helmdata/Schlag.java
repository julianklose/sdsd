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
	@CsvBindByName
	@HelmTransient
	private Long schlagId;
	@CsvBindByName
	@HelmIdReference(Teilbetrieb.class)
	@HelmTransient
	private Long teilBetriebsId;
	@CsvBindByName
	private Long erntejahr;
	@CsvBindByName
	@HelmLabel
	private String schlagbezeichnung;
	@CsvBindByName
	private Long schlagNr;
	@CsvBindByName
	private String satzNr;
	@CsvBindByName
	private Double ha;
	@CsvBindByName
	private String flik;
	@CsvBindByName
	@HelmTransient
	private Double wgs84n;
	@CsvBindByName
	@HelmTransient
	private Double wgs84e;
	@CsvBindByName
	private String kultur;
	@CsvBindByName
	private String sorte;
	@CsvBindByName
	private String vorfrucht;
	@CsvBindByName
	private String vorfruchtSorte;
	@CsvBindByName
	private String zwischenfrucht;
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date saatdatum;
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date oekoLkm;
	@CsvBindByName
	private String oekoAs;

	@Override
	public Long getId() {
		return schlagId;
	}

	@Override
	public String getGeoLabel() {
		return this.schlagbezeichnung;
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
		return "Schlag [schlagId=" + schlagId + ", teilBetriebsId=" + teilBetriebsId + ", erntejahr=" + erntejahr
				+ ", schlagbezeichnung=" + schlagbezeichnung + ", schlagNr=" + schlagNr + ", satzNr=" + satzNr + ", ha="
				+ ha + ", flik=" + flik + ", wgs84n=" + wgs84n + ", wgs84e=" + wgs84e + ", kultur=" + kultur
				+ ", sorte=" + sorte + ", vorfrucht=" + vorfrucht + ", vorfruchtSorte=" + vorfruchtSorte
				+ ", zwischenfrucht=" + zwischenfrucht + ", saatdatum=" + saatdatum + ", oekoLkm=" + oekoLkm
				+ ", oekoAs=" + oekoAs + "]";
	}
}