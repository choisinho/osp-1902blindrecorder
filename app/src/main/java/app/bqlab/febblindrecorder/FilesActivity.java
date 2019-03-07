package app.bqlab.febblindrecorder;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilesActivity extends AppCompatActivity {

    //variables
    int focus, soundMenuEnd, soundDisable;
    String fileDir;
    String[] fileNames;
    //objects
    TextToSpeech mTTS;
    SoundPool mSoundPool;
    //layouts
    LinearLayout filesBody;
    List<FileLayout> filesBodyLayouts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        init();
        loadFiles();
        setupTTS();
        resetFocus();
        setupSoundPool();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                clickRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                clickLeft();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                clickUp();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                clickDown();
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
                clickVToggle();
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                clickXToggle();
                return true;
            default:
                return true;
        }
    }

    private void init() {
        //initialize
        filesBody = findViewById(R.id.files_body);
        filesBodyLayouts = new ArrayList<FileLayout>();
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        //setup
        findViewById(R.id.files_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickUp();
            }
        });
        findViewById(R.id.files_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDown();
            }
        });
        findViewById(R.id.files_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLeft();
            }
        });
        findViewById(R.id.files_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRight();
            }
        });
        findViewById(R.id.files_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickVToggle();
            }
        });
        findViewById(R.id.files_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickXToggle();
            }
        });
    }

    private void clickUp() {
        focus--;
        if (focus < 0) {
            mSoundPool.play(soundMenuEnd, 1, 1, 0, 0, 1);
            focus = 0;
        }
        speakFocus();
        resetFocus();
    }

    private void clickDown() {
        focus++;
        if (focus > filesBodyLayouts.size() - 1) {
            mSoundPool.play(soundMenuEnd, 1, 1, 0, 0, 1);
            focus = filesBodyLayouts.size() - 1;
        }
        speakFocus();
        resetFocus();
    }

    private void clickLeft() {
        startActivity(new Intent(FilesActivity.this, SearchActivity.class));
        finish();
    }

    private void clickRight() {
        String fileName = fileNames[focus];
        if (new File(fileDir, fileName).exists()) {
            Intent i = new Intent(FilesActivity.this, PlayActivity.class);
            i.putExtra("fileName", fileName);
            i.putExtra("flag", "list");
            startActivity(i);
        } else {
            Toast.makeText(FilesActivity.this, "파일이 존재하지 않습니다.", Toast.LENGTH_LONG).show();
            loadFiles();
        }
    }

    private void clickVToggle() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void clickXToggle() {
        startActivity(new Intent(FilesActivity.this, SearchActivity.class));
        finish();
    }

    private void loadFiles() {
        filesBody.removeAllViews();
        File dir = new File(fileDir);
        fileNames = dir.list();
        if (fileNames.length != 0) {
            for (int i = 0; i < fileNames.length; i++) {
                FileLayout fileLayout = new FileLayout(this, String.valueOf(i + 1), fileNames[i]);
                filesBodyLayouts.add(fileLayout);
                filesBody.addView(fileLayout);
            }
        }
    }

    private void resetFocus() {
        for (int i = 0; i < filesBodyLayouts.size(); i++) {
            if (i != focus) {
                //포커스가 없는 버튼 처리
                filesBodyLayouts.get(i).setColor(getDrawable(R.drawable.app_button));
            } else {
                //포커스를 가진 버튼 처리
                filesBodyLayouts.get(i).setColor(getDrawable(R.drawable.app_button_focussed));
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
                        Toast.makeText(FilesActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(FilesActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
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
                    speak("파일목록");
                    Thread.sleep(1500);
                    speakFocus();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void speakFocus() {
        final Button button = filesBodyLayouts.get(focus).getButton();
        speak(String.valueOf(focus + 1) + "번파일." + button.getText().toString().replace(".mp4", ""));
    }
}
