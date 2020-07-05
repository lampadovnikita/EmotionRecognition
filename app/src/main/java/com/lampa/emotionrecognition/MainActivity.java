package com.lampa.emotionrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.lampa.emotionrecognition.classifiers.TFLiteImageClassifier;
import com.lampa.emotionrecognition.utils.ImageUtils;
import com.lampa.emotionrecognition.utils.SortingHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 0;
    private static final int TAKE_PHOTO_REQUEST_CODE = 1;

    private final String MODEL_FILE_NAME = "simple_classifier.tflite";

    private final int SCALED_IMAGE_BIGGEST_SIZE = 480;

    private TFLiteImageClassifier mClassifier;

    private ProgressBar mClassificationProgressBar;

    private ImageView mImageView;

    private Button mPickImageButton;
    private Button mTakePhotoButton;

    private ExpandableListView mClassificationExpandableListView;

    private Uri mCurrentPhotoUri;

    private Map<String, List<Pair<String, String>>> mClassificationResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClassificationProgressBar = findViewById(R.id.classification_progress_bar);

        mClassifier = new TFLiteImageClassifier(
                this.getAssets(),
                MODEL_FILE_NAME,
                getResources().getStringArray(R.array.emotions));

        mClassificationResult = new LinkedHashMap<>();

        mImageView = findViewById(R.id.image_view);

        mPickImageButton = findViewById(R.id.pick_image_button);
        mPickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        mTakePhotoButton = findViewById(R.id.take_photo_button);
        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        mClassificationExpandableListView = findViewById(R.id.classification_expandable_list_view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mClassifier.close();

        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        for (File tempFile : picturesDir.listFiles()) {
            tempFile.delete();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // When an image from the gallery was successfully selected
                case GALLERY_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    // We can immediately get an image URI from an intent data
                    Uri pickedImageUri = data.getData();
                    processImageRequestResult(pickedImageUri);
                    break;
                // When a photo was taken successfully
                case TAKE_PHOTO_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    processImageRequestResult(mCurrentPhotoUri);
                    break;

                default:
                    break;
            }
        }
    }

    private void clearClassificationExpandableListView() {
        Map<String, List<Pair<String, String>>> emptyMap = new LinkedHashMap<>();
        ClassificationExpandableListAdapter adapter =
                new ClassificationExpandableListAdapter(emptyMap);

        mClassificationExpandableListView.setAdapter(adapter);
    }

    // Function to handle successful new image acquisition
    private void processImageRequestResult(Uri resultImageUri) {
        Bitmap scaledResultImageBitmap = getScaledImageBitmap(resultImageUri);

        mImageView.setImageBitmap(scaledResultImageBitmap);

        // Clear the result of a previous classification
        mClassificationResult.clear();

        setCalculationStatusUI(true);

        detectFaces(scaledResultImageBitmap);
    }

    // Function to create an intent to take an image from the gallery
    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Function to create an intent to take a photo
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Make sure that there is activity of the camera that processes the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                mCurrentPhotoUri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        photoFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
            }
        }
    }

    private Bitmap getScaledImageBitmap(Uri imageUri) {
        Bitmap scaledImageBitmap = null;

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(),
                    imageUri);

            int scaledHeight;
            int scaledWidth;

            // How many times you need to change the sides of an image
            float scaleFactor;

            // Get larger side and start from exactly the larger side in scaling
            if (imageBitmap.getHeight() > imageBitmap.getWidth()) {
                scaledHeight = SCALED_IMAGE_BIGGEST_SIZE;
                scaleFactor = scaledHeight / (float) imageBitmap.getHeight();
                scaledWidth = (int) (imageBitmap.getWidth() * scaleFactor);

            } else {
                scaledWidth = SCALED_IMAGE_BIGGEST_SIZE;
                scaleFactor = scaledWidth / (float) imageBitmap.getWidth();
                scaledHeight = (int) (imageBitmap.getHeight() * scaleFactor);
            }

            scaledImageBitmap = Bitmap.createScaledBitmap(
                    imageBitmap,
                    scaledWidth,
                    scaledHeight,
                    true);

            // An image in memory can be rotated
            scaledImageBitmap = ImageUtils.rotateToNormalOrientation(
                    getContentResolver(),
                    scaledImageBitmap,
                    imageUri);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scaledImageBitmap;
    }

    private void detectFaces(Bitmap imageBitmap) {
        FirebaseVisionFaceDetectorOptions faceDetectorOptions =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .setMinFaceSize(0.1f)
                        .build();

        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(faceDetectorOptions);


        final FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(imageBitmap);

        Task<List<FirebaseVisionFace>> result =
                faceDetector.detectInImage(firebaseImage)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    // When the search for faces was successfully completed
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        Bitmap imageBitmap = firebaseImage.getBitmap();
                                        // Temporary Bitmap for drawing
                                        Bitmap tmpBitmap = Bitmap.createBitmap(
                                                imageBitmap.getWidth(),
                                                imageBitmap.getHeight(),
                                                imageBitmap.getConfig());

                                        // Create an image-based canvas
                                        Canvas tmpCanvas = new Canvas(tmpBitmap);
                                        tmpCanvas.drawBitmap(
                                                imageBitmap,
                                                0,
                                                0,
                                                null);

                                        Paint paint = new Paint();
                                        paint.setColor(Color.GREEN);
                                        paint.setStrokeWidth(2);
                                        paint.setTextSize(48);

                                        // Coefficient for indentation of face number
                                        final float textIndentFactor = 0.1f;

                                        // If at least one face was found
                                        if (!faces.isEmpty()) {
                                            // faceId ~ face text number
                                            int faceId = 1;

                                            for (FirebaseVisionFace face : faces) {
                                                Rect faceRect = getInnerRect(
                                                        face.getBoundingBox(),
                                                        imageBitmap.getWidth(),
                                                        imageBitmap.getHeight());

                                                // Draw a rectangle around a face
                                                paint.setStyle(Paint.Style.STROKE);
                                                tmpCanvas.drawRect(faceRect, paint);

                                                // Draw a face number in a rectangle
                                                paint.setStyle(Paint.Style.FILL);
                                                tmpCanvas.drawText(
                                                        Integer.toString(faceId),
                                                        faceRect.left +
                                                                faceRect.width() * textIndentFactor,
                                                        faceRect.bottom -
                                                                faceRect.height() * textIndentFactor,
                                                        paint);

                                                // Get subarea with a face
                                                Bitmap faceBitmap = Bitmap.createBitmap(
                                                        imageBitmap,
                                                        faceRect.left,
                                                        faceRect.top,
                                                        faceRect.width(),
                                                        faceRect.height());

                                                classifyEmotions(faceBitmap, faceId);

                                                faceId++;
                                            }

                                            // Set the image with the face designations
                                            mImageView.setImageBitmap(tmpBitmap);

                                            ClassificationExpandableListAdapter adapter =
                                                    new ClassificationExpandableListAdapter(mClassificationResult);

                                            mClassificationExpandableListView.setAdapter(adapter);

                                            // If single face, then immediately open the list
                                            if (faces.size() == 1) {
                                                mClassificationExpandableListView.expandGroup(0);
                                            }
                                        // If no faces are found
                                        } else {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    getString(R.string.faceless),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }

                                        setCalculationStatusUI(false);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        e.printStackTrace();

                                        setCalculationStatusUI(false);
                                    }
                                });
    }

    private void classifyEmotions(Bitmap imageBitmap, int faceId) {
        Map<String, Float> result = mClassifier.classify(imageBitmap, true);

        // Sort by increasing probability
        LinkedHashMap<String, Float> sortedResult =
                (LinkedHashMap<String, Float>) SortingHelper.sortByValues(result);

        ArrayList<String> reversedKeys = new ArrayList<>(sortedResult.keySet());
        // Change the order to get a decrease in probabilities
        Collections.reverse(reversedKeys);

        ArrayList<Pair<String, String>> faceGroup = new ArrayList<>();
        for (String key : reversedKeys) {
            String percentage = String.format("%.1f%%", sortedResult.get(key) * 100);
            faceGroup.add(new Pair<>(key, percentage));
        }

        String groupName = getString(R.string.face) + " " + faceId;
        mClassificationResult.put(groupName, faceGroup);
    }

    // Get a rectangle that lies inside the image area
    private Rect getInnerRect(Rect rect, int areaWidth, int areaHeight) {
        Rect innerRect = new Rect(rect);

        if (innerRect.top < 0) {
            innerRect.top = 0;
        }
        if (innerRect.left < 0) {
            innerRect.left = 0;
        }
        if (rect.bottom > areaHeight) {
            innerRect.bottom = areaHeight;
        }
        if (rect.right > areaWidth) {
            innerRect.right = areaWidth;
        }

        return innerRect;
    }

    // Create a temporary file for the image
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "ER_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        return image;
    }

    //Change the interface depending on the status of calculations
    private void setCalculationStatusUI(boolean isCalculationRunning) {
        if (isCalculationRunning) {
            mClassificationProgressBar.setVisibility(ProgressBar.VISIBLE);
            mTakePhotoButton.setEnabled(false);
            mPickImageButton.setEnabled(false);
        } else {
            mClassificationProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mTakePhotoButton.setEnabled(true);
            mPickImageButton.setEnabled(true);
        }
    }
}

