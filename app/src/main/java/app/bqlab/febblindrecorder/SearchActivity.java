package app.bqlab.febblindrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    //constants
    final int SPEECH_TO_TEXT = 1000;
    //variables
    int focus;
    String fileDir;
    //objects
    TextToSpeech mTTS;
    //layouts
    LinearLayout searchBody;
    List<View> searchBodyButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        init();
        setupTTS();
        speakFirst();
        resetFocus();
    }

    private void init() {
        //initialize
        searchBody = findViewById(R.id.search_body);
        searchBodyButtons = new ArrayList<View>();
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        //setup
        for (int i = 0; i < searchBody.getChildCount(); i++)
            searchBodyButtons.add(searchBody.getChildAt(i));
        findViewById(R.id.search_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus--;
                if (focus <= 0)
                    focus = 0;
                Log.d("버튼개수", String.valueOf(searchBodyButtons));
                Log.d("인덱스", String.valueOf(focus));
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.search_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus++;
                if (focus >= searchBodyButtons.size() - 1)
                    focus = searchBodyButtons.size() - 1;
                Log.d("버튼개수", String.valueOf(searchBodyButtons));
                Log.d("인덱스", String.valueOf(focus));
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.search_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.search_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.search_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.search_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void resetFocus() {
        for (int i = 0; i < searchBodyButtons.size(); i++) {
            if (i != focus) {
                //포커스가 없는 버튼 처리
                searchBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button));
            } else {
                //포커스를 가진 버튼 처리
                searchBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button_focussed));
            }
        }
    }

    private void setupTTS() {
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(SearchActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                    finishAffinity();
                }
            }
        });
        mTTS.setPitch(0.7f);
        mTTS.setSpeechRate(1.2f);
    }

    private void speak(String text) {
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void speakFirst() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    speak("파일찾기메뉴");
                    Thread.sleep(1500);
                    speak("파일 이름으로 찾기");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void speakFocus() {
        final Button button = (Button) searchBodyButtons.get(focus);
        speak(button.getText().toString());
    }

    private void requestSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "파일명을 말하세요.");
        startActivityForResult(intent, SPEECH_TO_TEXT);
    }
}
