package app.bqlab.febblindrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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
    final int SEARCH_BY_NAME = 0;       //파일 이름으로 찾기
    final int SEARCY_BY_LIST = 1;       //파일 목록
    final int SPEECH_TO_TEXT = 1000;
    //variables
    int focus;
    String fileDir;
    ArrayList<String> speech;
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
        resetFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTTS();
        speakFirst();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shutupTTS();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutupTTS();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SPEECH_TO_TEXT) {
                if (data != null) {
                    speech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    switch (focus) {
                        case SEARCH_BY_NAME:
                            String fileName = speech.get(0) + ".mp4";
                            if (new File(fileDir, fileName).exists()) {
                                Intent i = new Intent(this, PlayActivity.class);
                                i.putExtra("fileName", fileName);
                                i.putExtra("flag", "name");
                                startActivity(i);
                            }
                            break;
                    }
                }
            }
        }
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
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.search_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SearchActivity.this, MainActivity.class));
                finish();
            }
        });
        findViewById(R.id.search_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case SEARCH_BY_NAME:
                        requestSpeech();
                        break;
                    case SEARCY_BY_LIST:
                        startActivity(new Intent(SearchActivity.this, FilesActivity.class));
                        break;
                }
            }
        });
        findViewById(R.id.search_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case SEARCH_BY_NAME:
                        requestSpeech();
                        break;
                    case SEARCY_BY_LIST:
                        startActivity(new Intent(SearchActivity.this, FilesActivity.class));
                        finish();
                        break;
                }
            }
        });
        findViewById(R.id.search_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SearchActivity.this, MainActivity.class));
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

    private void shutupTTS() {
        mTTS.shutdown();
        mTTS.stop();
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
                    speakFocus();
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
