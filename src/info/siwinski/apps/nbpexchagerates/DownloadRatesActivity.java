package info.siwinski.apps.nbpexchagerates;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author Łukasz Siwiński
 *
 */
public class DownloadRatesActivity extends Activity {

	private static final String ENCODING_UTF_8 = "UTF-8";
	private static final String DOWNLOAD_RATES_ACTIVITY = "DownloadRatesActivity";
	public final static String SQL_CHECK_IF_RATE_EXIST = "SELECT COUNT(*) AS CNT FROM CURR_RATES WHERE RATE_CODE=?";
	public final static String SQL_INSERT_RATE_ROW = "INSERT INTO CURR_RATES (RATE_CODE, RATE_NAME, RATE_VALUE, RATE_DATE) VALUES ({0},{1},{2},{3})";
	public final static String SQL_UPDATE_RATE_ROW = "UPDATE CURR_RATES SET RATE_VALUE={2}, RATE_DATE={3} WHERE RATE_CODE={0} {1}";

	List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
	String nbpSite;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.download_online_data);

		// String downloadInProgressMsg =
		// getResources().getString(R.string.download_in_progress);
		// ProgressDialog dialog =
		// ProgressDialog.show(DownloadRatesActivity.this,
		// "",downloadInProgressMsg, true);
		rates = getExchangeRatesFromWWW();
		boolean success = saveRatesOnDevice(rates);
		// dialog.dismiss();

		String conditionalRatesDownloadedMsg;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (success) {

			conditionalRatesDownloadedMsg = getResources().getString(
					R.string.ratesDowloadedOKMsg);

			builder.setMessage(conditionalRatesDownloadedMsg)
					.setCancelable(false)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									startActivity(new Intent(
											"info.siwinski.apps.nbpexchagerates.CALCULATE_FORM"));
									DownloadRatesActivity.this.finish();
								}
							});
		} else {

			conditionalRatesDownloadedMsg = getResources().getString(
					R.string.ratesDowloadedFailedMsg);
			String downloadMsg = getResources().getString(R.string.download);
			String cancelMsg = getResources().getString(R.string.cancel);

			builder.setMessage(conditionalRatesDownloadedMsg)
					.setCancelable(false)
					.setPositiveButton(downloadMsg,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									startActivity(new Intent(
											"info.siwinski.apps.nbpexchagerates.DOWNLOAD"));
									DownloadRatesActivity.this.finish();
								}
							})
					.setNegativeButton(cancelMsg,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									startActivity(new Intent(
											"info.siwinski.apps.nbpexchagerates.CALCULATE_FORM"));
									DownloadRatesActivity.this.finish();
								}
							});
		}
		AlertDialog alert = builder.create();
		alert.show();
	}

	private boolean saveRatesOnDevice(List<ExchangeRate> rates) {

		String dbName = getResources().getString(R.string.sqlDbName);
		SQLiteDatabase myDB = this.openOrCreateDatabase(dbName, MODE_PRIVATE,
				null);

		if (rates != null && !rates.isEmpty()) {

			for (ExchangeRate rate : rates) {
				try {
					boolean exist = false;
					String sqlCheck = SQL_CHECK_IF_RATE_EXIST;
					String[] selectionArgs = new String[1];
					selectionArgs[0] = rate.getRateCode();
					Cursor c = myDB.rawQuery(sqlCheck, selectionArgs);

					int colId = c.getColumnIndex("CNT");
					c.moveToFirst();
					if (c != null) {
						if (c.isFirst()) {
							do {
								exist = c.getInt(colId) > 0;
							} while (c.moveToNext());
						}
					}

					String sqlUpdate;
					if (exist) {
						sqlUpdate = SQL_UPDATE_RATE_ROW;
					} else {
						sqlUpdate = SQL_INSERT_RATE_ROW;
					}

					MessageFormat mf = new MessageFormat(sqlUpdate);

					String[] args = new String[4];
					args[0] = "'" + rate.getRateCode() + "'";
					if (!exist) {
						args[1] = "'" + rate.getRateName() + "'";
					} else {
						args[1] = "";
					}
					args[2] = rate.getRateValue().toString();
					args[3] = "" + rate.getRateDate().getTime();

					StringBuffer sqlFetchRows = new StringBuffer();
					sqlFetchRows.append(mf.format(args));

					myDB.execSQL(sqlFetchRows.toString());
					c.close();

				} catch (Exception e) {
					Log.e(DOWNLOAD_RATES_ACTIVITY, e.toString());
					return false;
				}
			}
		} else {
			Log.e("DownloadRatesActivity.saveOnDevice()",
					"ERROR List containing rates is null or empty");
			return false;
		}

		if (myDB != null)
			myDB.close();

		return true;
	}

	private List<ExchangeRate> getExchangeRatesFromWWW() {

		String xml = getXmlContentFromNbpSite();
		Document document = xmlFromString(xml);
		return documentToExchangeRatesList(document);
	}

	private String getXmlContentFromNbpSite() {
		
		String url = "http://nbp.pl/kursy/xml/LastA.xml";
		Log.d(DOWNLOAD_RATES_ACTIVITY, url.toString());
		String xml = null;
		try {
			
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			try {
				HttpResponse resp = client.execute(get);
				xml = EntityUtils.toString(resp.getEntity()); 
		        Log.d(DOWNLOAD_RATES_ACTIVITY, "UserData "+xml);
			} catch (Exception e) {
				Log.e(DOWNLOAD_RATES_ACTIVITY, "error on http get execute", e);
			}
		} catch (Exception e) {
			Log.e(DOWNLOAD_RATES_ACTIVITY, "error occured", e);
		}
		
		return xml;
	}

	private Document xmlFromString(String xml) {

		DocumentBuilderFactory factory;
		DocumentBuilder builder;
		InputStream is;
		Document dom = null;
	    try {
	        factory = DocumentBuilderFactory.newInstance();
	        is = new ByteArrayInputStream(xml.getBytes(ENCODING_UTF_8));
	        builder = factory.newDocumentBuilder();
	        dom = builder.parse(is);
	        Log.d(DOWNLOAD_RATES_ACTIVITY, dom.toString());
        }
	    catch(Exception e){
	    	Log.e(DOWNLOAD_RATES_ACTIVITY, "Error on parsing XML String",e);
	    }
		return dom;
	}

	private List<ExchangeRate> documentToExchangeRatesList(Document doc) {

		List<ExchangeRate> exchangeRates = new ArrayList<ExchangeRate>();
		ExchangeRate rate = null;

		// date
		NodeList dataPublikacjiNodes = doc.getElementsByTagName("data_publikacji");
		Element dataPublikacjiNode = (Element) dataPublikacjiNodes.item(0);
		String dateString = getNodeValue(dataPublikacjiNode);
		
		// rates
		NodeList pozycjaNodes = doc.getElementsByTagName("pozycja"); 
		for (int i = 0; i < pozycjaNodes.getLength(); i++) {
			
			Element pozycjaNode = (Element) pozycjaNodes.item(i);
			Log.d(DOWNLOAD_RATES_ACTIVITY, "> " + pozycjaNode.getLocalName());
			rate = getRate(pozycjaNode);
			rate.setRateDate(Date.valueOf(dateString));
			
			exchangeRates.add(rate);Log.d(DOWNLOAD_RATES_ACTIVITY, ">>>>> rate: \n"+rate);
		}
		
		rate = getPLNRate(dateString);
		exchangeRates.add(rate);

		return exchangeRates ;
	}

	private ExchangeRate getPLNRate(String dateString) {
		ExchangeRate rate;
		rate = new ExchangeRate();
		rate.setRateCode("PLN");
		rate.setRateDate(Date.valueOf(dateString));
		rate.setRateName("złoty polski");
		rate.setRateValue(Double.valueOf(1.0));
		return rate;
	}

	private String getNodeValue(Element dataPublikacjiVal) {
		return dataPublikacjiVal.getChildNodes().item(0).getNodeValue();
	}

	private ExchangeRate getRate(Element pozycjaNode) {
		ExchangeRate rate = new ExchangeRate();
		NodeList pozycjaChildNodes = pozycjaNode.getChildNodes();
		for (int i = 0; i < pozycjaChildNodes.getLength(); i++) {
			Node pozycjaChildNode = pozycjaChildNodes.item(i);
			Log.d(DOWNLOAD_RATES_ACTIVITY, ">> pozycjaChildNode.nodeType: "+ pozycjaChildNode.getNodeType());
			if(Node.ELEMENT_NODE == pozycjaChildNode.getNodeType()) {
				
				Element pozycjaChildElement = (Element) pozycjaChildNode;
				String nodeName = pozycjaChildElement.getNodeName();
				Log.d(DOWNLOAD_RATES_ACTIVITY, ">>> pozycjaChildElement.nodeName: " + pozycjaChildElement.getNodeName());
				String nodeValue = getNodeValue(pozycjaChildElement); Log.d(DOWNLOAD_RATES_ACTIVITY, ">>>> pozycjaChildElement.nodeValue: "+ pozycjaChildElement.getNodeValue());
				
				if("nazwa_waluty".equals(nodeName)){
					rate.setRateName(nodeValue);
				} else if ("kod_waluty".equals(nodeName)) {
					rate.setRateCode(nodeValue);
				} else if ("kurs_sredni".equals(nodeName)) {
					NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
					Number number = null;
					try {
						number = formatter.parse(nodeValue);
					} catch (ParseException e) {
						e.printStackTrace();
					}
			    	Double doubleRateValue = Double.valueOf(number.doubleValue());
					rate.setRateValue(doubleRateValue);
				}
			}
			
		}
		return rate;
	}


}
