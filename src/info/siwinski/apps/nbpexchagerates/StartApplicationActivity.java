package info.siwinski.apps.nbpexchagerates;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * CONFIGURATION @ strings.xml
 * @author Lukasz Siwinski
 */
public class StartApplicationActivity extends Activity {
	
	String nbpSiteSourceCode = "";
	List<ExchangeRate> exchangeRates;
	private TextView currencyDateText;
	public String valToConvertTxt = "";
	private EditText givenAmountTxt;
	private Spinner rateFromSpinner;
	private Spinner rateToSpinner;
	private TextView resultView;
	String nbpSite;
	
	String choosenRateFrom;
	String choosenRateTo;
	
	public static final String RATE_PLN = "PLN";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
    
    	initializeDB();
    	
    	if (checkDataAvailabilityOnDevice()) {
    		
    		setContentView(R.layout.calculate_form);
    		
    		exchangeRates = getRatesFromDevice();
			
    		currencyDateText = (TextView) findViewById(R.id.currencyDateText);
    		Date currDeviceDate = exchangeRates.get(0).getRateDate();
    		String text = currencyDateText.getText().toString();
    		if(currDeviceDate!=null) {
    			text += " " + currDeviceDate.toString();
    		}
    		currencyDateText.setText(text);
    		
			givenAmountTxt = (EditText) findViewById(R.id.pushValueEditText);
			resultView = (TextView) findViewById(R.id.result);
			
			String[] rateCodesArray = getRateCodeArray(exchangeRates);
			ArrayAdapter<String> rateCodesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,rateCodesArray);
		    rateCodesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
		    rateFromSpinner = (Spinner) findViewById(R.id.rate_from_spinner);
		    rateFromSpinner.setAdapter(rateCodesAdapter);
		    rateFromSpinner.setPrompt(getResources().getString(R.string.rate_from_prompt));
		    rateFromSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

