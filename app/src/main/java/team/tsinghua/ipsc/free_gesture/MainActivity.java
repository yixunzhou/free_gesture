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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MainActivity extends AppCompatActivity {
    private int exp_time = 180;  // seconds
    private VideoView videoview; // demo video
    private VideoView trigger; // data collecting area
    private TextView timer_view;
    private TextView mode_view;
    private Uri uri;
    private PopupWindow popup_window;
    private String exp_info;
    private int exp_info_int;
    private String mode;
    private String current_date;
    private String device_sc;
    private String m_groupNumber;
    private String m_sendAddress;
    private boolean m_isCollecting;

    private String src_file_dir_str;
    private String dst_file_dir_str;
    private int currTabPos; // position # of the tab
    private DatagramSocket m_udpClient;

    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    private CountDownTimer m_countDownTimer;


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


    void SendCmd(final int cmd){   //0:send start cmd  1: send stop cmd
        new Thread(new Runnable(){
            public void run() {
                try {
                    Log.d("touch_info", "on action down");
                    byte[] sendBuf = new byte[8];
                    sendBuf[0] = Byte.parseByte(device_sc);//dev number
                    if(cmd == 0){
                        sendBuf[1] = 0x01;//0x01:start, 0x02:stop
                    }
                    else{
                        sendBuf[1] = 0x02;//0x01:start, 0x02:stop
                    }
                    sendBuf[2] = (byte)((exp_info_int >> 24) & 0xff);//usr number
                    sendBuf[3] = (byte)((exp_info_int >> 16) & 0xff);
                    sendBuf[4] = (byte)((exp_info_int >>  8) & 0xff);
                    sendBuf[5] = (byte)((exp_info_int & 0xff));
//                    sendBuf[2] = Byte.parseByte(exp_info);//usr number

                    sendBuf[6] = (byte)(((currTabPos + 1)) & 0xFF) ; //0x00:hold  0x01:arch  0x02:slide  0x03:touch
                    sendBuf[7] = Byte.parseByte(m_groupNumber);
//                                                sendBuf[4] = 0x01;
                    int port = 4772;
                    DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(m_sendAddress), port);
                    m_udpClient.send(sendPacket);
                    Log.d("touch_info", "on action down");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
        }

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
                currTabPos = tabs.getSelectedTabPosition();

                if (currTabPos == 0) {
                    timer_view = findViewById(R.id.zoom_in_timer);
                    videoview = (VideoView) findViewById(R.id.ZoomInVideo);
                    trigger = (VideoView) findViewById(R.id.ZoomInCollection);
                    mode_view = findViewById(R.id.zoom_in_mode);
                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.zoom_in);
                    mode = "zoom_in/";
                } else if (currTabPos == 1) {
                    timer_view = findViewById(R.id.zoom_out_timer);
                    videoview = (VideoView) findViewById(R.id.ZoomOutVideo);
                    trigger = (VideoView) findViewById(R.id.ZoomOutCollection);
                    mode_view = findViewById(R.id.zoom_out_mode);
                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.zoom_out);
                    mode = "zoom_out/";
                } else if (currTabPos == 2) {
                    timer_view = findViewById(R.id.slide_lr_timer);
                    videoview = (VideoView) findViewById(R.id.SlideLRVideo);
                    trigger = (VideoView) findViewById(R.id.SlideLRCollection);
                    mode_view = findViewById(R.id.slide_lr_mode);
                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.slide_lr);
                    mode = "slide_lr/";
                } else if (currTabPos == 3) {
                    timer_view = findViewById(R.id.slide_ud_timer);
                    videoview = (VideoView) findViewById(R.id.SlideUDVideo);
                    trigger = (VideoView) findViewById(R.id.SlideUDCollection);
                    mode_view = findViewById(R.id.slide_ud_mode);
                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.slide_ud);
                    mode = "slide_ud/";
                } else if (currTabPos == 4) {
                    timer_view = findViewById(R.id.touch_timer);
                    videoview = (VideoView) findViewById(R.id.TouchVideo);
                    trigger = (VideoView) findViewById(R.id.TouchCollection);
                    mode_view = findViewById(R.id.touch_mode);
                    uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.touch);
                    mode = "touch/";
                }

                // change button state
                fab.setEnabled(false);
                fab.setVisibility(View.INVISIBLE);


                timer_view.setTextColor(Color.BLACK);
                mode_view.setTextColor(Color.BLACK);

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
                        EditText exp_info_input = pop_up_view.findViewById(R.id.exp_info_input);
                        exp_info = exp_info_input.getText().toString();
                        exp_info_int = Integer.parseInt(exp_info);
                        Log.d("exp_info", exp_info);

                        viewPager.setBackgroundColor(Color.WHITE);

                        // play and loop video
                        videoview.setVideoURI(uri);
                        videoview.start();
                        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.setLooping(true);
                            }
                        });

                        if (currTabPos == 0 || currTabPos == 1 || currTabPos == 4) {
                            timer_view.setVisibility(View.VISIBLE);
                        }

                        timer_view.setBackgroundColor(0x00000);
                        mode_view.setBackgroundColor(0x00000);

                        // init udp socket
                        try {
                            m_udpClient = new DatagramSocket();
                            Log.d("socket_info", "create socket success");
                        } catch (SocketException e) {
                            Log.d("socket_info", "can not create socket");
                            e.printStackTrace();
                        }

                        m_sendAddress = "192.168.31.170";

                        final String src_folder_str = "/storage/emulated/0/com.huawei.lcagent/";
                        final String temp_folder_str = "/storage/emulated/0/raw_unzipped/";
                        final String dst_folder_str = "/storage/emulated/0/gathered_data/" + mode;
                        File dst_folder = new File(dst_folder_str);
                        if (!dst_folder.exists()){
                            dst_folder.mkdir();
                        }
                        device_sc = new File("/storage/emulated/0/dev_info/").list()[0];
                        m_sendAddress = new File("/storage/emulated/0/NetConfig/").list()[0];
                        Log.d("send_address", m_sendAddress);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        current_date = sdf.format(new Date());
                        String dst_file_folder_dir_str = dst_folder_str + exp_info + "_" + device_sc;
                        Log.d("dst folder dir str", dst_file_folder_dir_str);
                        final File dst_file_folder_dir = new File(dst_file_folder_dir_str);
                        if (!dst_file_folder_dir.exists()) {
                            dst_file_folder_dir.mkdir();
                        }


                        if(currTabPos <= 4){
                            if (dst_file_folder_dir.list().length == 0) {
                                dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_01.thplog";
                                m_groupNumber = "1";
                            } else {
                                String[] file_list = new File(dst_file_folder_dir_str).list();
                                int this_num = 0;
                                String this_name;
                                for (int i = 0; i < 10; i++) {
                                    String file_num = "0" + (i + 1);
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
                                } else {
                                    this_name = "10";
                                }
                                m_groupNumber = this_name;
                                dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_" + this_name + ".thplog";
                            }
                        }
                        else{
                            if (dst_file_folder_dir.list().length == 0) {
                                dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_01";
                                m_groupNumber = "1";
                            } else {
                                String[] file_list = new File(dst_file_folder_dir_str).list();
                                int this_num = 0;
                                String this_name;
                                for (int i = 0; i < 10; i++) {
                                    String file_num = "0" + (i + 1);
                                    this_num = i + 1;
                                    boolean find_file = false;
                                    for (int j = 0; j < file_list.length; j++) {
                                        String file_name = file_list[j].split("_")[2];
                                        if (file_name.equals(file_num)) {
                                            find_file = true;
                                            break;
                                        }
                                    }
                                    if (!find_file)
                                        break;
                                }
                                if (this_num <= 9) {
                                    this_name = "0" + this_num;
                                } else {
                                    this_name = "10";
                                }
                                m_groupNumber = this_name;
                                dst_file_dir_str = dst_file_folder_dir_str + "/" + current_date + "_" + exp_info + "_" + this_name;
                            }
                        }

                        m_countDownTimer = new CountDownTimer(exp_time*1000, 1000) {
                            public void onTick(long millisUntilFinished) {
                                timer_view.setText("Remain: " + millisUntilFinished / 1000 + "s");
                            }

                            public void onFinish() {

                                SendCmd(1);
                                viewPager.setBackgroundColor(Color.BLACK);
                                m_isCollecting = false;
                                timer_view.setTextColor(Color.WHITE);
                                timer_view.setTextSize(18);
                                timer_view.setText("切换到 My Application 删除后拷贝");
                                // end video loop
                                videoview.stopPlayback();
                                videoview.setVisibility(View.GONE);

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
                                            timer_view.setText("ERROR");
                                            timer_view.setTextColor(Color.RED);
                                        } finally {
                                            timer_view.setText(" ");
                                            save_file_button.setIconResource(R.drawable.ic_file_download_black_24dp);
                                            save_file_button.setText("保存文件");
                                            save_file_button.setOnClickListener(new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    String unzipped_folder_str = temp_folder_str + "thplog/";
                                                    File unzipped_folder = new File(unzipped_folder_str);

                                                    if (currTabPos == 2 || currTabPos == 3) {
                                                        int max_size_index = 0;
                                                        File max_size_file = new File(unzipped_folder_str + unzipped_folder.list()[0]);
                                                        Long max_size = max_size_file.length();
                                                        for (int i = 1; i <= unzipped_folder.list().length - 1; i++) {
                                                            File curr_file = new File(unzipped_folder_str + unzipped_folder.list()[i]);
                                                            Long curr_size = curr_file.length();
                                                            if (curr_size > max_size) {
                                                                max_size = curr_size;
                                                                max_size_index = i;
                                                            }
                                                        }
                                                        src_file_dir_str = temp_folder_str + "thplog/" + unzipped_folder.list()[max_size_index];


                                                    } else {
                                                        String[] unzipped_files = unzipped_folder.list();
                                                        for (String temp : unzipped_files) {
                                                            if (new File(unzipped_folder, temp).length() <= 1.8 * 1024 * 1024) {
                                                                new File(unzipped_folder, temp).delete();
                                                            }
                                                        }
                                                        src_file_dir_str = temp_folder_str + "thplog/";

                                                    }

                                                    try {
                                                        if (currTabPos == 2 || currTabPos == 3) {
                                                            copy_file(
                                                                    new File(src_file_dir_str),
                                                                    new File(dst_file_dir_str)
                                                            );
                                                        } else {
                                                            copy_directory(
                                                                    new File(src_file_dir_str),
                                                                    new File(dst_file_dir_str)
                                                            );
                                                        }
                                                        save_file_button.setEnabled(false);
                                                        save_file_button.setVisibility(View.INVISIBLE);
                                                        fab.setEnabled(true);
                                                        fab.setVisibility(View.VISIBLE);
                                                        timer_view.setTextSize(30);
                                                        save_file_button.setIconResource(R.drawable.ic_done_all_black_24dp);
                                                        save_file_button.setText("文件已保存");

                                                        if (dst_file_folder_dir.list().length == 10) {
//                                                            File new_folder = new File(dst_folder_str + current_date + "_" + exp_info + "_" + device_sc);
//                                                            dst_file_folder_dir.renameTo(new_folder);

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


                                                    } catch (IOException e) {
                                                        timer_view.setText("保存失败");
                                                        e.printStackTrace();
                                                    } finally {
                                                        timer_view.setText("保存成功");
                                                        videoview.setVisibility(View.VISIBLE);
                                                        videoview.seekTo(0);
                                                        DeleteRecursive(new File(temp_folder_str));
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        };


                        trigger.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event){
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    Log.d("OnTouch", "arch action down");
                                    //start timer
                                    if(!m_isCollecting){
                                        SendCmd(0);//0 start  1 stop
                                        m_countDownTimer.start();
                                        m_isCollecting = true;
                                    }



                                } else if (event.getAction() == MotionEvent.ACTION_UP){
                                    Log.d("OnTouch", "arch action up");
                                    //reset timer
                                    if ((currTabPos == 2 || currTabPos == 3) && m_isCollecting){
                                        SendCmd(1);
                                        m_countDownTimer.cancel();
                                        timer_view.setText("Remain: " + exp_time + "s");
                                        m_isCollecting = false;
                                    }

                                }
                                return true;
                            }
                        });



//                        };

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
