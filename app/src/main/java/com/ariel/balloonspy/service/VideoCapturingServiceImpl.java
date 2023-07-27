package com.ariel.balloonspy.service;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;

import com.ariel.balloonspy.activity.MainActivity;
import com.ariel.balloonspy.utility.LoRaSendMessage;
import com.ariel.balloonspy.utility.Log;
import com.ariel.balloonspy.utility.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

import wseemann.media.FFmpegMediaMetadataRetriever;


/**
 * The aim of this service is to secretly take pictures (without preview or opening device's camera app)
 * from all available cameras using Android Camera 2 API
 *
 * @author hzitoun (zitoun.hamed@gmail.com)
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP) //NOTE: camera 2 api was added in API level 21
public class VideoCapturingServiceImpl extends APictureCapturingService {

    private static final String TAG = VideoCapturingServiceImpl.class.getSimpleName();

    private String pictureUrl;
    private String compressUrl;

    private CameraDevice cameraDevice;
    //private ImageReader imageReader;
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;
    private CameraCaptureSession mSession;
    /***
     * camera ids queue.
     */
    private Queue<String> cameraIds;

    private String currentCameraId;
    private boolean cameraClosed;
    /**
     * stores a sorted map of (pictureUrlOnDisk, PictureData).
     */
    private TreeMap<String, byte[]> picturesTaken;
    private PictureCapturingListener capturingListener;

    /***
     * private constructor, meant to force the use of {@link #getInstance}  method
     */
    private VideoCapturingServiceImpl(final Activity activity) {
        super(activity);
    }

    /**
     * @param activity the activity used to get the app's context and the display manager
     * @return a new instance
     */
    public static APictureCapturingService getInstance(final Activity activity) {
        return new VideoCapturingServiceImpl(activity);
    }

    /**
     * Starts pictures capturing treatment.
     *
     * @param listener picture capturing listener
     */
    @Override
    public void startCapturing(final PictureCapturingListener listener) {

        //stopCapturing(listener);

        this.picturesTaken = new TreeMap<>();
        this.capturingListener = listener;
        this.cameraIds = new LinkedList<>();
        try {
            final String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length > 0) {
                //this.cameraIds.addAll(Arrays.asList(cameraIds));
                //this.currentCameraId = this.cameraIds.poll();
                //openCamera();

                for (String cameraId : cameraIds) {
                    CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing == CameraMetadata.LENS_FACING_BACK) {
                        this.cameraIds.addAll(Arrays.asList(cameraId));
                        this.currentCameraId = this.cameraIds.poll();
                        openCamera();
                        break;
                    }
                }

            } else {
                //No camera detected!
                capturingListener.onDoneCapturingAllPhotos(picturesTaken);
            }

        } catch (final CameraAccessException e) {
            Log.e(TAG, "Exception occurred while accessing the list of cameras", e);
        }
    }

    /**
     * Starts pictures capturing treatment.
     */
    @Override
    public void stopCapturing() {
        try {
            stopRecordingVideo();
        } catch (final Exception e) {
            Log.e(TAG, "Exception occurred while accessing the list of cameras", e);
        }
    }

    private void openCamera() {
        Log.d(TAG, "opening camera " + currentCameraId);
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(currentCameraId, stateCallback, null);
            }
        } catch (final Exception e) {
            Log.e(TAG, " exception occurred while opening camera " + currentCameraId, e);
        }
    }

    private final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (picturesTaken.lastEntry() != null) {
                capturingListener.onCaptureDone(picturesTaken.lastEntry().getKey(), picturesTaken.lastEntry().getValue());
                Log.i(TAG, "done taking picture from camera " + cameraDevice.getId());
            }
            closeCamera();
        }
    };


    /*private final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader imReader) -> {
        final Image image = imReader.acquireLatestImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        saveImageToDisk(bytes);
        image.close();
    };*/

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraClosed = false;
            Log.d(TAG, "camera " + camera.getId() + " opened");
            cameraDevice = camera;
            Log.i(TAG, "Taking picture from camera " + camera.getId());
            //Take the picture after some delay. It may resolve getting a black dark photos.
            new Handler().postDelayed(() -> {
                try {
                    takeVideo();
                } catch (final CameraAccessException e) {
                    Log.e(TAG, " exception occurred while taking picture from " + currentCameraId, e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 250);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " camera " + camera.getId() + " disconnected");
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true;
                cameraDevice.close();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            cameraClosed = true;
            Log.d(TAG, "camera " + camera.getId() + " closed");
            //once the current camera has been closed, start taking another picture
            if (!cameraIds.isEmpty()) {
                takeAnotherPicture();
            } else {
                capturingListener.onDoneCapturingAllPhotos(picturesTaken);
            }
        }


        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "camera in error, int code " + error);
            if (cameraDevice != null && !cameraClosed) {
                cameraDevice.close();
            }
        }
    };


    /*private void takePicture() throws CameraAccessException {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        Size[] jpegSizes = null;
        StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap != null) {
            jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        }
        final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
        int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
        int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
        final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        final List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(reader.getSurface());
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        captureBuilder.addTarget(reader.getSurface());
        //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_SCENE_MODE_BEACH);
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());

        reader.setOnImageAvailableListener(onImageAvailableListener, null);
        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            //session.capture(captureBuilder.build(), captureListener, null);
                            session.setRepeatingRequest(captureBuilder.build(),
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                                        }

                                        @Override
                                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                            super.onCaptureCompleted(session, request, result);
                                        }
                                    }, null);

                        } catch (final CameraAccessException e) {
                            Log.e(TAG, " exception occurred while accessing " + currentCameraId, e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }
                , null);
    }*/

    private void takeVideo() throws CameraAccessException, IOException {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        //final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        //Size[] jpegSizes = null;
        //StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //if (streamConfigurationMap != null) {
        //    jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        //}
        //final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
        //int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
        //int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
        //final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

        setUpMediaRecorder();

        final List<Surface> outputSurfaces = new ArrayList<>();
        //outputSurfaces.add(reader.getSurface());
        outputSurfaces.add(mMediaRecorder.getSurface());

        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        //captureBuilder.addTarget(reader.getSurface());
        captureBuilder.addTarget(mMediaRecorder.getSurface());

        //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());
        //reader.setOnImageAvailableListener(onImageAvailableListener, null);

        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //captureBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 1L);
        //captureBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, 10000000000L);

        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            mSession = session;
                            mMediaRecorder.start();

                            mSession.setRepeatingRequest(captureBuilder.build(),
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                                        }

                                        @Override
                                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                            super.onCaptureCompleted(session, request, result);
                                        }
                                    }, null);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    stopRecordingVideo();
                                }
                            }, vidTime * 1000);

                        } catch (final CameraAccessException e) {
                            Log.e(TAG, " exception occurred while accessing " + currentCameraId, e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        mSession = session;
                    }
                }
                , null);
    }

    private void takeAnotherPicture() {
        this.currentCameraId = this.cameraIds.poll();
        openCamera();
    }

    private void closeCamera() {
        try {
            Log.d(TAG, "closing camera " + cameraDevice.getId());
            if (null != cameraDevice && !cameraClosed) {
                cameraDevice.close();
                cameraDevice = null;
            }
            /*if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void setUpMediaRecorder() throws CameraAccessException, IOException {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        try {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mMediaRecorder.setVideoEncodingBitRate(2000000);
        mMediaRecorder.setVideoFrameRate(24);

        // Choose the sizes for camera preview and video recording
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        StreamConfigurationMap map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new RuntimeException("Cannot get available preview/video sizes");
        }

        Size mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setMaxDuration(vidTime * 1000); // 20 seconds
        mMediaRecorder.setMaxFileSize(5 * 1000000); // Approximately 5 megabytes
        mMediaRecorder.setOrientationHint(getOrientation());


        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String pictureFile = "prbl_" + timeStamp;
        if (null != metadata && metadata.length() > 0) {
            pictureFile = pictureFile + metadata;
        }
        if (vidTime < 10) {
            pictureFile = pictureFile + "_P";
        } else {
            pictureFile = pictureFile + "_V";
        }
        final String pathname = Environment.getExternalStorageDirectory() + File.separator + "prbl" + File.separator + "vid" + File.separator;
        final File outputFile = new File(pathname + pictureFile + ".mp4");

        pictureUrl = pathname + pictureFile;
        compressUrl = pathname + "/compressed/" + pictureFile;

        Log.i("VDO", outputFile.getName());
        LoRaSendMessage.performPostCall(outputFile.getName(), 3);
        Utils.showToast(MainActivity.getInstance(), "Video saved to " + outputFile.getName());

        outputFile.createNewFile();
        mMediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        mMediaRecorder.prepare();
    }

    public void stopRecordingVideo() {
        try {
            try {
                mSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            closeCamera();

            FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever();
            med.setDataSource(pictureUrl + ".mp4");
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Bitmap bmp = med.getFrameAtTime(24*10*1000*5, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            //convert array of bytes into file
            FileOutputStream fileOuputStream =
                    new FileOutputStream(pictureUrl + ".jpeg");
            fileOuputStream.write(out.toByteArray());
            byte[] ba = out.toByteArray();
            fileOuputStream.close();

//            AndroidBmpUtil.save(bmp3,pictureUrl + ".bmp");

//            VideoCompress.compressVideoLow(pictureUrl + ".mp4", compressUrl + ".mp4",
//                    new VideoCompress.CompressListener() {
//
//                        @Override
//                        public void onStart() {
//                            // Compression is started.
//                        }
//
//                        @Override
//                        public void onSuccess() {
//                            // Compression is successfully finished.
//                            // Utils.showToast(MainActivity.getInstance(), "Compression is finished for " + compressUrl);
//                        }
//
//                        @Override
//                        public void onFail() {
//                            // Compression is failed.
//                            // Utils.showToast(MainActivity.getInstance(), "Compression is failed for " + compressUrl);
//                        }
//
//                        @Override
//                        public void onProgress(float percent) {
//                            // Compression is in progress.
//                            // Utils.showToast(MainActivity.getInstance(), "Compression is in progress for " + compressUrl);
//                        }
//                    });

        } catch (RuntimeException stopException) {
            mMediaRecorder.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
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
            return choices[0];
        }
    }
}
