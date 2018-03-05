package org.daylightingsociety.speakfree;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
import android.widget.LinearLayout;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Arrays;
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

    private boolean isplaying = false;
    private final Random rfrequency = new Random();
    private final Random bonus = new Random();
    private Thread t = null;
    private ArrayList<String> tips = new ArrayList<String>();
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

        // Either get tips from file, or use the ones in resources.
        try {

            File filedir = this.getFilesDir();
            File f = new File(filedir, "tips.txt");
            if(!f.exists())
            {
                throw new FileNotFoundException();
            }

            String str = null;
            FileInputStream raw = this.openFileInput("tips.txt");
            if (raw != null) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(raw));
                    while ((str = reader.readLine()) != null) {
                        tips.add(str);
                    }
                } catch (IOException e) {
                    try {
                        raw.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
            else {
                throw new FileNotFoundException();
            }
        }
        catch (FileNotFoundException e) {
            tips = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.tips)));
        }

        tips_index = rgenerator.nextInt(tips.size());

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
                            tips_index %= tips.size();
                            final TextView tv = (TextView) findViewById(R.id.main_tip);
                            out.setAnimationListener(new Animation.AnimationListener()
                            {
                                // when fade out ends, change the tip and fade in
                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    tv.setText(tips.get(tips_index));
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
                        if(t != null) {
                            t.interrupt();
                        }
                        t = null;
                        System.gc();
                    }
                    else
                    {
                        isplaying = true;
                        ImageButton playButton = (ImageButton) findViewById(R.id.imageButton_playButton);
                        playButton.setBackgroundResource(R.drawable.pause);
                        ImageView statusImage =(ImageView) findViewById(R.id.status_image);
                        statusImage.setImageResource(R.drawable.active);

                        t = new Thread() {
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
                                double twopi = 8.0 * Math.atan(1.0);
                                double fr;
                                double ph = 0.0;
                                try {
                                    audioTrack.play();
                                }
                                catch (Exception e) {
                                    if(audioTrack != null){
                                        audioTrack.release();
                                    }
                                    return;
                                }

                                // randomization loop
                                try {
                                    while (!Thread.currentThread().isInterrupted()) {
                                        // fill the buffer
                                        for (int i = 0; i < buffsize; i++) {
                                            samples[i] = (short) (amp * Math.sin(ph));
                                            double tempph = 0.0;
                                            for (int j = 0; j < 10; j++) {
                                                fr = (double) ((rfrequency.nextInt(3400 - 300) + 1) + 300);
                                                short prev = (short) (amp * Math.sin(tempph));
                                                short curr = (short) (amp * Math.sin(ph + twopi * fr / sr));
                                                if (Math.abs(curr) > Math.abs(prev)) {
                                                    tempph = ph + twopi * fr / sr;
                                                }
                                            }
                                            //ph += twopi*fr/sr;
                                            ph = tempph;
                                        }
                                        // randomly swap a wave in the buffer with a new one
                                        int selection = bonus.nextInt(samples.length);
                                        fr = (double) ((rfrequency.nextInt(3400 - 300) + 1) + 300);
                                        samples[selection] = (short) (amp * Math.sin(ph + twopi * fr / sr));
                                        audioTrack.write(samples, 0, buffsize);
                                    }
                                    if(audioTrack != null)
                                    {
                                        audioTrack.stop();
                                        audioTrack.release();
                                    }
                                }
                                catch(Exception e) {
                                    //if(audioTrack != null) {
                                        audioTrack.stop();
                                        audioTrack.release();
                                    //}
                                    return;
                                }
                            }
                        };
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
                        mLayout = findViewById(R.id.activity_main);
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
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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
                                        if(responseCode == 200)
                                        {
                                            mLayout = findViewById(R.id.activity_main);
                                            Snackbar.make(mLayout, R.string.tip_submitted, Snackbar.LENGTH_INDEFINITE)
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
                                            mLayout = findViewById(R.id.activity_main);
                                            Snackbar.make(mLayout, R.string.tip_submitted_error, Snackbar.LENGTH_INDEFINITE)
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
                        mLayout = findViewById(R.id.activity_main);
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
                {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Confirm")
                            .setMessage("Download new anti-surveillance tips?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    refreshTips();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            }
        });
    }

    private void refreshTips()
    {
        AsyncTask.execute(new Runnable()
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

                        FileOutputStream os;
                        try
                        {
                            os = openFileOutput("tips.xml", MODE_PRIVATE);
                            os.write(response.toString().getBytes());
                            os.close();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            if(conn != null)
                            {
                                conn.disconnect();
                            }
                            return;
                        }
                        try {
                            // Parse the xml tree to access individual elements
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            InputSource is = new InputSource(new StringReader(response.toString()));
                            Document document = builder.parse(is);
                            document.getDocumentElement().normalize();
                            NodeList nList = document.getElementsByTagName("item");

                            FileOutputStream out = null;
                            String tip = null;
                            try {
                                out = MainActivity.this.openFileOutput("tips.txt", Context.MODE_PRIVATE);
                                try
                                {
                                    for(int tmp = 0; tmp < nList.getLength(); tmp++)
                                    {
                                        tip = nList.item(tmp).getTextContent()+"\n";

                                        // Get rid of escaped characters
                                        tip = tip.replaceAll("\\\\", "");
                                        out.write(tip.getBytes());
                                    }
                                }
                                catch(IOException e) { /* TODO: log errors */}
                                System.out.println("Saved tips.");
                            }
                            catch (FileNotFoundException e)
                            { /* Do nothing if we can't save the file for some reason */ }
                            finally
                            {
                                if(out != null)
                                {
                                    try { out.close(); } catch (Throwable ignore) { }
                                }
                            }




                        }
                        catch (SAXException e){System.out.println("SAMException");}
                        catch (ParserConfigurationException e){
                            System.out.println("Failed to parse.");
                        }
                        finally {
                            in.close();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(responseCode == 200)
                    {
                        mLayout = findViewById(R.id.activity_main);
                        Snackbar.make(mLayout, R.string.tips_refreshed, Snackbar.LENGTH_INDEFINITE)
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
                        mLayout = findViewById(R.id.activity_main);
                        Snackbar.make(mLayout, R.string.tips_refreshed_error, Snackbar.LENGTH_INDEFINITE)
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
        MainActivity.this.recreate();
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
                mLayout = findViewById(R.id.activity_main);
                Log.i(TAG, "INTERNET permission was NOT granted.");
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }


}
