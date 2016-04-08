package pe.edu.upc.ticket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import pe.edu.upc.ticket.model.Ticket;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private TextView barcodeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cameraView = (SurfaceView)findViewById(R.id.camera_view);
        WindowManager mWinMgr = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);
        int width = 500 + (mWinMgr.getDefaultDisplay().getWidth()-500)/2;
        cameraView.getHolder().setFixedSize(width, width-100);
        barcodeInfo = (TextView)findViewById(R.id.code_info);

        qrReading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void qrReading(){
        barcodeDetector =new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(640,480).build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException e) {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size()>0) {
                    barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            cameraSource.stop();
                            requestTicket(barcodes.valueAt(0).displayValue);
                            Intent intent = new Intent(MainActivity.this, TicketActivity.class);
                            //intent.putExtra("qrCode", barcodes.valueAt(0).displayValue);
                            startActivity(intent);
                        }
                    });
                }
            }
        });
    }

    private void requestTicket(String qrValue){

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, qrValue, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                        Ticket ticket = gson.fromJson(response.toString(), Ticket.class);
                        saveTicket(ticket);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("RequestTicket", "ERROR");
                    }
                });

        queue.add(jsObjRequest);
    }

    private Ticket getTicket(){
        Ticket ticket = new Ticket();
        ticket.setCompanyName("BBVA Banco Continental");
        ticket.setCompanyBranch("Sucursal San Borja");
        ticket.setTimeLeft(1000 * 60 * 62);
        ticket.setServerTime(new Date());
        ticket.setNumberTicket("AV-258");
        ticket.setPeopleQuantity(4);

        return ticket;
    }

    private void saveTicket(Ticket ticket){
        SharedPreferences  mPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        if(ticket!=null) {
            String json = gson.toJson(ticket);
            prefsEditor.putString("TICKET", json);
        }else{
            prefsEditor.putString("TICKET", "");
        }
        prefsEditor.commit();
    }
}
