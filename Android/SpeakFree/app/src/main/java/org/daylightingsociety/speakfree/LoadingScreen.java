package org.daylightingsociety.speakfree;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LoadingScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_screen);

        /*
        // Show the loading screen briefly before going to main activity
        try
        {
            Thread.sleep(2500);
            System.out.println("Waiting.");
        }
        catch(InterruptedException e) { }
        */

        startActivity(new Intent(LoadingScreen.this, MainActivity.class));
        finish();
    }
}
