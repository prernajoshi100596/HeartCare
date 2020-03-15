package com.example.heartcare;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private TextView resultTV, heartTV;
    private ImageView centerImage;
    private String url = "";
    private User user;
    private Intent i;

    private static int SP = 0, DP = 0, HR = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultTV = (TextView)findViewById(R.id.resultText);
        centerImage = (ImageView)findViewById(R.id.centerimg);
        heartTV = (TextView)findViewById(R.id.heartData);

        url = "http://13.233.193.154:8080/heart";

        i = getIntent();
        user = (User) i.getSerializableExtra("user");
        SP = i.getIntExtra("SP", 0);
        DP = i.getIntExtra("DP", 0);
        HR = i.getIntExtra("HR", 0);

        heartTV.setText("PULSE RATE : "+HR+" bpm"+"\n"+
                "BLOOD PRESSURE : "+SP+" - "+DP+" mmHg");
        passParameters();
    }

    private void passParameters() {
        requestQueue = Volley.newRequestQueue(this);
        JSONObject params = new JSONObject();
        try{
            params.put("age",Double.toString(user.getAge()));
            params.put("gender",Double.toString(user.getGender()));
            params.put("height",Double.toString(user.getHeight()));
            params.put("weight",Double.toString(user.getWeight()));
            params.put("s_bp",Integer.toString(SP));
            params.put("d_bp",Integer.toString(DP));
            params.put("cholestrol",Double.toString(user.getCholesterol()));
            params.put("gluc",Double.toString(user.getGlucose()));
            params.put("smoke",Double.toString(user.getSmoke()));
            params.put("alco",Double.toString(user.getAlco()));
            params.put("active",Double.toString(user.getActive()));

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON object creation error", Toast.LENGTH_SHORT).show();
        }

        final String tempResp = "The probability of having or to have a Cardiovascular Disease is: ";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try{
                        String responseString = response.getString("result");
                        double res = Double.parseDouble(responseString);
                        res = 1 - res;
                        res = Math.round(res * 10000.0) / 100.0;

                        if(res>=50){
                            resultTV.setText(tempResp+""+res+"%\nYou must visit a doctor to check it :(");
                            centerImage.setBackgroundResource(R.drawable.res3);
                        }else if(res<50 && res>=30){
                            resultTV.setText(tempResp+""+res+"%\nProbably you are healthy :/");
                            centerImage.setBackgroundResource(R.drawable.res2);
                        }else{
                            resultTV.setText(tempResp+""+res+"%\nYou are totally healthy :)");
                            centerImage.setBackgroundResource(R.drawable.res);
                        }
                    }catch (Exception e){
                        Toast.makeText(ResultActivity.this, "Waiting for result", Toast.LENGTH_SHORT).show();
                    }

//                    resultTV.setText(response.toString());
//                    System.out.println(response);
                }
             }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ResultActivity.this, "| Check your Internet |"+error.toString(), Toast.LENGTH_SHORT).show();
//                System.out.println("| Check your Internet |"+error.toString());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
