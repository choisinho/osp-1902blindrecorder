package app.bqlab.febblindrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    //constants
    final int SPEECH_TO_TEXT = 1000;
    //variables
    String speech, path, name;
    boolean recording, playing;
    //objects
    MediaRecorder mRecorder;
    TextToSpeech mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        init();
        checkTTS();
        speakFirst();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SPEECH_TO_TEXT) {
                if (data != null) {
                    ArrayList<String> input = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    speech = input.get(0);
                }
            }
        }
    }

    private void init() {
        //setting
        findViewById(R.id.record_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.record_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
        findViewById(R.id.record_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
        findViewById(R.id.record_bot_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                if (new File(path).exists()) {
                    Intent i =new Intent(RecordActivity.this, MenuActivity.class);
                    i.putExtra("path", path);
                    Log.d("경로", path);
                    startActivity(i);
                } else {
                    Toast.makeText(RecordActivity.this, "녹음파일이 생성되지 않았습니다.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupRecorder() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(path);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    private void startRecording() {
        speak("");
        ((Button) findViewById(R.id.record_body_start)).setText("녹음중지");
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장" + File.separator + System.currentTimeMillis()+".mp4";
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else {
            try {
                recording = true;
                setupRecorder();
                mRecorder.prepare();
                mRecorder.start();
            } catch (IOException e) {
                recording = false;
                Log.d("startRecording()", "IOException");
                Toast.makeText(RecordActivity.this, "파일의 경로에 접근할 수 없습니다.", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                recording = false;
                Log.d("startRecording()", "IllegalStateException");
                Toast.makeText(RecordActivity.this, "파일의 경로에 접근할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        recording = false;
        ((Button) findViewById(R.id.record_body_start)).setText("녹음시작");
    }

    private void checkTTS() {
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(RecordActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
                        finishAffinity();
                    }
                } else {
                    Toast.makeText(RecordActivity.this, "지원하지 않는 서비스입니다.", Toast.LENGTH_LONG).show();
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
                    speak("녹음화면");
                    Thread.sleep(1500);
                    speak("녹음을 시작하려면 실행버튼을 눌러주세요. 녹음이 끝나면 X토글키를 눌러주세요.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
