package app.bqlab.febblindrecorder;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class PlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        init();
    }

    private void init() {
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
                PlayActivity.super.onBackPressed();
            }
        });
        findViewById(R.id.play_bot_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //disable
            }
        });
    }
}
