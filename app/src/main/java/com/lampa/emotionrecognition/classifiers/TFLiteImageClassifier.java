package com.lampa.emotionrecognition.classifiers;

import com.lampa.emotionrecognition.utils.ImageUtils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

// Классификатор изображений, использующий формат tflite
public class TFLiteImageClassifier extends TFLiteClassifier {
    // Индекс тензора с параметрами входного изображения
    private final static int IMAGE_TENSOR_INDEX = 0;

    // Индексы параметров входного изображения
    private final static int MODEL_INPUT_WIDTH_INDEX = 1;
    private final static int MODEL_INPUT_HEIGHT_INDEX = 2;
    private final static int MODEL_INPUT_COLOR_DIM_INDEX = 3;

    // Индекс параметра выходного массива-результата
    private final static int MODEL_OUTPUT_LENGTH_INDEX = 1;

    private int mImageColorLength;

    private int mImageWidth;
    private int mImageHeight;

    public TFLiteImageClassifier(AssetManager assetManager, String modelFileName, String[] labels) {
        super(assetManager, modelFileName, labels);

        // Получаем параметры входного изображения из модели
        int[] inputShape = mTFLiteInterpreter.getInputTensor(IMAGE_TENSOR_INDEX).shape();
        mImageWidth = inputShape[MODEL_INPUT_WIDTH_INDEX];
        mImageHeight = inputShape[MODEL_INPUT_HEIGHT_INDEX];
        mImageColorLength = inputShape[MODEL_INPUT_COLOR_DIM_INDEX];

        // Получаем параметры выходного массива-результата
        int[] outputShape = mTFLiteInterpreter.getOutputTensor(IMAGE_TENSOR_INDEX).shape();
        // Проверяем на соответствие с массиво строк, заданным в конструкторе
        if (labels.length != outputShape[MODEL_OUTPUT_LENGTH_INDEX]) {
            Formatter formatter = new Formatter();

            throw new IllegalArgumentException(formatter.format(
                    "labels array length must be equal to %1$d, but actual length is %2$d",
                    outputShape[MODEL_OUTPUT_LENGTH_INDEX],
                    labels.length
            ).toString());
        }
    }

    @Override
    public Map<String, Float> classify(float[] input) {
        // Проверяем размер входного массива на соответствие
        if (input.length != (mImageHeight * mImageWidth * mImageColorLength)) {
            Formatter formatter = new Formatter();

            throw new IllegalArgumentException(formatter.format(
                    "input array length must be equal to %1$d * %2$d * %3$d = %4$d," +
                            " but actual length is %5$d",
                    mImageHeight,
                    mImageWidth,
                    mImageColorLength,
                    mImageHeight * mImageWidth * mImageColorLength,
                    input.length
            ).toString());
        }

        // Переводим массив в матрицу
        float[][][][] formattedInput = new float[1][mImageHeight][mImageWidth][mImageColorLength];

        for (int y = 0; y < mImageHeight; y++) {
            for (int x = 0; x < mImageWidth; x++) {
                for (int c = 0; c < mImageColorLength; c++) {
                    formattedInput[0][y][x][c] = input[y * mImageHeight + x * mImageColorLength + c];
                }
            }
        }

        // Массив с результатами
        float[][] outputArr = new float[1][mLabels.size()];

        // Запускаем классификацию
        mTFLiteInterpreter.run(formattedInput, outputArr);

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
    
    public Map<String, Float> classify(Bitmap imageBitmap, boolean useFilter) {
        float[] preprocessedImage = preprocessImage(imageBitmap, useFilter);

        return classify(preprocessedImage);
    }

    // Предварительная обработка изображения
    private float[] preprocessImage(Bitmap imageBitmap, boolean useFilter) {
        Bitmap scaledImage = Bitmap.createScaledBitmap(
                imageBitmap,
                mImageWidth,
                mImageHeight,
                useFilter);

        int[] greyScaleImage = ImageUtils.toGreyScale(scaledImage);

        float[] preprocessedImage = new float[greyScaleImage.length];
        for (int i = 0; i < preprocessedImage.length; i++) {
            preprocessedImage[i] = greyScaleImage[i] / 255.0f;
        }

        return preprocessedImage;
    }
}
