package importsdkdemo.dji.com.importsdkdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.*;
import android.support.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.ContentType;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import android.os.Environment;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.Rotation;
import dji.common.mission.waypoint.WaypointDownloadProgress;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.MediaManager;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.camera.MediaFile;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.util.CommonCallbacks;
import dji.common.camera.SettingsDefinitions;
import dji.sdk.camera.DownloadListener;


import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.mission.timeline.actions.GimbalAttitudeAction;
import dji.sdk.mission.timeline.actions.GoHomeAction;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.HotpointAction;
import dji.sdk.mission.timeline.actions.RecordVideoAction;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.timeline.actions.TakeOffAction;
import dji.sdk.mission.timeline.actions.AircraftYawAction;
import dji.sdk.mission.timeline.triggers.AircraftLandedTrigger;
import dji.sdk.mission.timeline.triggers.BatteryPowerLevelTrigger;
import dji.sdk.mission.timeline.triggers.Trigger;
import dji.sdk.mission.timeline.triggers.TriggerEvent;
import dji.sdk.mission.timeline.triggers.WaypointReachedTrigger;
import dji.common.model.LocationCoordinate2D;

import java.net.URI;
import java.net.URISyntaxException;
import dji.thirdparty.org.java_websocket.handshake.ServerHandshake;
import dji.thirdparty.org.java_websocket.client.WebSocketClient;
import dji.thirdparty.org.java_websocket.handshake.ClientHandshake;

