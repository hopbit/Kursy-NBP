package info.siwinski.apps.nbpexchagerates;

import java.sql.Date;

/**
 * 
 * @author Łukasz Siwiński
 *
 */
public class ExchangeRate {

	private String rateCode;
	private String rateName;
	private Double rateValue;
	private Date rateDate;
	
	public Date getRateDate() {
		return rateDate;
	}
	public void setRateDate(Date rateDate) {
		this.rateDate = rateDate;
	}
	public String getRateName() {
		return rateName;
	}
	public void setRateName(String rateName) {
		this.rateName = rateName;
	}

	public Double getRateValue() {
		return rateValue;
	}
	public void setRateValue(Double rateValue) {
		this.rateValue = rateValue;
	}
	public String getRateCode() {
		return rateCode;
	}
	public void setRateCode(String rateCode) {
		this.rateCode = rateCode;
	}
}