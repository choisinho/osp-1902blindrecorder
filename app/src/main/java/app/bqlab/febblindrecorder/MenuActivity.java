package app.bqlab.febblindrecorder;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    //constants
    final int FILE_SAVE = 0;          //저장
    final int RESUME_RECORD = 1;      //이어서 녹음
    final int RE_RECORD = 2;          //재 녹음
    final int RETURN_MAIN = 3;        //메뉴로 돌아가기
    final int SPEECH_TO_TEXT = 1000;  //STT 데이터 요청
    //variables
    int focus;
    boolean playing;
    //layouts
    LinearLayout menuBody;
    List<View> menuBodyButtons;
    //objects
    TextToSpeech mTTS;
    MediaPlayer mPlayer;
    MediaRecorder mRecorder;
    app.bqlab.febblindrecorder.VoiceMemoManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        init();
        checkTTS();
        speakFirst();
        resetFocus();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SPEECH_TO_TEXT) {
                if (data != null) {
                    ArrayList<String> input = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                    if (input.get(0).equals("전체"))
//                        checkPermission();
//                    else
//                        searchFiles(input.get(0));
                }
            }
        }
    }

    private void init() {
        //initialization
        menuBody = findViewById(R.id.menu_body);
        menuBodyButtons = new ArrayList<View>();
        //setting
        for (int i = 0; i < menuBody.getChildCount(); i++)
            menuBodyButtons.add(menuBody.getChildAt(i));
        findViewById(R.id.menu_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus--;
                if (focus <= 0)
                    focus = 0;
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.menu_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus++;
                if (focus >= menuBodyButtons.size() - 1)
                    focus = menuBodyButtons.size() - 1;
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.menu_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.menu_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.menu_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.menu_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuActivity.super.onBackPressed();
            }
        });
    }

    private void resetFocus() {
        for (int i = 0; i < menuBodyButtons.size(); i++) {
            if (i != focus) {
                //포커스가 없는 버튼 처리
                menuBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button));
            } else {
                //포커스를 가진 버튼 처리
                menuBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button_focussed));
            }
        }
    }

    private void checkTTS() {
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MenuActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(MenuActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
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
                    speak("노옥음메뉴");
                    Thread.sleep(1500);
                    speak("저장");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void speakFocus() {
        final Button button = (Button) menuBodyButtons.get(focus);
        speak(button.getText().toString());
    }

    private void requestSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "검색어를 말씀하세요.");
        startActivityForResult(intent, SPEECH_TO_TEXT);
    }
}