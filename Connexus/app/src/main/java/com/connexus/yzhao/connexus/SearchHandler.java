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
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SearchHandler extends ActionBarActivity {
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private String TAG  = "SearchHandler";
    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_handler);

        final String searchContent = getIntent().getStringExtra("search_content");
        String searchContentUrl = searchContent.replaceAll(" ", "_");
        String requestUrl = "http://connexusminiproject.appspot.com/andSearchHandler/"+searchContentUrl;
        final TextView responseText = (TextView) this.findViewById(R.id.search_result_message);
        //responseText.setText(new String(streamName));

        httpClient.get(requestUrl, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                //responseText.setText(new String(response));
                //System.out.println(new String(response));

                final ArrayList<String> imageURLs = new ArrayList<String>();
                final ArrayList<String> streamIDs = new ArrayList<String>();
                final ArrayList<String> streamNames = new ArrayList<String>();
                try {
                    JSONObject jObject = new JSONObject(new String(response));
                    JSONArray streamsFoundCoverURL = jObject.getJSONArray("streamsFoundCoverURL");
                    JSONArray streamsFoundNames = jObject.getJSONArray("streamsFoundNames");
                    JSONArray streamsFoundIDs = jObject.getJSONArray("streamsFoundIDs");
                    for(int i=0;i<streamsFoundCoverURL.length();i++) {
                        String coverURL = streamsFoundCoverURL.getString(i);
                        if (coverURL.equals("")){
                            coverURL = "http://upload.wikimedia.org/wikipedia/en/0/0d/Null.png";
                        }
                        imageURLs.add(coverURL);
                        String streamIDNum = streamsFoundIDs.getString(i);
                        streamIDs.add(streamIDNum);
                        String streamName = streamsFoundNames.getString(i);
                        streamNames.add(streamName);
                        //System.out.println(imageURLs.size());
                        //System.out.println(coverURL);
                    }
                    GridView gridview = (GridView) findViewById(R.id.gridview);
                    gridview.setAdapter(new ImageAdapter(context,imageURLs));
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View v,
                                                int position, long id) {

                            Intent intent= new Intent(SearchHandler.this, ViewSingleStream.class);
                            intent.putExtra("position",position);
                            intent.putExtra("numOfMatches",streamNames.size());
                            intent.putExtra("streamName",streamNames.get(position));
                            intent.putExtra("streamID",streamIDs.get(position));
                            startActivity(intent);

                        }
                    });
                    String searchResultsMessage = streamNames.size()+" results for "+ searchContent+"\n"+"click on an image to view stream";
                    responseText.setText(new String(searchResultsMessage));
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
        getMenuInflater().inflate(R.menu.search_handler, menu);
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
}
