package app.bqlab.febblindrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
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
import java.util.List;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    //constants
    final int SPEECH_TO_TEXT = 1000;
    //variables
    int soundDisable, soundStartEnd;
    boolean recording, resuming;
    String speech, fileDir, fileName, filePath, targetPath, targetName;
    List<String> sourcePathes;
    //objects
    MediaRecorder mRecorder;
    SoundPool mSoundPool;
    TextToSpeech mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        init();
        setupSoundPool();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTTS();
        checkResumedFile();
        speakFirst();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
        shutupTTS();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        cleanupSources();
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
        //initialize
        sourcePathes = new ArrayList<>();
        fileDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "음성메모장";
        //setup
        findViewById(R.id.record_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickUp();
            }
        });
        findViewById(R.id.record_bot_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickDown();
            }
        });
        findViewById(R.id.record_bot_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLeft();
            }
        });
        findViewById(R.id.record_bot_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRight();
            }
        });
        findViewById(R.id.record_bot_enter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickVToggle();
            }
        });
        findViewById(R.id.record_bot_close).setOnClickListener(new View.OnClickListener() {
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
        startActivity(new Intent(RecordActivity.this, MainActivity.class));
        finish();
    }

    private void clickRight() {
        mSoundPool.play(soundDisable, 1, 1, 0, 0, 1);
    }

    private void clickVToggle() {
        if (!recording) {
            mSoundPool.play(soundStartEnd, 1, 1, 0, 0, 1);
            startRecording();
        } else {
            stopRecording();
            mSoundPool.play(soundStartEnd, 1, 1, 0, 0, 1);
        }
    }

    private void clickXToggle() {
        if (recording)
            stopRecording();
        if (targetName != null) {
            //소스 파일 병합
            mergeAudioFiles(sourcePathes, targetPath);
            cleanupSources();
            //파일명, 소스 파일 리스트 전달
            Intent i = new Intent(RecordActivity.this, MenuActivity.class);
            i.putExtra("fileName", targetName);
            startActivity(i);
            finish();
        } else {
            speak("파일을 찾을 수 없습니다.");
        }
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
        shutupTTS();
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
                Toast.makeText(RecordActivity.this, "파일의 경로에 접근할 수 없습니다.", Toast.LENGTH_LONG).show();
            } catch (IllegalStateException e) {
                recording = false;
                Toast.makeText(RecordActivity.this, "파일의 경로에 접근할 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void stopRecording() {
        //음성안내 정지
        shutupTTS();
        //레이아웃 세팅
        ((Button) findViewById(R.id.record_body_start)).setText("녹음시작");
        //녹음 종료
        try {
            recording = false;
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
            targetName = System.currentTimeMillis() + ".mp4";
            targetPath = fileDir + File.separator + targetName;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
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
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build();
        soundDisable = mSoundPool.load(this, R.raw.app_sound_disable, 0);
        soundStartEnd = mSoundPool.load(this, R.raw.app_sound_start_end, 0);
    }

    private void setupTTS() {
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

    private void shutupTTS() {
        mTTS.stop();
        mTTS.shutdown();
    }

    private void speak(String text) {
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void speakFirst() {
        if (!resuming) {
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        speak("잠시 후 녹음이 다시 진행됩니다.");
                        Thread.sleep(2500);
                        if (!recording) {
                            startRecording();
                        } else {
                            stopRecording();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            resuming = true;
        }
    }
}
