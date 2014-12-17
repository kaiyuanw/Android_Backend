package com.connexus.yzhao.connexus;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SubscribedStreams extends ActionBarActivity {

    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private String TAG  = "SubscribedStreams";
    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribed_streams);
        String userName = getIntent().getStringExtra("userName");
        String REQUEST_ViewAllStreams = "http://connexusminiproject.appspot.com/andSubscribedStream/"+userName;
        httpClient.get(REQUEST_ViewAllStreams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                //responseText.setText(new String(response));
                //System.out.println(new String(response));

                final ArrayList<String> imageURLs = new ArrayList<String>();
                final ArrayList<String> streamIDs = new ArrayList<String>();
                final ArrayList<String> streamNames = new ArrayList<String>();
                try {
                    JSONObject jObject = new JSONObject(new String(response));
                    String user = jObject.getString("user");
                    JSONArray streamsDictArr = jObject.getJSONArray("streamDictsArr");

                    for(int i=0;i<streamsDictArr.length();i++) {
                        String streamsDict = streamsDictArr.getString(i);
                        JSONObject jObject2 = new JSONObject(streamsDict);
                        String coverURL = jObject2.getString("coverURL");
                        if (coverURL.equals("")){
                            coverURL = "http://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                        }
                        imageURLs.add(coverURL);
                        String streamIDNum = jObject2.getString("streamID");
                        streamIDs.add(streamIDNum);
                        String streamName = jObject2.getString("streamName");
                        streamNames.add(streamName);
                        //System.out.println(imageURLs.size());
                        //System.out.println(coverURL);
                    }
                    GridView gridview = (GridView) findViewById(R.id.gridview);
                    gridview.setAdapter(new ImageAdapter(context,imageURLs));
                    gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View v,
                                                       int position, long id) {
                            Toast.makeText(context, streamNames.get(position), Toast.LENGTH_SHORT).show();
                            return true;

                        }
                    });
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {

                            Intent intent= new Intent(SubscribedStreams.this, ViewSingleStream.class);
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
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.subscribed_streams, menu);
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
}
