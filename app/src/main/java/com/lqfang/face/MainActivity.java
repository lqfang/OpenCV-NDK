package com.lqfang.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.xinhuo.facesdk.FaceNative;
import com.xinhuo.facesdk.module.FaceInfo;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    // 获取 FaceNative 对象
    FaceNative faceNative = null;
    // 存放模型文件夹的路径
    public static String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.image);

        // ***************  获取人脸检测相关信息  ************
        faceNative = new FaceNative();
        path = getExternalFilesDir("model").getPath();

        // 加载模型，获取FD对象的值（long）
        faceNative.fd = faceNative.createFD(faceNative.getFDPath(this));
        // 加载模型，获取FL对象的值（long）
        faceNative.fl = faceNative.createFL(faceNative.getFLPath(this));

        String headPath = faceNative.getHeadPath(this);
//        Log.e("tag", "head===="+ headPath);
        getRect(headPath);
    }

    /**
     * 传入人脸图像，获取矩形框和关键点的坐标，绘制到人脸
     */
    private void getRect(String pathName) {
        //图片路径转Bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap1 = BitmapFactory.decodeFile(pathName, options);
        Log.e("tag", "bitmap1===="+ bitmap1);
        if(bitmap1 == null) return;

        Mat mat1 = new Mat();
        Utils.bitmapToMat(bitmap1, mat1);

        // 将4通道转为3通道
        Mat mat = new Mat();
        Imgproc.cvtColor(mat1, mat, Imgproc.COLOR_RGBA2BGR);

        //得到当前一帧图像的内存地址
        long resultAddress = mat.getNativeObjAddr();
        if (resultAddress < 0) {
            return;
        }
        // long转16进制的(String)
        String sAddr = Long.toHexString(resultAddress);
        Log.e("FaceDetector", "图片的内存地址====" + sAddr);

        // 获取人脸矩形框的坐标值
        FaceInfo faceInfo = faceNative.detectRect(faceNative.fd, resultAddress);
        Log.e("tag", "faceInfo===="+ faceInfo);
        if (faceInfo == null) return;

        // 定义一个左上角点坐标为(_x, _y)的_width*_height矩形窗口；
        // 在识别出来的人脸周围画出一个方框
        Rect rect = new Rect(faceInfo.getX(), faceInfo.getY(), faceInfo.getWidth(), faceInfo.getHeight());

        // 矩形框绘制在人像上
        Imgproc.rectangle(mat, new Point(rect.x - 2, rect.y - 2),
                new Point(rect.x + rect.width, rect.y + rect.height),
                new Scalar(255, 0, 255), 3);

        // 获取人脸关键点定位的坐标值
        double[] point = faceNative.landmarkPoint(faceNative.fd, faceNative.fl, resultAddress);
        if (point == null) return;

        // 返回了162个数，其实是81个点的坐标，所以i<点的个数
        for (int i = 0; i < point.length / 2; i++) {
            // 获取关键点的坐标值()
            double doubleX = point[2 * i];
            double doubleY = point[2 * i + 1];
            // 关键点绘制在人像上
            Imgproc.circle(mat, new Point(doubleX, doubleY), 2, new Scalar(128, 255, 128), -1);
        }

        // 显示把图片三通道转为四通道（如果图片不转为四通道，显示的是灰度图）
        Mat mat2 = new Mat();
        Imgproc.cvtColor(mat, mat2, Imgproc.COLOR_BGR2RGBA);
        // 将Mat转为Bitmap
        Utils.matToBitmap(mat2, bitmap1);

//        Log.e("tag", "bitmap===="+ bitmap1);
        imageView.setImageBitmap(bitmap1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁模型
        if(faceNative != null){
            faceNative.destroyFD(faceNative.fd);
            faceNative.destroyFL(faceNative.fl);
            faceNative = null;
        }

    }
}
