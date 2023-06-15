package com.example.july;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



import java.util.UUID;

import me.itangqi.waveloadingview.WaveLoadingView;

public class MainApp extends AppCompatActivity {

    static final int STATE_CONNECTED=1;
    static final int STATE_MESSAGE_RECEIVED=3;
    static final int STATE_CONNECTION_FAILED=2;
    public SendRecieve sendRecieve;
    BluetoothSocket finalbtSocket=null, btsocket= null;
    Boolean bt_connection_state=false,bt_connection_state2=false;

    public char set_temp_unit='C';
    public String[] tempdata;
    public int r_val=0,g_val=0,b_val=0;
    TextView julyInApp_logo,message_bt,message_firebase,tempviewdata,humidviewdata,tempindexviewdata;

    ImageButton button_halo,button_pump,button_main_light,button_night_light,button_fan,button_led_1,button_led_2,button_led_3;
    boolean button_flag_halo=false,button_flag_pump=false,button_flag_main_light=false,button_flag_night_light=false,button_flag_fan=false;
    boolean button_flag_led_1=false,button_flag_led_2=false,button_flag_led_3=false;
    ImageView button_img_halo,button_img_pump,button_img_main_light,button_img_night_light,button_img_fan,button_img_led_1,button_img_led_2,button_img_led_3;

    TextView lut_date,lut_hr,lut_min,lut_sec;

    ImageView bluetooth_icon,sd_icon,wifi_icon;

    ImageView colorpicker,led_img_display;
    Bitmap bitmap;
    WaveLoadingView water_pump_level;
    int water_fullCounter=0;
    int water_emptyCounter=0;

    private NotificationManagerCompat notificationManager;

    private InputStream INPUTstream=null;
    private OutputStream outputStream=null;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        notificationManager = NotificationManagerCompat.from(this);

        //initialise the switches
        findviewbyID();
        setlistners();
        colorpicker.setDrawingCacheEnabled(true);
        colorpicker.buildDrawingCache(true);

