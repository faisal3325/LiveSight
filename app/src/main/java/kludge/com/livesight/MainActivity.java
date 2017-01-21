package kludge.com.livesight;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.RelativeLayout;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Map map;
    private CompositeFragment compositeFragment;
    private ARController arController;
    private ARRadar m_radar;
    private Button startButton;
    private Button stopButton;
    private Button toggleObjectButton;
    private Image image;
    private boolean objectAdded;
    private static final double RADAR_RATIO = 4d;
    private GeoCoordinate center;
    private List<ARObject> arObjects = new ArrayList<>();

    private Handler m_handler = new Handler();

    public double lat = 21.1644825;
    public double lng = 79.0816253;

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
            lng = GPSTracker.longitude;
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

        startButton = (Button) findViewById(R.id.startLiveSight);
        stopButton = (Button) findViewById(R.id.stopLiveSight);
        toggleObjectButton = (Button) findViewById(R.id.toggleObject);
    }

    private void setupLiveSight() {
        arController = compositeFragment.getARController();
        arController.setUseDownIconsOnMap(true);
        arController.setAlternativeCenter(new GeoCoordinate(lat, lng, 0.0));
        arController.addOnCameraEnteredListener(onARStarted);
        arController.addOnCameraExitedListener(onARStopped);
        arController.addOnRadarUpdateListener(onRadarUpdate);
    }

    public void startLiveSight(View view) {
        if (arController != null) {
            Error error = arController.start();

            if (error == Error.NONE) {
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);

                Toast.makeText(getApplicationContext(), " COORD" + lat + "" + lng + " " + error.toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void stopLiveSight(View view) {
        if (arController != null) {
            Error error = arController.stop(true);

            if (error == Error.NONE) {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(getApplicationContext(), "Error stopping LiveSight: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void toggleObject(View view) {
        if (arController != null) {
            if (!objectAdded) {

                if(image == null) {
                    image = new Image();
                    try {
                        image.setImageResource(R.drawable.s1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                double longitude = center.getLongitude();
                double latitude = center.getLatitude();
                for (int i = 0; i < 10; ++i) {

                    double lat = Math.random() * 0.004  + latitude;
                    double lng = Math.random() * 0.004 + longitude;

                    ARIconObject object = new ARIconObject(new GeoCoordinate(
                            lat, lng), (View) null, image);

                    arController.addARObject(object);
                    arObjects.add(object);
                }
                objectAdded = true;
                toggleObjectButton.setText("Remove Objects");
            } else {
                for (ARObject object : arObjects) {
                    arController.removeARObject(object);
                }
                arObjects.clear();
                objectAdded = false;
                toggleObjectButton.setText("Add Object");
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
}
