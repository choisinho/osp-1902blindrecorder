package app.bqlab.febblindrecorder;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
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
    int focus, soundMenuEnd, soundDisable;
    boolean allowedExit;
    String fileName, fileDir;
    List<String> speech;
    //objects
    TextToSpeech mTTS;
    SoundPool mSoundPool;
    //layouts
    LinearLayout menuBody;
    List<View> menuBodyButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        init();
        resetFocus();
        setupSoundPool();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTTS();
        speakFirst();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutupTTS();
        if (!allowedExit) {
            File file = new File(fileDir, fileName);
            boolean success = file.delete();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SPEECH_TO_TEXT) {
                if (data != null) {
                    speech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    switch (focus) {
                        case FILE_SAVE:
                            String newName = speech.get(0);
                            File file = new File(fileDir, fileName);
                            if (file.exists()) {
                                File renamedFile = new File(fileDir + File.separator, newName + ".mp4");
                                if (file.renameTo(renamedFile)) {
                                    Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            } else {
                                new AlertDialog.Builder(this)
                                        .setCancelable(false)
                                        .setMessage("녹음파일이 삭제되었거나 임의로 수정되었습니다.")
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        }).show();
                            }
                    }
                }
            }
        }
    }

    private void init() {
        //initialization
        menuBody = findViewById(R.id.menu_body);
        menuBodyButtons = new ArrayList<View>();
        fileName = getIntent().getStringExtra("fileName");
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        //setting
        for (int i = 0; i < menuBody.getChildCount(); i++)
            menuBodyButtons.add(menuBody.getChildAt(i));
        findViewById(R.id.menu_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus--;
                if (focus < 0) {
                    mSoundPool.play(soundMenuEnd, 1, 1, 0, 0, 1);
                    focus = 0;
                }
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.menu_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus++;
                if (focus > menuBodyButtons.size() - 1) {
                    focus = menuBodyButtons.size() - 1;
                    mSoundPool.play(soundMenuEnd, 1, 1, 0, 0, 1);
                }
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.menu_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, RecordActivity.class);
                i.putExtra("fileName", fileName);
                startActivity(i);
                finish();
            }
        });
        findViewById(R.id.menu_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
            }
        });
        findViewById(R.id.menu_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case FILE_SAVE:
                        requestSpeech();
                        break;
                    case RESUME_RECORD:
                        allowedExit = true;
                        Intent i = new Intent(MenuActivity.this, RecordActivity.class);
                        i.putExtra("fileName", fileName);
                        startActivity(i);
                        finish();
                        break;
                    case RE_RECORD:
                        allowedExit = false;
                        startActivity(new Intent(MenuActivity.this, RecordActivity.class));
                        finish();
                        break;
                    case RETURN_MAIN:
                        allowedExit = false;
                        finish();
                        break;
                }
            }
        });
        findViewById(R.id.menu_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, RecordActivity.class);
                i.putExtra("fileName", fileName);
                startActivity(i);
                finish();
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

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();
        soundMenuEnd = mSoundPool.load(this, R.raw.app_sound_menu_end, 0);
        soundDisable = mSoundPool.load(this, R.raw.app_sound_disable, 0);
    }

    private void setupTTS() {
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

    private void shutupTTS() {
        mTTS.stop();
        mTTS.shutdown();
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
                    speak("녹음메뉴");
                    Thread.sleep(1500);
                    speakFocus();
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
        speak("파일명을 말하세요.");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "파일명을 말하세요.");
        startActivityForResult(intent, SPEECH_TO_TEXT);
    }
}