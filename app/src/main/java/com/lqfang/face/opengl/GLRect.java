package com.lqfang.face.opengl;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GLRect {

    private FloatBuffer floatBuffer;
    private IntBuffer intBuffer;
    private int programId;
    private int aPosition;

    public GLRect() {
    }

    private int width;
    private int height;
    private int faceCount = 0;

    public GLRect(int width, int height) {
//        Log.e("tag", "width====" + width + ", height====" + height);
        this.width = width;
        this.height = height;
    }

    private float[] vertexData = {
            0.5f, 0.5f, 0.0f,    // top right
            0.5f, -0.5f, 0.0f,   // bottom right
            -0.5f, -0.5f, 0.0f,  // bottom left
            -0.5f, 0.5f, 0.0f    // top left
    };

    private int[] index = {
            0, 1,  //右上
            0, 3,  //右下
            1, 2,  //左下
            2, 3   //左上
    };

    // 顶点缓冲区对象即Vertex Buffer Object，简称VBO
    private int[] VBO = new int[1];
    // 数组缓冲对象
    private int[] VAO = new int[1];
    // 索引缓冲对象
    private int[] VEO = new int[1];

    // 顶点着色器
    private String vertexShader =
            "attribute vec3 aPosition;\n" +
                    "void main() {\n" +
                    "    gl_Position = vec4(aPosition, 1.0);\n" +
                    "}";
    // 片元着色器
    private String fragmentShader =
            "void main() {\n" +
                    "    gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);\n" +
                    "}";

    /**
     * 在OpenGL ES 3.0中，可以使用三种方法将通用顶点属性索引映射到顶点着色器中的一个属性变量名称，
     * 一是在顶点着色器源代码中用布局限定符layout指定，推荐使用这种方式，
     * 二是OpenGL ES 3.0在程序链接阶段为没有指定索引的顶点绑定一个属性索引，
     * 三是应用程序使用glBindAttribLocation进行绑定
     */
    public void initRect() {
//        Log.e("tag", "init====");
        // 创建缓冲区对象，n为对象数量， buffers保存创建的对象
        GLES30.glGenBuffers(1, VBO, 0);
        GLES30.glGenVertexArrays(1, VAO, 0);
        GLES30.glGenBuffers(1, VEO, 0);

        floatBuffer = ByteBuffer.allocateDirect(4 * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        floatBuffer.position(0);

        intBuffer = ByteBuffer.allocateDirect(index.length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        intBuffer.position(0);

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
//        Log.e("tag", "programId====" + programId);
        // glGetAttribLocation查询绑定到属性变量name的顶点属性索引，如果没有绑定，则内部实现自动绑定到一个顶点属性索引，这发生在链接阶段，失败时返回-1。
        aPosition = GLES30.glGetAttribLocation(programId, "aPosition");
//        Log.e("tag", "aPosition====" + aPosition);
    }

    public void drawRect(Rect[] rects) {
        if (rects == null || rects.length == 0) {
            return;
        }

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        // 新生成的线条坐标数组
        vertexData = new float[rects.length * 4 * 3];
//        Log.e("tag", "vertexData====" + vertexData.length);

        // 索引数组对象
        index = new int[rects.length * 4 * 2];
//        Log.e("tag", "index====" + index.length);
        for (int i = 0; i < rects.length; i++) {
            Rect rect = rects[i];
            float right = (float) (halfWidth - rect.left) / halfWidth;
            float top = (float) (halfHeight - rect.top) / halfHeight;
            float left = -(float) (rect.right - halfWidth) / halfWidth;
            float bottom = -(float) (rect.bottom - halfHeight) / halfHeight;

            // ****  镜像 左右调换 ****
            // 右上角
            vertexData[i * 12] = right;
            vertexData[i * 12 + 1] = top;
            vertexData[i * 12 + 2] = 0.0f;

            // 右下角
            vertexData[i * 12 + 3] = right;
            vertexData[i * 12 + 4] = bottom;
            vertexData[i * 12 + 5] = 0.0f;

            // 左下角
            vertexData[i * 12 + 6] = left;
            vertexData[i * 12 + 7] = bottom;
            vertexData[i * 12 + 8] = 0.0f;

            // 左上角
            vertexData[i * 12 + 9] = left;
            vertexData[i * 12 + 10] = top;
            vertexData[i * 12 + 11] = 0.0f;

            index[i * 8] = 4 * i;
            index[i * 8 + 1] = 4 * i + 1;
            index[i * 8 + 2] = 4 * i;
            index[i * 8 + 3] = 4 * i + 3;
            index[i * 8 + 4] = 4 * i + 1;
            index[i * 8 + 5] = 4 * i + 2;
            index[i * 8 + 6] = 4 * i + 2;
            index[i * 8 + 7] = 4 * i + 3;
        }

        if (faceCount != rects.length) {
            floatBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            intBuffer = ByteBuffer.allocateDirect(index.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
            faceCount = rects.length;
        }

        floatBuffer.rewind();
        floatBuffer.put(vertexData);
        floatBuffer.position(0);

        intBuffer.rewind();
        intBuffer.put(index);
        intBuffer.position(0);

        draw(faceCount);
    }

    public void drawRect(Rect rect) {
//        Log.e("tag", "rect====" + rect);
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        float left = - -(float) (rect.right - halfWidth) / halfWidth;
        float right = (float) (halfWidth - rect.left) / halfWidth;
        float top = (float) (halfHeight - rect.top) / halfHeight;
        float bottom = -(float) (rect.bottom - halfHeight) / halfHeight;

//        Log.e("tag", "left====" + left +"\n, right==="+ right +
//                "\n, top===" + top + "\n, bottom==="+ bottom);

        float[] newRect = {
                // 右上角
                right, top, 0.0f,
                // 右下角
                right, bottom, 0.0f,
                // 左上角
                left, top, 0.0f,
                // 右下角
                left, bottom, 0.0f
        };
        floatBuffer.rewind();
        floatBuffer.put(newRect);
        floatBuffer.position(0);

        intBuffer.rewind();
        intBuffer.put(index);
        intBuffer.position(0);

        draw(1);
    }

    private void draw(int count) {
        // 绑定、激活缓冲区对象， buffer为缓冲区对象，不存在时自动创建，之前的绑定将失效，0为保留值，将解绑所有缓冲区对象
        GLES30.glBindBuffer(GLES20.GL_ARRAY_BUFFER, VBO[0]);
        GLES30.glBindVertexArray(VAO[0]);
        GLES30.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, VEO[0]);
        // 创建并初始化缓冲区对象数据，之前数据将被清除，size为对象的数据长度，data为对象的数据（可以为NULL）
        // GL_STATIC_DRAW 修改一次，较少使用，陈谷修改数据，用于GL绘图和图片相关命令
        GLES30.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, floatBuffer, GLES20.GL_STATIC_DRAW);
        GLES30.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, index.length * 4, intBuffer, GLES20.GL_STATIC_DRAW);

        GLES30.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, 0);
        //启用矩形顶点的句柄
        GLES20.glEnableVertexAttribArray(aPosition);
        // 将程序加入到OpenGLES3.0的环境
        GLES30.glUseProgram(programId);
        GLES30.glLineWidth(5);
        //索引法绘制矩形
        GLES30.glDrawElements(GLES20.GL_LINES, count * 8, GLES20.GL_UNSIGNED_INT, 0);

    }

    public void release() {
        GLES30.glDeleteProgram(programId);
        GLES30.glDeleteBuffers(1, index, 0);
    }
}
