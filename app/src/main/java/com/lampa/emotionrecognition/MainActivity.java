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

    private final String FER2013_V1_MODEL_FILE_NAME = "classifier.tflite";

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
                FER2013_V1_MODEL_FILE_NAME,
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
                // Когда было успешно выбрано изображение из галереи
                case GALLERY_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    // Можем сразу получить URI изображения из данных намерения
                    Uri pickedImageUri = data.getData();
                    processImageRequestResult(pickedImageUri);
                    break;
                // Когда была успешно сделана фотография
                case TAKE_PHOTO_REQUEST_CODE:
                    clearClassificationExpandableListView();
                    processImageRequestResult(mCurrentPhotoUri);
                    break;

                default:
                    break;
            }
        }
    }

    // Функция для очистки выпадающего древовидного списка с информацией о классификации
    private void clearClassificationExpandableListView() {
        Map<String, List<Pair<String, String>>> emptyMap = new LinkedHashMap<>();
        ClassificationExpandableListAdapter adapter =
                new ClassificationExpandableListAdapter(emptyMap);

        mClassificationExpandableListView.setAdapter(adapter);
    }

    // Функция для обработки успешного получения нового изображения
    private void processImageRequestResult(Uri resultImageUri) {
        // Получаем смасштабированное изображение
        Bitmap scaledResultImageBitmap = getScaledImageBitmap(resultImageUri);
        // Выводим смасшабированное изображение
        mImageView.setImageBitmap(scaledResultImageBitmap);
        // Очищаем результат классификации
        mClassificationResult.clear();

        setCalculationStatusUI(true);
        // Запускаем процесс распознавания лиц
        detectFaces(scaledResultImageBitmap);
    }

    // Функция для создания намерения для взятия изображения из галереи
    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        String[] mimeTypes = {"image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    // Функция для создания намерения для взятия фотографии
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
                startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
            }
        }
    }

    // Функция в которой происходит обнаружение лиц
    private void detectFaces(Bitmap imageBitmap) {
        // Параметры детектора
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
                                    // Когда удачно завершился поиск лиц
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        Bitmap imageBitmap = firebaseImage.getBitmap();
                                        // Временный Bitmap для рисования
                                        Bitmap tmpBitmap = Bitmap.createBitmap(
                                                imageBitmap.getWidth(),
                                                imageBitmap.getHeight(),
                                                imageBitmap.getConfig());

                                        // Создаём холст для рисования на основе изображения
                                        Canvas tmpCanvas = new Canvas(tmpBitmap);
                                        tmpCanvas.drawBitmap(
                                                imageBitmap,
                                                0,
                                                0,
                                                null);

                                        // Параметры рисования
                                        Paint paint = new Paint();
                                        paint.setColor(Color.GREEN);
                                        paint.setStrokeWidth(2);
                                        paint.setTextSize(30);

                                        // Коэффициент для отступа номера лица
                                        final float textIndentFactor = 0.1f;

                                        // Если было найдено хотя бы одно лицо
                                        if (!faces.isEmpty()) {
                                            int faceId = 1;
                                            for (FirebaseVisionFace face : faces) {
                                                Rect faceRect = getInnerRect(
                                                        face.getBoundingBox(),
                                                        imageBitmap.getWidth(),
                                                        imageBitmap.getHeight());

                                                // Рисуем прямоугольник вокруг лица
                                                paint.setStyle(Paint.Style.STROKE);
                                                tmpCanvas.drawRect(faceRect, paint);

                                                // Рисуем номер лица в прямоугольнике
                                                paint.setStyle(Paint.Style.FILL);
                                                tmpCanvas.drawText(
                                                        Integer.toString(faceId),
                                                        faceRect.left +
                                                                faceRect.width() * textIndentFactor,
                                                        faceRect.bottom -
                                                                faceRect.height() * textIndentFactor,
                                                        paint);
                                                // Получаем подобласть с лицом
                                                Bitmap faceBitmap = Bitmap.createBitmap(
                                                        imageBitmap,
                                                        faceRect.left,
                                                        faceRect.top,
                                                        faceRect.width(),
                                                        faceRect.height());
                                                // Классифицируем эмоции лица
                                                classifyEmotions(faceBitmap, faceId);

                                                faceId++;
                                            }

                                            // Устанавливаем изображение с обозначениями лиц
                                            mImageView.setImageBitmap(tmpBitmap);

                                            // Создаём список с информацией о классификации
                                            ClassificationExpandableListAdapter adapter =
                                                    new ClassificationExpandableListAdapter(mClassificationResult);

                                            mClassificationExpandableListView.setAdapter(adapter);

                                            // Если лицо одно, то сразу раскрываем список
                                            if (faces.size() == 1) {
                                                mClassificationExpandableListView.expandGroup(0);
                                            }
                                        // Если лиц не найдено
                                        } else {
                                            // Выводи соответствующее сообщение
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

    // Функция для классификации эмоций
    private void classifyEmotions(Bitmap imageBitmap, int faceId) {
        // Получаем результат классификации
        Map<String, Float> result =
                mClassifier.classify(
                        imageBitmap,
                        true);

        // Сортируем по возрастанию вероятностей
        LinkedHashMap<String, Float> sortedResult =
                (LinkedHashMap<String, Float>) SortingHelper.sortByValues(result);

        ArrayList<String> reversedKeys = new ArrayList<>(sortedResult.keySet());
        // Меняем порядок, получаем убывание вероятностей
        Collections.reverse(reversedKeys);

        ArrayList<Pair<String, String>> faceGroup = new ArrayList<>();
        for (String key : reversedKeys) {
            String percentage = String.format("%.1f%%", sortedResult.get(key) * 100);
            faceGroup.add(new Pair<>(key, percentage));
        }

        String groupName = getString(R.string.face) + " " + faceId;
        mClassificationResult.put(groupName, faceGroup);
    }

    // Получаем прямоугольник, который лежит внутри области изображения
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

    // Получаем масшабированное изображение
    private Bitmap getScaledImageBitmap(Uri imageUri) {
        Bitmap scaledImageBitmap = null;

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(),
                    imageUri);

            int scaledHeight;
            int scaledWidth;
            // То, во сколько раз нужно изменить стороны изображения
            float scaleFactor;
            // Получаем большую сторону и отталкиваемся в масштабировании именно от большей стороны
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

            // Изображение в памяти может храниться в повёрнутом состоянии
            scaledImageBitmap = ImageUtils.rotateToNormalOrientation(
                    getContentResolver(),
                    scaledImageBitmap,
                    imageUri);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scaledImageBitmap;
    }

    // Создаём временный файл для изображения
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

    // Меняем интерфейс в зависимости от статуса вычислений
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

