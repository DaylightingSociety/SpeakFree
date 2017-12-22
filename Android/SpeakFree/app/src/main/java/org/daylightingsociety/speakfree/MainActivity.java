package org.daylightingsociety.speakfree;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.sql.Types.NULL;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_INTERNET = 0;
    public static final String TAG = "MainActivity";
    private View mLayout;
    private String m_Text = "";

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

        ImageButton newTipButton = (ImageButton) findViewById(R.id.imageButton_newTip);
        newTipButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // First, check for permissions
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.INTERNET);

                if(permissionCheck != PackageManager.PERMISSION_GRANTED)
                { // If we don't have permission, then
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.INTERNET))
                    { // If we need to explain why we want the permission
                        Snackbar.make(mLayout, R.string.permission_internet_rationale, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.ok, new View.OnClickListener()
                            {
                                @Override
                                 public void onClick(View view)
                                {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.INTERNET},
                                            REQUEST_INTERNET);
                                }
                            }).show();
                    }
                    else
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.INTERNET},
                                REQUEST_INTERNET);
                    }
                }
                else
                { // If we have permission...
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Suggest a new tip");

                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_Text = input.getText().toString();

                            AsyncTask.execute(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    HttpsURLConnection conn = null;
                                    try {
                                        URL tipAddress = new URL("https://speakfree.daylightingsociety.org/submitTip");
                                        conn = (HttpsURLConnection) tipAddress.openConnection();
                                        conn.setReadTimeout(10000);
                                        conn.setConnectTimeout(15000);
                                        conn.setRequestMethod("POST");
                                        conn.setDoOutput(true);
                                        conn.setDoInput(true);

                                        StringBuilder query = new StringBuilder();
                                        query.append(URLEncoder.encode("tip", "UTF-8"));
                                        query.append("=");
                                        query.append(URLEncoder.encode(m_Text,"UTF-8"));
                                        String urlParams = query.toString();

                                        //conn.setRequestProperty("Content-Length",Integer.toString(m_Text.length()));
                                        //conn.setRequestProperty("tip",query.toString());
                                        System.out.println(urlParams);
                                        OutputStream os = conn.getOutputStream();
                                        DataOutputStream writer = new DataOutputStream(os);

                                        writer.writeBytes(urlParams);
                                        writer.flush();
                                        writer.close();
                                        os.close();



                                        int responseCode = conn.getResponseCode();

                                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                        String inputLine;
                                        StringBuilder response = new StringBuilder();
                                        while((inputLine = in.readLine()) != null)
                                        {
                                            response.append(inputLine);
                                        }
                                        //System.out.println(response);
                                        in.close();

                                    }
                                    catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }
                                    catch(SocketTimeoutException e) {
                                        e.printStackTrace();
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    finally
                                    {
                                        if(conn != null)
                                        {
                                            conn.disconnect();
                                        }
                                    }
                                }
                            });
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();


                }
            }
        });

        ImageButton refreshButton = (ImageButton) findViewById(R.id.imageButton_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {// First, check for permissions
                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.INTERNET);

                if(permissionCheck != PackageManager.PERMISSION_GRANTED)
                { // If we don't have permission, then
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.INTERNET))
                    { // If we need to explain why we want the permission
                        Snackbar.make(mLayout, R.string.permission_internet_rationale, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.ok, new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.INTERNET},
                                                REQUEST_INTERNET);
                                    }
                                }).show();
                    }
                    else
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.INTERNET},
                                REQUEST_INTERNET);
                    }
                }
                else
                {AsyncTask.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        HttpsURLConnection conn = null;
                        try {
                            URL tipAddress = new URL("https://speakfree.daylightingsociety.org/getTips/android");
                            conn = (HttpsURLConnection) tipAddress.openConnection();
                            conn.setReadTimeout(10000);
                            conn.setConnectTimeout(15000);
                            conn.setRequestMethod("GET");
                            conn.setDoInput(true);

                            int responseCode = conn.getResponseCode();
                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                String inputLine;
                                StringBuffer response = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                System.out.println(response);
                                //File f = new File(MainActivity.this.getFilesDir(), "tips.xml");
                                FileOutputStream os;
                                try
                                {
                                    os = openFileOutput("tips.xml", MODE_PRIVATE);
                                    os.write(response.toString().getBytes());
                                    os.close();
                                }
                                catch (Exception e) { e.printStackTrace();}
                                /*try {
                                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                    DocumentBuilder builder = factory.newDocumentBuilder();
                                    InputSource is = new InputSource(new StringReader(response.toString()));
                                    Document document = builder.parse(is);
                                    document.getDocumentElement().normalize();
                                    NodeList nList = document.getElementsByTagName("item");
                                    for(int tmp = 0; tmp < nList.getLength(); tmp++)
                                    {
                                        System.out.println(nList.item(tmp).getTextContent());
                                    }
                                }
                                catch (SAXException e){System.out.println("SAMException");}
                                catch (ParserConfigurationException e){
                                    System.out.println("Failed to parse.");
                                }*/
                                in.close();
                            }
                            catch (IOException e) {e.printStackTrace();}

                        }
                        catch (MalformedURLException e) {
                            e.printStackTrace();
                            //Snackbar.make(mLayout, "Malformed URL Exception",
                            //Snackbar.LENGTH_SHORT).show();
                        }
                        catch(SocketTimeoutException e) {
                            e.printStackTrace();
                            //Snackbar.make(mLayout, "Socket Timeout Exception",
                            //Snackbar.LENGTH_SHORT).show();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            //Snackbar.make(mLayout, "IOException",
                            //Snackbar.LENGTH_SHORT).show();
                        }
                        finally
                        {
                            if(conn != null)
                            {
                                conn.disconnect();
                            }
                        }
                    }
                });
                }
            }
        });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if(requestCode == REQUEST_INTERNET)
        {
            Log.i(TAG, "Received response for Internet permission request.");
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            { // Permission not granted
                Log.i(TAG, "INTERNET permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }


}
