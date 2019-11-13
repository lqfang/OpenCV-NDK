package com.lqfang.face;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.lqfang.face.opengl.CameraOverlap;
import com.lqfang.face.opengl.EGLUtils;
import com.lqfang.face.opengl.GLBitmap;
import com.lqfang.face.opengl.GLFrame;
import com.lqfang.face.opengl.GLFramebuffer;
import com.lqfang.face.opengl.GLPoints;
import com.lqfang.face.opengl.GLRect;
import com.xinhuo.facesdk.FaceNative;
import com.xinhuo.facesdk.module.FaceInfo;

import java.util.ArrayList;

public class PreviewActivity extends AppCompatActivity {

    private String[] denied;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        // 通过动态权限打开相机
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    list.add(permissions[i]);
                }
            }
            if (list.size() != 0) {
                denied = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    denied[i] = list.get(i);
                }
                ActivityCompat.requestPermissions(this, denied, 5);
            } else {
                init();
            }
        } else {
            init();
        }
    }


    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private byte[] mNv21Data;
    private CameraOverlap cameraOverlap;
    private final Object lockObj = new Object();

    private SurfaceView mSurfaceView;

    private EGLUtils mEglUtils;
    private GLFramebuffer mFramebuffer;
    private GLFrame mFrame;
    private GLPoints mPoints;
    private GLBitmap mBitmap;
    private GLRect mRect;

    private int mWidth = CameraOverlap.PREVIEW_WIDTH;
    private int mHeight = CameraOverlap.PREVIEW_HEIGHT;

    // 获取 FaceNative 对象
    FaceNative faceNative = null;

    private void init() {
        cameraOverlap = new CameraOverlap(this);
        mNv21Data = new byte[mWidth * mHeight * 2];
        mFramebuffer = new GLFramebuffer();
        mFrame = new GLFrame();
        mPoints = new GLPoints();
        // Todo 注意宽高的数值，宽要比高小
        mRect = new GLRect(mHeight, mWidth);

        mBitmap = new GLBitmap(this, R.drawable.ic_launcher);
        mHandlerThread = new HandlerThread("DrawFacePointsThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        // ******************  获取人脸检测的相关信息  ***********************
        faceNative = new FaceNative();
        faceNative.fd = faceNative.createFD(faceNative.getFDPath(this));
        faceNative.fl = faceNative.createFL(faceNative.getFLPath(this));


        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mEglUtils != null) {
                            mEglUtils.release();
                        }
                        mEglUtils = new EGLUtils();
                        mEglUtils.initEGL(holder.getSurface());
                        mFramebuffer.initFramebuffer();
                        mFrame.initFrame();
                        mFrame.setSize(width, height, mHeight, mWidth);
                        mPoints.initPoints();
                        mBitmap.initFrame(mHeight, mWidth);
                        mRect.initRect();
                        cameraOverlap.openCamera(mFramebuffer.getSurfaceTexture());
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cameraOverlap.release();
                        mFramebuffer.release();
                        mFrame.release();
                        mPoints.release();
                        mRect.release();
                        mBitmap.release();
                        if (mEglUtils != null) {
                            mEglUtils.release();
                            mEglUtils = null;
                        }
                    }
                });
            }
        });
        if (mSurfaceView.getHolder().getSurface() != null && mSurfaceView.getWidth() > 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mEglUtils != null) {
                        mEglUtils.release();
                    }
                    mEglUtils = new EGLUtils();
                    mEglUtils.initEGL(mSurfaceView.getHolder().getSurface());
                    mFramebuffer.initFramebuffer();
                    mFrame.initFrame();
                    mFrame.setSize(mSurfaceView.getWidth(), mSurfaceView.getHeight(), mHeight, mWidth);
                    mPoints.initPoints();
                    mBitmap.initFrame(mHeight, mWidth);
                    mRect.initRect();
                    cameraOverlap.openCamera(mFramebuffer.getSurfaceTexture());
                }
            });
        }


        cameraOverlap.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                synchronized (lockObj) {
                    System.arraycopy(data, 0, mNv21Data, 0, data.length);
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mEglUtils == null) {
                            return;
                        }
                        mFrame.setS(1.0f);
                        mFrame.setH(0.0f);
                        mFrame.setL(0.0f);

                        // 保存每一帧头像到本地
                        // BitmapUtils.getBitmap(data, mWidth, mHeight, PreviewActivity.this);

                        // Frame绘制要在关键点和矩形框之前
                        mFrame.drawFrame(0, mFramebuffer.drawFrameBuffer(), mFramebuffer.getMatrix());

                        // 获取人脸矩形框的坐标值
                        FaceInfo faceInfo = faceNative.detect(faceNative.fd, data, mWidth, mHeight, 270);

                        // 获取人脸关键点定位的坐标值
                        double[] point = faceNative.landmark(faceNative.fd, faceNative.fl, data, mWidth, mHeight, 270);

                        // 预览相机中没有人像，就绘制相机
                        if(faceInfo == null || point == null){
                            mEglUtils.swap();
                            return;
                        }

                        // 返回矩形框的处理
                        if (faceInfo != null) {
                            // 在识别出来的人脸周围画出一个方框
                            int left = faceInfo.getX();
                            int top = faceInfo.getY();
                            int right = left + faceInfo.getWidth();
                            int bottom = faceInfo.getHeight() + top;
                            // 左上(x, y) ， 右下(x + width, y + height)
                            Rect rect1 = new Rect( left, top,   right, bottom);

                            if (mRect != null) {
                                Rect[] rects = {rect1};
                                mRect.drawRect(rects);
                            }
                        }
                        // 返回关键点的处理
                        if (point != null){
                            getPoint(point);
                        }

                        mEglUtils.swap();
                    }
                });
            }
        });
    }


    /**
     * 关键点 landmark 处理
     */
    private void getPoint(double[] point) {
        float[] p = null;
        float[] points = new float[81 * 2];

        // 返回了162个数，其实是81个点的坐标，所以i<点的个数
        for (int i = 0; i < point.length / 2; i++) {
            // 获取关键点的坐标值()
            double doubleX = CameraOverlap.PREVIEW_HEIGHT - point[2 * i];
            double doubleY = point[2 * i + 1];
            // 调整关键点的坐标
            int x = (int) doubleX;
            int y = (int) doubleY;
            points[i * 2] = view2openglX(x, CameraOverlap.PREVIEW_HEIGHT);
            points[i * 2 + 1] = view2openglY(y, CameraOverlap.PREVIEW_WIDTH);
            if (i == 70) {
                p = new float[8];
                p[0] = view2openglX(x + 20, CameraOverlap.PREVIEW_HEIGHT);
                p[1] = view2openglY(y - 20, CameraOverlap.PREVIEW_WIDTH);
                p[2] = view2openglX(x - 20, CameraOverlap.PREVIEW_HEIGHT);
                p[3] = view2openglY(y - 20, CameraOverlap.PREVIEW_WIDTH);
                p[4] = view2openglX(x + 20, CameraOverlap.PREVIEW_HEIGHT);
                p[5] = view2openglY(y + 20, CameraOverlap.PREVIEW_WIDTH);
                p[6] = view2openglX(x - 20, CameraOverlap.PREVIEW_HEIGHT);
                p[7] = view2openglY(y + 20, CameraOverlap.PREVIEW_WIDTH);
            }
        }

        if (points != null) {
            mPoints.setPoints(points);
            mPoints.drawPoints();
        }
    }

    private float view2openglX(int x, int width) {
        float centerX = width / 2.0f;
        float t = x - centerX;
        return t / centerX;
    }

    private float view2openglY(int y, int height) {
        float centerY = height / 2.0f;
        float s = centerY - y;
        return s / centerY;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5) {
            boolean isDenied = false;
            for (int i = 0; i < denied.length; i++) {
                String permission = denied[i];
                for (int j = 0; j < permissions.length; j++) {
                    if (permissions[j].equals(permission)) {
                        if (grantResults[j] != PackageManager.PERMISSION_GRANTED) {
                            isDenied = true;
                            break;
                        }
                    }
                }
            }
            if (isDenied) {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                init();

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放打开的相机资源
        if(cameraOverlap != null){
            cameraOverlap.release();
            cameraOverlap = null;
        }

    }
}
