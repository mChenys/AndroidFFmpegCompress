package mchenys.net.csdn.blog.testffmpeg;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 视频录制View
 * Created by mChenys on 2017/1/5.
 */
public class RecordView extends SurfaceView implements MediaRecorder.OnErrorListener {

    private static final String TAG = "RecordView";
    private SurfaceHolder mSurfaceHolder;
    private int mWidthPixel, mHeightPixel;//视频录制分辨率宽度宽高
    private int maxDuration;//最大录制时长,单位秒
    private Camera mCamera;//相机
    private int mPictureSize;//最大支持的像素
    private OnRecordCallback mOnRecordCallback;
    private File mRecordFile;//录制的视频
    private MediaRecorder mMediaRecorder;


    public interface OnRecordCallback {
        void onFinish();

        void onProgress(int total, int curr);
    }

    public void setOnRecordCallback(OnRecordCallback c) {
        this.mOnRecordCallback = c;
    }

    public RecordView(Context context) {
        this(context, null);
    }

    public RecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecorderView);
        mWidthPixel = a.getInteger(R.styleable.RecorderView_width, 640);
        mHeightPixel = a.getInteger(R.styleable.RecorderView_height, 360);
        maxDuration = a.getInteger(R.styleable.RecorderView_maxDuration, 10);
        a.recycle();
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(new CustomCallBack());
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


    }


    private class CustomCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCamera();
        }
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        if (null != mCamera) {
            releaseCamera();
        }
        try {
            if (checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK)) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);//默认打开后置摄像头
            } else if (checkCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            } else {
                Toast.makeText(getContext(), "未发现有摄像头可用", Toast.LENGTH_SHORT).show();
                return;
            }
            Camera.Parameters params = mCamera.getParameters();
            params.set("orientation", "portrait");//竖屏
            //获取最大支持像素
            setBestPictureSize(params);
            //设置最好的预览尺寸
            setBestPreviewSize(params);
            //设置最好的录制尺寸
            setBestVideoSize(params);

            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90); //摆正摄像头
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();//开始预览
            mCamera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
    }

    private void setBestPictureSize(Camera.Parameters params) {
        //遍历获取课支持的最大像素
        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
        Camera.Size best = null;
        for (Camera.Size s : pictureSizes) {
            if (null == best) {
                best = s;
            } else {
                best = (s.height * s.width) > best.height * best.width ? s : best;
            }
        }
        if (null != best) {
            mPictureSize = best.width * best.height;
            Log.e(TAG, "supportedPictureSizes->" + mPictureSize + " width:" + best.width + " height:" + best.height);
        }
    }

    /**
     * 设置支持最好的像素
     */
    private void setBestPreviewSize(Camera.Parameters params) {
        //获取手机支持的分辨率集合，并以宽度为基准降序排序
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        Collections.sort(previewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width > rhs.width) {
                    return -1;
                } else if (lhs.width == rhs.width) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        float minDiff = 100f;
        float ratio = 3.0f / 4.0f;//高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        Camera.Size best = null;
        for (Camera.Size s : previewSizes) {
            float tmp = Math.abs(((float) s.height / (float) s.width) - ratio);
            if (tmp < minDiff) {
                minDiff = tmp;
                best = s;
            }
        }
        if (best != null) {
            //设置最好的预览的size
            params.setPreviewSize(best.width, best.height);
            Log.e(TAG, "getSupportedPreviewSizes best->width:" + best.width + " height:" + best.height);
            Camera.Size big = previewSizes.get(0);
            Log.e(TAG, "getSupportedPreviewSizes big->width:" + big.width + " height:" + big.height);

        }
    }

    public void setBestVideoSize(Camera.Parameters params) {
        if (params.getSupportedVideoSizes() == null || params.getSupportedVideoSizes().size() == 0) {
            //大部分手机支持的预览尺寸和录制尺寸是一样的，也有特例，有些手机获取不到，那就把录制尺寸设置为预览的尺寸
            mWidthPixel = params.getPreviewSize().width;
            mHeightPixel = params.getPreviewSize().height;
        } else {
            //获取手机支持的分辨率集合，并以宽度为基准降序排序
            List<Camera.Size> previewSizes = params.getSupportedVideoSizes();
            Collections.sort(previewSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    if (lhs.width > rhs.width) {
                        return -1;
                    } else if (lhs.width == rhs.width) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            });

            float minDiff = 100f;
            float ratio = 3.0f / 4.0f;//高宽比率3:4，且最接近屏幕宽度的分辨率
            Camera.Size best = null;
            for (Camera.Size s : previewSizes) {
                float tmp = Math.abs(((float) s.height / (float) s.width) - ratio);
                if (tmp < minDiff) {
                    minDiff = tmp;
                    best = s;
                }
            }
            if (null != best) {
                Log.e(TAG, "getSupportedVideoSizes best->width:" + best.width + "...height:" + best.height);
                //设置录制尺寸
                mWidthPixel = best.width;
                mHeightPixel = best.height;

                Camera.Size big = previewSizes.get(0);
                Log.e(TAG, "getSupportedVideoSizes big->width:" + big.width + " height:" + big.height);
            }
        }
    }


    /**
     * 检查是否有摄像头
     *
     * @param facing 前置还是后置
     * @return
     */
    private boolean checkCameraFacing(int facing) {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (facing == info.facing) {
                return true;
            }
        }
        return false;
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mRecordFile = new File(dir, System.currentTimeMillis() + ".mp4");
        if (null == mCamera) {
            initCamera();
        }
        if (null == mMediaRecorder) {
            initRecord();
        }
        mMediaRecorder.start();//开始录制
        startTimeCount();
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        endTimeCount();
        releaseRecord();
        releaseCamera();
    }


    private void initRecord() {
        try {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.reset();
            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);//视频源
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//音频源
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//视频输出格式 也可设为3gp等其他格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//音频格式
            mMediaRecorder.setVideoSize(mWidthPixel, mHeightPixel);//设置分辨率
//        mMediaRecorder.setVideoFrameRate(25);// 设置每秒帧数 这个设置有可能会出问题，有的手机不支持这种帧率就会录制失败，这里使用默认的帧率，当然视频的大小肯定会受影响
            if (mPictureSize < 3000000) {//这里设置可以调整清晰度
                mMediaRecorder.setVideoEncodingBitRate(3 * 1024 * 512);
            } else if (mPictureSize <= 5000000) {
                mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 512);
            } else {
                mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 512);
            }
            mMediaRecorder.setOrientationHint(90);//输出旋转90度，保持竖屏录制
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//视频录制格式
            //mMediaRecorder.setMaxDuration(Constant.MAXVEDIOTIME * 1000);
            mMediaRecorder.setOutputFile(mRecordFile.getAbsolutePath());
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
        }
    }


    private CountDownTimer mCountDownTimer;

    /**
     * 开始计时
     */
    private void startTimeCount() {
        endTimeCount();
        mCountDownTimer = new CountDownTimer(maxDuration * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (null != mOnRecordCallback) {
                    mOnRecordCallback.onProgress(maxDuration, (int) (maxDuration - millisUntilFinished / 1000));
                }
            }

            @Override
            public void onFinish() {

                if (null != mOnRecordCallback) {
                    mOnRecordCallback.onFinish();
                }
            }
        }.start();
    }

    private void endTimeCount() {
        if (null != mCountDownTimer) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
    }

    private void releaseRecord() {
        if (null != mMediaRecorder) {
            try {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewDisplay(null);
                mCamera.stopPreview();
                mCamera.lock();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回录像文件
     *
     * @return recordFile
     */
    public File getRecordFile() {
        return mRecordFile;
    }
}
