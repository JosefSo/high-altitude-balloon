package com.ariel.balloonspy.utility;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Class that simplifies the use of Camera 2 api
 *
 * @author Omar Aflak
 * @since 23/02/2017
 */

public class EZCam {

    private static final String TAG = EZCam.class.getSimpleName();

    private Context context;
    private EZCamCallback cameraCallback;
    private boolean cameraClosed;

    private SparseArray<String> camerasList;
    private String currentCamera;
    private Size previewSize;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest.Builder captureRequestBuilderImageReader;
    private ImageReader imageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public EZCam(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    /**
     * Set callback to receive camera states
     * @param cameraCallback callback
     */
    public void setCameraCallback(EZCamCallback cameraCallback) {
        this.cameraCallback = cameraCallback;
    }

    /**
     * Get available cameras
     * @return SparseArray of available cameras ids
     */
    public SparseArray<String> getCamerasList(){
        camerasList = new SparseArray<>();
        try {
            String[] camerasAvailable = cameraManager.getCameraIdList();
            CameraCharacteristics cam;
            Integer characteristic;
            for (String id : camerasAvailable){
                cam = cameraManager.getCameraCharacteristics(id);
                characteristic = cam.get(CameraCharacteristics.LENS_FACING);
                if (characteristic!=null){
                    switch (characteristic){
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            camerasList.put(CameraCharacteristics.LENS_FACING_FRONT, id);
                            break;

                        case CameraCharacteristics.LENS_FACING_BACK:
                            camerasList.put(CameraCharacteristics.LENS_FACING_BACK, id);
                            break;

                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                camerasList.put(CameraCharacteristics.LENS_FACING_EXTERNAL, id);
                            }
                            break;
                    }
                }
            }
            return camerasList;
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
            return null;
        }
    }

    /**
     * Select the camera you want to open : front, back, external(s)
     * @param id Id of the camera which can be retrieved with getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK)
     */
    public void selectCamera(String id) {
        if(camerasList == null){
            getCamerasList();
        }

        currentCamera = camerasList.indexOfValue(id)<0?null:id;
        if(currentCamera == null) {
            notifyError("Camera id not found.");
            return;
        }

        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(currentCamera);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map != null) {
                previewSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                // Ronyr
                Size[] jpegSizes = null;
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigurationMap != null) {
                    jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                }
                final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
                int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
                int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
                imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
                // end rony

