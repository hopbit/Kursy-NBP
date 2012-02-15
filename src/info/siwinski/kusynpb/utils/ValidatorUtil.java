package info.siwinski.kusynpb.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Łukasz Siwiński
 *
 */
public class ValidatorUtil {
	
	private List<String> errors;
	private String doublePattern = "\\d+\\.{0,1}\\d+";
	
	public ValidatorUtil() {
		errors = new ArrayList<String>();
	}
	
	public String getErrorMessage() {
		
		if(errors.size()>0) {
			
			StringBuffer buffer = new StringBuffer(""); 
			
			for(Iterator<String> it = errors.iterator(); it.hasNext();) {
				String s = (String) it.next();
				buffer.append(s);
				if(it.hasNext()) {
					buffer.append("\n");
				}
			}

			return buffer.toString();
			
		} else {
			return null;
		}
	}
	
	public boolean hasErrors() {
		return errors.size()>0;
	}
 
	public void validateInputRateValue(String inputRateValueString) {
		
		if(inputRateValueString==null || "".equals(inputRateValueString)) {
			errors.add("Kwota do przeliczenia nie moze byc pusta.");
		} else if (!inputRateValueString.matches(doublePattern)) {
			errors.add("Niewlasciwy format kwoty. Podaj liczbe.");
		}
	}

	public void validateInputRateFrom(String inputRateFromCode) {
		// TODO Auto-generated method stub
	}



	public void validateInputRateTo(String inputRateFromCode) {
		// TODO
	}

}
