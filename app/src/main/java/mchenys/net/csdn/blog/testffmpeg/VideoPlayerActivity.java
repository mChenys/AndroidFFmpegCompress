package mchenys.net.csdn.blog.testffmpeg;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * Created by mChenys on 2017/1/5.
 */
public class VideoPlayerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoView videoView = new VideoView(this);
        setContentView(videoView);
        videoView.setMediaController(new MediaController(this));
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText( VideoPlayerActivity.this, "播放完成了", Toast.LENGTH_SHORT).show();
            }
        });
        String videoPath = getIntent().getStringExtra("videoPath");
        try {
            videoView.setVideoPath(videoPath);
            videoView.start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(VideoPlayerActivity.this, "视频不存在", Toast.LENGTH_SHORT).show();
        }
    }
}
