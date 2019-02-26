package app.bqlab.febblindrecorder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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
    int focus;
    boolean playing;
    //layouts
    LinearLayout mainBody;
    List<View> mainBodyButtons;
    //objects
    TextToSpeech mTTS;
    MediaPlayer mPlayer;
    MediaRecorder mRecorder;
    VoiceMemoManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
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

    private void init() {
        //initialization
        mainBody = findViewById(R.id.main_body);
        mainBodyButtons = new ArrayList<View>();
        //setting
        for (int i = 0; i < mainBody.getChildCount(); i++)
            mainBodyButtons.add(mainBody.getChildAt(i));
        findViewById(R.id.main_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus--;
                if (focus <= 0)
                    focus = 0;
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.main_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focus++;
                if (focus >= mainBodyButtons.size() - 1)
                    focus = mainBodyButtons.size() - 1;
                speakFocus();
                resetFocus();
            }
        });
        findViewById(R.id.main_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.main_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case FOCUS_VOICE_MEMO:
                        startActivity(new Intent(MainActivity.this, RecordActivity.class));
                        break;
                    case FOCUS_INSTANT_PLAY:
                        playRecentFile();
                        break;
                    case FOCUS_SEARCH_MEMO:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        break;
                    case FOCUS_USER_CHANGE:
                        Toast.makeText(MainActivity.this, "아직 구현되지 않았습니다.", Toast.LENGTH_LONG).show();
                        break;
                    case FOCUS_APP_EXIT:
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("앱을 종료합니다.")
                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finishAffinity();
                                    }
                                })
                                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                }
            }
        });
        findViewById(R.id.main_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (focus) {
                    case FOCUS_VOICE_MEMO:
                        checkDirectory();
                        startActivity(new Intent(MainActivity.this, RecordActivity.class));
                        stopPlaying();
                        break;
                    case FOCUS_INSTANT_PLAY:
                        checkDirectory();
                        playRecentFile();
                        break;
                    case FOCUS_SEARCH_MEMO:
                        checkDirectory();
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        stopPlaying();
                        break;
                    case FOCUS_USER_CHANGE:
                        Toast.makeText(MainActivity.this, "아직 구현되지 않았습니다.", Toast.LENGTH_LONG).show();
                        break;
                    case FOCUS_APP_EXIT:
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("앱을 종료합니다.")
                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        stopPlaying();
                                        finishAffinity();
                                    }
                                })
                                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                }
            }
        });
        findViewById(R.id.main_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("앱을 종료합니다.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAffinity();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });
    }

    private void checkDirectory() {
        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "음성메모장");
        boolean success;
        if (!dir.exists())
            success = dir.mkdir();
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

    private void checkTTS() {
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                mManager = new VoiceMemoManager(this);
            else {
                requestPermissions();
                return;
            }
            final String path = mManager.getList().get(mManager.getList().size() - 1).getPath();
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(path);
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
                                playing = false;
                            }
                        });
                        mPlayer.setDataSource(path);
                        mPlayer.prepare();
                        mPlayer.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Toast.makeText(this, "최근의 파일을 재생합니다.", Toast.LENGTH_LONG).show();
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
    }

    private void requestPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

}