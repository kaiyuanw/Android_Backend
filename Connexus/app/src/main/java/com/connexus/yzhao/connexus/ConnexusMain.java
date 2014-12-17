package com.connexus.yzhao.connexus;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class ConnexusMain extends ActionBarActivity {
    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connexus_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connexus_main, menu);
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
    public void login(View view){

        final TextView gmail_id = (TextView) this.findViewById(R.id.gmail_id);
        Intent intent = new Intent(this, ViewAllStreams.class);
        String userNameText = gmail_id.getText().toString();
        intent.putExtra("userName", userNameText.split("@")[0]);
        if (userNameText != null && !userNameText.isEmpty()) {
            Toast.makeText(context, "Logged In", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(context, "Not Logged In", Toast.LENGTH_SHORT).show();
        }
        startActivity(intent);
    }
    public void viewAllStreams(View view){
        Intent intent = new Intent(this, ViewAllStreams.class);
        startActivity(intent);
    }
}
