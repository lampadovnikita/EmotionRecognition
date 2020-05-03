package com.lampa.emotionrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private final int GALLERY_REQUEST_CODE = 0;

    private final String FER2013_V1_MODEL_FILE_NAME = "fer2013_v1.tflite";
    private final String FER2013_V1_LABELS_FILE_NAME = "labels_fer2013_v1.txt";

    private final int FER2013_IMAGE_WIDTH = 48;
    private final int FER2013_IMAGE_HEIGHT = 48;

    private final int SCALED_IMAGE_WIDTH = 720;

    private TFLiteImageClassifier mClassifier;

    private ProgressBar classificationProgressBar;

    private ImageView faceImageView;

    private Button pickImageButton;

    private ExpandableListView classificationExpandableListView;

    HashMap<String, List<Pair<String, String>>> item;

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

        item = new HashMap<>();

        faceImageView = findViewById(R.id.face_image_view);

        pickImageButton = findViewById(R.id.pick_image_button);
        pickImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();

            }
        });

        classificationExpandableListView = findViewById(R.id.classification_expandable_list_view);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:

                    Uri pickedImageUri = data.getData();
                    try {
                        Bitmap pickedImageBitmap = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(),
                                pickedImageUri);

                        int scaledHeight;
                        if (pickedImageBitmap.getHeight() > pickedImageBitmap.getWidth()) {
                            scaledHeight =
                                    (int) (pickedImageBitmap.getHeight() *
                                            ((float) SCALED_IMAGE_WIDTH / pickedImageBitmap.getWidth()));
                        } else {
                            scaledHeight = (SCALED_IMAGE_WIDTH / 3) * 4;
                        }

                        Bitmap scaledPickedImageBitmap = Bitmap.createScaledBitmap(
                                pickedImageBitmap,
                                SCALED_IMAGE_WIDTH,
                                scaledHeight,
                                true);

                        faceImageView.setImageBitmap(scaledPickedImageBitmap);
                        item.clear();

                        setCalculationStatusUI(true);

                        detectFaces(scaledPickedImageBitmap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    break;
            }
        }
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

                                            faceImageView.setImageBitmap(tmpBitmap);

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

                                            classificationExpandableListView.expandGroup(0);

                                        } else {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "No face detected",
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
        HashMap<String, Float> result =
                (HashMap<String, Float>) mClassifier.classify(
                        imageBitmap,
                        true);

        LinkedHashMap<String, Float> sortedResult = new LinkedHashMap<>();

        result.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sortedResult.put(x.getKey(), x.getValue()));


        ArrayList<Pair<String, String>> faceGroup = new ArrayList<>();
        for (Map.Entry<String, Float> entry : sortedResult.entrySet()) {
            String percentage = String.format("%.2f%%", entry.getValue() * 100);
            faceGroup.add(new Pair<>(entry.getKey(), percentage));
        }

        String groupName = "Face " + faceId;
        item.put(groupName, faceGroup);
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        String[] mimeTypes = {"image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void setCalculationStatusUI(boolean isCalculationRunning) {
        if (isCalculationRunning) {
            classificationProgressBar.setVisibility(ProgressBar.VISIBLE);
            pickImageButton.setEnabled(false);
        } else {
            classificationProgressBar.setVisibility(ProgressBar.INVISIBLE);
            pickImageButton.setEnabled(true);
        }
    }
}

