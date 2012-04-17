package info.siwinski.apps.nbpexchagerates;

import java.sql.Date;

/**
 * 
 * @author Lukasz Siwinski
 * @param <T>
 *
 */
public final class ExchangeRate implements Comparable<ExchangeRate>{

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
	public int compareTo(ExchangeRate another) {
		String thisCode = getRateCode();
		String anotherCode = another.getRateCode();
		return thisCode.compareTo(anotherCode);
	}
}