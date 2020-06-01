package com.lampa.emotionrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
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
import com.lampa.emotionrecognition.utils.SortingHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private static final int GALLERY_REQUEST_CODE = 0;
    private static final int TAKE_PHOTO_REQUEST = 1;

    private final String FER2013_V1_MODEL_FILE_NAME = "fer2013_v1.tflite";
    private final String FER2013_V1_LABELS_FILE_NAME = "labels_fer2013_v1.txt";

    private final int FER2013_IMAGE_WIDTH = 48;
    private final int FER2013_IMAGE_HEIGHT = 48;

    private final int SCALED_IMAGE_WIDTH = 720;

    private TFLiteImageClassifier mClassifier;

    private ProgressBar classificationProgressBar;

    private ImageView imageView;

    private Button pickImageButton;
    private Button takePhotoButton;

    private ExpandableListView classificationExpandableListView;

    private Uri mCurrentPhotoUri;

    private Map<String, List<Pair<String, String>>> item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        classificationProgressBar = findViewById(R.id.classification_progress_bar);

        mClassifier = new TFLiteImageClassifier(
                this.getAssets(),
                FER2013_V1_MODEL_FILE_NAME,
                FER2013_V1_LABELS_FILE_NAME,
                FER2013_IMAGE_WIDTH,
                FER2013_IMAGE_HEIGHT,
                TFLiteImageClassifier.ImageColorMode.GREYSCALE);

        item = new LinkedHashMap<>();

        imageView = findViewById(R.id.face_image_view);

        pickImageButton = findViewById(R.id.pick_image_button);
        pickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        takePhotoButton = findViewById(R.id.take_photo_button);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        classificationExpandableListView = findViewById(R.id.classification_expandable_list_view);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    Uri pickedImageUri = data.getData();
                    processImageRequestResult(pickedImageUri);
                    break;

                case TAKE_PHOTO_REQUEST:
                    clearClassificationExpandableListView();
                    processImageRequestResult(mCurrentPhotoUri);
                    break;

                default:
                    break;
            }
        }
    }

    void clearClassificationExpandableListView() {
        Map<String, List<Pair<String, String>>> emptyMap = new LinkedHashMap<>();
        ClassificationExpandableListAdapter adapter = new ClassificationExpandableListAdapter(emptyMap);
        classificationExpandableListView.setAdapter(adapter);
    }

    private void processImageRequestResult(Uri resultImageUri) {
        Bitmap scaledResultImageBitmap = getScaledImageBitmap(resultImageUri);

        imageView.setImageBitmap(scaledResultImageBitmap);
        item.clear();

        setCalculationStatusUI(true);

        detectFaces(scaledResultImageBitmap);
    }

    private Bitmap getScaledImageBitmap(Uri imageUri) {
        Bitmap scaledImageBitmap = null;

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(),
                    imageUri);

            int scaledHeight;
            if (imageBitmap.getHeight() > imageBitmap.getWidth()) {
                scaledHeight =
                        (int) (imageBitmap.getHeight() *
                                ((float) SCALED_IMAGE_WIDTH / imageBitmap.getWidth()));
            } else {
                scaledHeight = (SCALED_IMAGE_WIDTH / 3) * 4;
            }

            scaledImageBitmap = Bitmap.createScaledBitmap(
                    imageBitmap,
                    SCALED_IMAGE_WIDTH,
                    scaledHeight,
                    true);

            scaledImageBitmap = rotateToNormalOrientation(scaledImageBitmap, imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scaledImageBitmap;
    }

    public Bitmap rotateToNormalOrientation(Bitmap imageBitmap, Uri imageUri) {
        int orientationAngle = getOrientationAngle(imageUri);
        if (orientationAngle != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientationAngle);
            imageBitmap = Bitmap.createBitmap(
                    imageBitmap,
                    0,
                    0,
                    imageBitmap.getWidth(),
                    imageBitmap.getHeight(),
                    matrix,
                    true);
        }

        return imageBitmap;
    }

    public int getOrientationAngle(Uri uri) {
        int degree = 0;
        try {
            Log.d(TAG, uri.toString());

            InputStream inputStream = getContentResolver().openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(inputStream);

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

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        String[] mimeTypes = {"image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Удостоверимся, что есть активность камеры, которая обработает интент
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
                startActivityForResult(intent, TAKE_PHOTO_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".png",
                storageDir
        );

        return image;
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
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        Bitmap imageBitmap = firebaseImage.getBitmap();
                                        Bitmap tmpBitmap = Bitmap.createBitmap(
                                                imageBitmap.getWidth(),
                                                imageBitmap.getHeight(),
                                                imageBitmap.getConfig());

                                        Canvas tmpCanvas = new Canvas(tmpBitmap);
                                        tmpCanvas.drawBitmap(
                                                imageBitmap,
                                                0,
                                                0,
                                                null);

                                        Paint paint = new Paint();
                                        paint.setColor(Color.GREEN);
                                        paint.setStrokeWidth(2);
                                        paint.setStyle(Paint.Style.STROKE);
                                        paint.setTextSize(40);

                                        if (!faces.isEmpty()) {
                                            int faceId = 1;
                                            for (FirebaseVisionFace face : faces) {
                                                Rect faceRect = face.getBoundingBox();
                                                tmpCanvas.drawRect(faceRect, paint);
                                                tmpCanvas.drawText(Integer.toString(faceId), faceRect.left + 20, faceRect.bottom - 20, paint);

                                                faceId++;
                                            }

                                            imageView.setImageBitmap(tmpBitmap);

                                            faceId = 1;
                                            for (FirebaseVisionFace face : faces) {
                                                Rect faceRect = face.getBoundingBox();

                                                if (faceRect.top < 0) {
                                                    faceRect.top = 0;
                                                }
                                                if (faceRect.left < 0) {
                                                    faceRect.left = 0;
                                                }
                                                if (faceRect.bottom > imageBitmap.getHeight()) {
                                                    faceRect.bottom = imageBitmap.getHeight();
                                                }
                                                if (faceRect.right > imageBitmap.getWidth()) {
                                                    faceRect.right = imageBitmap.getWidth();
                                                }

                                                Bitmap faceBitmap = Bitmap.createBitmap(
                                                        imageBitmap,
                                                        faceRect.left,
                                                        faceRect.top,
                                                        faceRect.width(),
                                                        faceRect.height());

                                                classifyEmotions(faceBitmap, faceId);

                                                faceId++;
                                            }

                                            ClassificationExpandableListAdapter adapter = new ClassificationExpandableListAdapter(item);
                                            classificationExpandableListView.setAdapter(adapter);

                                            if (faces.size() == 1) {
                                                classificationExpandableListView.expandGroup(0);
                                            }

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
        Map<String, Float> result =
                mClassifier.classify(
                        imageBitmap,
                        true);

        LinkedHashMap<String, Float> sortedResult = (LinkedHashMap<String, Float>) SortingHelper.sortByValues(result);

        ArrayList<String> reversedKeys = new ArrayList<>(sortedResult.keySet());
        Collections.reverse(reversedKeys);

        ArrayList<Pair<String, String>> faceGroup = new ArrayList<>();
        for (String key : reversedKeys) {
            String percentage = String.format("%.2f%%", sortedResult.get(key) * 100);
            faceGroup.add(new Pair<>(key, percentage));
        }

        String groupName = getString(R.string.face) + faceId;
        item.put(groupName, faceGroup);
    }

    private void setCalculationStatusUI(boolean isCalculationRunning) {
        if (isCalculationRunning) {
            classificationProgressBar.setVisibility(ProgressBar.VISIBLE);
            takePhotoButton.setEnabled(false);
            pickImageButton.setEnabled(false);
        } else {
            classificationProgressBar.setVisibility(ProgressBar.INVISIBLE);
            takePhotoButton.setEnabled(true);
            pickImageButton.setEnabled(true);
        }
    }
}

