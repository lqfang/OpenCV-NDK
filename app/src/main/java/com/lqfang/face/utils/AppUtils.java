package com.lqfang.face.utils;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppUtils {

    /**
     * 获取资源路径（把assets等文件夹中的资源文件保存在本地）
     */
    public static String getPath(Context context, String path, String fileName) {
        String pathName = "";
        byte[] mData = new byte[0];
        //从assets中读
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            //缓冲
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] bytes = new byte[1024];
            while ((len = inputStream.read(bytes)) != -1) {
                //缓冲  bytes:写多少  0 从哪开始写   len 内容
                baos.write(bytes, 0, len);
            }
            mData = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(path == null) path = context.getExternalFilesDir("model").getPath();
        File mFile = new File(path);
        if (!mFile.exists()) {
            mFile.mkdir();//创建文件夹
        }
        //存 SD
        try {
            FileOutputStream fos = new FileOutputStream(new File(mFile, fileName));
            fos.write(mData, 0, mData.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pathName = String.valueOf(mFile);

        return pathName;
    }
}
