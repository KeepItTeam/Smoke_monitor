package com.example.swr_v2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    TextView timerText;
    Button stopStartButton;
    Timer timer;
    TimerTask timerTask;
    Double time = 0.0;
    ListView listView;
    List list = new ArrayList();
    String regStr = new String();
    ArrayAdapter adapter;
    boolean timerStarted = false;
    JSONObject getJson = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerText = (TextView) findViewById(R.id.timerText);
        stopStartButton = (Button) findViewById(R.id.startStopButton);
        timer = new Timer();

        // получаем данные по прошлых походах покурить с сервера
        updateListView();

    }
    public void sendTestReq(View view) {
        String url = "http://192.168.0.116:3000/postdata2";
        JSONObject json = new JSONObject();
        try {
            json.put("name", "Matvei");
            json.put("job", "Programmer");
            sendPost(url, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void sendMessage(View view) {
        String url = "http://192.168.0.116:3000/test/lol"; // your URL
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // загрузка данных о куреве с сервера на listview
    private void updateListView()
    {

        String url = "http://192.168.0.116:3000/dataList";
        final RequestQueue queue = Volley.newRequestQueue(this);
        queue.start();
        AtomicReference<JSONObject> json = new AtomicReference<>();
        AtomicReference<String> data = null;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                new JSONObject(),
                response -> {
                    try {
                        listView = findViewById(R.id.list_view);
                        for(int i = 0; i < response.getJSONArray("data").length(); i++)
                            list.add(response.getJSONArray("data").getString(i));
                        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, list);
                        listView.setAdapter(adapter);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }, error -> {
                    System.out.println(error.toString());
                    System.out.println(error.getMessage());
                });
        queue.add(jsObjRequest);
    }

    private void sendPost(String url, JSONObject json) throws JSONException {
        final RequestQueue queue = Volley.newRequestQueue(this);
        queue.start();
        JsonObjectRequest jsObjRequest = new
                JsonObjectRequest(Request.Method.POST,
                url,
                json,
                response -> {
                    try {
                        System.out.println(response.getString("message"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
                    System.out.println(error.toString());
                    System.out.println(error.getMessage());
                });
        queue.add(jsObjRequest);
    }

    // Нажата кнопка GO SMOKE/STOP SMOKING
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startStopTapped(View view)
    {
        // ПОШЛИ КУРИТЬ
        if(timerStarted == false)
        {
            timerStarted = true;
            setButtonUI("STOP SMOKING", R.color.white);
            startTimer();
        }
        // ЗАКОНЧИЛИ КУРИТЬ
        else
        {
            timerStarted = false;
            setButtonUI("GO SMOKE", R.color.black);
            // Добавляем время сколько и когда покурили в ListView
            int hours, minutes, seconds;
            hours = (int) (time / 3600);
            minutes = (int)((time % 3600) / 60);
            seconds = (int)(time % 60);
            if(hours!=0)
                regStr = hours +"ч. " + minutes+ "мин. "+ seconds + "сек.";
            else if(minutes !=0)
                regStr = minutes+ "мин. "+ seconds + "сек.";
            else
                regStr = seconds + "сек.";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy --- HH:mm");
            LocalDateTime now = LocalDateTime.now();
            regStr += "---"+dtf.format(now);
            list.add(regStr);
            listView = findViewById(R.id.list_view);
            adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1,list);
            listView.setAdapter(adapter);
            // Обнуляем таймер
            timerTask.cancel();
            time = 0.0;
            timerText.setText(formatTime(0,0,0));

            // Отправляем запись(regStr) на сервер
            String url = "http://192.168.0.116:3000/registration";
            JSONObject json = new JSONObject();
            try {
                json.put("reg", regStr);
                sendPost(url, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void startTimer()
    {
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        time++;
                        timerText.setText(getTimerText());
                    }
                });
            }

        };
        timer.scheduleAtFixedRate(timerTask, 0 ,1000);
    }

    private void setButtonUI(String start, int color)
    {
        stopStartButton.setText(start);
        stopStartButton.setTextColor(ContextCompat.getColor(this, color));
    }

    private String getTimerText()
    {
        int rounded = (int) Math.round(time);

        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);

        return formatTime(seconds, minutes, hours);
    }

    private String formatTime(int seconds, int minutes, int hours)
    {
        return String.format("%02d",hours) + " : " + String.format("%02d",minutes) + " : " + String.format("%02d",seconds);
    }

    /* public class CallAPI extends AsyncTask<String, String, String> {

        public CallAPI(){
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            //String urlString = params[0]; // URL to call
            String urlString = "http://192.168.0.126:3000/";
            //String data = params[1]; //data to post
            String data = "LOLOLOLOLOLOLOL";
            OutputStream out = null;

            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();
                out.close();
                //urlConnection.setRequestMethod("POST");
                urlConnection.connect();

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return "";
        }
    }*/

}