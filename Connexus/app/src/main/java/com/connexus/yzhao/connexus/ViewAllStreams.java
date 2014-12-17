package com.connexus.yzhao.connexus;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class ViewAllStreams extends ActionBarActivity {
    public static String REQUEST_ViewAllStreams = "http://connexusminiproject.appspot.com/andViewAllStreams";
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private String TAG  = "ViewAllStreams";
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_streams);

        //final TextView responseText = (TextView) this.findViewById(R.id.response_text);
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
                    gridview.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {

                            Intent intent= new Intent(ViewAllStreams.this, ViewSingleStream.class);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_all_streams, menu);
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

    public void searchHandler(View view){
        Intent intent = new Intent(this,SearchHandler.class);
        EditText text = (EditText)findViewById(R.id.search_content);
        String returnText = text.getText().toString();

        intent.putExtra("search_content",returnText);
        startActivity(intent);
    }

    public void viewNearbyPhotos(View view){
        Intent intent = new Intent(this,NearbyPhotos.class);
        intent.putExtra("indexes","0_15");
        startActivity(intent);
    }
    public void viewSubscribedStreams(View view){
        Intent intent = new Intent(this,SubscribedStreams.class);
        String userName = getIntent().getStringExtra("userName");
        intent.putExtra("userName",userName);
        startActivity(intent);
    }
}
