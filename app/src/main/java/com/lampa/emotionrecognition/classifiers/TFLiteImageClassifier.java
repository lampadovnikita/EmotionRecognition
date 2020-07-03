package com.lampa.emotionrecognition.classifiers;

import com.lampa.emotionrecognition.classifiers.behaviors.TFLiteImageClassification;
import com.lampa.emotionrecognition.utils.ImageUtils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

// Классификатор изображений, использующий формат tflite
public class TFLiteImageClassifier extends TFLiteClassifier {

    public TFLiteImageClassifier(AssetManager assetManager, String modelFileName, String[] labels) {
        super(assetManager, modelFileName, labels);

        classifyBehavior = new TFLiteImageClassification(mInterpreter);
    }

    public Map<String, Float> classify(Bitmap imageBitmap, boolean useFilter) {
        float[] preprocessedImage = preprocessImage(imageBitmap, useFilter);

        return classify(preprocessedImage);
    }

    private Map<String, Float> classify(float[] input) {
        float[][] outputArr = classifyBehavior.classify(input);

        // Проверяем на соответствие с массиво строк, заданным в конструкторе
        if (mLabels.size() != outputArr[0].length) {
            Formatter formatter = new Formatter();

            throw new IllegalArgumentException(formatter.format(
                    "labels array length must be equal to %1$d, but actual length is %2$d",
                    outputArr[0].length,
                    mLabels.size()
            ).toString());
        }

        Map<String, Float> outputMap = new HashMap<>();
        // Переводи массив с результатами в Map
        String predictedLabel;
        float probability;
        for (int i = 0; i < outputArr[0].length; i++) {
            predictedLabel = mLabels.get(i);
            probability = outputArr[0][i];

            outputMap.put(predictedLabel, probability);
        }

        return outputMap;
    }

    // Предварительная обработка изображения
    private float[] preprocessImage(Bitmap imageBitmap, boolean useFilter) {
        Bitmap scaledImage = Bitmap.createScaledBitmap(
                imageBitmap,
                mInputShape[MODEL_INPUT_WIDTH_INDEX],
                mInputShape[MODEL_INPUT_HEIGHT_INDEX],
                useFilter);

        int[] greyScaleImage = ImageUtils.toGreyScale(scaledImage);

        float[] preprocessedImage = new float[greyScaleImage.length];
        for (int i = 0; i < preprocessedImage.length; i++) {
            preprocessedImage[i] = greyScaleImage[i] / 255.0f;
        }

        return preprocessedImage;
    }
}
