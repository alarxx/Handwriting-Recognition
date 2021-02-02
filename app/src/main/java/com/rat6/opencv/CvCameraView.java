package com.rat6.opencv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.retro.androidgames.framework.Graphics;
import com.retro.androidgames.framework.Input;
import com.retro.androidgames.framework.impl.AndroidGraphics;
import com.retro.androidgames.framework.impl.AndroidInput;

import org.bytedeco.opencv.opencv_core.*;
//import org.bytedeco.javacpp.opencv_core.Mat;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

//import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_NV21;
//import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NV21;
//import org.bytedeco.ffmpeg.avutil.*;

//import org.bytedeco.ffmpeg.avutil..;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NV21;

/**
 * Проблемы:
 *  1) Может быть надо сделать чтобы рисовался Bitmap не с разрешением экрана (может быть большим), а с установленным разрешением.
 *      В таком случае есть проблема растягивания и на разных экранах будет по разному вытянуто, за то сейчас есть черные полоски по краям!
 */

public class CvCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    String LOG_TAG = "DEMO_CvCameraPreview";
    private static final int STOPPED = 0, STARTED = 1;

    public static final int CAMERA_BACK = 99, CAMERA_FRONT = 98;

    private static final int MAGIC_TEXTURE_ID = 10;

    /**
     * ASPECT_RATIO_W and ASPECT_RATIO_H define the aspect ratio
     * of the Surface. They are used when {@link #onMeasure(int, int)}
     * is called.
     */
    private int CANVAS_WIDTH, CANVAS_HEIGHT;

    private int frameWidth, frameHeight;
    private int CAMERA_WIDTH = 1080, CAMERA_HEIGHT = 1920;


    private float CAMERA_RESOLUTION_RATIO = CAMERA_WIDTH / CAMERA_HEIGHT;

    int[] cameraSizes = null;
    public void setCameraSize(int CAMERA_WIDTH, int CAMERA_HEIGHT){
        this.CAMERA_WIDTH = CAMERA_WIDTH;
        this.CAMERA_HEIGHT = CAMERA_HEIGHT;
        CAMERA_RESOLUTION_RATIO = CAMERA_WIDTH / CAMERA_HEIGHT;
        cameraSizes = new int[]{CAMERA_WIDTH, CAMERA_HEIGHT};
    }



    private Bitmap cacheBitmap;

    private Painter painter;

    /**
     * In this example we look at camera preview buffer functionality too.<br />
     * This is the array that will be filled everytime a single preview frame is
     * ready to be processed (for example when we want to show to the user
     * a transformed preview instead of the original one, or when we want to
     * make some image analysis in real-time without taking full-sized pictures).
     */
    private byte[] previewBuffer;

    /**
     * The "holder" is the underlying surface.
     */
    private SurfaceHolder surfaceHolder;

    private FFmpegFrameFilter filter;
    private int chainIdx = 0;
    private boolean stopThread = false;
    private boolean cameraFrameReady = false;
    protected boolean enabled = true;
    private boolean surfaceExist;

    private Thread thread;
    private CvCameraViewListener listener;
    private AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
    private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

    protected Frame[] cameraFrame;
    private int state = STOPPED;
    private final Object syncObject = new Object();
    private int cameraId = -1;
    private int cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;

    private Camera cameraDevice;
    private SurfaceTexture surfaceTexture;

    private Context context;
    private boolean isLandscape;

    private Input input;



    public CvCameraView(Context context, int camType, boolean isLandscape) {
        super(context);
        this.context = context;
        this.isLandscape = isLandscape;
        input = new AndroidInput(context, this, 1, 1);
        initializer(camType == CAMERA_BACK ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private void initializer(int camType) {

        this.surfaceHolder = this.getHolder();
        this.surfaceHolder.addCallback(this);

        this.cameraType = camType;

        // deprecated setting, but required on Android versions prior to API 11
        //if (Build.VERSION.SDK_INT < 11) this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    public void setCvCameraViewListener(CvCameraViewListener listener) {
        this.listener = listener;
    }

    /**
     * Called when the surface is created for the first time. It sets all the
     * required {@link #cameraDevice}'s parameters and starts the preview stream.
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /* Do nothing. Wait until surfaceChanged delivered */
    }

    /**
     * [IMPORTANT!] A SurfaceChanged event means that the parent graphic has changed its layout
     * (for example when the orientation changes). It's necessary to update the {@link CvCameraView}
     * orientation, so the preview is stopped, then updated, then re-activated.
     *
     * @param holder The SurfaceHolder whose surface has changed
     * @param format The new PixelFormat of the surface
     * @param w      The new width of the surface
     * @param h      The new height of the surface
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (this.surfaceHolder.getSurface() == null) {
            Log.e(LOG_TAG, "surfaceChanged(): surfaceHolder is null, nothing to do.");
            return;
        }

        synchronized (syncObject) {
            if (!surfaceExist) {
                surfaceExist = true;
                checkCurrentState();
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters */
                /* Pretend that old surface has been destroyed */
                surfaceExist = false;
                checkCurrentState();
                /* Now use new surface. Say we have it now */
                surfaceExist = true;
                checkCurrentState();
            }
        }
    }

    /**
     * Called when syncObject lock is held
     */
    public void checkCurrentState() {
        Log.d(LOG_TAG, "call checkCurrentState");
        int targetState;

        if (enabled && surfaceExist && getVisibility() == VISIBLE) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != state) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(state);
            state = targetState;
            processEnterState(state);
        }
    }

    private void processExitState(int state) {
        Log.d(LOG_TAG, "call processExitState: " + state);
        switch (state) {
            case STARTED:
                onExitStartedState();
                break;
            case STOPPED:
                onExitStoppedState();
                break;
        }
    }

    private void processEnterState(int state) {
        Log.d(LOG_TAG, "call processEnterState: " + state);
        switch (state) {
            case STARTED:
                onEnterStartedState();
                if (listener != null) {
                    listener.onCameraViewStarted(CAMERA_WIDTH, CAMERA_HEIGHT);
                }
                break;
            case STOPPED:
                onEnterStoppedState();
                if (listener != null) {
                    listener.onCameraViewStopped();
                }
                break;
        }
    }

    private void onEnterStoppedState() {
        /* nothing to do */
    }

    private void onExitStoppedState() {
        /* nothing to do */
    }

    // NOTE: The order of bitmap constructor and camera connection is important for android 4.1.x
    // Bitmap must be constructed before surface
    private void onEnterStartedState() {
        Log.d(LOG_TAG, "call onEnterStartedState");
        /* Connect camera */
        if (!connectCamera()) {
            AlertDialog ad = new AlertDialog.Builder(getContext()).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setMessage("It seems that you device does not support camera (or it is locked). Application will be closed.");
            ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ((Activity) getContext()).finish();
                }
            });
            ad.show();

        }
    }

    private void onExitStartedState() {
        disconnectCamera();
        if (cacheBitmap != null) {
            cacheBitmap.recycle();
        }
        if (filter != null) {
            try {
                filter.release();
            } catch (FrameFilter.Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(LOG_TAG, "surfaceDestroyed");
        synchronized (syncObject) {
            surfaceExist = false;
            checkCurrentState();
        }
    }


    private boolean connectCamera() {
        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(LOG_TAG, "Connecting to camera");
        if (!initializeCamera())
            return false;

        /* now we can start update thread */
        Log.d(LOG_TAG, "Starting processing thread");
        stopThread = false;
        thread = new Thread(new CameraWorker());
        thread.start();

        return true;
    }

    private void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(LOG_TAG, "Disconnecting from camera");
        try {
            stopThread = true;
            Log.d(LOG_TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(LOG_TAG, "Wating for thread");
            if (thread != null)
                thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            thread = null;
        }

        stopCameraPreview();

        /* Now release camera */
        releaseCamera();

        cameraFrameReady = false;
    }

    private boolean initializeCamera() {
        synchronized (this) {
            if (this.cameraDevice != null) {
                // do the job only if the camera is not already set
                Log.i(LOG_TAG, "initializeCamera(): camera is already set, nothing to do");
                return true;
            }

            // we could reach this point in two cases:
            // - the API is lower than 9
            // - previous code block failed
            // hence, we try the classic method, that doesn't ask for a particular camera
            if (this.cameraDevice == null) {
                try {
                    this.cameraDevice = Camera.open();
                    this.cameraId = 0;
//                    this.cameraId = 0;

                } catch (RuntimeException e) {
                    // this is REALLY bad, the camera is definitely locked by the system.
                    Log.e(LOG_TAG,
                            "initializeCamera(): trying to open default camera but it's locked. "
                                    + "The camera is not available for this app at the moment.", e
                    );
                    return false;
                }
            }

            // here, the open() went good and the camera is available
            Log.i(LOG_TAG, "initializeCamera(): successfully set camera #" + this.cameraId);

            setupCamera();

            updateCameraDisplayOrientation();

            initFilter(frameWidth, frameHeight);

            //I don't know how to do it better and I'm in a hurry
            Canvas c = surfaceHolder.lockCanvas();
            CANVAS_WIDTH = c.getWidth();
            CANVAS_HEIGHT = c.getHeight();
            int width = CANVAS_WIDTH;
            int height = CAMERA_HEIGHT * CANVAS_WIDTH / CAMERA_WIDTH;
            painter = new Painter(context.getAssets(), CANVAS_WIDTH, CANVAS_HEIGHT, width, height);
            surfaceHolder.unlockCanvasAndPost(c);

            startCameraPreview(this.surfaceHolder);
        }

        return true;
    }


    /**
     * It sets all the required parameters for the Camera object, like preview
     * and picture size, format, flash modes and so on.
     * In this particular example it initializes the {@link #previewBuffer} too.
     */
    private boolean setupCamera() {
        if (cameraDevice == null) {
            Log.e(LOG_TAG, "setupCamera(): warning, camera is null");
            return false;
        }
        try {
            Camera.Parameters parameters = cameraDevice.getParameters();
            List<Size> sizes = null;
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR)
                sizes = parameters.getSupportedPreviewSizes();

            if (sizes != null) {
                Size bestPreviewSize = getBestSize(sizes);//, PREVIEW_MAX_WIDTH);

                if(cameraSizes!=null) {
                    Log.d("CvCameraPreview1", "s ");
                    for (Size s : sizes)
                        Log.d("CvCameraPreview1", "size: " +"WIDTH="+ s.width + " HEIGHT=" + s.height);

                    for (Size s : sizes) {
                        if ((cameraSizes[0] == s.width || cameraSizes[0]==s.height ) && (cameraSizes[1]==s.height || cameraSizes[1] == s.width)) {
                            bestPreviewSize = s;
                            Log.d("CvCameraPreview1", "s " + s.width + " " + s.height);
                            break;
                        }
                    }
                }



                frameWidth = bestPreviewSize.width;
                frameHeight = bestPreviewSize.height;

                if(isLandscape){
                    CAMERA_WIDTH = frameWidth<frameHeight ? frameHeight : frameWidth;
                    CAMERA_HEIGHT = frameWidth<frameHeight ? frameWidth : frameHeight;
                }else{
                    CAMERA_WIDTH = frameWidth<frameHeight ? frameWidth : frameHeight;
                    CAMERA_HEIGHT = frameWidth<frameHeight ? frameHeight : frameWidth;
                }

                parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);

                parameters.setPreviewFormat(ImageFormat.NV21); // NV21 is the most supported format for preview frames

                List<String> FocusModes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                    FocusModes = parameters.getSupportedFocusModes();
                }
                if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }
                }

                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                cameraDevice.setParameters(parameters); // save everything

                // print saved parameters
                int prevWidth = cameraDevice.getParameters().getPreviewSize().width;
                int prevHeight = cameraDevice.getParameters().getPreviewSize().height;
                int picWidth = cameraDevice.getParameters().getPictureSize().width;
                int picHeight = cameraDevice.getParameters().getPictureSize().height;

                Log.d(LOG_TAG, "setupCamera(): settings applied:\n\t"
                        + "preview size: " + prevWidth + "x" + prevHeight + "\n\t"
                        + "picture size: " + picWidth + "x" + picHeight
                );

                // here: previewBuffer initialization. It will host every frame that comes out
                // from the preview, so it must be big enough.
                // After that, it's linked to the camera with the setCameraCallback() method.
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                        this.previewBuffer = new byte[prevWidth * prevHeight * ImageFormat.getBitsPerPixel(cameraDevice.getParameters().getPreviewFormat()) / 8];
                    }
                    setCameraCallback();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "setupCamera(): error setting camera callback.", e);
                }

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void releaseCamera() {
        synchronized (this) {
            if (cameraDevice != null) {
                cameraDevice.release();
            }
            cameraDevice = null;
        }
    }

    /**
     * [IMPORTANT!] Sets the {@link #previewBuffer} to be the default buffer where the
     * preview frames will be copied. Also sets the callback function
     * when a frame is ready.
     *
     * @throws IOException
     */
    private void setCameraCallback() throws IOException {
        Log.d(LOG_TAG, "setCameraCallback()");
        cameraDevice.addCallbackBuffer(this.previewBuffer);
        cameraDevice.setPreviewCallbackWithBuffer(this);
    }

    /**
     * [IMPORTANT!] This is a convenient function to determine what's the proper
     * preview/picture size to be assigned to the camera, by looking at
     * the list of supported sizes and the maximum value given
     *
     * @param sizes          sizes that are currently supported by the camera hardware,
     *                       retrived with {@link Camera.Parameters#getSupportedPictureSizes()} or {@link Camera.Parameters#getSupportedPreviewSizes()}
     *  widthThreshold the maximum value we want to apply
     * @return an optimal size <= widthThreshold
     */
    private Size getBestSize(List<Size> sizes) {
        Size bestSize = null;

        for (Size currentSize : sizes) {
            Log.d("onCameraViewStarted", "current_size width="+currentSize.width +" height="+currentSize.height);

            boolean isDesiredRatio = (currentSize.width / currentSize.height) == CAMERA_RESOLUTION_RATIO;
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);

            if (isDesiredRatio && isBetterSize)
                bestSize = currentSize;
        }

        if (bestSize == null) {
            bestSize = sizes.get(0);
            Log.e(LOG_TAG, "determineBestSize(): can't find a good size. Setting to the very first...");
        }

        Log.i(LOG_TAG, "determineBestSize(): bestSize is " + bestSize.width + "x" + bestSize.height);

        return bestSize;
    }

    /**
     * In addition to calling {@link Camera#startPreview()}, it also
     * updates the preview display that could be changed in some situations
     *
     * @param holder the current {@link SurfaceHolder}
     */
    private synchronized void startCameraPreview(SurfaceHolder holder) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                surfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                cameraDevice.setPreviewTexture(surfaceTexture);
            } else
                cameraDevice.setPreviewDisplay(holder);
            cameraDevice.startPreview();
            filter.start();
        } catch (Exception e) {
            Log.e(LOG_TAG, "startCameraPreview(): error starting camera preview", e);
        }
    }


    /**
     * It "simply" calls {@link Camera#stopPreview()} and checks
     * for errors
     */
    public synchronized void stopCameraPreview() {
        try {
            cameraDevice.stopPreview();
            cameraDevice.setPreviewCallback(null);
            filter.stop();
        } catch (Exception e) {
            // ignored: tried to stop a non-existent preview
            Log.i(LOG_TAG, "stopCameraPreview(): tried to stop a non-running preview, this is not an error");
        }
    }

    /**
     * Gets the current screen rotation in order to understand how much
     * the surface needs to be rotated
     */
    private void updateCameraDisplayOrientation() {
        if (cameraDevice == null) {
            Log.e(LOG_TAG, "updateCameraDisplayOrientation(): warning, camera is null");
            return;
        }

        int degree = getRotationDegree();

        cameraDevice.setDisplayOrientation(degree); // save settings
    }

    private int getRotationDegree() {
        int result = 0;
        Activity parentActivity = (Activity) this.getContext();

        int rotation = parentActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (Build.VERSION.SDK_INT >= 9) {
            // on >= API 9 we can proceed with the CameraInfo method
            // and also we have to keep in mind that the camera could be the front one
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {
                // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
        } else {
            // TODO: on the majority of API 8 devices, this trick works good
            // and doesn't produce an upside-down preview.
            // ... but there is a small amount of devices that don't like it!
            result = Math.abs(degrees - 90);
        }
        return result;
    }

    private void initFilter(int width, int height) {
        int degree = getRotationDegree();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        boolean isFrontFaceCamera = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        Log.i(LOG_TAG, "init filter with width = " + width + " and height = " + height + " and degree = "
                + degree + " and isFrontFaceCamera = " + isFrontFaceCamera);
        String transposeCode;
        String formatCode = "format=pix_fmts=rgba";
        /*
         0 = 90CounterCLockwise and Vertical Flip (default)
         1 = 90Clockwise
         2 = 90CounterClockwise
         3 = 90Clockwise and Vertical Flip
         */
        switch (degree) {
            case 0:
                transposeCode = isFrontFaceCamera ? "transpose=3,transpose=2" : "transpose=1,transpose=2";
                break;
            case 90:
                transposeCode = isFrontFaceCamera ? "transpose=3" : "transpose=1";
                break;
            case 180:
                transposeCode = isFrontFaceCamera ? "transpose=0,transpose=2" : "transpose=2,transpose=2";
                break;
            case 270:
                transposeCode = isFrontFaceCamera ? "transpose=0" : "transpose=2";
                break;
            default:
                transposeCode = isFrontFaceCamera ? "transpose=3,transpose=2" : "transpose=1,transpose=2";
        }

        if (cameraFrame == null) {
            cameraFrame = new Frame[2];
            cameraFrame[0] = new Frame(width, height, Frame.DEPTH_UBYTE, 2);
            cameraFrame[1] = new Frame(width, height, Frame.DEPTH_UBYTE, 2);
        }

        filter = new FFmpegFrameFilter(transposeCode + "," + formatCode, width, height);
        filter.setPixelFormat(AV_PIX_FMT_NV21);
//        filter.setPixelFormat(24);


        Log.i(LOG_TAG, "filter initialize success");
    }

    @Override
    public void onPreviewFrame(byte[] raw, Camera cam) {
        processFrame(previewBuffer, cam);
        // [IMPORTANT!] remember to reset the CallbackBuffer at the end of every onPreviewFrame event.
        // Seems weird, but it works
        cam.addCallbackBuffer(previewBuffer);
    }

    /**
     * [IMPORTANT!] It's the callback that's fired when a preview frame is ready. Here
     * we can do some real-time analysis of the preview's contents.
     * Just remember that the buffer array is a list of pixels represented in
     * Y'UV420sp (NV21) format, so you could have to convert it to RGB before.
     *
     * @param raw the preview buffer
     * @param cam the camera that filled the buffer
     * @see <a href="http://en.wikipedia.org/wiki/YUV#Y.27UV420sp_.28NV21.29_to_ARGB8888_conversion">YUV Conversion - Wikipedia</a>
     */
    private void processFrame(byte[] raw, Camera cam) {
        if (cameraFrame != null) {
            synchronized (this) {
                ((ByteBuffer) cameraFrame[chainIdx].image[0].position(0)).put(raw);
                cameraFrameReady = true;
                this.notify();
            }
        }
    }

    private class CameraWorker implements Runnable {
        //Привет. Надеюсь вы понимаете что здесь написанно.
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (CvCameraView.this) {
                    try {
                        while (!cameraFrameReady && !stopThread) {
                            CvCameraView.this.wait();
                        }
                    } catch (InterruptedException e) {
                        Log.e(LOG_TAG, "CameraWorker interrupted", e);
                    }
                    if (cameraFrameReady) {
                        chainIdx = 1 - chainIdx;
                        cameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!stopThread && hasFrame) {
                    if (cameraFrame[1 - chainIdx] != null) {
                        try {
                            Frame frame;
                            filter.start();
                            filter.push(cameraFrame[1 - chainIdx]);
                            while ((frame = filter.pull()) != null) {
                                deliverAndDrawFrame(frame);
                                updateTime();
                                listener.update(deltaTime);
                            }
                            filter.stop();
                        } catch (FrameFilter.Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } while (!stopThread);
            Log.d(LOG_TAG, "Finish processing thread");
        }
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     *
     * @param frame - the current frame to be delivered
     */

    protected void deliverAndDrawFrame(Frame frame) {

        Mat processedMat = null;
        if (listener != null) {
            Mat mat = converterToMat.convert(frame);
            //if(mat!=null) mat.copyTo(Extra.mat);
            processedMat = listener.onCameraFrame(mat);

            frame = converterToMat.convert(processedMat);
            if (mat != null) mat.release();
        }
        cacheBitmap = converterToBitmap.convert(frame);
        if (cacheBitmap != null) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {


                painter.draw(canvas);

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        if (processedMat != null) processedMat.release();
    }


    long startTime = System.nanoTime();
    float deltaTime;
    private void updateTime(){
        deltaTime = (System.nanoTime()-startTime) / 1000000000.0f;
        startTime = System.nanoTime();
    }


    public interface CvCameraViewListener {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         *
         * @param camera_width  -  the width of the frames that will be delivered
         * @param camera_height - the height of the frames that will be delivered
         */
        public void onCameraViewStarted(int camera_width, int camera_height);

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        public void onCameraViewStopped();

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        public Mat onCameraFrame(Mat mat);

        public void update(float deltaTime);

        public void present(float deltaTime);
    }

    public class Painter{
        Graphics graphics;
        Bitmap frameBuffer;
        Rect dstCamera;

        public Painter(AssetManager assetManager, int CANVAS_WIDTH, int CANVAS_HEIGHT, int CAMERA_WIDTH, int CAMERA_HEIGHT){
            frameBuffer = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.RGB_565);
            dstCamera = new Rect(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
            graphics = new AndroidGraphics(assetManager, frameBuffer);
        }

        public void draw(Canvas canvas){
            //canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            graphics.clear(Color.BLACK);
            graphics.drawBitmap(cacheBitmap, null, dstCamera, null);
            listener.present(deltaTime);
            canvas.drawBitmap(frameBuffer, 0, 0, null);
        }

        public Graphics getGraphics(){
            return graphics;
        }
    }

    public Bitmap getCacheBitmap(){ return cacheBitmap; }
    public Painter getPainter(){ return painter; }
    public Graphics getGraphics(){ return painter.getGraphics(); }
    public Input getInput(){ return input; }

    public int getCANVAS_WIDTH() { return CANVAS_WIDTH; }
    public int getCANVAS_HEIGHT() { return CANVAS_HEIGHT; }

    public int getCAMERA_HEIGHT() { return CAMERA_HEIGHT; }
    public int getCAMERA_WIDTH() { return CAMERA_WIDTH; }

}