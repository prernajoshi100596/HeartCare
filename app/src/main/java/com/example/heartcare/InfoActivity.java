package com.example.heartcare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class InfoActivity extends AppCompatActivity {

    Button submitBtn;
    EditText ageET, heightET, weightET, cholestrolET, glucoseET;
    RadioGroup genderRG, alcoRG, smokeRG, activeRG;

    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        submitBtn = (Button)findViewById(R.id.submit);
        ageET = (EditText)findViewById(R.id.age);
        heightET = (EditText)findViewById(R.id.height);
        weightET = (EditText)findViewById(R.id.weight);
        genderRG = (RadioGroup)findViewById(R.id.radioGrp);
        alcoRG = (RadioGroup)findViewById(R.id.radioGrpAlco);
        smokeRG = (RadioGroup)findViewById(R.id.radioGrpSmoke);
        activeRG = (RadioGroup)findViewById(R.id.radioGrpActive);
        cholestrolET = (EditText)findViewById(R.id.cholestrol);
        glucoseET = (EditText)findViewById(R.id.glucose);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(checkDetails() && user!=null){
                Intent i = new Intent(InfoActivity.this, HeartActivity.class);
                i.putExtra("user", user);
                startActivity(i);
                finish();
            }else{
                Toast.makeText(InfoActivity.this, "check your data again", Toast.LENGTH_SHORT).show();
            }
            }
        });
    }

    public boolean checkDetails(){
        String age = ageET.getText().toString();
        String ht = heightET.getText().toString();
        String wt = weightET.getText().toString();
        String cholestrol =  cholestrolET.getText().toString(); cholestrol = Character.toString(cholestrol.charAt(cholestrol.length()-1));
        String glucose = glucoseET.getText().toString();        glucose = Character.toString(glucose.charAt(glucose.length()-1));

        double ageNum, htNum, wtNum, gender = -1.0, cholestrolNum = 1.0, glucoseNum = 1.0, active = -1.0, smoke = -1.0, alco = -1.0;
        try {
            ageNum = Integer.parseInt(age);
            htNum = Float.parseFloat(ht);
            wtNum = Float.parseFloat(wt);
            cholestrolNum = Integer.parseInt(cholestrol);
            glucoseNum = Integer.parseInt(glucose);

            int selectedId = genderRG.getCheckedRadioButtonId();
            RadioButton genderRB = findViewById(selectedId);

            if(genderRB.getText().toString().trim().equals("Male"))
                gender = 2.0;
            else if(genderRB.getText().toString().trim().equals("Female"))
                gender = 1.0;

            selectedId = alcoRG.getCheckedRadioButtonId();
            RadioButton alcoRB = findViewById(selectedId);

            if(alcoRB.getText().toString().trim().equals("YES"))
                alco = 1.0;
            else if(alcoRB.getText().toString().trim().equals("NO"))
                alco = 0.0;

            selectedId = smokeRG.getCheckedRadioButtonId();
            RadioButton smokeRB = findViewById(selectedId);

            if(smokeRB.getText().toString().trim().equals("YES"))
                smoke = 1.0;
            else if(smokeRB.getText().toString().trim().equals("NO"))
                smoke = 0.0;

            selectedId = activeRG.getCheckedRadioButtonId();
            RadioButton activeRB = findViewById(selectedId);

            if(activeRB.getText().toString().trim().equals("YES"))
                active = 1.0;
            else if(activeRB.getText().toString().trim().equals("NO"))
                active = 0.0;

            System.out.println("AGE:"+ageNum);
            System.out.println("HT:"+htNum);
            System.out.println("WT:"+wtNum);
            System.out.println("GENDER:"+gender);
            System.out.println("SMOKE:"+smoke);
            System.out.println("ALCOHOL:"+alco);
            System.out.println("ACTIVE:"+active);
            System.out.println("CHOLESTEROL:"+cholestrol);
            System.out.println("GLUCOSE:"+glucose);


            if(ageNum>=1 && ageNum<=110 && htNum>=4 && htNum<=12 && wtNum>=20 && wtNum<=200 && (gender==1 || gender==2) && (alco==1 || alco==0) && (smoke==1 || smoke==0) && (active==1 || active==0) && cholestrolNum>=1 && cholestrolNum<=3 && glucoseNum>=1 && glucoseNum<=3){
                user = new User(htNum, wtNum, gender, ageNum, cholestrolNum, glucoseNum, smoke, alco, active);
//                System.out.println(user);
                return true;
            }



        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Incorrect details", Toast.LENGTH_SHORT).show();
            return false;
        }

        return false;
    }
}
