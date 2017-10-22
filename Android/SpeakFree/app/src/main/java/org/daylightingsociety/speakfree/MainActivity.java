package org.daylightingsociety.speakfree;

import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private boolean isplaying;
    private final Random rfrequency = new Random();
    private final Random bonus = new Random();
    private final Thread t = new Thread() {
        public void run() {
            int sr = 44100;
            setPriority(Thread.MAX_PRIORITY);
            int buffsize = AudioTrack.getMinBufferSize(sr,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffsize, AudioTrack.MODE_STREAM);
            short samples[] = new short[buffsize];
            int amp = 10000;
            double twopi = 8.0*Math.atan(1.0);
            double fr;
            double ph = 0.0;
            audioTrack.play();

            // randomization loop
            while(isplaying)
            {
                // fill the buffer
                for(int i = 0; i < buffsize; i++)
                {
                    samples[i] = (short) (amp*Math.sin(ph));
                    double tempph = 0.0;
                    for(int j = 0; j < 10; j++)
                    {
                        fr = (double)((rfrequency.nextInt(3400-300)+1)+300);
                        short prev = (short) (amp*Math.sin(tempph));
                        short curr = (short) (amp*Math.sin(ph + twopi*fr/sr));
                        if(Math.abs(curr) > Math.abs(prev) )
                        {
                            tempph = ph + twopi*fr/sr;
                        }
                    }
                    //ph += twopi*fr/sr;
                    ph = tempph;
                }
                // randomly swap a wave in the buffer with a new one
                int selection = bonus.nextInt(samples.length);
                fr = (double)((rfrequency.nextInt(3400-300)+1)+300);
                samples[selection] = (short) (amp*Math.sin(ph+twopi*fr/sr));
                audioTrack.write(samples, 0, buffsize);
            }
            audioTrack.stop();
            audioTrack.release();
        }
    };
    private String[] tips;
    private int tips_index = 0;
    static final int MIN_DISTANCE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Thread tip_thread;
        Random rgenerator = new Random();
        isplaying = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Resources res = getResources();

        tips = res.getStringArray(R.array.tips);

        tips_index = rgenerator.nextInt(tips.length);

        final Animation in = new AlphaAnimation(0.0f,1.0f);
        in.setDuration(1000);

        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(1000);


        tip_thread = new Thread() {
            public void run() {
                //noinspection InfiniteLoopStatement
                while(true)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                            tips_index += 1;
                            tips_index %= tips.length;
                            final TextView tv = (TextView) findViewById(R.id.main_tip);
                            out.setAnimationListener(new Animation.AnimationListener()
                            {
                                // when fade out ends, change the tip and fade in
                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    tv.setText(tips[tips_index]);
                                    tv.startAnimation(in);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}

                                @Override
                                public void onAnimationStart(Animation animation){}
                            });
                            tv.startAnimation(out);
                        }
                    });
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        tip_thread.start();


        ImageButton startButton = (ImageButton) findViewById(R.id.imageButton_playButton);
        startButton.setOnClickListener(new View.OnClickListener()
                {
                public void onClick(View v) {
                    if (isplaying)
                    {
                        isplaying = false;
                        ImageButton playButton = (ImageButton) findViewById(R.id.imageButton_playButton);
                        playButton.setBackgroundResource(R.drawable.play);
                        ImageView statusImage =(ImageView) findViewById(R.id.status_image);
                        statusImage.setImageResource(R.drawable.inactive);
                    }
                    else
                    {
                        isplaying = true;
                        ImageButton playButton = (ImageButton) findViewById(R.id.imageButton_playButton);
                        playButton.setBackgroundResource(R.drawable.pause);
                        ImageView statusImage =(ImageView) findViewById(R.id.status_image);
                        statusImage.setImageResource(R.drawable.active);
                        t.start();
                    }
                }
            }
        );


        ImageButton helpButton = (ImageButton) findViewById(R.id.imageButton_help);
        helpButton.setOnClickListener(new View.OnClickListener()
                                      {
                                      public void onClick(View v)
                                      {
                                          startActivity(new Intent(MainActivity.this, Help.class));
                                      }
                                  }
        );


        ImageButton infoButton = (ImageButton) findViewById(R.id.imageButton_info);
        infoButton.setOnClickListener(new View.OnClickListener()
                                  {
                                      public void onClick(View v)
                                      {
                                          startActivity(new Intent(MainActivity.this, Info.class));
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
                float deltax = x2-x1;
                if(Math.abs(deltax) > MIN_DISTANCE)
                {
                    if (x2 > x1) // left to right swipe
                    {
                        startActivity(new Intent(MainActivity.this, Info.class));
                    }
                    if (x1 > x2) // right to left swipe
                    {
                        startActivity(new Intent(MainActivity.this, Help.class));
                    }
                    break;
                }
        }
        return super.onTouchEvent(event);
    }



}
