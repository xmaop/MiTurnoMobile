package pe.edu.upc.ticket;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import pe.edu.upc.ticket.model.Ticket;

public class TicketActivity extends AppCompatActivity {

    private Ticket mTicket = null;
    public static final int NOTIFICACION_ID=1;

    private ImageView imageCompany;
    private TextView tviCompany, tviBranch, tviTime, tviNumber, tviPeople;
    private Button btnCancel, btnPostpone;
    private AlertDialog.Builder cancerAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        findViews();
        findButtons();
        setButtonsActions();
        retrieveTicket();
        showValues();
        //Toast.makeText(this, getIntent().getStringExtra("qrCode"), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        retrieveTicket();
        showValues();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
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
        Drawable draw = getResources().getDrawable(R.drawable.bbva);
        Bitmap bitmap = ((BitmapDrawable)draw).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArrayImage = stream.toByteArray();
        //String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);

        byte[] imageByteArray = byteArrayImage;
        Bitmap bitmap2 = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        imageCompany.setImageBitmap(bitmap2);

        tviCompany.setText(mTicket.getCompanyName());
        tviBranch.setText(mTicket.getCompanyBranch());
        tviNumber.setText(mTicket.getNumberTicket());
        tviPeople.setText(String.valueOf(mTicket.getPeopleQuantity()));
        Long value = mTicket.getTimeLeft()-((new Date()).getTime() - mTicket.getServerTime().getTime());
        new CountDownTimer(value,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tviTime.setText(String.format("%02d:%02d",millisUntilFinished/(60*60*1000), (millisUntilFinished/(60*1000))%60));
            }

            @Override
            public void onFinish() {
                tviTime.setText("00:00");
                sendNotificationTurnArrived();
            }
        }.start();
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
                cancerAlert.setTitle("Cancelar Turno de Aplicación");
                cancerAlert.setMessage("¿Está seguro de cancelar su turno de atención?\nRecuerde que al confirmar esta acción deberá obtener un nuevo ticket si desea ser atendido.");
                cancerAlert.setPositiveButton("SI",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //TODO:
                            }
                        });
                cancerAlert.setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                cancerAlert.show();
            }
        });
    }

    private void sendNotificationTurnArrived() {
        //Construction of implicit intent action
        Intent intent= new Intent(Intent.ACTION_VIEW, Uri.parse("http://developer.android.com/index.html"));
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,0);

        //Construccion de la notificacion;
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle("Notificacion de LLegada de Turno");
        builder.setContentText("Tu turno ha llegado!");
        builder.setSubText("Acercate a la ventanilla 5 para ser atendido.");

        //Enviar la notificacion
        NotificationManager notificationManager= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICACION_ID,builder.build());
    }
}