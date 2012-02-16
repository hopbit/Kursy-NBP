package info.siwinski.apps.nbpexchagerates;

import info.siwinski.apps.nbpexchagerates.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
					Log.e(this.getClass().getSimpleName(), e.toString());
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

		String html = getHtmlContentFromNbpSite();
		List<ExchangeRate> list = getExchangeRatesFromHtmlString(html);
		return list;

	}

	private List<ExchangeRate> getExchangeRatesFromHtmlString(String html) {

		List<String> ratesDict = new ArrayList<String>(
				Arrays.asList(getResources().getStringArray(
						R.array.currency_codes_array)));
		List<ExchangeRate> exchangeRates = new ArrayList<ExchangeRate>();

		// POBIERAM DATE DANYCH
		int datePosition = html.indexOf("</b> z dnia <b>");
		String dataDateStr = html.substring(datePosition + 15,
				datePosition + 25);
		Date dataDate = Date.valueOf(dataDateStr);

		String currentRate;

		// pobieram kursy i zapisuje je w liscie
		for (String s : ratesDict) {

			int ratePosition = html.indexOf(s);
			currentRate = new String(html.substring(ratePosition + 31,
					ratePosition + 37));

			Double value = null;
			try {
				if (s.equals("PLN")) {
					value = 1.0;
				} else {
					value = Double.parseDouble(currentRate.substring(0, 1)
							+ "."
							+ currentRate.substring(2, currentRate.length()));
				}
			} catch (NumberFormatException e) {
				Log.e(this.getClass().getName(),"ERROR on parsing : " + this.getClass().getName(), e);
			}

			ExchangeRate obj = new ExchangeRate();
			obj.setRateCode(s);

			String packageName = getPackageName();
			int resId = getResources().getIdentifier(s, "string", packageName);
			String rateName = getString(resId);
			obj.setRateName(rateName);

			obj.setRateValue(value);
			obj.setRateDate(dataDate);

			exchangeRates.add(obj);

		}

		if (exchangeRates.isEmpty()) {
			// setContentView(XXX);
			return null;
		} else {
			// setContentView(XXX);
			return exchangeRates;
		}

	}

	private String getHtmlContentFromNbpSite() {

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
				Log.e(this.getClass().getSimpleName().toString(), e.toString()); // TODO:
																					// handle
																					// exception
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			html = buffer.toString();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// do nothing?
		}

		return html;
	}

}