import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import com.android.volley.VolleyError;
import com.android.volley.NetworkError;
import com.android.volley.ServerError;
import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private Handler mHandler;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }
        setContentView(R.layout.activity_main);
        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());

        // for websocket
        setContentView(R.layout.activity_main);
        username = findViewById(R.id.username);
        output = findViewById(R.id.output);
        fetchButton = findViewById(R.id.fetch);
        launchButton = findViewById(R.id.launch);
        downloadButton = findViewById(R.id.download);
        sendButton = findViewById(R.id.send);
        landButton = findViewById(R.id.land);
        client = new OkHttpClient();
    }
    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }
    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering, please wait...");
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                showToast("Register Success");
                                DJISDKManager.getInstance().startConnectionToProduct();
                                fetchButton.setEnabled(true);
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            mProduct = newProduct;
                            if(mProduct != null) {
                                mProduct.setBaseProductListener(mDJIBaseProductListener);
                                initDroneState();
                                output("Connected to drone: " + mProduct.getModel().getDisplayName());
                            }
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
    }
    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void launchMission(View v) {
        if (mProduct != null && mProduct.isConnected() && mProduct instanceof Aircraft) {
            initTimeline();
            downloadButton.setEnabled(true);
        }
    }

    public void downloadImages(View v) {
        if (mProduct != null && mProduct.isConnected() && mProduct instanceof Aircraft) {
            output("fetching images");
            final MediaManager mMediaManager = mProduct.getCamera().getMediaManager();
            mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        List<MediaFile> mediaFileList = mMediaManager.getInternalStorageFileListSnapshot();

                        for (int wpi = 0; wpi < NUM_WAYPOINTS; wpi++) {
                            List<String> filePaths = new LinkedList<>();

                            for (int i = mediaFileList.size() - 1 - (NUM_PHOTOS * wpi); i >= mediaFileList.size() - (NUM_PHOTOS * (wpi + 1)); i--) {
                                final MediaFile file = mediaFileList.get(i);
                                filePaths.add(imageSaveLocation.getAbsolutePath() + "/" + file.getFileName());
                                file.fetchFileData(imageSaveLocation, null, new DownloadListener<String>() {
                                    @Override
                                    public void onFailure(DJIError error) {
                                        output("failure: "+ error.getDescription());
                                    }
                                    @Override
                                    public void onProgress(long total, long current) {
                                    }
                                    @Override
                                    public void onRateUpdate(long total, long current, long persize) {
                                    }
                                    @Override
                                    public void onStart() {
                                        output("Downloading: " + file.getFileName());
                                    }
                                    @Override
                                    public void onSuccess(String filePath) {
                                        output("Finished downloading: " + file.getFileName());
                                        output("\n"); // flush
                                    }
                                });
                            }

                            List<String> identifier = waypointIdentifiers.get(NUM_WAYPOINTS - wpi - 1);
                            output("Adding: " + identifier.toString());
                            metaData.put(identifier, filePaths);
                        }
                        output("metaData (size = " + metaData.keySet() + "): " + metaData.toString());
                        sendButton.setEnabled(true);
                    } else {
                        output("error: " + djiError.getDescription());
                    }

                }
            });

        }
    }


    public void sendImages(View v) {
            new Thread() {
                @Override
                public void run() {
                    int totalImgCount = metaData.keySet().size() * NUM_PHOTOS;
                    int currImg = 1;
                    int currPostRequest = 1;
                    output("Sending " + totalImgCount + " Images...");
                    for (List<String> key : metaData.keySet()) {
                        CloseableHttpClient httpClient = HttpClients.createDefault();
                        output("KEY: " + key.toString());
                        List<String> filePaths = metaData.get(key);
                        HttpPost post = new HttpPost("http://ipp.prod.aerie-tech.com:5555/waypointReached");
                        StringBody userIdBody = new StringBody(USER_ID, ContentType.MULTIPART_FORM_DATA);
                        StringBody sectorIdBody = new StringBody(key.get(0), ContentType.MULTIPART_FORM_DATA);
                        StringBody waypointIdBody = new StringBody(key.get(1), ContentType.MULTIPART_FORM_DATA);
                        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                        builder.addPart("user_id", userIdBody);
                        builder.addPart("sector_id", sectorIdBody);
                        builder.addPart("waypoint_id", waypointIdBody);

                        for (int i = 0; i < filePaths.size(); i++) {
                            File file = new File(filePaths.get(i));
                            FileBody fileBody = new FileBody(file);
                            builder.addPart("" + i, fileBody);
                            output("Building Image " + currImg + " of " + totalImgCount);
                            currImg++;
                        }

                        HttpEntity entity = builder.build();
                        post.setEntity(entity);
                        try {
                            HttpResponse response = httpClient.execute(post);
                            output("Successfully Completed " + currPostRequest + " of " + metaData.keySet().size() + " Requests");
                            currPostRequest++;
                            httpClient.close();
                        } catch (IOException e) {
                            output(e.toString());
                        }
                    }
                    output("Successfully Sent " + totalImgCount + " Images");
                }
            }.start();

    }

    public void maydayLanding(View v) {
        if (mProduct != null && mProduct.isConnected() && mProduct instanceof Aircraft) {
            MissionControl.getInstance().stopTimeline();
            FlightController mFlightController = ((Aircraft) mProduct).getFlightController();
            mFlightController.startLanding(null);
        }
    }

    private static int NUM_PHOTOS = 7;
    private int NUM_WAYPOINTS = 0;
    private String USER_ID = null;

    private HashMap<List<String>, List<String>> metaData = new HashMap<>();
    private List<List<String>> waypointIdentifiers = new LinkedList<>();

    private double homeLatitude = 0;
    private double homeLongitude = 0;
    private WaypointMissionOperator wmo;
    WebSocketClient mWebSocketClient = null;
    private String JSON_mission = null;
    private File imageSaveLocation = new File(Environment.getExternalStorageDirectory().getPath() + "/aerie/");
    private EditText username;
    private Button fetchButton;
    private Button launchButton;
    private Button downloadButton;
    private Button sendButton;
    private Button landButton;
    private OkHttpClient client;
    private TextView output;

    private void initDroneState() {
        FlightController mFlightController = ((Aircraft) mProduct).getFlightController();
        homeLatitude = mFlightController.getState().getAircraftLocation().getLatitude();
        homeLongitude = mFlightController.getState().getAircraftLocation().getLongitude();
        wmo = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
    }

    private WaypointMission buildWaypointMission() {
        output("Building Waypoint Mission");
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            return null;
        }

        try {
            final JSONObject obj = new JSONObject(JSON_mission);
            final JSONArray json_waypoints = obj.getJSONArray("waypoints");
            List<AerieWaypoint> aerieWaypoints = new LinkedList<>();
            for (int i = 0; i < json_waypoints.length(); ++i) {
                final JSONObject waypoint = json_waypoints.getJSONObject(i);
                String user_id = waypoint.getString("user_id");
                String sector_id = waypoint.getString("sector_id");
                String waypoint_id = waypoint.getString("waypoint_id");

                USER_ID = user_id;
                List<String> identifier = new LinkedList<String>();
                identifier.add(sector_id);
                identifier.add(waypoint_id);
                waypointIdentifiers.add(identifier);

                double lng = waypoint.getDouble("lng");
                double lat = waypoint.getDouble("lat");
                double alt = waypoint.getDouble("alt");
                AerieWaypoint newWaypoint = new AerieWaypoint(user_id, sector_id, waypoint_id, lng, lat, alt);
                aerieWaypoints.add(newWaypoint);
            }

            WaypointMission.Builder waypointMissionBuilder = new WaypointMission.Builder().autoFlightSpeed(5f)
                    .maxFlightSpeed(10f)
                    .finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                    .headingMode(WaypointMissionHeadingMode.AUTO);

            List<Waypoint> djiWaypoints = new LinkedList<>();

            for (AerieWaypoint awp: aerieWaypoints) {
                // action waypoint: rotate and take photos
                int rotationAmount = 360 / NUM_PHOTOS;

                Waypoint actionWP = new Waypoint(awp.lat, awp.lng, awp.alt);
                actionWP.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
                for (int i = 0; i < NUM_PHOTOS; i++) {
                    int direction = rotationAmount * i;
                    if (direction / 180 >= 1) {
                        direction = (-180) + direction % 180;
                    }
                    output("i: " + i + "direction: " + direction);
                    actionWP.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, direction));
                    actionWP.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 0));
                }
                djiWaypoints.add(actionWP);
                NUM_WAYPOINTS++;
            }
            waypointMissionBuilder.waypointList(djiWaypoints).waypointCount(djiWaypoints.size());
            output("Finished Building Waypoint Mission");
            return waypointMissionBuilder.build();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initTimeline() {
        output("Building Mission");
        if (!GeneralUtils.checkGpsCoordinate(homeLatitude, homeLongitude)) {
            return;
        }
        assert(JSON_mission != null);

        landButton.setEnabled(true);
        List<TimelineElement> elements = new ArrayList<>();
        MissionControl missionControl = MissionControl.getInstance();
        MissionControl.Listener listener = new MissionControl.Listener() {
            @Override
            public void onEvent(@Nullable TimelineElement element, TimelineEvent event, DJIError error) {}
        };

        //Step 1: takeoff from the ground
        elements.add(new TakeOffAction());

        // Step 2: rise to 10m
        elements.add(new GoToAction(new LocationCoordinate2D(homeLatitude, homeLongitude), 10f));

        // Step 3: adjust camera angle
        elements.add(new GimbalAttitudeAction(new Attitude(-60, Rotation.NO_ROTATION, Rotation.NO_ROTATION)));

        //Step 3: start a waypoint mission
        TimelineElement waypointMission = TimelineMission.elementFromWaypointMission(buildWaypointMission());
        elements.add(waypointMission);

        //Step 4: go back home
        elements.add(new GoHomeAction());

        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything();
            missionControl.removeAllListeners();
        }

        DJIError error = missionControl.scheduleElements(elements);
        missionControl.addListener(listener);
        output("Starting Mission");
        missionControl.startTimeline();
    }

    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            USER_ID = username.getText().toString();
            webSocket.send("{\"user_id\":\"" + USER_ID + "\", \"lng\":45, \"lat\":67, \"alt\":89}");
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if(text.equals("ACK")) return;
            output("Mission Received: " + text);
            JSON_mission = text;
            launchButton.setEnabled(true);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            //output("Closing : " + code + " / " + reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            //output("Error : " + t.getMessage());
        }
    }
    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            output.setText(output.getText().toString() + "\n\n" + txt);
            output.setText(output.getText().toString() + "\n");
            //output.setText(txt);
            }
        });
    }
    public void fetchMission(View v) {
        output("Waiting to receive mission...");
        Request request = new Request.Builder().url("ws://fpe.prod.aerie-tech.com:4444/registerDrone").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }
}