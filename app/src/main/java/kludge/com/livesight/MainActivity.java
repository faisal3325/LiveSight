package kludge.com.livesight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.ar.ARController;
import com.here.android.mpa.ar.ARController.Error;
import com.here.android.mpa.ar.ARController.OnCameraEnteredListener;
import com.here.android.mpa.ar.ARController.OnCameraExitedListener;
import com.here.android.mpa.ar.ARIconObject;
import com.here.android.mpa.ar.ARObject;
import com.here.android.mpa.ar.ARRadarProperties;
import com.here.android.mpa.ar.CompositeFragment;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private Double latiUser;
    private Double lngiUser;
    String name;
    ArrayList<String> namePlace = new ArrayList<>();
    ArrayList<Double> lngi = new ArrayList<>();
    ArrayList<Double> lati = new ArrayList<>();
    private Map map;
    private CompositeFragment compositeFragment;
    private ARController arController;
    private ARRadar m_radar;
    private Image image;
    private boolean objectAdded;
    private static final double RADAR_RATIO = 4d;
    private GeoCoordinate center;
    private List<ARObject> arObjects = new ArrayList<>();
    private String type;
    private int flag;
    private Handler m_handler = new Handler();

    public double lat, lng;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GPSTracker gpsTracker = new GPSTracker(this);
        if(!gpsTracker.canGetLocation()) {
            gpsTracker.showSettingsAlert();
        }   else {
            gpsTracker.getLocation();
            lat = GPSTracker.latitude;
            latiUser = lat;
            lng = GPSTracker.longitude;
            lngiUser = lng;
            center = new GeoCoordinate(lat, lng);
        }

        Toast.makeText(getApplicationContext(), "" + center, Toast.LENGTH_LONG).show();
        compositeFragment = (CompositeFragment) getFragmentManager().findFragmentById(R.id.compositefragment);
        compositeFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    map = compositeFragment.getMap();
                    map.setCenter(center, Map.Animation.NONE);
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    setupLiveSight();
                } else {
                    System.out.println("ERROR: Cannot initialize Composite Fragment");
                }
            }
        });

        /*monument = (ImageView) findViewById(R.id.button6);
        monument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = "monument";
                flag = 1;
                namePlace.clear();
                lati.clear();
                lngi.clear();
                add.setVisibility(View.GONE);
                nearby.setVisibility(View.GONE);

                startLiveSight(view);
                getPlaces(type);


            }
        });
        //subway_station

        metro = (ImageView) findViewById(R.id.button5);
        metro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = "train_station|subway_station";
                namePlace.clear();
                lati.clear();
                lngi.clear();
                add.setVisibility(View.GONE);
                nearby.setVisibility(View.GONE);

                startLiveSight(view);
                getPlaces(type);
            }
        });

        police = (ImageView) findViewById(R.id.button3);
        police.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = "police";
                namePlace.clear();
                lati.clear();
                lngi.clear();
                add.setVisibility(View.GONE);
                nearby.setVisibility(View.GONE);

                startLiveSight(view);
                getPlaces(type);
            }
        });

        hospital = (ImageView) findViewById(R.id.button8);
        hospital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = "hospital";
                namePlace.clear();
                lati.clear();
                lngi.clear();
                add.setVisibility(View.GONE);
                nearby.setVisibility(View.GONE);

                startLiveSight(view);
                getPlaces(type);
            }
        });

        toilet = (ImageView) findViewById(R.id.button4);
        toilet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = "toilet";
                flag = 2;
                namePlace.clear();
                lati.clear();
                lngi.clear();
                getPlaces(type);
                add.setVisibility(View.GONE);
                nearby.setVisibility(View.GONE);

                startLiveSight(view);
            }
        });
        startButton = (Button) findViewById(R.id.startLiveSight);
        stopButton = (Button) findViewById(R.id.stopLiveSight);
        toggleObjectButton = (Button) findViewById(R.id.toggleObject);
        add = (LinearLayout) findViewById(R.id.add);
        nearby = (LinearLayout) findViewById(R.id.nearby);*/
    }

    private void setupLiveSight() {
        arController = compositeFragment.getARController();
        arController.setUseDownIconsOnMap(true);
        arController.setAlternativeCenter(new GeoCoordinate(lat, lng, 0.0));
        arController.addOnCameraEnteredListener(onARStarted);
        arController.addOnCameraExitedListener(onARStopped);
        arController.addOnRadarUpdateListener(onRadarUpdate);
        arController.addOnTapListener(new ARController.OnTapListener() {
            @Override
            public boolean onTap(PointF point) {

                // retrieve ARObject at point (if one exists)
                // and trigger press animation
                ARObject arObject = arController.press(point);

                if (arObject != null) {
                    // focus object

                    /*for (int i = 0; i < lati.size(); i++) {
                        if(arObject.getCoordinate().getLatitude() == lati.get(i)
                                && arObject.getCoordinate().getLatitude() == lngi.get(i))  {
                            Log.d("PlaceName", namePlace.get(i));
                        }
                        Log.d(String.valueOf(arObject.getCoordinate().getLatitude()), String.valueOf(lati.get(i)));
                        Log.d(String.valueOf(arObject.getCoordinate().getLongitude()), String.valueOf(lngi.get(i)));
                    }*/

                    Log.d("AR", "TEST");
                    arController.focus(arObject);
                }
                return false;
            }
        });
    }

    public void toiletStartLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {
                type = "toilet";
                flag = 2;
                namePlace.clear();
                lati.clear();
                lngi.clear();
                getPlaces(type);

                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void ambulanceStartLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {

                type = "hospital";
                namePlace.clear();
                lati.clear();
                lngi.clear();

                getPlaces(type);

                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void policeStartLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {
                type = "police";
                namePlace.clear();
                lati.clear();
                lngi.clear();

                getPlaces(type);
                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void metroStartLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {
                type = "train_station|subway_station";
                namePlace.clear();
                lati.clear();
                lngi.clear();

                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void monumentStartLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {

                type = "monument";
                flag = 1;
                namePlace.clear();
                lati.clear();
                lngi.clear();

                getPlaces(type);

                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void stopLiveSight(View view) {
        if (arController != null) {
            Error error = arController.stop(true);
            lati.clear();
            lngi.clear();
            namePlace.clear();
            if (error == Error.NONE) {
            } else {
                Toast.makeText(getApplicationContext(), "Error stopping LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void toggleObject(ArrayList<Double> latit, ArrayList<Double> lngit, ArrayList<String> namePlace, String t) {
        if (arController != null) {
            if (!objectAdded) {

                    image = new Image();
                    try {
                        if(t == "metro")    image.setImageResource(R.drawable.metro);
                        else if(t == "ambulance")    image.setImageResource(R.drawable.ambulance);
                        else if(t == "police")    image.setImageResource(R.drawable.police);
                        else if(t == "monument")    image.setImageResource(R.drawable.monument);
                        else if(t == "toilet")    image.setImageResource(R.drawable.toilet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                for (int i = 0; i < latit.size(); ++i) {

                    ARIconObject object = new ARIconObject(new GeoCoordinate(
                            latit.get(i), lngit.get(i)), findViewById(R.drawable.bank), image);

                    //object.setInfoMaxHeight(5);
                    //object.setInfoMaxWidth(5);
                    /*TextView tv = new TextView(this);
                    tv.setText(namePlace.get(i));*/

                    Location loc1 = new Location("");
                    loc1.setLatitude(latit.get(i));
                    loc1.setLongitude(lngit.get(i));

                    Location loc2 = new Location("");
                    loc2.setLatitude(latiUser);
                    loc2.setLongitude(lngiUser);

                    float distanceInMeters = loc1.distanceTo(loc2);

                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = inflater.inflate(R.layout.info, null);
                    //LinearLayout lv = (LinearLayout) v.findViewById(R.id.lv);
                    TextView tv1 = (TextView) v.findViewById(R.id.textView);
                    tv1.setText(namePlace.get(i));

                    TextView tv2 = (TextView) v.findViewById(R.id.textView2);
                    tv2.setText("" + String.valueOf(distanceInMeters) + "m");

                    object.setIcon(ARObject.IconType.INFO, v);

                    //object.setIcon(ARObject.IconType.INFO, findViewById(R.string.app_name));
                    object.setIconSizeScale(ARObject.IconType.FRONT, 4);
                    arController.addARObject(object);
                    arObjects.add(object);

                }
                objectAdded = true;
            } else {
                for (ARObject object : arObjects) {
                    arController.removeARObject(object);
                }
                arObjects.clear();
                objectAdded = false;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (arController != null) {
            arController.removeOnRadarUpdateListener(onRadarUpdate);
            arController.removeOnCameraEnteredListener(onARStarted);
            arController.removeOnCameraExitedListener(onARStopped);
        }
        super.onDestroy();
    }

    private ARController.OnRadarUpdateListener onRadarUpdate = new ARController.OnRadarUpdateListener() {

        @Override
        public void onRadarUpdate(ARRadarProperties radar) {
            if (m_radar != null && radar != null) {
                m_radar.Update(radar);
            }
        }
    };

    private OnCameraEnteredListener onARStarted = new OnCameraEnteredListener() {

        @Override
        public void onCameraEntered() {
            startRadar();
        }
    };

    private OnCameraExitedListener onARStopped = new OnCameraExitedListener() {

        @Override
        public void onCameraExited() {
            stopRadar();
        }
    };


    private void startRadar() {

        if (arController == null || m_radar != null) {
            return;
        }

        final RelativeLayout layout = (RelativeLayout) MainActivity.this
                .findViewById(R.id.mainlayout);

        if (layout == null) {
            return;
        }

        m_handler.post(new Runnable() {
            public void run() {

            if (m_radar != null) {
                m_radar.clearAnimation();
            }
            m_radar = new ARRadar(getApplicationContext() , ARController.CameraParams.getHorizontalFov());

            final int width = compositeFragment.getWidth();
            final int height = compositeFragment.getHeight();
            final int size = (int) (Math.min(width, height) / RADAR_RATIO);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
            params.setMargins(0, 5, 5, 0);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layout.addView(m_radar, params);

            Animation animation;
            animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setFillAfter(true);
            animation.setDuration(1000);
            m_radar.startAnimation(animation);
            }
        });
    }

    private void stopRadar() {

        final RelativeLayout layout = (RelativeLayout) MainActivity.this.
                findViewById(R.id.mainlayout);

        if (m_radar == null || layout == null) {
            return;
        }

        m_handler.post(new Runnable() {
            public void run() {

                m_radar.clearAnimation();

                Animation animation;
                animation = new AlphaAnimation(1.0f, 0.0f);
                animation.setFillAfter(true);
                animation.setDuration(1000);

                animation.setAnimationListener(new AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        if (m_radar == null) {
                            return;
                        }
                        layout.removeView(m_radar);
                        m_radar.clear();
                        m_radar = null;
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                });
                m_radar.startAnimation(animation);
            }
        });
    }

    //https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=23.006000,72.601100&
    // types=point_of_interest&radius=50000&sensor=false&key=AIzaSyCTDwYXf7ho--fpTRRQufe0w3OoRRb0Mn0
    private void getPlaces(final String type) {
        class getPlaces extends AsyncTask<String, Void, String> {

            Double lat, lng;
            URL url = null;
            final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,"","Fetching places...");

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected String doInBackground(String... params) {

                String type = params[0];
                Log.d("", "Inside doInBackGround: type = " + type);
                namePlace.clear();
                lati.clear();
                lngi.clear();

                //DefaultHttpClient client = new DefaultHttpClient();
                if(flag == 1) {
                    try {
                            url = new URL("https://maps.googleapis.com/maps/api/place/textsearch/json?query=monuments&location=" + latiUser + "," +
                                    lngiUser + "&radius=2000&key=AIzaSyCTDwYXf7ho--fpTRRQufe0w3OoRRb0Mn0");
                            flag = 0;
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                else if(flag == 2) {
                    try {
                        url = new URL("https://maps.googleapis.com/maps/api/place/textsearch/json?query=toilets&location=" + latiUser + "," +
                            lngiUser + "&radius=2000&key=AIzaSyCTDwYXf7ho--fpTRRQufe0w3OoRRb0Mn0");
                        flag = 0;
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            url = new URL("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latiUser + "," +
                                    lngiUser + "&radius=2000&type=" + type + "&key=AIzaSyCTDwYXf7ho--fpTRRQufe0w3OoRRb0Mn0");
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }

                Log.d("TEST", "");
                Log.d("", "" + "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latiUser + "," +
                        lngiUser + "&radius=2000&type=" + type + "&key=AIzaSyCTDwYXf7ho--fpTRRQufe0w3OoRRb0Mn0");
                try {
                    JSONObject jsonobj = new JSONObject(JSONParser.downloadUrl(String.valueOf(url)));
                    JSONArray resarray = jsonobj.getJSONArray("results");
                    if (resarray.length() == 0) {
                        Log.d("LENGTH!::", String.valueOf(resarray.length()));
                    } else {
                        int len = resarray.length();
                        Log.d("LENGTH!:!!!!:", String.valueOf(len));
                        for (int j = 0; j < len; j++) {
                            try {
                                name = resarray.getJSONObject(j).getString("name");
                                namePlace.add(name);
                                JSONObject geo = resarray.getJSONObject(j).getJSONObject("geometry");
                                JSONObject loc = geo.getJSONObject("location");
                                lat = Double.valueOf(loc.getString("lat"));
                                lati.add(lat);
                                lng = Double.valueOf(loc.getString("lng"));
                                lngi.add(lng);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                return "Executed";
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == "Executed") {
                    dialog.dismiss();
                    for (int i = 0; i < namePlace.size(); i++) {
                        // mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(lati.get(i)),
                        // Double.parseDouble(lngi.get(i)))).title(namePlace.get(i)));
                        Log.d("Place", namePlace.get(i));
                    }
                    if (flag == 2)  {
                        toggleObject(lati, lngi, namePlace,  "toilet");
                    }   else if (flag == 1) {
                        toggleObject(lati, lngi, namePlace, "monument");
                    }   else if (type == "metro")   {
                        toggleObject(lati, lngi, namePlace, "metro");
                    }   else if (type == "police")  {
                        toggleObject(lati, lngi, namePlace, "police");
                    }   else if (type == "hospital")    {
                        toggleObject(lati, lngi, namePlace, "ambulance");
                    }
                }
            }
        }
        getPlaces gh = new getPlaces();
        gh.execute(type);
    }
}
