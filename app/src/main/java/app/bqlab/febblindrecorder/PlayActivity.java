package app.bqlab.febblindrecorder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

public class PlayActivity extends AppCompatActivity {

    //constants
    final int FOCUS_VOICE_MEMO = 0;     //음성 메모
    final int FOCUS_INSTANT_PLAY = 1;   //파일 바로 재생
    final int FOCUS_SEARCH_MEMO = 2;    //메모 찾기
    final int FOCUS_USER_CHANGE = 3;    //사용자 변경
    final int FOCUS_APP_EXIT = 4;       //종료
    //variables
    boolean playing, speaking;
    String fileName, fileDir, filePath;
    //objects
    File mFile;
    TextToSpeech mTTS;
    MediaPlayer mPlayer;
    MediaRecorder mRecorder;
    VoiceMemoManager mManager;
    HashMap<String, String> mTTSMap;
    //layouts
    Button playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        init();
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

    private void init() {
        //initialize
        fileName = getIntent().getStringExtra("fileName");
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        mFile = new File(fileDir, fileName);
        //setup
        findViewById(R.id.play_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
            }
        });
        findViewById(R.id.play_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
            }
        });
        findViewById(R.id.play_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String flag = getIntent().getStringExtra("flag");
                if (flag.equals("list")) {
                    startActivity(new Intent(PlayActivity.this, FilesActivity.class));
                    finish();
                } else if (flag.equals("name")) {
                    startActivity(new Intent(PlayActivity.this, SearchActivity.class));
                    finish();
                }

            }
        });
        findViewById(R.id.play_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
            }
        });
        findViewById(R.id.play_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shutupTTS();
                if (playing)
                    stopPlaying();
                else
                    startPlaying();
            }
        });
        findViewById(R.id.play_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
            }
        });
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
                speaking = false;
                startPlaying();
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
                    speak(fileName.replace(".mp4", "") + new SimpleDateFormat("yyyy년 MM월 dd일").format(mFile.lastModified()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startPlaying() {
        if (!playing) {
            filePath = fileDir + File.separator + fileName;
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(filePath);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
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
                                startActivity(new Intent(PlayActivity.this, FilesActivity.class));
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
