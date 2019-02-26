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
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    //constants
    final int SPEECH_TO_TEXT = 1000;
    //variables
    boolean recording, speaking, resuming;
    String speech, fileDir, fileName, filePath, targetPath, targetName;
    List<String> sourcePathes;
    //objects
    MediaRecorder mRecorder;
    TextToSpeech mTTS;
    HashMap<String, String> mTTSMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        init();
        checkResumedFile();
        setupTTS();
    }

    @Override
    protected void onResume() {
        super.onResume();
        speakFirst();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        cleanupSources();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mTTS.shutdown();
        mTTS.stop();
    }

    private void init() {
        //initialize
        sourcePathes = new ArrayList<>();
        mTTSMap = new HashMap<String, String>();
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        //setup
        findViewById(R.id.record_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.record_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
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
                try {
                    //소스 파일 병합
                    File file = new File(fileDir, fileName);
                    mergeAudioFiles(sourcePathes, targetPath);
                    cleanupSources();
                    //파일명, 소스 파일 리스트 전달
                    Intent i = new Intent(RecordActivity.this, MenuActivity.class);
                    i.putExtra("fileName", targetName);
                    startActivity(i);
                    finish();
                } catch (NullPointerException e) {
                    speak("아직 녹음이 되지 않았습니다.");
                }
            }
        });
    }

    private void setupRecorder() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(filePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

    private void startRecording() {
        //음성안내 정지
        if (speaking) {
            mTTS.stop();
            mTTS.shutdown();
        }
        //레이아웃 세팅
        ((Button) findViewById(R.id.record_body_start)).setText("녹음중지");
        //파일 경로 세팅
        fileName = System.currentTimeMillis() + ".mp4";
        filePath = fileDir + File.separator + fileName;
        //파일을 이어 붙이기 위한 배열 세팅
        sourcePathes.add(filePath);
        //녹음 시작
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
        //음성안내 정지
        if (speaking) {
            mTTS.stop();
            mTTS.shutdown();
        }
        //레이아웃 세팅
        ((Button) findViewById(R.id.record_body_start)).setText("녹음시작");
        //녹음 종료
        recording = false;
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        targetName = System.currentTimeMillis() + ".mp4";
        targetPath = fileDir + File.separator + targetName;
    }

    private void cleanupSources() {
        for (String path : sourcePathes) {
            String name = path.replace(fileDir + File.separator, "");
            File file = new File(fileDir, name);
            boolean success = file.delete();
        }
    }

    private void mergeAudioFiles(List<String> sources, String target) {
        try {
            List<Movie> movies = new ArrayList<>();
            List<Track> tracks = new ArrayList<>();
            for (String source : sources)
                movies.add(MovieCreator.build(source));
            for (Movie movie : movies)
                tracks.addAll(movie.getTracks());
            Movie output = new Movie();
            if (!tracks.isEmpty())
                output.addTrack(new AppendTrack(tracks.toArray(new Track[0])));
            Container container = new DefaultMp4Builder().build(output);
            FileChannel fileChannel = new RandomAccessFile(target, "rw").getChannel();
            container.writeContainer(fileChannel);
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupTTS() {
        mTTSMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "unique_id");
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
        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                speaking = true;
            }

            @Override
            public void onDone(String utteranceId) {
                speaking = false;
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void speak(String text) {
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, mTTSMap);
    }

    private void speakFirst() {
        if (!resuming) {
            speaking = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        speak("녹음화면");
                        Thread.sleep(1000);
                        speak("녹음을 시작하려면 실행버튼을 눌러주세요. 녹음이 끝나면 X토글키를 눌러주세요.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void checkResumedFile() {
        String resumedFile = getIntent().getStringExtra("fileName");
        if (resumedFile != null) {
            sourcePathes.add(fileDir + File.separator + resumedFile);
            speak("잠시 후 녹음이 다시 진행됩니다.");
            if (!recording) {
                startRecording();
            } else {
                stopRecording();
            }
            resuming = true;
        }
    }
}
