package gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.util.Log;

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";

    // simple quad
    private final float[] VERTEX_COORDS = {
            -1f,  1f, 0.0f,
            -1f, -1f, 0.0f,
             1f, -1f, 0.0f,
             1f,  1f, 0.0f
    };

    private final float[] TEX_COORDS = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private FloatBuffer vertexBuffer, texBuffer;
    private int program;
    private int textureId = -1;

    private int viewWidth = 0, viewHeight = 0;
    private int frameWidth = 0, frameHeight = 0;

    // volatile frame data written from UI thread and consumed in GL thread
    private volatile byte[] latestFrameRGBA = null;

    public GLRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(VERTEX_COORDS).position(0);

        texBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texBuffer.put(TEX_COORDS).position(0);
    }

    public void updateFrame(byte[] rgbaBytes, int width, int height) {
        // called from UI thread
        this.latestFrameRGBA = rgbaBytes;
        this.frameWidth = width;
        this.frameHeight = height;
    }

    @Override
    public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl10,
                                 javax.microedition.khronos.egl.EGLConfig eglConfig) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        textureId = createTexture();
    }

    @Override
    public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl10,
                                 int width, int height) {
        viewWidth = width;
        viewHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        int positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        int texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        int textureHandle = GLES20.glGetUniformLocation(program, "uTexture");

        GLES20.glUseProgram(program);

        // set up vertices
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
                3 * 4, vertexBuffer);

        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false,
                2 * 4, texBuffer);

        // upload frame if available
        if (latestFrameRGBA != null && frameWidth > 0 && frameHeight > 0) {
            ByteBuffer bb = ByteBuffer.allocateDirect(latestFrameRGBA.length);
            bb.order(ByteOrder.nativeOrder());
            bb.put(latestFrameRGBA).position(0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            // Upload the RGBA pixels (no glPixelStorei necessary for default packing)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    frameWidth, frameHeight, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        // draw quad (triangle fan)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    private int createTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        int textureId = tex[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return textureId;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Shader compile failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private int createProgram(String vs, String fs) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vertexShader);
        GLES20.glAttachShader(prog, fragmentShader);
        GLES20.glLinkProgram(prog);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            prog = 0;
        }
        return prog;
    }

    // Vertex & fragment shaders
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "  vec4 color = texture2D(uTexture, vTexCoord);\n" +
            "  gl_FragColor = color;\n" +
            "}\n";
}
