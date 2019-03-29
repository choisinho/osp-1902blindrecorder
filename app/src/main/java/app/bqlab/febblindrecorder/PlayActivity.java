package app.bqlab.febblindrecorder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

public class PlayActivity extends AppCompatActivity {

    //variables
    int soundDisable;
    boolean playing, speaking;
    String fileName, fileDir, filePath;
    //objects
    File mFile;
    TextToSpeech mTTS;
    MediaPlayer mPlayer;
    MediaRecorder mRecorder;
    HashMap<String, String> mTTSMap;
    SoundPool mSoundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        init();
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
        stopPlaying();
        shutupTTS();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlaying();
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
            case KeyEvent.KEYCODE_BUTTON_X:
                clickVToggle();
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                clickXToggle();
                return true;
            default:
                return true;
        }
    }

    private void init() {
        //check
        //initialize
        fileName = getIntent().getStringExtra("fileName");
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        mFile = new File(fileDir, fileName);
        //setup
        findViewById(R.id.play_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickUp();
            }
        });
        findViewById(R.id.play_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDown();
            }
        });
        findViewById(R.id.play_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLeft();
            }
        });
        findViewById(R.id.play_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRight();
            }
        });
        findViewById(R.id.play_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickVToggle();
            }
        });
        findViewById(R.id.play_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickXToggle();
            }
        });
    }

    private void clickUp() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void clickDown() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void clickLeft() {
        String flag = getIntent().getStringExtra("flag");
        if (flag.equals("list")) {
            startActivity(new Intent(PlayActivity.this, FilesActivity.class));
            finish();
        } else if (flag.equals("name")) {
            startActivity(new Intent(PlayActivity.this, SearchActivity.class));
            finish();
        }
    }

    private void clickRight() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void clickVToggle() {
        shutupTTS();
        if (playing)
            stopPlaying();
        else
            startPlaying();
    }

    private void clickXToggle() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();
        soundDisable = mSoundPool.load(this, R.raw.app_sound_disable, 0);
    }

    private void setupTTS() {
        mTTSMap = new HashMap<String, String>();
        mTTSMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "unique_id");
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(PlayActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(PlayActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                    finishAffinity();
                }
            }
        });
        mTTS.setPitch(0.7f);
        mTTS.setSpeechRate(1.2f);
        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                speaking = true;
            }

            @Override
            public void onDone(String utteranceId) {
                try {
                    Thread.sleep(2000);
                    speaking = false;
                    startPlaying();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String utteranceId) {
                speaking = false;
            }
        });
    }

    private void shutupTTS() {
        mTTS.shutdown();
        mTTS.stop();
    }

    private void speak(String text) {
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, mTTSMap);
    }

    private void speakFirst() {
        new Thread(new Runnable() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    //STT로 검색하여 이 화면에 도달하였을 경우 추가적으로 음성 출력
                    if (getIntent().getStringExtra("searchResult") != null) {
                        speak(getIntent().getStringExtra("searchResult"));
                        Thread.sleep(1500);
                    }
                    //파일 정보 음성으로 출력
                    speak(fileName.replace(".mp4", "") + new SimpleDateFormat("yyyy년 MM월 dd일").format(mFile.lastModified()));
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startPlaying() {
        if (!playing) {
            //파일 경로 지정
            filePath = fileDir + File.separator + fileName;
            //MediaRecorder 속성 세팅 (확장자, 코덱 등)
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(filePath);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //녹음파일 MediaPlayer를 활용하여 재생
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        playing = true;
                        mPlayer = new MediaPlayer();
                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                stopPlaying();
                                String flag = getIntent().getStringExtra("flag");
                                if (flag.equals("list")) {
                                    startActivity(new Intent(PlayActivity.this, FilesActivity.class));
                                    finish();
                                } else if (flag.equals("name")) {
                                    startActivity(new Intent(PlayActivity.this, SearchActivity.class));
                                    finish();
                                }
                                finish();
                            }
                        });
                        mPlayer.setDataSource(filePath);
                        mPlayer.prepare();
                        mPlayer.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            findViewById(R.id.play_button).setBackground(getDrawable(R.drawable.play_button_stop));
        } else
            Toast.makeText(this, "이미 파일을 재생하는 중입니다.", Toast.LENGTH_LONG).show();
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
        playing = false;
        findViewById(R.id.play_button).setBackground(getDrawable(R.drawable.play_button_play));
    }
}
