package com.example.smartcity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.support.v7.app.ActionBarActivity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.*;
import android.widget.*;



public class MainActivity extends ActionBarActivity {
	
	// Private members of the class
    private TextView lat;
    private TextView lng;
    private TextView photo_path;
    private EditText editDate;
    private EditText address;
    private EditText description;
    private Spinner problem_type;
    private int pYear;
    private int pMonth;
    private int pDay;
    // This integer will uniquely define the dialog to be used for displaying date picker.
    static final int DATE_DIALOG_ID = 0;    
	private static final int SELECT_PICTURE = 1;

     
    // Create a new dialog for date picker
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG_ID:
            return new DatePickerDialog(this, pDateSetListener, pYear, pMonth, pDay);
        }
        return null;
    }	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// load more threads
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
			.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

        // Capture our View elements
        editDate = (EditText) findViewById(R.id.editText4);
        lat = (TextView) findViewById(R.id.textView6);
        lng = (TextView) findViewById(R.id.textView5);
        photo_path = (TextView) findViewById(R.id.textView7);
        address = (EditText) findViewById(R.id.editText2);
        description = (EditText) findViewById(R.id.editText3);
        problem_type = (Spinner) findViewById(R.id.spinner1);
 
        // Get the current date
        final Calendar cal = Calendar.getInstance();
        pYear = cal.get(Calendar.YEAR);
        pMonth = cal.get(Calendar.MONTH);
        pDay = cal.get(Calendar.DAY_OF_MONTH);
 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
		return super.onOptionsItemSelected(item);
	}
	
	// open the datepicker
	@SuppressWarnings("deprecation")
	public void showDatePicker(View button) {
		showDialog(DATE_DIALOG_ID);
	}
	
    // Callback received when the user "picks" a date in the dialog
    private DatePickerDialog.OnDateSetListener pDateSetListener = new DatePickerDialog.OnDateSetListener() {
    	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    		pYear = year;
    		pMonth = monthOfYear;
    		pDay = dayOfMonth;
    		updateDisplay();
    		displayToast();
    	}
    };
     
    // Updates the date in the TextView
    private void updateDisplay() {
    	editDate.setText(
    			new StringBuilder()
    			// Month is 0 based so add 1
    			.append(pMonth + 1).append("/")
    			.append(pDay).append("/")
    			.append(pYear).append(" "));
    }
     
    // Displays a notification when the date is updated
    private void displayToast() {
        Toast.makeText(this, new StringBuilder().append("Date choosen is ").append(editDate.getText()),  Toast.LENGTH_SHORT).show();
             
    }
	
	// get current location from gps
	public void currentLocation(View button) {
		// check if GPS enabled
		GPSTracker gpsTracker = new GPSTracker(this);
		if (gpsTracker.canGetLocation())
		{
			// get the values
			String stringLatitude = String.valueOf(gpsTracker.latitude);
			String stringLongitude = String.valueOf(gpsTracker.longitude);
			// set values to view
			lat.setText(stringLatitude);
			lng.setText(stringLongitude);
		}
		else
		{
			// can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
			gpsTracker.showSettingsAlert();
		}
	}
	
	// add photo path
	public void addPhotoAndPath(View button) {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
	}
	
	public void checkForm(View button) {

		// get address and description fields
		String currentaddress = address.getText().toString();
		String currentlat = lat.getText().toString();
		String currentlng = lng.getText().toString();
		String currentdescription = description.getText().toString();
		// translate item to vale of spinner
		String problem = getResources().getStringArray(R.array.spinner_values)[(problem_type).getSelectedItemPosition()];
		// get date
		String selectedDate = editDate.getText().toString();

		boolean readyForm = true;
		String message = "";
	    if(currentdescription == null || currentdescription.length() <= 0){
	    	message += "\nΤο πεδίο 'Συντομη Περιγραφή' είναι άδειο.";
	    	readyForm = false;
	    }
	    if(problem == null || problem.length() <= 0){
	    	message += "\nΤο πεδίο 'Τύπος προβλήματος' είναι άδειο.";
	    	readyForm = false;
	    }
	    if(selectedDate == null || selectedDate.length() <= 0){
	    	message += "\nΤο πεδίο 'Ημερομηνία παρατήρησης' είναι άδειο.";
	    	readyForm = false;
	    }
	    if((currentaddress == null || currentaddress.length() <= 0) && (currentlat == null || currentlat.length() <= 0) && (currentlng == null || currentlng.length() <= 0)){
	    	message += "\nΤο πεδίο 'Διεύθηνση ή τα πεδία lat long' είναι άδειa.";
	    	readyForm = false;
	    }
	    
	    if(!readyForm){
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			// set title
			alertDialogBuilder.setTitle("Error");
			// set dialog message
			alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {}
				});
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
				// show it
				alertDialog.show();
			
	    }else{
			makePostRequest();
	    }

	}	
	
	// make the post request
	private void makePostRequest() {
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost("http://150.140.15.50/sdy51/2014/senddata.php");

		// Post Data
		List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
		nameValuePair.add(new BasicNameValuePair("date", editDate.getText().toString()));
		nameValuePair.add(new BasicNameValuePair("problem_type", getResources().getStringArray(R.array.spinner_values)[(problem_type).getSelectedItemPosition()]));
		nameValuePair.add(new BasicNameValuePair("lat", lat.getText().toString()));
		nameValuePair.add(new BasicNameValuePair("lng", lng.getText().toString()));
		nameValuePair.add(new BasicNameValuePair("address", address.getText().toString()));
		nameValuePair.add(new BasicNameValuePair("description", description.getText().toString()));
		nameValuePair.add(new BasicNameValuePair("image_path", photo_path.getText().toString()));

		//Encoding POST data
		try {
		      httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
		 
		} catch (UnsupportedEncodingException e)  {
		     e.printStackTrace();
		}
		 
		try {
		    HttpResponse response = httpClient.execute(httpPost);
		    // write response to log
		    String jsonString = EntityUtils.toString(response.getEntity());
		    Log.d("Http Post Response:", jsonString);

		    if ( jsonString.contains("200") ) {
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				// set title
				alertDialogBuilder.setTitle("Congratulation!");
				// set dialog message
				alertDialogBuilder
					.setMessage("Your form has been sent")
					.setCancelable(false)
					.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {}
					});
					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();
					// show it
					alertDialog.show();
		    }else if(jsonString.contains("400")){
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
				// set title
				alertDialogBuilder.setTitle("Sending Failure!");
				// set dialog message
				alertDialogBuilder
					.setMessage("Something went wrong please try again later.")
					.setCancelable(false)
					.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {}
					});
					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();
					// show it
					alertDialog.show();
		    }
		} catch (ClientProtocolException e) {
		    // Log exception
		    e.printStackTrace();
		} catch (IOException e) {
		    // Log exception
		    e.printStackTrace();
		}		
	}
	

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
	    if (resultCode == RESULT_OK) {
	        if (requestCode == SELECT_PICTURE) {
	            Uri selectedImageUri = data.getData();
	            String realPath = getRealPathFromURI_API19(this, selectedImageUri);
	            photo_path.setText(realPath);
	        }
	    }
	}

	public static String getRealPathFromURI_API19(Context context, Uri uri){
		String filePath = "";
		String wholeID = DocumentsContract.getDocumentId(uri);

	     // Split at colon, use second item in the array
	     String id = wholeID.split(":")[1];

	     String[] column = { MediaStore.Images.Media.DATA };     

	     // where id is equal to             
	     String sel = MediaStore.Images.Media._ID + "=?";

	     Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
	                               column, sel, new String[]{ id }, null);
	     
	     int columnIndex = cursor.getColumnIndex(column[0]);

	     if (cursor.moveToFirst()) {
	         filePath = cursor.getString(columnIndex);
	     }   

	     cursor.close();
	     
	     return filePath;
	}

}