				public void onItemSelected(AdapterView<?> arg0,
						View arg1, int arg2, long arg3) {
					choosenRateFrom = arg0.getSelectedItem().toString();
					
				}

				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
				}
			});
		    
		    rateToSpinner = (Spinner) findViewById(R.id.rate_to_spinner);
		    rateToSpinner.setAdapter(rateCodesAdapter);
		    rateToSpinner.setPrompt(getResources().getString(R.string.rate_to_prompt));
		    rateToSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

				public void onItemSelected(AdapterView<?> arg0,
						View arg1, int arg2, long arg3) {
					choosenRateTo = arg0.getSelectedItem().toString();
					
				}

				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});

		    Button calculateButton = (Button) findViewById(R.id.calculateButton);
			
			calculateButton.setOnClickListener(new Button.OnClickListener(){
				
				public void onClick(View v) {
					
					Double result = 0.0;
					Double inputRateValue = null;
					ValidatorUtil validator = new ValidatorUtil();
					
					String inputRateValueString = givenAmountTxt.getText().toString();
					validator.validateInputRateValue(inputRateValueString);

					
					String inputRateFromCode = rateFromSpinner.getSelectedItem().toString();
					validator.validateInputRateFrom(inputRateFromCode);
					
					String inputRateToCode = rateToSpinner.getSelectedItem().toString();
					validator.validateInputRateTo(inputRateToCode);
					
					if(validator.hasErrors()) {
						String errorMsg = validator.getErrorMessage();
						Toast.makeText(StartApplicationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
					} else {
					
						try {
							inputRateValue = new Double (inputRateValueString);
						} catch (NumberFormatException e) {
							Toast.makeText(StartApplicationActivity.this, "Podales niepoprawna kwote", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
						
						result = calculateRateMagicBox(inputRateValue, inputRateFromCode,inputRateToCode);
						
						NumberFormat myFormatter = NumberFormat.getInstance(Locale.getDefault());
						myFormatter.setMinimumFractionDigits(2);
						myFormatter.setMaximumFractionDigits(2);
						String output = myFormatter.format(result) + " " + inputRateToCode.substring(0, 3);
						
						resultView.setText(output);
					
					}
				}
			});
    		
    		try {
				if(newRatesAvailable()) {
				
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setMessage("Pojawily sie nowe kursy walut na stronie NBP. Czy chcesz je pobrac?")
							.setCancelable(false)
							.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									
									setContentView(R.layout.download_online_data);
					     		
									Button downloadButton = (Button) findViewById(R.id.downloadButton);
									downloadButton.setOnClickListener(new Button.OnClickListener(){
										public void onClick(View v) {
											startActivity(new Intent("info.siwinski.apps.nbpexchagerates.DOWNLOAD"));
										}
					        	   });
					           }
					       }).setNegativeButton("Nie", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.dismiss();
								}
						});
					builder.create().show();
				} 
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} else {
    		
    		setContentView(R.layout.download_online_data);
    		
    		Button downloadButton = (Button) findViewById(R.id.downloadButton);
    		downloadButton.setOnClickListener(new Button.OnClickListener(){
				public void onClick(View v) {
					startActivity(new Intent("info.siwinski.apps.nbpexchagerates.DOWNLOAD"));
				}
			});
			
    	}

    }
    

	private Double calculateRateMagicBox(Double inputValue,
			String codeFromOrg, String codeToOrg) {

		String codeFrom = codeFromOrg.substring(0,3);
		String codeTo = codeToOrg.substring(0,3);
		
		Double rateValue;
		if(!RATE_PLN.equals(codeFrom)) {
			rateValue = getRateValueByRateCode(codeFrom);
		} else  {
			rateValue = getRateValueByRateCode(codeTo);
		}
		
		
		if(codeFrom.equals(codeTo)) {
			return Double.valueOf(inputValue);
		} else if (RATE_PLN.equals(codeFrom) && !RATE_PLN.equals(codeTo)) { // WZOR 1
			
			return Double.valueOf(inputValue/rateValue);

		} else if (!RATE_PLN.equals(codeFrom) && RATE_PLN.equals(codeTo)) { // WZOR 2
		
			return Double.valueOf(inputValue*rateValue);
		
		} else if (!RATE_PLN.equals(codeFrom) && !RATE_PLN.equals(codeTo)) { // WZÓR 3
		
			rateValue = getRateValueByRateCode(codeFrom); // Krok 1 - pobieram aktualny kurs dla codeFrom
			Double ilePln = (inputValue*rateValue); // Krok 2 - obliczam ile PLN dostane za inputValue waluty codeFrom
			rateValue = getRateValueByRateCode(codeTo); // Krok 3 - pobieram kurs waluty codeTo 
			return ilePln/rateValue; // Krok 4 - ile waluty codeTo dostane za ilePln pln
	
		}
		
		return null;
	}

	private Double getRateValueByRateCode(String codeTo) {
		SQLiteDatabase myDB = null;
		String[] queryArgs = new String[] {codeTo};
		Double rateValue = null;
		try {

			myDB = this.openOrCreateDatabase(getResources().getString(R.string.sqlDbName),MODE_PRIVATE, null);
			Cursor c = myDB.rawQuery(getResources().getString(R.string.sqlSelectRateValueByCode),queryArgs);
			int valueCol = c.getColumnIndex(getResources().getString(R.string.sqlColumnRatesValue));

			c.moveToFirst();
			
			if (c != null) {
			
				if (c.isFirst()) {
				
					do {
						rateValue = c.getDouble(valueCol);
					} while (c.moveToNext());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (myDB != null)
				myDB.close();
		}
		return rateValue;
	}

	private boolean newRatesAvailable() throws Exception {
		
		Date deviceDate = getDeviceRatesDate();
		if(deviceDate==null) {
			return true;
		}
		
		Date nbpDate = getNbpRatesDate();
		
		if(deviceDate!=null && nbpDate!=null) {
			return nbpDate.getTime() > deviceDate.getTime();
		}
		
		return false;
	}

	private Date getNbpRatesDate() {
		String html = getNbpHtmlContent();
		return getCurrNbpDateFromHtml(html);
	}

	private Date getCurrNbpDateFromHtml(String html) {

		String dateString;
		Date date;

		// POBIERAM DATE DANYCH
		int datePosition = html.indexOf("</b> z dnia <b>");
		dateString = html.substring(datePosition+15,datePosition+25);
		
		date = Date.valueOf(dateString);
		
		return date;
		
	}

	private String getNbpHtmlContent() {
		
		String html = new String();
		
		try {
			nbpSite = getResources().getString(R.string.nbpSite);
    		URL url = new URL(nbpSite);
    		InputStream inputStream = url.openStream();
    		StringBuffer buffer = new StringBuffer();
    		try {
    			int ch;
    			while ((ch = inputStream.read()) != -1) {
    				buffer.append((char) ch);
    			}
    		} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			html = buffer.toString();
			
    	} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return html;
	}

	private Date getDeviceRatesDate() throws Exception {
		SQLiteDatabase myDB = null;
		Date result = null;
		try {
			
			String dbName = getResources().getString(R.string.sqlDbName);
			myDB = this.openOrCreateDatabase(dbName,MODE_PRIVATE, null);

			String query = getResources().getString(R.string.sqlGetRatesDate);
			Cursor c = myDB.rawQuery(query,null);

			String dateCol = getResources().getString(R.string.sqlColumnRatesDate);
			int colCnt = c.getColumnIndex(dateCol);

			c.moveToFirst();
			
			if (c != null) {
			
				if (c.isFirst()) {
				
					do {
						long x = c.getLong(colCnt);
						result = new Date(x);
					} while (false); // because I wan't only first row
				}
			}
			

		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.toString());
		} finally {
			if (myDB != null)
				myDB.close();
		}
		return result;
	}

	private String[] getRateCodeArray(List<ExchangeRate> exchangeRates) {
		Collections.sort(exchangeRates);
		String[] array = new String[exchangeRates.size()];
		int i= 0;
		for(ExchangeRate r : exchangeRates) {
			NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
			String rateValueStr = formatter.format(r.getRateValue());
			array[i++]=r.getRateCode() + " " + r.getRateName() + " " + rateValueStr;
		}
		return array;
	}

	private List<ExchangeRate> getRatesFromDevice() {
		
		SQLiteDatabase myDB = null;
		List<ExchangeRate> list = new ArrayList<ExchangeRate>();
		try {

			myDB = this.openOrCreateDatabase(getResources().getString(R.string.sqlDbName),MODE_PRIVATE, null);
			Cursor c = myDB.rawQuery(getResources().getString(R.string.sqlSelectRateRows),null);

			int codeCol = c.getColumnIndex(getResources().getString(R.string.sqlColumnRatesCode));
			int valueCol = c.getColumnIndex(getResources().getString(R.string.sqlColumnRatesValue));
			int dateCol = c.getColumnIndex("RATE_DATE");
			int nameCol = c.getColumnIndex("RATE_NAME");

			c.moveToFirst();
			
			if (c != null) {
			
				if (c.isFirst()) {
				
					do {
						ExchangeRate obj = new ExchangeRate();
						obj.setRateCode(c.getString(codeCol));
						obj.setRateValue(c.getDouble(valueCol));
						obj.setRateDate(new Date(c.getLong(dateCol)));
						obj.setRateName(c.getString(nameCol));
						list.add(obj);
					} while (c.moveToNext());
				}
			}
			

		} catch (Exception e) {
			Log.e("StartApplicationActivity.initializeDB()", "Error on intializing data base.");
			return null;
		} finally {
			if (myDB != null)
				myDB.close();
		}
		return list;
	}

	private boolean checkDataAvailabilityOnDevice() {
		SQLiteDatabase myDB = null;
		int result = 0;
		try {
			
			String dbName =  getResources().getString(R.string.sqlDbName);
			myDB = this.openOrCreateDatabase(dbName,MODE_PRIVATE, null);

			String query = getResources().getString(R.string.sqlCountRateRows);
			Cursor c = myDB.rawQuery(query,null);

			int colCnt = c.getColumnIndex("CNT");

			c.moveToFirst();
			
			if (c != null) {
			
				if (c.isFirst()) {
				
					do {
						result = c.getInt(colCnt);
					} while (c.moveToNext());
				}
			}
			
			c.close();

		} catch (Exception e) {
			Log.e("StartApplicationActivity.initializeDB()", "Error on intializing data base.\n");
			Log.e("ERROR",e.toString());
			return false;
		} finally {
			if (myDB != null)
				myDB.close();
		}
		return result > 0;
	}

	private void initializeDB() {
		SQLiteDatabase myDB = null;
		try {
			/* Create the Database (no Errors if it already exists) */ 
			myDB = this.openOrCreateDatabase(getResources().getString(R.string.sqlDbName),MODE_PRIVATE, null);
			/* Create a Table in the Database. */ 
			myDB.execSQL(getResources().getString(R.string.sqlCreateTable));
		} catch (Exception e) {
			Log.e("StartApplicationActivity.initializeDB()", "Error on intializing data base.");
		} finally {
			if (myDB != null)
				myDB.close();
		}
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
}