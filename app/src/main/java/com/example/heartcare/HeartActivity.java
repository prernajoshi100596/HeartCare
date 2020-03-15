package com.example.heartcare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.BitmapCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heartcare.Math.Fft;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT;
import static java.lang.Math.ceil;

public class HeartActivity extends AppCompatActivity {

    User user;

    private static final String TAG = "HeartActivity";
    private TextureView textureView; //TextureView to deploy camera data
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    // Thread handler member variables
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    //Heart rate detector member variables
    private int mCurrentRollingAverage;
    private int mLastRollingAverage;
    private int mLastLastRollingAverage;
    private long [] mTimeArray;
    private int numCaptures = 0;
    private int mNumBeats = 0;
    TextView tv;

    private LineChart mChart;
    private Thread thread;
    private boolean plotData = true;

    //    -------------------------------------------------------
    private Button bpm_btn;
    private boolean bpm_start = false;
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    public int Beats=0;
    public double bufferAvgB=0;

    //Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    //Arraylist
    public ArrayList<Double> GreenAvgList=new ArrayList<Double>();
    public ArrayList<Double> RedAvgList=new ArrayList<Double>();
    public int counter = 0;

    //BloodPressure variables
    public double Agg ,Hei ,Wei;   //height should be in feet // weight should be in lbs
    public double Q =5;   //4.5 for female and 5 for male
    private static int SP = 0, DP = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart);

        user = (User) getIntent().getSerializableExtra("user");
        Agg = user.getAge();
        Hei = user.getHeight()*30.48;
        Wei = user.getWeight()*2.205;
        if(user.getGender()==2.0)
            Q = 4.5;


        textureView =  findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        mTimeArray = new long [15];
        tv = (TextView)findViewById(R.id.neechewalatext);
        bpm_btn = (Button) (findViewById(R.id.bpm_btn));

        mChart = (LineChart)findViewById(R.id.linechart);
        mChart.setTouchEnabled(false);
        mChart.setScaleEnabled(true);
        mChart.setDragEnabled(false);
        mChart.setDrawGridBackground(true);
        mChart.setGridBackgroundColor(Color.BLACK);
        mChart.setPinchZoom(false);
        mChart.getDescription().setEnabled(false);
        mChart.getDescription().setText("Heart Monitor");

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        entries = new ArrayList<Entry>();

        Legend l = mChart.getLegend();

        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(true);

        mChart.setScaleYEnabled(true);

        startPlot();

