package com.google.sample.cloudvision.MainService;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.document.FirebaseVisionCloudDocumentRecognizerOptions;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.google.sample.cloudvision.MainActivity;
import com.google.sample.cloudvision.NotificationServiceTool.AlwaysOnNotificationBarService;
import com.google.sample.cloudvision.NotificationServiceTool.NotificationManagement;
import com.google.sample.cloudvision.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;



public class TextRecognition_screenshot extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyAT4yeZeEV1J9BHydi2HMBoRkJDbrZK5NU";
    public static final String FILE_NAME = "Screenshot2.jpg";
    public static NotificationManagement notiman;

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int SCREENSHOT_PERMISSIONS_REQUEST = 0;
    public static final int SCREENSHOT_IMAGE_REQUEST = 1;

    private boolean trigger_flag = false;

    private TextView mImageDetails;
    private ImageView mMainImage;
    private long startTimeMS;
    private float uploadDurationSec;

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void turnServiceTrigger(View view) {
        Button ServiceButton = (Button) findViewById(R.id.ServiceTrigger);
        if(!isServiceRunning(AlwaysOnNotificationBarService.class)){
            ServiceButton.setText("서비스 비활성화");
            Log.e("service checkout", "Turn On Service");
            Intent service_intent = new Intent(getApplicationContext(), AlwaysOnNotificationBarService.class);
            startService(service_intent);
        }else{
            ServiceButton.setText("서비스 활성화");
            Log.e("service checkout", "Turn Off Service");
            Intent service_intent = new Intent(getApplicationContext(), AlwaysOnNotificationBarService.class);
            stopService(service_intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FirebaseVisionTextRecognizer FV = FirebaseVision.getInstance().getCloudTextRecognizer();
        notiman = new NotificationManagement(this);
        Button ServiceButton = (Button) findViewById(R.id.ServiceTrigger);
        if(isServiceRunning(AlwaysOnNotificationBarService.class))
            ServiceButton.setText("서비스 비활성화");
        else
            ServiceButton.setText("서비스 활성화");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SCREENSHOT_PERMISSIONS_REQUEST);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, SCREENSHOT_PERMISSIONS_REQUEST);


        }
        else {
            //do nothing at the moment
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplicationContext(),"hello", Toast.LENGTH_LONG).show();
                View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
                Bitmap bitmap = getScreenShot(rootView);
                store(bitmap, FILE_NAME);
            }

        });

        mImageDetails = (TextView) findViewById(R.id.image_details);
        mMainImage = (ImageView) findViewById(R.id.main_image);

    }

    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    //store the image on the device
    public void store(Bitmap bm, String fileName) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dirPath, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG,10, fos);
            fos.flush();
            fos.close();


            Uri uri = Uri.fromFile(file);
            Toast.makeText(getApplicationContext(),uri.toString(), Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setType("application/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            uploadImage(uri);
            //startActivityForResult(intent, SCREENSHOT_IMAGE_REQUEST);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 //   @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (data != null) {
//            Toast.makeText(getApplicationContext(),data.getData().toString(), Toast.LENGTH_LONG).show();
//
//        }
//
//        if (requestCode == SCREENSHOT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
//            Toast.makeText(getApplicationContext(),data.getData().toString(), Toast.LENGTH_LONG).show();
//            uploadImage(data.getData());
//    }
//    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==SCREENSHOT_PERMISSIONS_REQUEST) {
            if (grantResults[0] ==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                //do nothing at the momment
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    public void uploadImage(Uri uri) {
        startTimeMS = System.currentTimeMillis();
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                //callCloudVision(bitmap);

                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                recognizeTextCloud(image);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

//    private void callCloudVision(final Bitmap bitmap) throws IOException {
//        // Switch text to loading
//        mImageDetails.setText(R.string.loading_message);
//
//        // Do the real work in an async task, because we need to use the network anyway
//        new AsyncTask<Object, Void, String>() {
//            @Override
//            protected String doInBackground(Object... params) {
//                try {
//                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
//                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
//
//                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
//                    builder.setVisionRequestInitializer(new
//                            VisionRequestInitializer(CLOUD_VISION_API_KEY));
//                    Vision vision = builder.build();
//
//                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
//                            new BatchAnnotateImagesRequest();
//                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
//                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
//
//                        // Add the image
//                        Image base64EncodedImage = new Image();
//                        // Convert the bitmap to a JPEG
//                        // Just in case it's a format that Android understands but Cloud Vision
//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
//                        byte[] imageBytes = byteArrayOutputStream.toByteArray();
//
//                        // Base64 encode the JPEG
//                        base64EncodedImage.encodeContent(imageBytes);
//                        annotateImageRequest.setImage(base64EncodedImage);
//
//                        // add the features we want
//                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
//                            Feature labelDetection = new Feature();
//                            labelDetection.setType("TEXT_DETECTION");
//                            labelDetection.setMaxResults(10);
//                            add(labelDetection);
//                        }});
//
//                        // Add the list of one thing to the request
//                        add(annotateImageRequest);
//                    }});
//
//                    Vision.Images.Annotate annotateRequest =
//                            vision.images().annotate(batchAnnotateImagesRequest);
//                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
//                    annotateRequest.setDisableGZipContent(true);
//                    Log.d(TAG, "created Cloud Vision request object, sending request");
//
//                    long sendMS = System.currentTimeMillis();
//                    BatchAnnotateImagesResponse response = annotateRequest.execute();
//                    uploadDurationSec = (System.currentTimeMillis() - sendMS) / 1000f;
//                    return convertResponseToString(response);
//
//                } catch (GoogleJsonResponseException e) {
//                    Log.d(TAG, "failed to make API request because " + e.getContent());
//                } catch (IOException e) {
//                    Log.d(TAG, "failed to make API request because of other IOException " +
//                            e.getMessage());
//                }
//                return "Cloud Vision API request failed. Check logs for details.";
//            }
//
//            protected void onPostExecute(String result) {
//                mImageDetails.setText(result);
//            }
//        }.execute();
//    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        long spentMS = System.currentTimeMillis() - startTimeMS;
        String message = "Recognition results:\n";
        StringBuilder builder = new StringBuilder(message);
        builder.append(String.format("(Total spent %.2f secs, including %.2f secs for upload)\n\n", spentMS / 1000f, uploadDurationSec));

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        Log.i("JackTest", "total labels:" + labels.size());
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++ ) {
                EntityAnnotation label = labels.get(i);
                if (i == 0) {
                    builder.append("Locale: ");
                    builder.append(label.getLocale());
                }
                builder.append(label.getDescription());
                builder.append("\n");
                //TODO: Draw rectangles later
                break;
            }
        } else {
            builder.append("nothing");
        }

        return builder.toString();
    }


    //여기서부터 firebase 사용 이중에 하나만 쓰거나 혹은 몇개 선택해서 쓰면 될것 같네
    private void recognizeText(FirebaseVisionImage image) {

        // [START get_detector_default]
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // [END get_detector_default]

        // [START run_detector]
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                // [START_EXCLUDE]
                                // [START get_text]
                                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                                    Rect boundingBox = block.getBoundingBox();
                                    Point[] cornerPoints = block.getCornerPoints();
                                    String text = block.getText();

                                    for (FirebaseVisionText.Line line: block.getLines()) {
                                        // ...
                                        for (FirebaseVisionText.Element element: line.getElements()) {
                                            // ...

                                        }
                                    }
                                }
                                // [END get_text]
                                // [END_EXCLUDE]
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e("Fail tag","failed!");
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        // [END run_detector]
    }

    private void recognizeTextCloud(FirebaseVisionImage image) {
        // [START set_detector_options_cloud]
        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "ko"))
                .build();
        // [END set_detector_options_cloud]

        // [START get_detector_cloud]
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer();
        // Or, to change the default settings:
        //   FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
        //          .getCloudTextRecognizer(options);
        // [END get_detector_cloud]

        // [START run_detector_cloud]
        Task<FirebaseVisionText> result = detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        // [START_EXCLUDE]
                        // [START get_text_cloud]
                        String last_text = "";
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            Rect boundingBox = block.getBoundingBox();
                            Point[] cornerPoints = block.getCornerPoints();
                            String text = block.getText();
                            Log.e("whole tag",text);
                            last_text+=text+"\n";
                            for (FirebaseVisionText.Line line: block.getLines()) {
                                // ...
                                String lineText = line.getText();

                                for (FirebaseVisionText.Element element: line.getElements()) {
                                    // ...
                                    String elementText = element.getText();
                                }
                            }
                        }

                        mImageDetails.setText(last_text);
                        // [END get_text_cloud]
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
        // [END run_detector_cloud]
    }

    private void processTextBlock(FirebaseVisionText result) {
        // [START mlkit_process_text_block]
        String resultText = result.getText();
        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                }
            }
        }
        // [END mlkit_process_text_block]
    }

    private FirebaseVisionDocumentTextRecognizer getLocalDocumentRecognizer() {
        // [START mlkit_local_doc_recognizer]
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();
        // [END mlkit_local_doc_recognizer]

        return detector;
    }

    private FirebaseVisionDocumentTextRecognizer getCloudDocumentRecognizer() {
        // [START mlkit_cloud_doc_recognizer]
        // Or, to provide language hints to assist with language detection:
        // See https://cloud.google.com/vision/docs/languages for supported languages
        FirebaseVisionCloudDocumentRecognizerOptions options =
                new FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                        .setLanguageHints(Arrays.asList("en", "hi"))
                        .build();
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer(options);
        // [END mlkit_cloud_doc_recognizer]

        return detector;
    }

    private void processDocumentImage() {
        // Dummy variables
        FirebaseVisionDocumentTextRecognizer detector = getLocalDocumentRecognizer();
        FirebaseVisionImage myImage = FirebaseVisionImage.fromByteArray(new byte[]{},
                new FirebaseVisionImageMetadata.Builder().build());

        // [START mlkit_process_doc_image]
        detector.processImage(myImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                    @Override
                    public void onSuccess(FirebaseVisionDocumentText result) {
                        // Task completed successfully
                        // ...
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
        // [END mlkit_process_doc_image]
    }

    private void processDocumentTextBlock(FirebaseVisionDocumentText result) {
        // [START mlkit_process_document_text_block]
        String resultText = result.getText();
        for (FirebaseVisionDocumentText.Block block: result.getBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockRecognizedLanguages = block.getRecognizedLanguages();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionDocumentText.Paragraph paragraph: block.getParagraphs()) {
                String paragraphText = paragraph.getText();
                Float paragraphConfidence = paragraph.getConfidence();
                List<RecognizedLanguage> paragraphRecognizedLanguages = paragraph.getRecognizedLanguages();
                Rect paragraphFrame = paragraph.getBoundingBox();
                for (FirebaseVisionDocumentText.Word word: paragraph.getWords()) {
                    String wordText = word.getText();
                    Float wordConfidence = word.getConfidence();
                    List<RecognizedLanguage> wordRecognizedLanguages = word.getRecognizedLanguages();
                    Rect wordFrame = word.getBoundingBox();
                    for (FirebaseVisionDocumentText.Symbol symbol: word.getSymbols()) {
                        String symbolText = symbol.getText();
                        Float symbolConfidence = symbol.getConfidence();
                        List<RecognizedLanguage> symbolRecognizedLanguages = symbol.getRecognizedLanguages();
                        Rect symbolFrame = symbol.getBoundingBox();
                    }
                }
            }
        }
        // [END mlkit_process_document_text_block]
    }
}






