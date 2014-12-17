package com.connexus.yzhao.connexus;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ViewSingleStream extends ActionBarActivity {
    private String TAG  = "ViewSingleStream";
    Context context = this;
    public static String REQUEST_ViewSingleStream = "http://connexusminiproject.appspot.com/andViewSingleStream";
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_single_stream);


        String streamName = getIntent().getStringExtra("streamName");
        String streamID = getIntent().getStringExtra("streamID");
        TextView responseText = (TextView) this.findViewById(R.id.stream_name);
        responseText.setText(new String(streamName));

        final String request_url = REQUEST_ViewSingleStream +"/"+streamID+"/0_15";

        httpClient.get(request_url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                final ArrayList<String> imageURLs = new ArrayList<String>();
                try {
                    JSONObject jObject = new JSONObject(new String(response));
                    String user = jObject.getString("user");
                    JSONArray displayImages = jObject.getJSONArray("displayImages");

                    for(int i=0;i<displayImages.length();i++) {

                        imageURLs.add(displayImages.getString(i));

                        //System.out.println(imageURLs.size());
                        System.out.println(displayImages.getString(i));
                    }
                    GridView gridview = (GridView) findViewById(R.id.gridview);
                    gridview.setAdapter(new ImageAdapter(context,imageURLs));
//                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                        @Override
//                        public void onItemClick(AdapterView<?> parent, View v,
//                                                int position, long id) {
//
//                            Intent intent= new Intent(ViewAllStreams.this, ViewSingleStream.class);
//                            intent.putExtra("position",position);
//                            intent.putExtra("streamName",streamNames.get(position));
//                            intent.putExtra("streamID",streamNames.get(position));
//                            startActivity(intent);
//
//                        }
//                    });
                }
                catch(JSONException j){
                    System.out.println("JSON Error");
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_single_stream, menu);
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

    public void viewAllStreams(View view){
        Intent intent = new Intent(this, ViewAllStreams.class);
        startActivity(intent);
    }
    public void uploadImage(View view){
        Intent intent = new Intent(this,upload.class);
        String streamName = getIntent().getStringExtra("streamName");
        String streamID = getIntent().getStringExtra("streamID");
        intent.putExtra("streamName",streamName);
        intent.putExtra("streamID",streamID);
        startActivity(intent);
    }

}
