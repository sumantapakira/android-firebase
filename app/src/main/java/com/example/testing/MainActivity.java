package com.example.testing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.widget.Toast;
import java.util.ArrayList;
import android.widget.TextView;
import android.util.Log;
import android.net.Uri;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;
import android.provider.Browser;
import android.util.Base64;
public class MainActivity extends AppCompatActivity {

    private TextToSpeech mTTS;
    private EditText mEditText;
    private SeekBar mSeekBarPitch;
    private SeekBar mSeekBarSpeed;
    private Button mButtonSpeak;
    private final int REQ_CODE = 100;
    TextView textView;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener fireAuthListener;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        //get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, " user : "+user.getEmail() );
        Log.d(TAG, " user : "+user.getDisplayName() );

        fireAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user1 = firebaseAuth.getCurrentUser();

                if (user1 == null) {
                    //user not login
                    MainActivity.this.startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    MainActivity.this.finish();
                }
            }
        };


        mButtonSpeak = findViewById(R.id.button_speak);

        textView = findViewById(R.id.displaytext);

        ImageView speak = findViewById(R.id.speak);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry your device not supported",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.d(TAG, " TextToSpeech : " );
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        mButtonSpeak.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });


        mEditText = findViewById(R.id.edit_text);
        mSeekBarPitch = findViewById(R.id.seek_bar_pitch);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed);

        speak();

        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, " onClick : " );
                speak();
            }
        });
    }

    private void speak() {
        Log.d(TAG, "On speak is: " );

        String text = "hello world";
        float pitch = (float) mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);

        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d(TAG, result.get(0).toString() );
                    textView.setText(result.get(0).toString());
                    String replyText = null;
                    if(result.get(0).toString().contains("approve")){
                        replyText = "Good Decision, Would you like to plublish now or later?";
                    }else if(result.get(0).toString().contains("reject")){
                        replyText = "Seems you are not in a good mood today";
                        // call AEM servlet
                        // scheduleWorkItem(String userName, String itemId, String time, String decision);
                    }else if(result.get(0).toString().contains("open")){

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.8.133:4502/content/we-retail/language-masters/en.html?wcmmode=disabled"));

                        String authorization = "admin" + ":" + "admin";
                        String authorizationBase64 = Base64.encodeToString(authorization.getBytes(), Base64.NO_WRAP);

                        Bundle bundle = new Bundle();
                        bundle.putString("Authorization", "Basic " + authorizationBase64);
                        browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);

                        startActivity(browserIntent);

                    }else if(result.get(0).toString().matches(".*\\d.*")){
                        replyText = "The item has been scheduled later!";
                        // call AEM servlet
                        // scheduleWorkItem(String userName, String itemId, String time, String decision);
                    }else if(result.get(0).toString().contains("later") || result.get(0).toString().contains("schedule")){
                        replyText = "What time would like to schedule?";
                    }
                    else{
                        replyText = "Sorry, I do not understand that!";
                    }

                    float pitch = (float) mSeekBarPitch.getProgress() / 50;
                    if (pitch < 0.1) pitch = 0.1f;
                    float speed = (float) mSeekBarSpeed.getProgress() / 50;
                    if (speed < 0.1) speed = 0.1f;

                    mTTS.setPitch(pitch);
                    mTTS.setSpeechRate(speed);

                    mTTS.speak(replyText, TextToSpeech.QUEUE_FLUSH, null);
                }
                break;
            }
        }
    }
}
