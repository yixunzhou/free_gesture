package team.tsinghua.ipsc.free_gesture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.VideoView;
import team.tsinghua.ipsc.free_gesture.ui.main.SectionsPagerAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MainActivity extends AppCompatActivity {
    private int taskTime = 3;  // seconds
    private final int roundTotalNum = 25;
    private final int taskNum = 6;
    private int countIndex = 0;
    private VideoView videoView; // demo video
    private TextView trigger; // data collecting area
    private TextView timerView, counterView;
    private TextView modeView;
    private Uri uri;
    private PopupWindow popup_window;
    private String exp_info;
    private int[] modeNums, modeNums2;
    private int modeNum;
    private String current_date;
    private String device_sc;
    private String m_sendAddress;
    private boolean m_isCollecting;
    private String[] args;
    private String numOft;

    private String src_file_dir_str;
    private String dst_file_dir_str, dst_file_dir_str2;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    private CountDownTimer m_countDownTimer2;
    private BufferedWriter writer = null;
    public void write(File file, String txt){
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8));
            writer.write(txt);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void unzip(File src_dir, File dst_dir) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(src_dir)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(dst_dir, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
    }


    public static void copy_file(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }


    public void copy_directory(File sourceLocation , File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
            }
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copy_directory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            // make sure the directory we plan to store the recording in exists
            File directory = targetLocation.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }


    void DeleteRecursive(File dir) {
        Log.d("DeleteRecursive", "DELETEPREVIOUS TOP" + dir.getPath());
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                File temp = new File(dir, child);
                if (temp.isDirectory()) {
                    Log.d("DeleteRecursive", "Recursive Call" + temp.getPath());
                    DeleteRecursive(temp);
                } else {
                    Log.d("DeleteRecursive", "Delete File" + temp.getPath());
                    boolean b = temp.delete();
                    if (!b) {
                        Log.d("DeleteRecursive", "DELETE FAIL");
                    }
                }
            }
        }
        dir.delete();
    }


    public static int[] indexGenerator(int min, int max, int n){
        if (n > (max - min + 1) || max < min) {
            return null;
        }
        int[] result = new int[n];
        int count = 0;
        while(count < n) {
            int num = (int) (Math.random() * (max - min)) + min;
            boolean flag = true;

            for (int j = 0; j < n; j++) {

                if (num == result[j]) {
                    flag = false;
                    break;

                }
            }

            if(flag){
                result[count] = num;
                count++;
            }
            if (count == n-1){
                break;
            }
        }
        return result;
    }


    public static String[] readSettings(String filePath){
        String line;
        String[] args = new String[20];
        int i;
        try{
            BufferedReader in=new BufferedReader(new FileReader(filePath));
            line=in.readLine();
            for (i=0;i<args.length;i++){
                if (line != null){
                    args[i] = line;
                    line = in.readLine();
                }
            }
            in.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return args;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
        }

        args = readSettings("/storage/emulated/0/gathered_data2/settings.txt");
        numOft = args[0];

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        final ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.beginFakeDrag();

        final TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        final ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                // prepare video
                timerView = findViewById(R.id.expTimer);
                videoView = findViewById(R.id.expVideo);
                trigger = findViewById(R.id.expDataCollectionArea);
                modeView = findViewById(R.id.expMode);
                counterView = findViewById(R.id.counter);

                // change button state
                fab.setEnabled(false);
                fab.setVisibility(View.INVISIBLE);
                timerView.setTextColor(Color.BLACK);
                modeView.setTextColor(Color.BLACK);
                trigger.setBackgroundColor(Color.BLACK);

                // pop_up_input
                final CoordinatorLayout main_activity_layout = findViewById(R.id.main_activity_layout);
                final LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                @SuppressLint("InflateParams") final View pop_up_view = layoutInflater.inflate(R.layout.pop_up_input, null);

                //instantiate popup window
                popup_window = new PopupWindow(pop_up_view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                popup_window.showAtLocation(main_activity_layout, Gravity.CENTER, 0, 0);
                popup_window.setFocusable(true);
                popup_window.update();

                //close the popup window on button click
                Button close_pop_up_button = pop_up_view.findViewById(R.id.close_pop_up_button);
                close_pop_up_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popup_window.dismiss();

                        /***********************/

                        @SuppressLint("InflateParams") final View expView = layoutInflater.inflate(R.layout.exp_layout, null);
                        EditText exp_info_input = pop_up_view.findViewById(R.id.exp_info_input);
                        exp_info = exp_info_input.getText().toString();
                        Log.d("exp_info", exp_info);
                        viewPager.setBackgroundColor(Color.WHITE);

                        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.zoom_in);
                        videoView.setVideoURI(uri);
                        videoView.start();
                        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.setLooping(true);
                            }
                        });

                        timerView.setVisibility(View.VISIBLE);
                        timerView.setBackgroundColor(0x00000);
                        modeView.setBackgroundColor(0x00000);
                        counterView.setBackgroundColor(0x00000);

                        final String src_folder_str = "/storage/emulated/0/com.huawei.lcagent/";
                        final String temp_folder_str = "/storage/emulated/0/raw_unzipped/";
                        final String dst_folder_str = "/storage/emulated/0/gathered_data2/";

                        File dst_folder = new File(dst_folder_str);
                        if (!dst_folder.exists()) {
                            dst_folder.mkdir();
                        }
                        device_sc = new File("/storage/emulated/0/dev_info/").list()[0];
                        m_sendAddress = new File("/storage/emulated/0/NetConfig/").list()[0];

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        current_date = sdf.format(new Date());
                        String dst_file_folder_dir_str = dst_folder_str + exp_info + "_" + device_sc;
                        Log.d("dst folder dir str", dst_file_folder_dir_str);
                        final File dst_file_folder_dir = new File(dst_file_folder_dir_str);
                        if (!dst_file_folder_dir.exists()) {
                            dst_file_folder_dir.mkdir();
                        }

                        final String mode_seq = dst_file_folder_dir_str + "_seq";
                        Log.d("mode seq", mode_seq);
                        final File mode_seq_file = new File(mode_seq);
                        if (!mode_seq_file.exists()) mode_seq_file.mkdir();


                        if (mode_seq_file.list().length == 0) {
                            dst_file_dir_str2 = mode_seq + "/" + current_date + "_" + exp_info + "_01.txt";
                        } else {
                            String[] file_list = new File(mode_seq).list();
                            int this_num = 0;
                            String this_name;
                            for (int i = 0; i < Integer.parseInt(numOft); i++) {
                                String file_num;
                                if (i < 9) {
                                    file_num = "0" + (i + 1);
                                }else{
                                    file_num = String.valueOf(i+1);
                                }
                                this_num = i + 1;
                                boolean find_file = false;
                                for (int j = 0; j < file_list.length; j++) {
                                    String file_name = file_list[j].split("_")[2].split("\\.")[0];
                                    if (file_name.equals(file_num)) {
                                        find_file = true;
                                        break;
                                    }
                                }
                                if (!find_file)
                                    break;
                            }
                            /***********************/
                            if (this_num <= 9) {
                                this_name = "0" + this_num;
                            } else if(this_num <= Integer.parseInt(numOft)){
                                this_name = String.valueOf(this_num);
                            } else{
                                this_name = numOft;
                            }
                            dst_file_dir_str2 = mode_seq + "/" + current_date + "_" + exp_info + "_" + this_name + ".txt";
                        }


                        if (dst_file_folder_dir.list().length == 0) {
                            dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_01";

                        } else {
                            String[] file_list = new File(dst_file_folder_dir_str).list();
                            int this_num = 0;
                            String this_name;
                            for (int i = 0; i < Integer.parseInt(numOft); i++) {
                                String file_num;
                                if (i < 9) {
                                    file_num = "0" + (i + 1);
                                }else{
                                    file_num = String.valueOf(i+1);
                                }
                                this_num = i + 1;
                                boolean find_file = false;
                                for (int j = 0; j < file_list.length; j++) {
                                    String file_name = file_list[j].split("_")[2].split("\\.")[0];
                                    if (file_name.equals(file_num)) {
                                        find_file = true;
                                        break;
                                    }
                                }
                                if (!find_file)
                                    break;
                            }
                            /***********************/
                            if (this_num <= 9) {
                                this_name = "0" + this_num;
                            } else if(this_num < Integer.parseInt(numOft)){
                                this_name = String.valueOf(this_num);
                            } else{
                                this_name = numOft;
                            }
                            dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_" + this_name;
                        }


                        modeNums = indexGenerator(0, 6, taskNum);
                        modeNums2 = new int[roundTotalNum*modeNums.length];
                        for (int k=0;k<roundTotalNum;k++){
                            System.arraycopy(modeNums, 0, modeNums2, 6 * k, modeNums.length);
                        }
                        timerView.setVisibility(View.VISIBLE);
                        timerView.setBackgroundColor(0x00000);
                        counterView.setVisibility(View.VISIBLE);
                        counterView.setBackgroundColor(0x00000);
                        modeView.setBackgroundColor(0x00000);
                        m_countDownTimer2 = new CountDownTimer(taskTime * 1000, 1000) {
                            @Override
                            public void onTick(long l) {

                                modeNum = modeNums2[countIndex];
                                if (modeNum == 0){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.zoom_in);
                                    modeView.setText("当前模式：双指缩小");

                                } else if (modeNum == 1){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.zoom_out);
                                    modeView.setText("当前模式：双指放大");

                                } else if (modeNum == 2){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slide_left);
                                    modeView.setText("当前模式：单指向左");

                                } else if (modeNum == 3){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slide_right);
                                    modeView.setText("当前模式：单指向右");

                                } else if (modeNum == 4){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slide_up);
                                    modeView.setText("当前模式：单指向上");

                                } else if (modeNum == 5){
                                    uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slide_down);
                                    modeView.setText("当前模式：单指向下");

                                }
                                // play and loop video
                                videoView.setVideoURI(uri);
                                videoView.start();
                                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        mp.setLooping(true);
                                    }
                                });


                                timerView.setText("剩余时间: " + l / 1000 + "秒");
                                counterView.setText("已完成" + countIndex + "个模式，共" + roundTotalNum * taskNum + "个模式");

                            }

                            @Override
                            public void onFinish() {

                                if (countIndex < taskNum*roundTotalNum-1){
                                    countIndex+=1;
                                    this.start();
                                }else{
                                    Log.d(String.valueOf(countIndex), "index");
                                    viewPager.setBackgroundColor(Color.BLACK);
                                    m_isCollecting = false;
                                    trigger.setTextColor(Color.WHITE);

                                    trigger.setTextSize(18);
                                    trigger.setText("切换到 My Application 删除后拷贝");
                                    // end video loop
                                    videoView.stopPlayback();
                                    videoView.setVisibility(View.GONE);
                                    // change file extractor button style
                                    final MaterialButton save_file_button = findViewById(R.id.save_file_button);
                                    save_file_button.setVisibility(View.VISIBLE);
                                    save_file_button.setEnabled(true);

                                    save_file_button.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {


                                            File temp_folder = new File(temp_folder_str);
                                            if (temp_folder.exists() && temp_folder.isDirectory()) {
                                                DeleteRecursive(temp_folder);
                                            }
                                            new File(temp_folder_str).mkdir();

                                            File tar_folder = new File(src_folder_str);
                                            try {
                                                unzip(
                                                        new File(src_folder_str + tar_folder.list()[0]),
                                                        new File(temp_folder_str)
                                                );
                                            } catch (Exception e) {
                                                Log.d("zip_info", "Unzip ERROR");
                                                timerView.setText("ERROR");
                                                timerView.setTextColor(Color.RED);
                                            } finally {
                                                timerView.setText(" ");
                                                save_file_button.setIconResource(R.drawable.ic_file_download_black_24dp);
                                                save_file_button.setText("保存文件");
                                                save_file_button.setOnClickListener(new View.OnClickListener() {
                                                    public void onClick(View v) {
                                                        String unzipped_folder_str = temp_folder_str + "thplog/";
                                                        File unzipped_folder = new File(unzipped_folder_str);

                                                        String[] unzipped_files = unzipped_folder.list();
                                                        for (String temp : unzipped_files) {
                                                            if (new File(unzipped_folder, temp).length() <= 0) {
                                                                new File(unzipped_folder, temp).delete();
                                                            }
                                                        }
                                                        src_file_dir_str = temp_folder_str + "thplog/";

                                                        try {

                                                            copy_directory(
                                                                    new File(src_file_dir_str),
                                                                    new File(dst_file_dir_str)
                                                            );
                                                            File f = new File(dst_file_dir_str2);
                                                            String s = "";
                                                            for (int m:modeNums){
                                                                s += m;
                                                            }
                                                            write(f,s);
                                                            countIndex = 0;
                                                            save_file_button.setEnabled(false);
                                                            save_file_button.setVisibility(View.INVISIBLE);
                                                            fab.setEnabled(true);
                                                            fab.setVisibility(View.VISIBLE);
                                                            trigger.setTextSize(30);
                                                            save_file_button.setIconResource(R.drawable.ic_done_all_black_24dp);
                                                            save_file_button.setText("文件已保存");

                                                            if (dst_file_folder_dir.list().length == Integer.parseInt(numOft)) {

                                                                @SuppressLint("InflateParams") final View pop_up_notification = layoutInflater.inflate(R.layout.pop_up_notification, null);
                                                                //instantiate popup window
                                                                popup_window = new PopupWindow(pop_up_notification, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                                popup_window.showAtLocation(main_activity_layout, Gravity.CENTER, 0, 0);
                                                                popup_window.setFocusable(true);
                                                                popup_window.update();

                                                                Button close_pop_up_button = pop_up_notification.findViewById(R.id.close_pop_up_button);
                                                                close_pop_up_button.setOnClickListener(new View.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(View v) {
                                                                        popup_window.dismiss();
                                                                    }
                                                                });
                                                            }
                                                            trigger.setTextColor(Color.WHITE);

                                                        } catch (IOException e) {
                                                            trigger.setText("保存失败");
                                                            e.printStackTrace();
                                                        } finally {

                                                            trigger.setText("保存成功");
                                                            videoView.setVisibility(View.VISIBLE);
                                                            videoView.seekTo(0);
                                                            DeleteRecursive(new File(temp_folder_str));

                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });

                                }
                            }

                        };






                        trigger.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    trigger.setTextColor(Color.BLACK);
                                    Log.d("OnTouch", "arch action down");
                                    //start timer
                                    if (!m_isCollecting && countIndex < 25) {
                                        m_countDownTimer2.start();
                                        m_isCollecting = true;
                                    }

                                    if (countIndex >= 25){
                                        m_isCollecting = false;
                                    }

                                }
                                return true;
                            }
                        });





                    }
                });


            }

        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("Permission", "permission: " + permissions[i] + ", result: " + grantResults[i]);
            }
        }
    }
}