//        ------------------------------------------

        bpm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bpm_start^=true;
            }
        });

    }

    private void startPlot(){
        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            //            ignore
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureUpdated");
            Bitmap bmp = textureView.getBitmap();
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int[] pixels = new int[height * width];
            // Get pixels from the bitmap, starting at (x,y) = (width/2,height/2)
            // and totaling width/20 rows and height/20 columns
            bmp.getPixels(pixels, 0, width, width / 2, height / 2, width / 20, height / 20);
            int sum = 0;
            for (int i = 0; i < height * width; i++) {
                int red = (pixels[i] >> 16) & 0xFF;
                sum = sum + red;
            }

            if(bpm_start) {
                computePressure(bmp);
            }
//             Waits 20 captures, to remove startup artifacts.  First average is the sum.
            if(plotData) {
                prev_sum = sum;
                addEntry(sum);
                plotData = false;
            }

            if (numCaptures == 20) {
                mCurrentRollingAverage = sum;
            }
            // Next 18 averages needs to incorporate the sum with the correct N multiplier
            // in rolling average.
            else if (numCaptures > 20 && numCaptures < 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*(numCaptures-20) + sum)/(numCaptures-19);
            }
            // From 49 on, the rolling average incorporates the last 30 rolling averages.
            else if (numCaptures >= 49) {
                mCurrentRollingAverage = (mCurrentRollingAverage*29 + sum)/30;
                if (mLastRollingAverage > mCurrentRollingAverage && mLastRollingAverage > mLastLastRollingAverage /*&& mNumBeats < 15*/) {
                    // a beat is considered

//                    tv.setText("beats="+mNumBeats/*+"\ntime="+mTimeArray[mNumBeats]*/);
                    mNumBeats++;

                }
            }

            // Another capture
            numCaptures++;
            // Save previous two values
            mLastLastRollingAverage = mLastRollingAverage;
            mLastRollingAverage = mCurrentRollingAverage;
        }
    };

    private void computePressure(Bitmap bmp){
        if (!processing.compareAndSet(false, true) && !bpm_start) return;

        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int bytes = BitmapCompat.getAllocationByteCount(bmp);

        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        bmp.copyPixelsToBuffer(buffer); //Move the byte data to the buffer

        byte[] pixels = buffer.array();

        double GreenAvg;
        double RedAvg;

        GreenAvg=ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(pixels.clone(), height, width,3); //1 stands for red intensity, 2 for blue, 3 for green
        RedAvg=ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(pixels.clone(), height, width,1); //1 stands for red intensity, 2 for blue, 3 for green

        GreenAvgList.add(GreenAvg);
        RedAvgList.add(RedAvg);

//        System.out.println("RED:"+RedAvg+" GREEN:"+GreenAvg+" counter="+counter);

        ++counter;  // countes number of frames in 30 seconds
        System.err.println("Red:"+RedAvg+" Green:"+GreenAvg+" counter:"+counter);

        // To check if we got a good red intensity to process if not return to the condition and set it again until we get a good red intensity
        if(RedAvg < 100){
            counter = 0;
            processing.set(false);
        }

        long endTime = System.currentTimeMillis();
        double totalTimeInSecs = (endTime - startTime) / 1000d; //to convert time to seconds
        System.err.println("totalTimeInSecs="+totalTimeInSecs);
        tv.setText("Remaining Time : "+(int)(30 - totalTimeInSecs));

        if (totalTimeInSecs >= 30) { //when 30 seconds of measuring passes do the following " we chose 30 seconds to take half sample since 60 seconds is normally a full sample of the heart beat

            Double[] Green = GreenAvgList.toArray(new Double[GreenAvgList.size()]);
            Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);

            SamplingFreq =  (counter/totalTimeInSecs); //calculating the sampling frequency

            double HRFreq = Fft.FFT(Green, counter, SamplingFreq); // send the green array and get its fft then return the amount of heartrate per second
            double bpm=(int)ceil(HRFreq*60);
            double HR1Freq = Fft.FFT(Red, counter, SamplingFreq);  // send the red array and get its fft then return the amount of heartrate per second
            double bpm1=(int)ceil(HR1Freq*60);

            // The following code is to make sure that if the heartrate from red and green intensities are reasonable
            // take the average between them, otherwise take the green or red if one of them is good

            if((bpm > 45 || bpm < 200) )
            {
                if((bpm1 > 45 || bpm1 < 200)) {

                    bufferAvgB = (bpm+bpm1)/2;
                }
                else{
                    bufferAvgB = bpm;
                }
            }
            else if((bpm1 > 45 || bpm1 < 200)){
                bufferAvgB = bpm1;
            }

            if (bufferAvgB < 45 || bufferAvgB > 200) { //if the heart beat wasn't reasonable after all reset the progresspag and restart measuring
//                Toast.makeText(getApplicationContext(), "Measurement Failed *pulse rate:"+bufferAvgB, Toast.LENGTH_SHORT).show();
                startTime = System.currentTimeMillis();
                counter=0;
                processing.set(false);
                return;
            }

            Beats=(int)bufferAvgB;
            double ROB = 18.5;
            double ET = (364.5-1.23*Beats);
            double BSA = 0.007184*(Math.pow(Wei,0.425))*(Math.pow(Hei,0.725));
            double SV = (-6.6 + (0.25*(ET-35)) - (0.62*Beats) + (40.4*BSA) - (0.51*Agg));
            double PP = SV / ((0.013*Wei - 0.007*Agg-0.004*Beats)+1.307);
            double MPP = Q*ROB;

            SP = (int) (MPP + 3/2*PP);
            DP = (int) (MPP - PP/3);
//            Toast.makeText(this, "MPP:"+MPP+" PP:"+PP, Toast.LENGTH_SHORT).show();
        }

        if(Beats != 0 && SP!=0 && DP!=0){ //if beasts were reasonable stop the loop
            tv.setText("Pulse Rate : "+Beats+"\nBlood Pressure : "+SP+" - "+DP);
            Intent i = new Intent(getApplicationContext(), ResultActivity.class);
            i.putExtra("user", user);
            i.putExtra("HR", Beats);
            i.putExtra("SP", SP);
            i.putExtra("DP", DP);

            bpm_start = false;
            processing.set(true);
            startActivity(i);

            finish();
            onStop();
            return;
        }

        //keeps taking frames tell 30 seconds
        processing.set(false);
    }

    LineData data;
    ArrayList<Entry> entries;
    LineDataSet set;

    int prev_sum = 0;

    private void addEntry(int sum){
        data = mChart.getData();
        set = (LineDataSet) data.getDataSetByIndex(0);
        if(set!=null)
            set.setCircleRadius(0f);

        if(data!=null){
            if(set==null){
                createSet();
                data.addDataSet(set);
            }

            if(sum<250000 && sum>=300000)
                return;

            entries.add(new Entry(set.getEntryCount()+0, (float)sum));
            System.out.println("SUM="+sum);

            set = new LineDataSet(entries, "DataSet 1");
            data = new LineData(set);
            mChart.setData(data);

            data.notifyDataChanged();

            mChart.setVisibleXRange(0,50);
            mChart.moveViewToX(data.getEntryCount()-51);
            mChart.getLegend().setEnabled(false);

            prev_sum = sum;
        }
    }

    private void createSet(){
        set = new LineDataSet(null, "DataSet 1");
        set.setAxisDependency(LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.WHITE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.05f);
        set.setFillAlpha(255);
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (cameraDevice != null)
                cameraDevice.close();
            cameraDevice = null;
        }
    };

    // onResume
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    // onPause
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(HeartActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Opening the rear-facing camera for use
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(HeartActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(HeartActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");

        closeCamera();
        stopBackgroundThread();
        bpm_start = false;

        if(thread!=null){
            thread.interrupt();
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        bpm_start = false;
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if(thread!=null){
            thread.interrupt();
        }
        super.onDestroy();
    }
}
