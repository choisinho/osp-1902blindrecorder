package app.bqlab.febblindrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //constants
    final int FOCUS_VOICE_MEMO = 0;     //음성 메모
    final int FOCUS_INSTANT_PLAY = 1;   //파일 바로 재생
    final int FOCUS_SEARCH_MEMO = 2;    //메모 찾기
    final int FOCUS_USER_CHANGE = 3;    //사용자 변경
    final int FOCUS_APP_EXIT = 4;       //종료
    //variables
    String fileDir;
    List<String> filePathes;
    int focus, soundMenuEnd, soundDisable;
    boolean playing;
    //layouts
    LinearLayout main, mainBody;
    List<View> mainBodyButtons;
    //objects
    File mFile;
    TextToSpeech mTTS;
    MediaPlayer mPlayer;
    MediaRecorder mRecorder;
    SoundPool mSoundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        init();
        resetFocus();
        checkDirectory();
        setupSoundPool();
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
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        mSoundPool.release();
        mSoundPool = null;
    }

    private void init() {
        //initialization
        main = findViewById(R.id.main);
        mainBody = findViewById(R.id.main_body);
        mainBodyButtons = new ArrayList<View>();
        //setting
        for (int i = 0; i < mainBody.getChildCount(); i++)
            mainBodyButtons.add(mainBody.getChildAt(i));
        findViewById(R.id.main_bot_up).setOnClickListener(new View.OnClickListener() {
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
        findViewById(R.id.main_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus++;
                if (focus > mainBodyButtons.size() - 1) {
                    mSoundPool.play(soundMenuEnd, 1, 1, 0, 0, 1);
                    focus = mainBodyButtons.size() - 1;
                }
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.main_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case FOCUS_VOICE_MEMO:
                        shutupTTS();
                        checkDirectory();
                        startActivity(new Intent(MainActivity.this, RecordActivity.class));
                        stopPlaying();
                        break;
                    case FOCUS_INSTANT_PLAY:
                        shutupTTS();
                        checkDirectory();
                        playRecentFile();
                        break;
                    case FOCUS_SEARCH_MEMO:
                        shutupTTS();
                        checkDirectory();
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        stopPlaying();
                        break;
                    case FOCUS_USER_CHANGE:
                        shutupTTS();
                        break;
                    case FOCUS_APP_EXIT:
                        shutupTTS();
                        finishAffinity();
                }
            }
        });
        findViewById(R.id.main_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
                shutupTTS();
            }
        });
        findViewById(R.id.main_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
            }
        });
        findViewById(R.id.main_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
            }
        });
    }

    private void checkDirectory() {
        fileDir = Environment.getExternalStorageDirectory() + File.separator + "음성메모장";
        mFile = new File(fileDir);
        boolean success;
        if (!mFile.exists())
            success = mFile.mkdir();
    }

    private void resetFocus() {
        for (int i = 0; i < mainBodyButtons.size(); i++) {
            if (i != focus) {
                //포커스가 없는 버튼 처리
                mainBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button));
            } else {
                //포커스를 가진 버튼 처리
                mainBodyButtons.get(i).setBackground(getDrawable(R.drawable.app_button_focussed));
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
                        Toast.makeText(MainActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
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
                    speak("홈메뉴");
                    Thread.sleep(1000);
                    speakFocus();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void speakFocus() {
        final Button button = (Button) mainBodyButtons.get(focus);
        speak(button.getText().toString());
    }

    private void playRecentFile() {
        if (!playing) {
            loadFiles();
            final String path = filePathes.get(filePathes.size() - 1);
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(path);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            disablelayouts(false, main);
            playing = true;
            try {
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(path);
                mPlayer.prepare();
                mPlayer.start();
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playing = false;
                        disablelayouts(true, main);
                        setupTTS();
                        speakFocus();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFiles() {
        //디렉토리의 파일을 파일 리스트로 불러옴
        filePathes = new ArrayList<>();
        String[] names = mFile.list();
        for (String name : names) {
            filePathes.add(Environment.getExternalStorageDirectory() + File.separator + "음성메모장" + File.separator + name);
        }
    }

    private void stopPlaying() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer = null;
        }
    }

    private void requestPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private void disablelayouts(boolean enable, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                disablelayouts(enable, (ViewGroup) child);
            }
        }
    }
}