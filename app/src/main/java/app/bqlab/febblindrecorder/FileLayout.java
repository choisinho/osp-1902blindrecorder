package app.bqlab.febblindrecorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
public class FileLayout extends LinearLayout {
    FileLayout(final Context context, final String index, final String name) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.layout_file, this);
        ((TextView)findViewById(R.id.file_index)).setText(index);
        ((Button)findViewById(R.id.file_name)).setText(name);
        findViewById(R.id.file_name).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, PlayActivity.class);
                i.putExtra("fileName", name);
                context.startActivity(i);
            }
        });
    }
    void setColor(Drawable drawable) {
        findViewById(R.id.file_index).setBackground(drawable);
        findViewById(R.id.file_name).setBackground(drawable);
    }
    Button getButton() {
        return findViewById(R.id.file_name);
    }
}
