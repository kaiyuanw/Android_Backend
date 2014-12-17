package com.connexus.yzhao.connexus;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.DialogFragment;
import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.content.IntentSender;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import android.location.Location;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import android.widget.Button;

public class NearbyPhotos extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private String TAG  = "NearbyPhotos";
    public static String indexes;
    Context context = this;
    // Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationClient mLocationClient;
    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    /*
                     * Try the request again
                     */
                        break;
                }
        }
    }
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            System.out.println("CONNECTION FAILED");
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
               // errorFragment.show(getSupportFragmentManager(),"Location Updates");
            }
            return false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_photos);
        if (servicesConnected()){
            System.out.println("servicesConnected");
            mLocationClient = new LocationClient(this, this, this);
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nearby_photos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        //Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        indexes = getIntent().getStringExtra("indexes");
        String location=mLocationClient.getLastLocation().getLatitude()+"_"+mLocationClient.getLastLocation().getLongitude();
        final String request_url = "http://connexusminiproject.appspot.com/andViewNearbyPhotos/"+indexes+"/"+location;
        final Button more_nearby_photos = (Button)findViewById(R.id.more_nearby_photos);

        httpClient.get(request_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                final ArrayList<String> imageURLs = new ArrayList<String>();
                final ArrayList<String> streamIDs = new ArrayList<String>();
                final ArrayList<String> streamNames = new ArrayList<String>();
                final ArrayList<String> strDistances = new ArrayList<String>();
                final String morePhotos;
                try {
                    JSONObject jObject = new JSONObject(new String(response));
                    JSONArray allPhotos = jObject.getJSONArray("allPhotos");
                    morePhotos = jObject.getString("morePhotos");
                    if (morePhotos.equals("True")){
                        more_nearby_photos.setVisibility(View.VISIBLE);
                    }
                    indexes = jObject.getString("indexURL");
                    for(int i=0;i<allPhotos.length();i++) {
                        String photoInfo = allPhotos.getString(i);
                        JSONObject jObject2 = new JSONObject(photoInfo);
                        String strDistance = jObject2.getString("strDistance");
                        strDistances.add(strDistance);
                        String photoServingURL = jObject2.getString("photoServingURL");
                        imageURLs.add(photoServingURL);
                        String streamIDNum = jObject2.getString("streamID");
                        streamIDs.add(streamIDNum);
                        String streamName = jObject2.getString("streamName");
                        streamNames.add(streamName);
                    }
                    GridView gridview = (GridView) findViewById(R.id.gridview);
                    gridview.setAdapter(new ImageAdapter(context,imageURLs));
                    gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View v,
                                                int position, long id) {
                            Toast.makeText(context, strDistances.get(position), Toast.LENGTH_SHORT).show();
                            return true;

                        }
                    });
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {
                            Intent intent= new Intent(NearbyPhotos.this, ViewSingleStream.class);
                            intent.putExtra("position",position);
                            intent.putExtra("streamName",streamNames.get(position));
                            intent.putExtra("streamID",streamIDs.get(position));
                            startActivity(intent);

                        }
                    });

                }
                catch(JSONException j){
                    System.out.println("JSON Error");
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG,"There was a problem in retrieving the url : " + e.toString());
            }
        });



    }
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            System.out.println(connectionResult.getErrorCode());
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }



    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();

    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }
    // Global variable to hold the current location
    //Location mCurrentLocation;
    //mCurrentLocation = mLocationClient.getLastLocation();
    public void viewAllStreams(View view){
        Intent intent = new Intent(this, ViewAllStreams.class);
        startActivity(intent);
    }
    public void moreNearbyPhotos(View view){
        Intent intent = new Intent(this, NearbyPhotos.class);
        intent.putExtra("indexes",indexes);
        startActivity(intent);
    }
}
