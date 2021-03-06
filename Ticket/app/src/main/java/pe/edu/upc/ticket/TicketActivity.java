package pe.edu.upc.ticket;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

import pe.edu.upc.ticket.model.Ticket;

public class TicketActivity extends BatchActivity {

    public static final int NOTIFICACION_ID=1;
    private Ticket mTicket = null;
    private ProgressDialog pDialog;

    private ImageView imageCompany;
    private TextView tviCompany, tviBranch, tviTime, tviNumber, tviPeople;
    private Button btnCancel, btnPostpone;
    private AlertDialog.Builder cancerAlert, postponeAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        try {
            pDialog = ProgressDialog.show(this, "", "Loading ...", true);
            findViews();
            findButtons();
            setButtonsActions();
            retrieveTicket();
            showValues();
        }catch (Exception e) {
            Toast.makeText(TicketActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        retrieveTicket();
        showValues();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        return super.onPrepareOptionsMenu(menu);
//    }

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

    private  void findButtons() {
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnPostpone = (Button) findViewById(R.id.btnPostpone);
    }

    private void findViews(){
        imageCompany = (ImageView) findViewById(R.id.imageCompany);
        tviCompany = (TextView) findViewById(R.id.tviCompany);
        tviBranch = (TextView) findViewById(R.id.tviBranch);
        tviTime = (TextView) findViewById(R.id.tviTime);
        tviNumber = (TextView) findViewById(R.id.tviNumber);
        tviPeople = (TextView) findViewById(R.id.tviPeople);
    }

    private void showValues(){
        /*Drawable draw = getResources().getDrawable(R.drawable.bbva);
        Bitmap bitmap = ((BitmapDrawable)draw).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArrayImage = stream.toByteArray();
        //String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);

        byte[] imageByteArray = byteArrayImage;
        Bitmap bitmap2 = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        imageCompany.setImageBitmap(bitmap2);*/
        if(mTicket!=null) {
            new LoadImage().execute(mTicket.getStatus());
            tviCompany.setText(mTicket.getCompanyName());
            tviBranch.setText(mTicket.getCompanyBranch());
            tviNumber.setText(mTicket.getNumberTicket());
            tviPeople.setText(String.valueOf(mTicket.getPeopleQuantity()));
            Long value = mTicket.getTimeLeft() - ((new Date()).getTime() - mTicket.getServerTime().getTime());
            new CountDownTimer(value, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    tviTime.setText(String.format("%02d:%02d", millisUntilFinished / (60 * 60 * 1000), (millisUntilFinished / (60 * 1000)) % 60));
                }

                @Override
                public void onFinish() {
                    tviTime.setText("00:00");
                    sendNotificationTurnArrived();
                }
            }.start();
        }
    }

    private void retrieveTicket(){
        SharedPreferences  mPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("TICKET", "");
        mTicket = gson.fromJson(json, Ticket.class);
    }

    private void setButtonsActions() {
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancerAlert = new AlertDialog.Builder(TicketActivity.this);
                cancerAlert.setTitle(R.string.cancel_turn_dialog_title);
                cancerAlert.setMessage(R.string.cancel_turn_dialog_message);
                cancerAlert.setPositiveButton(R.string.cancel_turn_dialog_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cancelTicket();
                                startActivity(new Intent(TicketActivity.this, MainActivity.class));
                            }
                        });
                cancerAlert.setNegativeButton(R.string.cancel_turn_dialog_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                cancerAlert.show();
            }
        });

        btnPostpone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postponeAlert = new AlertDialog.Builder(TicketActivity.this);
                postponeAlert.setTitle(R.string.postpose_turn_dialog_title);
                postponeAlert.setMessage(R.string.postpose_turn_dialog_message);
                postponeAlert.setPositiveButton(R.string.cancel_turn_dialog_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                refreshTicket();
                            }
                        });
                postponeAlert.setNegativeButton(R.string.cancel_turn_dialog_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                postponeAlert.show();
            }
        });
    }

    private void cancelTicket(){
        SharedPreferences  mPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("TICKET", "");
        prefsEditor.commit();
    }

    private void refreshTicket(){
        int newToken = (int)(Math.random()*9000)+1000;
        String url = "http://ticketserver-xmaop.c9users.io/token.php?token=" + newToken;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                        Ticket ticket = gson.fromJson(response.toString(), Ticket.class);
                        saveTicket(ticket);
                        retrieveTicket();
                        showValues();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("RequestTicket", "ERROR");
                    }
                });

        queue.add(jsObjRequest);
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

    private Bitmap bitmap;
    private class LoadImage extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Bitmap doInBackground(String... args) {
            try {
                bitmap = BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap image) {
            if(image != null){
                imageCompany.setImageBitmap(image);
            }else{
                Toast.makeText(TicketActivity.this, "Image Does Not exist or Network Error", Toast.LENGTH_SHORT).show();
            }
            if(pDialog != null && pDialog.isShowing()) pDialog.dismiss();
        }
    }

    private void sendNotificationTurnArrived() {
        //Construction of implicit intent action
        Intent intent= new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.android.com/index.html"));
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,0);

        //Construction of the Notification
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle("miTurno en " + mTicket.getCompanyName());
        builder.setContentText("Ya es tu Turno!!");
        builder.setSubText("Acércate a la ventanilla 5 para ser atendido.");
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));


        //Send the notification
        NotificationManager notificationManager= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICACION_ID,builder.build());
    }
}