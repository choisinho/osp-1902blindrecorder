package app.bqlab.febblindrecorder;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class VoiceMemoManager {
    //static
    private File dir;
    private List<VoiceMemo> list;
    //member
    private Context context;

    VoiceMemoManager(Context context) {
        this.context = context;
        loadDir();
        loadFiles();
    }

    private void loadDir() {
        //디렉토리를 멤버 필드로 불러옴
        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "음성메모장");
        boolean success = true;
        if (!dir.exists())
            success = dir.mkdir();
        this.dir = dir;
    }

    private void loadFiles() {
        //디렉토리의 파일을 파일 리스트로 불러옴
        list = new ArrayList<>();
        String[] names = dir.list();
        List<String> pathes = new ArrayList<>();
        for (String name : names) {
            pathes.add(Environment.getExternalStorageDirectory() + File.separator + "음성메모장"+File.separator+name);
        }
        for (String path : pathes) {
            VoiceMemo voiceMemo = new VoiceMemo(context, path);
            this.list.add(voiceMemo);
        }
    }

    List<VoiceMemo> getList() {
        return list;
    }
}
