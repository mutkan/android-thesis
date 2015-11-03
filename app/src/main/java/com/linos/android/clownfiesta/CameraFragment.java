package com.linos.android.clownfiesta;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by linos on 10/24/15.
 */
public class CameraFragment extends Fragment implements View.OnClickListener {
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    /*
    * Current Camera State
    * */
    private int mState = STATE_PREVIEW;
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    /**
     * Tag for debugging
     */
    private static final String TAG = "CameraFragment";
    /*
    * Method accessible from Activity , in order to modify GPStextField
    * */
    TextView mGPStext ;

    public void setGPStext(String val){
        mGPStext.setText(val);
    }
    /* Listener for several events on TextureView */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener(){
        @Override
        /* if Texture's surface is available , openCamera() */
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
            Log.e(TAG,"width = "+ width+ " height = "+ height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            /* if Texture's Surface's size is changed, modify the transform matrix to match the surface. */
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {

        }

    };

    /* Camera's unique ID */
    private String mCameraId;

    /* The main textureview where camera's live feed is going to */
    private AutoFitTextureView mTextureView;

    /* CameraSession for preview */
    private CameraCaptureSession mCameraCaptureSession;

    /* A pointer to the camera device we have opened */
    private CameraDevice mCameraDevice;

    /*  The Preview Size we are picking out of the StreamConfigurationMap */
    public static Size mPreviewSize;

    /*  Semaphore to acquiring/releasing on opening/closing */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private final CameraDevice.StateCallback mStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            createPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }
        /*  In case camera crashes */
        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice=null;
            Activity activity = getActivity();
            if (activity==null)
                    return;
            Toast.makeText(activity.getApplicationContext(),"Camera Error!",Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
                activity.finish();
        }
    };


    private void createPreview(){
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture!=null;

            /* Setting up our texture's buffer size , based on the chosen one , out of camera's StreamConfigureMap */
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            /* The surface on which we will display our preview */
            Surface surface = new Surface(texture);

            /* Setting up Preview request builder */
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            /* We must input all surfaces we're gonna use later (ImageReader's surface too) */
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession= cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /* A separate thread to handle other work , so that our app doesnt become unresponsive */
    private HandlerThread mBackgroundThread;

    /* A handler to run all the background work */
    private Handler mBackgroundHandler;

    /* ImageReader to render our captured image on before storing/viewing it */
    private ImageReader mImageReader;

    /* The output file */
    private File mFile;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(),mFile));
                }
            };
    /*The builder for our preview request*/
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /* Our request for previewing */
    private CaptureRequest mPreviewRequest;
    /****/
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process (CaptureResult res){

            /*
            * Determine what kind of result we have based on the camera state and do the work
            * */
            switch(mState) {
                case STATE_PREVIEW: {
                    /* No need for extra stuff for this state */
                    break;
                }
                case STATE_WAITING_LOCK: {
                    /* check if we can take a pic */
                    Integer afState = res.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null)
                        captureStillImage();
                    else if (CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = res.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillImage();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = res.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // We are ready to shoot~!
                    Integer aeState = res.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillImage();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            process(result);
        }
    };

    private void runPrecaptureSequence(){
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Notify the CaptureCallback to wait for precapture to finish
            mState=STATE_WAITING_PRECAPTURE;
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(),mCaptureCallback,
                    mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    private void captureStillImage(){
        try {
            final Activity act = getActivity();
            if(act==null || mCameraDevice == null){
                return;
            }
            /*  Starting to build the capture request by setting up the Auto-Focus and Auto-Exposure modes */
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            /*Setting up the correct orientation of the picture by get the window rotation out of the activity*/
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback=
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            showToast("Saved At ="+ mFile);
                            unlockFocus();
                        }
                    };
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    /*This is called right after we take the picture , and we want to unlock focus and go back to preview mode*/
    private void unlockFocus(){
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onClick(View view){
        lockFocus();
    }
    /* Start Taking the picture .Trigger Focus stabilizing */
    private void lockFocus(){
        try{
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            mState=STATE_WAITING_LOCK;
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(),mCaptureCallback,
                    mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    /*  The class that is responsible to save the image to the file on the SD card  */
    private static class ImageSaver implements Runnable{
        private final Image mImage;

        private final File mFile;

        public ImageSaver(Image image,File file){
            mImage= image;
            mFile = file;
        }
        public void run(){
            ByteBuffer buffer =mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                mImage.close();
                if(output != null){
                    try{
                        output.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void showToast(final String text){
        final Activity act = getActivity();
        if (act != null){
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(act, text,Toast.LENGTH_SHORT).show();
                }
            });
        }

    }
    /*Comparing to elements based on their areas*/
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    private void setUpCameraOutputs(int width,int height){
        Activity act = getActivity();
        CameraManager manager = (CameraManager)act.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()){
                CameraCharacteristics characteristics =
                        manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing!=null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null)
                    return;
                /* For still image captures, we use the largest available size.
                 *( we dont have to display it on our texture view so we dont care about sizes)
                 **/
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),
                        ImageFormat.JPEG,2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mBackgroundHandler);
                /*Too big preview size leads to distortion(Gamwwww , fact!)*/
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);

                Log.e(TAG,"PreviewSize: Width = "+ mPreviewSize.getWidth() + " height= "+mPreviewSize.getHeight());
                //Fit the aspect ratio of the TextureView to the chosen mPreviewSize
                /*int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE){
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(),mPreviewSize.getHeight());
                }else{
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(),mPreviewSize.getWidth());
                }*/

                mCameraId= cameraId;
                return;
            }

        }catch (CameraAccessException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }
    private Size chooseOptimalSize(Size[] map,int width,int height,Size aspectRatio){
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : map) {
            //Log.e(TAG,"Option width "+option.getWidth() + " Height = "+option.getHeight());
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return map[0];
        }
    }
    private void openCamera(int width,int height){
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity act = getActivity();
        CameraManager manager = (CameraManager) act.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("Time out waiting to lock camera opening");
            }

            manager.openCamera(mCameraId,mStateCallBack,mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera opening",e);
        }
    }
    private void closeCamera(){
        /*Tying up loose ends Kappa*/
        try {
            mCameraOpenCloseLock.acquire();
            if(mCameraCaptureSession!=null){
                mCameraCaptureSession.close();
                mCameraCaptureSession=null;
            }
            if (mCameraDevice!=null){
                mCameraDevice.close();
                mCameraDevice=null;
            }
            if (mImageReader!=null){
                mImageReader.close();
                mImageReader=null;
            }
        }catch (InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera closing.",e);
        }finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its Handler.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    /**
     * Stops the background thread and its Handler.
     */
    private void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread=null;
            mBackgroundHandler=null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }
    /*
    * Overriden fragment class' methods
    * **/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camerafragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.picture).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mGPStext = (TextView) view.findViewById(R.id.GPS);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /*A method that other classes can use to make new CameraFragment Objects*/
    public static CameraFragment newInstance(){
        return new CameraFragment();
    }

}
