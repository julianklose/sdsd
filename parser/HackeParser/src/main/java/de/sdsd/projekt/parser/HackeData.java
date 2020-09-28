package de.sdsd.projekt.parser;

import com.opencsv.bean.CsvBindByName;

/**
 * Bean for representing a line of data from CSV HackeData format. OpenCSV
 * annotations are used. OpenCSV is included by Maven.
 * 
 * @author ngs
 *
 */
public class HackeData {
	/**
	 * Because fields in data format are written in CAPSLOCK CsvBindByName(column=)
	 * is used. All values can be null. ATTENTION: Getter gives back Double.NaN for
	 * null values for Mocot and Dicot for use in MainParser!
	 */
	@CsvBindByName
	private Long time;
	@CsvBindByName(column = "LON")
	private Double lon;
	@CsvBindByName(column = "LAT")
	private Double lat;
	@CsvBindByName(column = "ALT")
	private Double alt;
	@CsvBindByName(column = "SECTION")
	private Integer section;
	@CsvBindByName(column = "LON_HEAD")
	private Double lon_head;
	@CsvBindByName(column = "LAT_HEAD")
	private Double lat_head;
	@CsvBindByName(column = "STATUS")
	private Integer status;
	@CsvBindByName(column = "RESULTID")
	private Integer resultId;
	@CsvBindByName(column = "SYSTIME")
	private Long systime;
	@CsvBindByName(column = "BEAVP")
	private Double beavp;
	@CsvBindByName(column = "BRSNN")
	private Double brsnn;
	@CsvBindByName(column = "DICOT")
	private Double dicot;
	@CsvBindByName(column = "GALAP")
	private Double galap;
	@CsvBindByName(column = "MATIN")
	private Double matin;
	@CsvBindByName(column = "MOCOT")
	private Double mocot;
	@CsvBindByName(column = "TRZAW")
	private Double trzaw;
	@CsvBindByName(column = "ZEAMX")
	private Double zeamx;

	@Override
	public String toString() {
		return "HackeData [time=" + time + ", lon=" + lon + ", lat=" + lat + ", alt=" + alt + ", section=" + section
				+ ", lon_head=" + lon_head + ", lat_head=" + lat_head + ", status=" + status + ", resultId=" + resultId
				+ ", systime=" + systime + ", beavp=" + beavp + ", brsnn=" + brsnn + ", dicot=" + dicot + ", galap="
				+ galap + ", matin=" + matin + ", mocot=" + mocot + ", trzaw=" + trzaw + ", zeamx=" + zeamx + "]";
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getAlt() {
		return alt;
	}

	public void setAlt(Double alt) {
		this.alt = alt;
	}

	public Integer getSection() {
		return section;
	}

	public void setSection(Integer section) {
		this.section = section;
	}

	public Double getLon_head() {
		return lon_head;
	}

	public void setLon_head(Double lon_head) {
		this.lon_head = lon_head;
	}

	public Double getLat_head() {
		return lat_head;
	}

	public void setLat_head(Double lat_head) {
		this.lat_head = lat_head;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getResultId() {
		return resultId;
	}

	public void setResultId(Integer resultId) {
		this.resultId = resultId;
	}

	public Long getSystime() {
		return systime;
	}

	public void setSystime(Long systime) {
		this.systime = systime;
	}

	public Double getBeavp() {
		return beavp;
	}

	public void setBeavp(Double beavp) {
		this.beavp = beavp;
	}

	public Double getBrsnn() {
		return brsnn;
	}

	public void setBrsnn(Double brsnn) {
		this.brsnn = brsnn;
	}

	public Double getDicot() {
		return dicot;
	}

	public void setDicot(Double dicot) {
		this.dicot = dicot;
	}

	public Double getGalap() {
		return galap;
	}

	public void setGalap(Double galap) {
		this.galap = galap;
	}

	public Double getMatin() {
		return matin;
	}

	public void setMatin(Double matin) {
		this.matin = matin;
	}

	public Double getMocot() {
		return mocot;
	}

	public void setMocot(Double mocot) {
		this.mocot = mocot;
	}

	public Double getTrzaw() {
		return trzaw;
	}

	public void setTrzaw(Double trzaw) {
		this.trzaw = trzaw;
	}

	public Double getZeamx() {
		return zeamx;
	}

	public void setZeamx(Double zeamx) {
		this.zeamx = zeamx;
	}

}
