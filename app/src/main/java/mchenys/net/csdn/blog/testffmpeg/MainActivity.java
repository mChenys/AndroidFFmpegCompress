package mchenys.net.csdn.blog.testffmpeg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import mchenys.net.csdn.blog.testffmpeg.compress.CompressListener;
import mchenys.net.csdn.blog.testffmpeg.compress.Compressor;
import mchenys.net.csdn.blog.testffmpeg.compress.InitListener;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private RecordView mRecordView;
    private TextView mDurationTv, mCompressInfoTv, mVideoSizeTv;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecordView = (RecordView) findViewById(R.id.recordView);
        mDurationTv = (TextView) findViewById(R.id.tv_duration);
        mCompressInfoTv = (TextView) findViewById(R.id.tv_info);
        mVideoSizeTv = (TextView) findViewById(R.id.tv_size);
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("正在压缩");
        mDialog.setCanceledOnTouchOutside(false);
        mRecordView.setOnRecordCallback(new RecordView.OnRecordCallback() {
            @Override
            public void onFinish() {
                mVideoSizeTv.setText("大小:" + mRecordView.getRecordFileSize() + "Mb");
            }

            @Override
            public void onProgress(int total, int curr) {
                mDurationTv.setText(curr + "/" + total);
                mVideoSizeTv.setText("大小:" + mRecordView.getRecordFileSize() + "Mb");
                Log.d(TAG, "onProgress:" + mRecordView.getRecordFileSize());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDurationTv.setText("");
        mCompressInfoTv.setText("");
        mVideoSizeTv.setText("");
    }

    //开始录制
    public void startRecord(View view) {
        if (getFreeSpace() < 10 * 1024 * 1024) {
            Toast.makeText(MainActivity.this, "剩余空间不够充足，请清理一下再试一次", Toast.LENGTH_SHORT).show();
            return;
        }
        mRecordView.startRecord();
        mCompressInfoTv.setVisibility(View.GONE);
    }

    //停止录制
    public void endRecord(View view) {
        mRecordView.stopRecord();
    }

    //查看原视频
    public void openSource(View view) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoPath", mRecordView.getRecordFilePath());
        startActivity(intent);
    }

    //查看压缩后视频
    public void openCompress(View view) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoPath", mRecordView.getCompressFilePath());
        startActivity(intent);
    }

    //开始压缩
    public void startCompress(View view) {
        mDialog.show();

        String source = mRecordView.getRecordFilePath();
        String target = mRecordView.getRecordFile().getParent() + "/compress_" + mRecordView.getRecordFile().getName();
        mRecordView.setCompressFilePath(target);
        final String cmd = "-y -i " + source + " -strict -2 -vcodec libx264 -preset ultrafast -crf 24 -acodec aac -ar 44100 -ac 2 -b:a 96k -s 640x352 -aspect 16:9 " + target;
        final Compressor com = new Compressor(this);
        com.loadBinary(new InitListener() {
            @Override
            public void onLoadSuccess() {
                com.execCommand(cmd, new CompressListener() {
                    @Override
                    public void onExecSuccess(String message) {
                        Log.e(TAG, message);
                        mCompressInfoTv.setVisibility(View.VISIBLE);
                        mCompressInfoTv.setText(message);
                        mVideoSizeTv.setText("\r\n压缩后的大小:" + mRecordView.getCompressFileSize()+"Mb");
                        mDialog.dismiss();
                    }

                    @Override
                    public void onExecFail(String reason) {
                        Log.i(TAG, reason);
                        mCompressInfoTv.setVisibility(View.VISIBLE);
                        mCompressInfoTv.setText(reason);
                        mDialog.dismiss();
                    }

                    @Override
                    public void onExecProgress(String message) {
                        Log.e(TAG, message);
                        mCompressInfoTv.setVisibility(View.VISIBLE);
                        mCompressInfoTv.setText(message);
                    }

                });
            }

            @Override
            public void onLoadFail(String reason) {
                Log.i("fail", reason);
            }
        });
    }

    /**
     * 获得可用存储空间
     *
     * @return 可用存储空间（单位b）
     */
    public long getFreeSpace() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize;//区块的大小
        long totalBlocks;//区块总数
        long availableBlocks;//可用区块的数量
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            totalBlocks = stat.getBlockCountLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            totalBlocks = stat.getBlockCount();
            availableBlocks = stat.getAvailableBlocks();
        }
        Log.e(TAG, "totalSpace：" + blockSize * totalBlocks + "...availableSpace：" + blockSize * availableBlocks);
        return blockSize * availableBlocks;
    }
}