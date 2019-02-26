package app.bqlab.febblindrecorder;

import android.content.Context;

public class VoiceMemo {
    private String sum, path;

    VoiceMemo(Context context, String path) {
        this.path = path;
        sum = context.getSharedPreferences("sum", Context.MODE_PRIVATE).getString(path, "");
    }

    public String getSum() {
        return sum;
    }

    public String getPath() {
        return path;
    }
}