        connectToBTorFirebase();
    }

    public void connectToBTorFirebase(){
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(!btAdapter.isEnabled())
        {
            while(!btAdapter.isEnabled()){
                btAdapter.enable();
            }
        }
        //System.out.println(btAdapter.getBondedDevices());
        finalbtSocket =getBtsocket(btAdapter);
        //System.out.println(finalbtSocket);
        //Sending the first string as soon as the app loads the second screen showing the status of connection
        if(bt_connection_state){
            String sendconnection_start="Connected";
            sendcommand(finalbtSocket, sendconnection_start);
            sendRecieve=new SendRecieve(finalbtSocket);
            sendRecieve.start();
        }
        run_firebase();
    }

    public void run_firebase(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("latest");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                set_data_from_firebase(value);
                //System.out.println("------------------------"+value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                message_firebase.setText("Not connected to the internet! JulyFi not available.");
                //System.out.println("....................."+error.toException());
            }
        });

    }

    //function to analyse the data
    public void analyse_msg(String message)
    {
        //System.out.println(message);
        message_bt.setText(message);
        if(message.charAt(0)=='C')
        {
            String message_data[]=message.split("\\s* \\s*");
            if(message_data.length==3){
                setTankWaterLevel(message_data[2]);
                setSwitches(message_data[1]);
            }
            else{
                message_bt.setText(message);
            }
        }
        else if(message.charAt(0)=='S' && message.charAt(2)==',')
        {
            String[] temp_time=getCurrentTime();
            lut_date.setText(temp_time[0]);
            lut_hr.setText(temp_time[1]);
            lut_min.setText(temp_time[2]);
            lut_sec.setText(temp_time[3]);
            set_temp_humidity(message);
        }
        else
        {
            printMessage(message);
        }
    }

    //sets the switch evertime the app starts
    private void setSwitches(String temp_switch_states)
    {
        if(temp_switch_states.charAt(0)=='0')
        {
            button_img_main_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_main_light=false;
        }
        else if(temp_switch_states.charAt(0)=='1')
        {
            button_img_main_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_main_light=true;
        }
        if(temp_switch_states.charAt(1)=='0')
        {
            button_img_night_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_night_light=false;
        }
        else if(temp_switch_states.charAt(1)=='1')
        {
            button_img_night_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_night_light=true;
        }
        if(temp_switch_states.charAt(2)=='0')
        {
            button_img_fan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_fan=false;
        }
        else if(temp_switch_states.charAt(2)=='1')
        {
            button_img_fan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_fan=true;
        }
        if(temp_switch_states.charAt(3)=='0')
        {
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_halo=false;
        }
        else if(temp_switch_states.charAt(3)=='1')
        {
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_halo=true;
        }
        if(temp_switch_states.charAt(4)=='0')
        {
            button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_pump=false;
        }
        else if(temp_switch_states.charAt(4)=='1')
        {
            button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_pump=true;
        }
        if(temp_switch_states.charAt(5)=='0')
        {
            button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_led_1=false;
        }
        else if(temp_switch_states.charAt(5)=='1')
        {
            button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_led_1=true;
        }
        if(temp_switch_states.charAt(6)=='0')
        {
            button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_led_2=false;
        }
        else if(temp_switch_states.charAt(6)=='1')
        {
            button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_led_2=true;
        }
    }

    //print the message and refresh the message display
    private void printMessage(String tempst)
    {
        if(tempst.charAt(0)=='R' && tempst.charAt(1)=='G'&& tempst.charAt(2)=='B')
        {
            if(tempst.charAt(4)=='1'&&tempst.charAt(6)=='1')
            {
                button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
                button_flag_led_1=true;
                button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
                button_flag_led_2=true;
            }
            else if(tempst.charAt(4)=='0'&&tempst.charAt(6)=='1')
            {
                button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
                button_flag_led_1=false;
                button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
                button_flag_led_2=true;
            }
            else if(tempst.charAt(4)=='1'&&tempst.charAt(6)=='0')
            {
                button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
                button_flag_led_1=true;
                button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
                button_flag_led_2=false;
            }
            else if(tempst.charAt(4)=='0'&&tempst.charAt(6)=='0')
            {
                button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
                button_flag_led_1=false;
                button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
                button_flag_led_2=false;
            }
        }
        else if(tempst.equals("s11")){
            button_img_main_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_main_light=true;
        }
        else if(tempst.equals("s12")){
            button_img_main_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_main_light=false;
        }
        else if(tempst.equals("s21")){
            button_img_night_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_night_light=true;
        }
        else if(tempst.equals("s22")){
            button_img_night_light.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_night_light=false;
        }
        else if(tempst.equals("s31")){
            button_img_fan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_fan=true;
        }
        else if(tempst.equals("s32")){
            button_img_fan.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_fan=false;
        }
        else if(tempst.equals("s41")){
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_halo=true;
        }
        else if(tempst.equals("s42")){
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_halo=false;

        }
        else if(tempst.equals("s51")){
            button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_pump=true;
        }
        else if(tempst.equals("s52")){
            button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_pump=false;
        }
        else if(tempst.equals("s61")){
            button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_led_1=true;
        }
        else if(tempst.equals("s62")){
            button_img_led_1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_led_1=false;
        }
        else if(tempst.equals("s71")){
            button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_led_2=true;
        }
        else if(tempst.equals("s72")){
            button_img_led_2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_led_2=false;
        }
        else if(tempst.equals("s81")){
            button_img_led_3.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_led_3=true;
        }
        else if(tempst.equals("s82")){
            button_img_led_3.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_led_3=false;
        }
        else{
            message_bt.setText(tempst);
        }
        refresh_text();
    }

    private void refresh_text()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        },5000);
    }

    public String[] getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy/MM/dd HH mm ss ");
        String strDate = mdformat.format(calendar.getTime());
        String strDate_t ="";
        String[] strDate_arr =new String[4];
        int ctr_c=0;
        for(int iss=0;iss<strDate.length();iss++)
        {
            if(strDate.charAt(iss)!=' '){
                strDate_t=strDate_t+strDate.charAt(iss);
            }else{
                strDate_arr[ctr_c++]=strDate_t;
                strDate_t="";
            }
        }
        return strDate_arr;
    }
    //set temperature data, water pump data, and halogen data and send back the data
    private void set_temp_humidity(String temps)
    {
        tempdata=temps.split("\\s*,\\s*");
        String withunit="";
        if(tempdata.length==7)
        {
            if (set_temp_unit == 'C') {
                withunit = tempdata[1].concat("°C");
                tempviewdata.setText(withunit);
                withunit = tempdata[3].concat(" %");
                humidviewdata.setText(withunit);
                withunit = tempdata[2].concat("°C");
                tempindexviewdata.setText(withunit);
            } else if (set_temp_unit == 'F') {
                double temp_inF = Double.parseDouble(tempdata[2]);
                temp_inF = (temp_inF * 1.8) + 32;
                withunit = String.format("%.2f", temp_inF).concat("°F");
                tempviewdata.setText(withunit);
                withunit = tempdata[3].concat("  %");
                humidviewdata.setText(withunit);
                temp_inF = Double.parseDouble(tempdata[1]);
                temp_inF = (temp_inF * 1.8) + 32;
                withunit = String.format("%.2f", temp_inF).concat("°F");
                tempindexviewdata.setText(withunit);
            }
            setTankWaterLevel(tempdata[4]);
            checktank_andcallhalo(tempdata[5], tempdata[6]);

        }
    }
    private void setTankWaterLevel(String waterlevel){
        int waterlevel_int = get_intValue(waterlevel);
        water_pump_level.setProgressValue(waterlevel_int);
        waterlevel = waterlevel + ".0%";
        if (waterlevel_int < 40) {
            water_pump_level.setBottomTitle("");
            water_pump_level.setCenterTitle(waterlevel);
            water_pump_level.setTopTitle("");
        } else if (waterlevel_int < 80 ) {
            water_pump_level.setBottomTitle("");
            water_pump_level.setCenterTitle(waterlevel);
            water_pump_level.setTopTitle("");
        } else {
            water_pump_level.setBottomTitle("");
            water_pump_level.setCenterTitle(waterlevel);
            water_pump_level.setTopTitle("");
        }
    }
    private void checktank_andcallhalo(String tempdata_a, String tempdata_b){
        if(tempdata_a.equals("1")||tempdata_a.equals("0"))
        {
            if(tempdata_a.equals("0")&&water_fullCounter==0)
            {
                water_emptyCounter=0;
                water_fullCounter=1;
                notify_tank("Water tank full. Turning the pump off.");
                button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
                button_flag_pump=false;
                checkhalo_andsend_data(tempdata_a,tempdata_b);
            }
            else if(tempdata_a.equals("1")&&water_emptyCounter==0)
            {
                water_emptyCounter=1;
                water_fullCounter=0;
                notify_tank("Water running low. Starting the pump.");
                button_img_pump.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
                button_flag_pump=true;
                checkhalo_andsend_data(tempdata_a,tempdata_b);
            }

        }
        else if (tempdata_a.equals("N")) {
            water_fullCounter = 0;
            water_emptyCounter = 0;
            checkhalo_andsend_data(tempdata_a,tempdata_b);
        }
    }
    private void checkhalo_andsend_data(String tempdata_a, String tempdata_b)
    {
        if(tempdata_b.equals("0") && button_flag_halo)
        {
            notify_halo("Good Morning. Turning halogens off.");
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_off)));
            button_flag_halo=false;
        }
        else if(tempdata_b.equals("1") && !button_flag_halo)
        {
            notify_halo("Good evening. Turning halogens on.");
            button_img_halo.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.button_ring_on)));
            button_flag_halo=true;
        }
        //String station_data=("S1,");
        //station_data =station_data.concat(tempdata_a.concat(","));
        //station_data = station_data .concat(tempdata_b);
        //sendcommand(btsocket, station_data);
    }


    //function that recieves string and returns int
    private int get_intValue(String string_number)
    {
        int temp_number=0;
        for(int ii=0;ii<string_number.length();ii++)
        {
            if (string_number.charAt(ii) == '0') {
                temp_number = (temp_number * 10);
            } else if (string_number.charAt(ii) == '1') {
                temp_number = (temp_number * 10) + 1;
            } else if (string_number.charAt(ii) == '2') {
                temp_number = (temp_number * 10) + 2;
            } else if (string_number.charAt(ii) == '3') {
                temp_number = (temp_number * 10) + 3;
            } else if (string_number.charAt(ii) == '4') {
                temp_number = (temp_number * 10) + 4;
            } else if (string_number.charAt(ii) == '5') {
                temp_number = (temp_number * 10) + 5;
            } else if (string_number.charAt(ii) == '6') {
                temp_number = (temp_number * 10) + 6;
            } else if (string_number.charAt(ii) == '7') {
                temp_number = (temp_number * 10) + 7;
            } else if (string_number.charAt(ii) == '8') {
                temp_number = (temp_number * 10) + 8;
            } else if (string_number.charAt(ii) == '9') {
                temp_number = (temp_number * 10) + 9;
            }
        }
        return (temp_number);
    }


    //view by id's and listeners initialisation
    private void setlistners()
    {

        julyInApp_logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bt_connection_state){
                    connectToBTorFirebase();
                }
            }
        });
        button_main_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_main_light)
                {
                    sendcommand(finalbtSocket, "s12");
                }else {
                    sendcommand(finalbtSocket, "s11");
                }
            }
        });
        button_img_main_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_main_light)
                {
                    sendcommand(finalbtSocket, "s12");
                }else {
                    sendcommand(finalbtSocket, "s11");
                }
            }
        });
        button_night_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_night_light)
                {
                    sendcommand(finalbtSocket, "s22");
                }else {
                    sendcommand(finalbtSocket, "s21");
                }
            }
        });
        button_img_night_light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_night_light)
                {
                    sendcommand(finalbtSocket, "s22");
                }else {
                    sendcommand(finalbtSocket, "s21");
                }
            }
        });
        button_fan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_fan)
                {
                    sendcommand(finalbtSocket, "s32");
                }else {
                    sendcommand(finalbtSocket, "s31");
                }
            }
        });
        button_img_fan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_fan)
                {
                    sendcommand(finalbtSocket, "s32");
                }else {
                    sendcommand(finalbtSocket, "s31");
                }
            }
        });
        button_halo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_halo) {
                    sendcommand(finalbtSocket, "s42");
                }else {
                    sendcommand(finalbtSocket, "s41");
                }
            }
        });
        button_img_halo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_halo) {
                    sendcommand(finalbtSocket, "s42");
                }else {
                    sendcommand(finalbtSocket, "s41");
                }
            }
        });
        button_pump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_pump)
                {
                    sendcommand(finalbtSocket, "s52");
                }else {
                    sendcommand(finalbtSocket, "s51");
                }
            }
        });
        button_img_pump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_pump)
                {
                    sendcommand(finalbtSocket, "s52");
                }else {
                    sendcommand(finalbtSocket, "s51");
                }
            }
        });
        button_led_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_1)
                {
                    sendcommand(finalbtSocket, "s62");
                }else {
                    sendcommand(finalbtSocket, "s61");
                }
            }
        });
        button_img_led_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_1)
                {
                    sendcommand(finalbtSocket, "s62");
                }else {
                    sendcommand(finalbtSocket, "s61");
                }
            }
        });
        button_led_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_2)
                {
                    sendcommand(finalbtSocket, "s72");
                }else {
                    sendcommand(finalbtSocket, "s71");
                }
            }
        });
        button_img_led_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_2)
                {
                    sendcommand(finalbtSocket, "s72");
                }else {
                    sendcommand(finalbtSocket, "s71");
                }
            }
        });
        button_led_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_3)
                {
                    sendcommand(finalbtSocket, "s82");
                }else {
                    sendcommand(finalbtSocket, "s81");
                }
            }
        });
        button_img_led_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(button_flag_led_3)
                {
                    sendcommand(finalbtSocket, "s82");
                }else {
                    sendcommand(finalbtSocket, "s81");
                }
            }
        });
        colorpicker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    bitmap=colorpicker.getDrawingCache();
                    int pixel_color=bitmap.getPixel((int)motionEvent.getX(),(int)motionEvent.getY());
                    r_val= Color.red(pixel_color);
                    g_val= Color.green(pixel_color);
                    b_val= Color.blue(pixel_color);
                    led_img_display.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(r_val,g_val,b_val)));
                }
                if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                    String color_temp="";

                    if(r_val==0)
                    {
                        color_temp="RGB,"+"R"+",";
                    }
                    else{
                        color_temp="RGB,"+r_val+",";
                    }
                    if(g_val==0)
                    {
                        color_temp=color_temp+"G"+",";
                    }
                    else{
                        color_temp=color_temp+g_val+",";
                    }
                    if(b_val==0)
                    {
                        color_temp=color_temp+"B";
                    }
                    else{
                        color_temp=color_temp+b_val;
                    }
                    sendcommand(finalbtSocket,color_temp);
                }
                return true;
            }
        });


    }
    private void findviewbyID()
    {


        julyInApp_logo=findViewById(R.id.julyInApp_logo);
        button_main_light=findViewById(R.id.button_main_light);
        button_img_main_light=findViewById(R.id.button_img_main_light);
        button_night_light=findViewById(R.id.button_night_light);
        button_img_night_light=findViewById(R.id.button_img_night_light);
        button_fan=findViewById(R.id.button_fan);
        button_img_fan=findViewById(R.id.button_img_fan);
        button_led_1=findViewById(R.id.button_led_1);
        button_img_led_1=findViewById(R.id.button_img_led_1);
        button_led_2=findViewById(R.id.button_led_2);
        button_img_led_2=findViewById(R.id.button_img_led_2);
        button_led_3=findViewById(R.id.button_led_3);
        button_img_led_3=findViewById(R.id.button_img_led_3);

        button_halo=findViewById(R.id.button_halo);
        button_img_halo=findViewById(R.id.button_img_halo);
        button_pump=findViewById(R.id.button_pump);
        button_img_pump=findViewById(R.id.button_img_pump);

        lut_date=findViewById(R.id.lut_date);
        lut_hr=findViewById(R.id.lut_hr);
        lut_min=findViewById(R.id.lut_min);
        lut_sec=findViewById(R.id.lut_sec);

        bluetooth_icon=findViewById(R.id.bluetooth_icon);
        wifi_icon=findViewById(R.id.wifi_icon);
        sd_icon=findViewById(R.id.sd_icon);

        water_pump_level=findViewById(R.id.water_pump_level);
        water_pump_level.setProgressValue(0);

        led_img_display=findViewById(R.id.led_img_display);
        colorpicker=findViewById(R.id.colorpicker);

        tempviewdata=findViewById(R.id.tempviewdata);
        humidviewdata=findViewById(R.id.humidviewdata);
        tempindexviewdata=findViewById(R.id.tempindexviewdata);


        message_firebase=findViewById(R.id.message_firebase);
        message_bt=findViewById(R.id.message_bt);
    }

    //un-used rest for program
    public void rest(long timerest)
    {

        try {
            Thread.sleep(timerest);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //handler to handle the message revieve
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what)
            {
                case STATE_CONNECTED:
                    message_bt.setText("Bluetooth Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    message_bt.setText("Bluetooth Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readbuff=(byte[]) msg.obj;
                    String readmsg=new String(readbuff,0,msg.arg1);
                    analyse_msg(readmsg);
                    break;
            }
            return true;
        }
    });


    //bluetooth connections and send and recieve functions
    public BluetoothSocket getBtsocket(BluetoothAdapter temp_btAdapter)
    {
        BluetoothDevice btDevice=temp_btAdapter.getRemoteDevice("00:19:10:08:14:16");
        //System.out.println(btDevice.getName());
        try {
            temp_btAdapter.cancelDiscovery();
            btsocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btsocket.connect();
            //System.out.println(btsocket);
            //System.out.println(btsocket.isConnected());
            bt_connection_state=true;
            Message connection_state=Message.obtain();
            connection_state.what=STATE_CONNECTED;
            handler.sendMessage(connection_state);
        }
        catch (IOException e) {
            e.printStackTrace();
            btDevice=temp_btAdapter.getRemoteDevice("00:19:10:08:16:A5");
            //System.out.println(btDevice.getName());
            try {
                temp_btAdapter.cancelDiscovery();
                btsocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                btsocket.connect();
                //System.out.println(btsocket);
                //System.out.println(btsocket.isConnected());
                bt_connection_state=true;
                Message connection_state=Message.obtain();
                connection_state.what=STATE_CONNECTED;
                handler.sendMessage(connection_state);
            }
            catch (IOException ee) {
                ee.printStackTrace();
                bt_connection_state=false;
                Message connection_state=Message.obtain();
                connection_state.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(connection_state);
                closeBluetooth();
            }
        }
        return btsocket;
    }


    public void sendcommand(BluetoothSocket tempbtSocket, String command)
    {
        try {
            outputStream = tempbtSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try
        {
            byte[] msgbuff=command.getBytes();
            outputStream.write( msgbuff);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private class SendRecieve extends Thread
    {
        private final BluetoothSocket BTsocket;

        public SendRecieve(BluetoothSocket socket)
        {
            BTsocket=socket;
            InputStream tempin=null;
            try {
                tempin=BTsocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            INPUTstream=tempin;

        }
        public void run()
        {

            int bytes=0;
            byte[] buffer=new byte[1024];
            while (true)
            {

                try {
                    bytes=INPUTstream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    bt_connection_state=false;
                    closeBluetooth();
                }
            }
        }
    }

    private void closeBluetooth() {
        try {
            if (outputStream != null)
                outputStream.close();
            if (INPUTstream != null)
                INPUTstream.close();
            if (finalbtSocket != null)
                finalbtSocket.close();
        } catch (IOException e) {
            message_bt.setText("Error closing Bluetooth: " + e.getMessage());
        }
    }

    private void set_data_from_firebase(String data_t){
        String tf_time=data_t.substring(0,20);
        tf_time=replace_all_delimiters_time(tf_time);

        String rest_data_t=data_t.substring(26,68);
        rest_data_t=rest_data_t.replaceAll("\\:",",");
        if(!bt_connection_state){
            set_time_data(tf_time);
            set_rest_data(rest_data_t);
            message_firebase.setText(data_t);
        }
        else{
            message_firebase.setText(data_t);
        }
    }

    private void set_time_data(String r_t){
        String[] split_array=r_t.split("\\s*,\\s*");
        lut_date.setText(split_array[0]);
        lut_hr.setText(split_array[1]);
        lut_min.setText(split_array[2]);
        lut_sec.setText(split_array[3]);

    }
    private void set_rest_data(String r_t){
        String[] split_array=r_t.split("\\s*,\\s*");
        set_temp_firebase(split_array);
        setTankWaterLevel(split_array[4]);
        setSwitches(split_array[7]);
        check_icons_firebase(split_array[8],split_array[9]);
    }
    private void check_icons_firebase(String icon_t1,String icon_t2){
        if(icon_t1.equals("CB")){
            bluetooth_icon.setVisibility(View.VISIBLE);
        }else if(icon_t1.equals("DB")){
            bluetooth_icon.setVisibility(View.GONE);
        }
        if(icon_t2.equals("SDA")){
            sd_icon.setVisibility(View.VISIBLE);
        }else if(icon_t2.equals("SDN")){
            sd_icon.setVisibility(View.GONE);
        }
    }
    private void set_temp_firebase(String[] temp_tf){
        String withunit="";
        if (set_temp_unit == 'C') {
            withunit = temp_tf[1].concat("°C");
            tempviewdata.setText(withunit);
            withunit = temp_tf[3].concat(" %");
            humidviewdata.setText(withunit);
            withunit = temp_tf[2].concat("°C");
            tempindexviewdata.setText(withunit);
        } else if (set_temp_unit == 'F') {
            double temp_inF = Double.parseDouble(temp_tf[2]);
            temp_inF = (temp_inF * 1.8) + 32;
            withunit = String.format("%.2f", temp_inF).concat("°F");
            tempviewdata.setText(withunit);
            withunit = tempdata[3].concat("  %");
            humidviewdata.setText(withunit);
            temp_inF = Double.parseDouble(tempdata[1]);
            temp_inF = (temp_inF * 1.8) + 32;
            withunit = String.format("%.2f", temp_inF).concat("°F");
            tempindexviewdata.setText(withunit);
        }
    }
    private String replace_all_delimiters_time(String r_t){
        String temp_t="";
        for(int i_temp_t=0; i_temp_t<r_t.length(); i_temp_t++){
            if(r_t.charAt(i_temp_t)==':'||r_t.charAt(i_temp_t)=='T'){
                temp_t=temp_t+",";
            }
            else if(r_t.charAt(i_temp_t)=='Z'){

            }else{
                temp_t=temp_t+r_t.charAt(i_temp_t);
            }
        }
        return temp_t;
    }

    //notification functions
    int global_variable_notification_id=2;

    int pump_notification_count=0;
    String prev_notification_pump="";

    int halo_notification_count=0;
    String prev_notification_halo="";

    public void notify_tank(String noti_s)
    {
        pump_notification_count+=1;
        if(pump_notification_count==1)
        {
            prev_notification_pump=noti_s;
            Notification notification=new NotificationCompat.Builder(this, com.example.july.notification.tank_notification_id)
                    .setSmallIcon(R.drawable.ic_tank)
                    .setContentTitle("Water Pump ")
                    .setContentText(noti_s)
                    // .setContentIntent(PendingIntent.getActivity(this, 0, (new Intent(this, MainApp.class)), 0))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setGroup("Pump_group")
                    .build();
            notificationManager.notify("July",global_variable_notification_id,notification);
            global_variable_notification_id+=1;
        }
        else if(pump_notification_count==2)
        {
            Notification notification=new NotificationCompat.Builder(this, com.example.july.notification.tank_notification_id)
                    .setSmallIcon(R.drawable.ic_tank)
                    .setContentTitle("Water Pump ")
                    .setContentText(noti_s)
                    // .setContentIntent(PendingIntent.getActivity(this, 0, (new Intent(this, MainApp.class)), 0))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setGroup("Pump_group")
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .build();

            Notification Summary_notification=new NotificationCompat.Builder(this, com.example.july.notification.tank_notification_id)
                    .setSmallIcon(R.drawable.house)
                    .setStyle(new NotificationCompat.InboxStyle()
                            .addLine(noti_s)
                            .addLine(prev_notification_pump)
                            .setBigContentTitle("2 new tasks")
                            .setSummaryText("Recent decisions taken."))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                    .setGroup("Pump_group")
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    .setGroupSummary(true)
                    .build();

            notificationManager.notify("July",global_variable_notification_id,notification);
            global_variable_notification_id+=1;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                notificationManager.notify("July",0,Summary_notification);
            }
            pump_notification_count=0;
        }

    }

    public void notify_halo(String noti_s)
    {
        halo_notification_count+=1;
        if(halo_notification_count==1)
        {
            prev_notification_halo=noti_s;
            Notification notification=new NotificationCompat.Builder(this, com.example.july.notification.halo_notification_id)
                    .setSmallIcon(R.drawable.ic_sunrise)
                    .setContentTitle("Halogen ")
                    .setContentText(noti_s)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                    .setGroup("Halogen_group")
                    .build();
            notificationManager.notify(global_variable_notification_id,notification);
            global_variable_notification_id+=1;
        }
        else if(halo_notification_count==2)
        {
            Notification notification=new NotificationCompat.Builder(this, com.example.july.notification.halo_notification_id)
                    .setSmallIcon(R.drawable.ic_sunrise)
                    .setContentTitle("Halogen.")
                    .setContentText(noti_s)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                    .setGroup("Halogen_group")
                    .build();

            Notification Summary_notification=new NotificationCompat.Builder(this, com.example.july.notification.halo_notification_id)
                    .setSmallIcon(R.drawable.house)
                    .setStyle(new NotificationCompat.InboxStyle()
                            .addLine(noti_s)
                            .addLine(prev_notification_halo)
                            .setBigContentTitle("2 new tasks")
                            .setSummaryText("Recent decisions taken."))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                    .setGroup("Halogen_group")
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    .setGroupSummary(true)
                    .build();

            notificationManager.notify(global_variable_notification_id,notification);
            global_variable_notification_id+=1;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                notificationManager.notify(1,Summary_notification);
            }
            halo_notification_count=0;
        }

    }


    //back button of phone function
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBluetooth();
    }

}