                //imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailable, backgroundHandler);
            }
            else{
                notifyError("Could not get configuration map.");
            }
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    /**
     * Open camera to prepare preview
     * @param templateType capture mode e.g. CameraDevice.TEMPLATE_PREVIEW
     * //@param textureView Surface where preview should be displayed
     */
    public void open(final int templateType, final TextureView textureView) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            notifyError("You don't have the required permissions.");
            return;
        }

        startBackgroundThread();

        try {
            cameraManager.openCamera(currentCamera, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraClosed = false;
                    cameraDevice = camera;
                    setupPreview(templateType, textureView);

                    //Rony
                    Log.i(TAG, "Taking picture from camera " + camera.getId());
                    //Take the picture after some delay. It may resolve getting a black dark photos.
                    new Handler().postDelayed(() -> {
                        try {
                            takePicture();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, 500);//rony
                    // end Rony
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    if(cameraCallback != null){
                        cameraCallback.onError("Camera device is no longer available for use.");
                        cameraCallback.onCameraDisconnected();
                    }
                    if (cameraDevice != null && !cameraClosed) {
                        cameraClosed = true;
                        cameraDevice.close();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    switch (error){
                        case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                            notifyError("Camera device has encountered a fatal error.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                            notifyError("Camera device could not be opened due to a device policy.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                            notifyError("Camera device is in use already.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                            notifyError("Camera service has encountered a fatal error.");
                            break;
                        case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                            notifyError("Camera device could not be opened because there are too many other open camera devices.");
                            break;
                    }
                    if (cameraDevice != null && !cameraClosed) {
                        cameraDevice.close();
                    }
                }

            }, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    private void setupPreview_(int templateType, TextureView textureView){
        Surface surface = new Surface(textureView.getSurfaceTexture());

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(templateType);
            captureRequestBuilder.addTarget(surface);

            captureRequestBuilderImageReader = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilderImageReader.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    if(cameraCallback != null){
                        cameraCallback.onCameraReady();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    notifyError("Could not configure capture session.");
                }
            }, backgroundHandler);
        } catch (Exception e) {
            notifyError(e.getMessage());
        }
    }

    private void setupPreview(final int templateType, final TextureView outputSurface){
        if(outputSurface.isAvailable()){
            setupPreview_(templateType, outputSurface);
        }
        else{
            outputSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setAspectRatioTextureView(outputSurface, width, height);
                    setupPreview_(templateType, outputSurface);
                }

                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {return false;}
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
        }
    }

    /**
     * Set CaptureRequest parameters for preview e.g. flash, auto-focus, macro mode, etc.
     * @param key e.g. CaptureRequest.CONTROL_EFFECT_MODE
     * @param value e.g. CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE
     */
    public<T> void setCaptureSetting(CaptureRequest.Key<T> key, T value){
        if(captureRequestBuilder!=null && captureRequestBuilderImageReader!=null) {
            captureRequestBuilder.set(key, value);
            captureRequestBuilderImageReader.set(key, value);
        }
    }

    /**
     * Get characteristic of selected camera e.g. available effects, scene modes, etc.
     * @param key e.g. CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS
     */
    public<T> T getCharacteristic(CameraCharacteristics.Key<T> key){
        if(cameraCharacteristics!=null) {
            return cameraCharacteristics.get(key);
        }
        return null;
    }

    private void setAspectRatioTextureView(TextureView textureView, int surfaceWidth, int surfaceHeight)
    {
        int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        int newWidth = surfaceWidth, newHeight = surfaceHeight;

        switch (rotation) {
            case Surface.ROTATION_0:
                newWidth = surfaceWidth;
                newHeight = (surfaceWidth * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_180:
                newWidth = surfaceWidth;
                newHeight = (surfaceWidth * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_90:
                newWidth = surfaceHeight;
                newHeight = (surfaceHeight * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_270:
                newWidth = surfaceHeight;
                newHeight = (surfaceHeight * previewSize.getWidth() / previewSize.getHeight());
                break;
        }

        textureView.setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
        rotatePreview(textureView, rotation, newWidth, newHeight);
    }

    private void rotatePreview(TextureView mTextureView, int rotation, int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * start the preview, capture request is built at each call here
     */
    public void startPreview(){
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    /**
     * stop the preview
     */
    public void stopPreview(){
        try {
            cameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    /**
     * shortcut to call stopPreview() then startPreview()
     */
    public void restartPreview(){
        stopPreview();
        startPreview();
    }

    /**
     * close the camera definitively
     */
    //public void close(){
    //    cameraDevice.close();
    //    stopBackgroundThread();
    //}

    public void close() {
        try {
            Log.d(TAG, "closing camera " + cameraDevice.getId());
            if (null != cameraDevice && !cameraClosed) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopBackgroundThread();
    }

    /**
     * take a picture
     */
    public void takePicture(){
        captureRequestBuilderImageReader.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
        try {
            cameraCaptureSession.capture(captureRequestBuilderImageReader.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        } catch (Exception e){
            notifyError(e.getMessage());
        }
    }

    public void takeBurstPicture(){
        List<CaptureRequest> list = new ArrayList<CaptureRequest>();
        captureRequestBuilderImageReader.set(CaptureRequest.JPEG_ORIENTATION, 90);

        for (int i=0; i<10; i++){
            list.add(captureRequestBuilderImageReader.build()); // create a list of captureRequest -- 10 is the number of burst images that will be taken.
        }
        try {
            cameraCaptureSession.setRepeatingBurst(list, null, backgroundHandler); // start burst capture by passing in the list of captureRequest
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    private ImageReader.OnImageAvailableListener onImageAvailable = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(cameraCallback != null){
                cameraCallback.onPicture(imageReader.acquireLatestImage());
            }
        }
    };

    private void notifyError(String message) {
        if (cameraCallback != null) {
            cameraCallback.onError(message);
        }
    }


    public static File saveImageToDisk(Image image) {
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String pictureFile = "prbl_" + timeStamp;
        final String pathname = Environment.getExternalStorageDirectory() + File.separator + "prbl" + File.separator + "pic" + File.separator;
        final File file = new File(pathname + pictureFile + ".jpg");

        try (final OutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            image.close();
            output.close();
        } catch (final IOException e) {
            Log.e("EZC", "Exception occurred while saving picture to external storage ", e);
        }
        return file;
    }


    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("EZCam");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            notifyError(e.getMessage());
        }
    }

}