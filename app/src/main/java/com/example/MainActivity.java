package com.example;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import com.github.anastr.speedviewlib.TubeSpeedometer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


public class MainActivity extends AppCompatActivity {
    private MainActivity mainActivity;

    private static final int REQUEST_PICK_IMAGE = 2;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private ImageView resultImageView;

    private TextView thresholdTextview;
    private TextView tvInfo;
    private TextView detectedObjectView;
    private TubeSpeedometer speedometer;
    private TextView tvSpeedLimit;
    private Button button,exitBtn;

    private final double threshold = 0.35;
    private final double nms_threshold = 0.7;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;

    private final AtomicBoolean detecting = new AtomicBoolean(false);
    private final AtomicBoolean detectPhoto = new AtomicBoolean(false);

    private long startTime = 0;
    private long endTime = 0;
    private int width;
    private int height;
    private static final Paint boxPaint = new Paint();

    private TextRecognizer recognizer;

    ArrayList<Detection> detected = new ArrayList<>();
    ArrayList<String> test = new ArrayList<>();

    private final HashMap<Integer, Detection> detectedSpeedLimits = new HashMap<Integer, Detection>();
    private final Stack<Detection> detectedSpeedLimitsStack = new Stack<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mainActivity = this;

        ActivityCompat.requestPermissions(this, PERMISSIONS, PackageManager.PERMISSION_GRANTED);
        while ((ContextCompat.checkSelfPermission(this.getApplicationContext(), PERMISSIONS[0]) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this.getApplicationContext(), PERMISSIONS[1]) == PackageManager.PERMISSION_DENIED)) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!this.isLocationEnabled(this)) {
            //show dialog if Location Services is not enabled
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.gps_not_found_title);  // GPS not found
            builder.setMessage(R.string.gps_not_found_message); // Want to enable?
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            //if no - bring user to selecting Static Location Activity
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(mainActivity, "Please enable Location-based service / GPS", Toast.LENGTH_LONG).show();
                }
            });
            builder.create().show();
        }

        setContentView(R.layout.activity_main);
        YOLOv4.init(getAssets());

        resultImageView = findViewById(R.id.imageView);
        thresholdTextview = findViewById(R.id.valTxtView);
        tvInfo = findViewById(R.id.tv_info);
        tvSpeedLimit = findViewById(R.id.tvSpeedLimit);
        button = findViewById(R.id.my_btn);
        exitBtn = findViewById(R.id.exit_btn);
        speedometer = (TubeSpeedometer) findViewById(R.id.speedView);
        speedometer.setMaxSpeed(200f);

        recyclerView = findViewById(R.id.rv_detections);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter(detected,this);
        recyclerView.setAdapter(adapter);
        recyclerView.hasFixedSize();
        final String format = "Thresh: %.2f, NMS: %.2f";
        thresholdTextview.setText(String.format(Locale.ENGLISH, format, threshold, nms_threshold));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detected.clear();
                adapter.notifyDataSetChanged();
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });

        // ML-Kit Text Recognizer
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        new SpeedTask(this).execute("string");

        resultImageView.setOnClickListener(v -> detectPhoto.set(false));
        startCamera();
    }


    private void startCamera() {
        CameraX.unbindAll();

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(CameraX.LensFacing.BACK)
                .setTargetResolution(new Size(416, 416))  // Resolution
                .build();

        Preview preview = new Preview(previewConfig);
        DetectAnalyzer detectAnalyzer = new DetectAnalyzer();
        CameraX.bindToLifecycle(this, preview, gainAnalyzer(detectAnalyzer));

    }

    private UseCase gainAnalyzer(DetectAnalyzer detectAnalyzer) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder();
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(416, 416));  // Output Preview ImageSize
        ImageAnalysisConfig config = analysisConfigBuilder.build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setAnalyzer(detectAnalyzer);
        return analysis;
    }

    private class DetectAnalyzer implements ImageAnalysis.Analyzer {
        final String LOG = "DETECTOR";

        @Override
        public void analyze(ImageProxy image, final int rotationDegrees) {
            if (detecting.get() || detectPhoto.get()) {
                return;
            }
            detecting.set(true);
            final Bitmap bitmapSrc = imageToBitmap(image);  // Format Conversion

            // Detection Thread
            Thread detectThread = new Thread(() -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                width = bitmapSrc.getWidth();
                height = bitmapSrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapSrc, 0, 0, width, height, matrix, false);

                startTime = System.currentTimeMillis();
                Box[] result = YOLOv4.detect(bitmap, threshold, nms_threshold);
                endTime = System.currentTimeMillis();

                final Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                float strokeWidth = 4 * (float) mutableBitmap.getWidth() / 800;
                float textSize = 30 * (float) mutableBitmap.getWidth() / 800;

                Canvas canvas = new Canvas(mutableBitmap);
                boxPaint.setAlpha(255);
                boxPaint.setTypeface(Typeface.SANS_SERIF);
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(strokeWidth);
                boxPaint.setTextSize(textSize);

                try {
                    for (Box box : result) {
                        boxPaint.setColor(box.getColor());
                        boxPaint.setStyle(Paint.Style.FILL);
                        String score = Integer.toString((int) (box.getScore() * 100));

                        final String[] label = {""};
                        label[0] = box.getLabel();

                        // Perform OCR for speed limit detections
                        if ("speed limit".equals(box.getLabel())) {
                            RectF rect = box.getRect();

                            if (!detectedSpeedLimits.containsKey(box.getId())) {
                                detectedSpeedLimits.put(box.getId(), new Detection(box.getLabelId(), box.getId()));
                            }

                            // Crop speed limit Bounding box
                            assert (rect.left < rect.right && rect.top < rect.bottom);
                            Bitmap croppedBmp = Bitmap.createBitmap(mutableBitmap,
                                    (int) rect.left, (int) rect.top,
                                    (int) rect.width(), (int) rect.height());
                            Bitmap scaledBmp = Bitmap.createScaledBitmap(croppedBmp,
                                    600, 600, true);
                            InputImage ocrImg = InputImage.fromBitmap(scaledBmp, 0);

                            // Do OCR
                            Task<Text> recognizerResult = recognizer.process(ocrImg)
                                    .addOnSuccessListener(visionText -> {
                                        // Task completed successfully
                                        String resultText = processTextRecognitionResult(visionText);
                                        label[0] = resultText;
                                        if (detectedSpeedLimits.containsKey(box.getId())) {
                                            Detection det = detectedSpeedLimits.get(box.getId());
                                            String lastText = det.getSpeed().replaceAll("[^0-9]", "");
                                            det.setSpeed(resultText);
                                            detectedSpeedLimits.replace(box.getId(), det);
                                            if (!lastText.equals(resultText.replaceAll("[^0-9]", ""))) {
                                                detectedSpeedLimitsStack.push(det);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Task failed with an exception
                                        Log.e(LOG + "OCR", e.toString());
                                    });
                            // Wait for OCR to finish
                            Tasks.await(recognizerResult);
                        }
                        // Set Bounding Boxes and Labels
                        canvas.drawText(label[0] + " [" + score + "%]",
                                box.x0 - strokeWidth, box.y0 - strokeWidth
                                , boxPaint);
                        canvas.drawText("id: " + box.getId(), box.x1 - strokeWidth, box.y1 - strokeWidth, boxPaint);
                        boxPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawRect(box.getRect(), boxPaint);
                        detected.add(new Detection(box.getLabel(),box.getId()));
                        Log.d("detected",detected.toString());
                    }
                } catch (Exception e) {
                    Log.e(LOG, e.toString());
                }

                // Run UI Drawing on UI Thread
                runOnUiThread(() -> {
                    resultImageView.setImageBitmap(mutableBitmap);
                    detecting.set(false);
                    long dur = endTime - startTime;
                    float fps = (float) (1000.0 / dur);
                    tvInfo.setText(String.format(Locale.ENGLISH,
                            "ImgSize: %dx%d\nUseTime: %d ms\nDetectFPS: %.2f",
                            height, width, dur, fps));
                    if (!detectedSpeedLimitsStack.empty()) {
                        Detection newDetection = detectedSpeedLimitsStack.pop();
                        tvSpeedLimit.setText(newDetection.getSpeed().replaceAll("[^0-9]", ""));
                    }
                    adapter.notifyDataSetChanged();
                    if(detected.size()>0){
                        recyclerView.scrollToPosition(detected.size()-1);
                    }

                });
            }, "detect");
            detectThread.start();
        }

        private Bitmap imageToBitmap(ImageProxy image) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ImageProxy.PlaneProxy y = planes[0];
            ImageProxy.PlaneProxy u = planes[1];
            ImageProxy.PlaneProxy v = planes[2];
            ByteBuffer yBuffer = y.getBuffer();
            ByteBuffer uBuffer = u.getBuffer();
            ByteBuffer vBuffer = v.getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            // U and V are swapped
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();

            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
    }

    @Override
    protected void onDestroy() {
        CameraX.unbindAll();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                this.finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        detectPhoto.set(true);
        Bitmap image = getPicture(data.getData());
        startTime = System.currentTimeMillis();
        Box[] result = YOLOv4.detect(image, threshold, nms_threshold);
        endTime = System.currentTimeMillis();

        final Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        float strokeWidth = 4 * (float) mutableBitmap.getWidth() / 800;
        float textSize = 30 * (float) mutableBitmap.getWidth() / 800;
        Canvas canvas = new Canvas(mutableBitmap);
        boxPaint.setAlpha(255);
        boxPaint.setTypeface(Typeface.SANS_SERIF);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);
        boxPaint.setTextSize(textSize);


        int scoreAvg = 0;
        for (Box box : result) {
            int score = (int) (box.getScore() * 100);
            scoreAvg += score;

            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel() + " [" + score + "%]",
                    box.x0 - strokeWidth, box.y0 - strokeWidth
                    , boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }

        if (result.length != 0) {
            scoreAvg = scoreAvg / result.length;
        }
        resultImageView.setImageBitmap(mutableBitmap);
        detecting.set(false);
        long dur = endTime - startTime;
        tvInfo.setText(String.format(Locale.CHINESE,
                "ImgSize: %dx%d\nUseTime: %d ms\nAvgMatchScore: %d%%",
                height, width, dur, scoreAvg));

    }

    private String processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        // Fallback to Object label
        if (blocks.size() == 0) {
            return "speed limit";
        }
        return texts.getText();
    }

    public Bitmap getPicture(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        int rotate = readPictureDegree(picturePath);
        return rotateBitmapByDegree(bitmap, rotate);
    }

    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        Bitmap returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    // Get speed by Location Manager
    private class SpeedTask extends AsyncTask<String, Void, String> {
        final MainActivity mainActivity;
        float speed = 0.0f;
        LocationManager locationManager;

        public SpeedTask(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        protected String doInBackground(String... strings) {
            locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
            return null;
        }

        protected void onPostExecute(String result) {
            LocationListener listener = new LocationListener() {
                float filtSpeed;
                float localspeed;

                @Override
                public void onLocationChanged(Location location) {
                    speed = location.getSpeed();
                    localspeed = speed * 3.6f;
                    filtSpeed = filter(filtSpeed, localspeed, 2);
                    speedometer.speedTo(filtSpeed);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                }
            };
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        }

        /**
         * Simple recursive filter
         *
         * @param prev Previous value of filter
         * @param curr New input value into filter
         * @return New filtered value
         */
        private float filter(final float prev, final float curr, final int ratio) {
            // If first time through, initialise digital filter with current values
            if (Float.isNaN(prev))
                return curr;
            // If current value is invalid, return previous filtered value
            if (Float.isNaN(curr))
                return prev;
            // Calculate new filtered value
            return (float) (curr / ratio + prev * (1.0 - 1.0 / ratio));
        }
    }

    private boolean isLocationEnabled(Context mContext) {
        LocationManager locationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
