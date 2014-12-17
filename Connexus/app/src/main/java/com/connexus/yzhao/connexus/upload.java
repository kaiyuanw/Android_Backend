package com.connexus.yzhao.connexus;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.provider.MediaStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import android.graphics.Bitmap;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import java.io.ByteArrayOutputStream;
import android.util.Base64;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.RequestParams;

public class upload extends ActionBarActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationClient mLocationClient;
    Context context = this;
    private static final int PICK_IMAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        if (servicesConnected()) {
            System.out.println("servicesConnected");
            mLocationClient = new LocationClient(this, this, this);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.upload, menu);
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

    public void createCameraPreview(View view){
        if (checkCameraHardware(this)==true){
            Intent intent = new Intent(this,CameraActivity.class);
            startActivityForResult(intent, 2);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_IMAGE && data != null && data.getData() != null) {
            Uri _uri = data.getData();

            //User had pick an image.
            Cursor cursor = getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
            cursor.moveToFirst();

            //Link to the image
            final String imageFilePath = cursor.getString(0);
            cursor.close();
            System.out.println(imageFilePath);
            ImageView imgView = (ImageView)findViewById(R.id.thumbnail);
            final Bitmap bitmapImage = BitmapFactory.decodeFile(imageFilePath);

            imgView.setImageBitmap(bitmapImage);
            final Button uploadButton = (Button)findViewById(R.id.upload_to_server);
            uploadButton.setClickable(true);

            uploadButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText text = (EditText)findViewById(R.id.upload_message);
                            String photoCaption = text.getText().toString();
                            //System.out.println(photoCaption);

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                            byte[] b = baos.toByteArray();
                            byte[] encodedImage = Base64.encode(b, Base64.DEFAULT);
                            String encodedImageStr = encodedImage.toString();
                            System.out.println(encodedImageStr);

                            String location=mLocationClient.getLastLocation().getLatitude()+"_"+mLocationClient.getLastLocation().getLongitude();

                            getUploadURL(b, photoCaption, location);
                        }
                    }
            );
        }
        else if (requestCode == 2){

           if (resultCode!=0) {
                final String imageFilePath = data.getStringExtra("imageFile");
                System.out.println(imageFilePath);
                ImageView imgView = (ImageView) findViewById(R.id.thumbnail);

                Bitmap bitmapImage = BitmapFactory.decodeFile(imageFilePath);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                final Bitmap rotated = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), matrix, true);

                imgView.setImageBitmap(rotated);




                final Button uploadButton = (Button) findViewById(R.id.upload_to_server);
                uploadButton.setClickable(true);
                uploadButton.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText text = (EditText) findViewById(R.id.upload_message);
                                String photoCaption = text.getText().toString();
                                //System.out.println(photoCaption);

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                rotated.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                                byte[] b = baos.toByteArray();
                                byte[] encodedImage = Base64.encode(b, Base64.DEFAULT);

                                String encodedImageStr = encodedImage.toString();
                                System.out.println(encodedImageStr);

                                String location=mLocationClient.getLastLocation().getLatitude()+"_"+mLocationClient.getLastLocation().getLongitude();


                                getUploadURL(b, photoCaption, location);
                            }
                        }
                );
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getUploadURL(final byte[] encodedImage, final String photoCaption, final String location){
        //System.out.println(location);
        AsyncHttpClient httpClient = new AsyncHttpClient();
        String streamID = getIntent().getStringExtra("streamID");
        String request_url="http://connexusminiproject.appspot.com/andGetUploadURL/"+streamID;
        System.out.println(request_url);
        httpClient.get(request_url, new AsyncHttpResponseHandler() {
            String upload_url;
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {

                try {
                    JSONObject jObject = new JSONObject(new String(response));

                    upload_url = jObject.getString("upload_url");
                    postToServer(encodedImage, photoCaption, location, upload_url);

                }
                catch(JSONException j){
                    System.out.println("JSON Error");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e("Get_serving_url","There was a problem in retrieving the url : " + e.toString());
            }
        });
    }
    private void postToServer(byte[] encodedImage,String photoCaption,String location,String upload_url){
        System.out.println(upload_url);
        RequestParams params = new RequestParams();
        //params.put("encodedImage",encodedImage);
        params.put("file",new ByteArrayInputStream(encodedImage));
        System.out.println(new ByteArrayInputStream(encodedImage));
        params.put("photoCaption",photoCaption);
        params.put("location",location);
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(upload_url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.w("async", "success!!!!");
                Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show();
                Intent intent= new Intent(upload.this, ViewSingleStream.class);
                String streamName = getIntent().getStringExtra("streamName");
                String streamID = getIntent().getStringExtra("streamID");
                intent.putExtra("streamName",streamName);
                intent.putExtra("streamID",streamID);
                startActivity(intent);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e("Posting_to_blob","There was a problem in retrieving the url : " + e.toString());
            }
        });
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
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

            return false;
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        //String location=mLocationClient.getLastLocation().getLatitude()+"_"+mLocationClient.getLastLocation().getLongitude();
        //System.out.println(location);

        String streamName = getIntent().getStringExtra("streamName");
        String streamID = getIntent().getStringExtra("streamID");
        TextView responseText = (TextView) this.findViewById(R.id.stream_name_upload);
        responseText.setText(streamName);

        Button chooseFromLibraryButton = (Button) findViewById(R.id.choose_from_library);
        chooseFromLibraryButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                    }
                }
        );

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
}
