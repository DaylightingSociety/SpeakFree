package org.daylightingsociety.speakfree;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class Info extends AppCompatActivity {

    static final int MIN_DISTANCE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // create imagebutton object + set its onclicklistener
        ImageButton backButton = (ImageButton) findViewById(R.id.imageButton_info_return);
        backButton.setOnClickListener(new View.OnClickListener()
                                      {
                                          public void onClick(View v)
                                          { // switch back to main
                                              //startActivity(new Intent(Info.this, MainActivity.class));
                                              finish();
                                          }
                                      }
        );
    }

    private float x1;
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x2;
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                if(x2 < x1) // left to right swipe
                {
                    float deltax = x1-x2;
                    if(deltax > MIN_DISTANCE)
                    {
                        finish();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

}
