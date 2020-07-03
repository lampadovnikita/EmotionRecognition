package com.lampa.emotionrecognition.classifiers.behaviors;

import com.lampa.emotionrecognition.classifiers.behaviors.ClassifyBehavior;

import org.tensorflow.lite.Interpreter;

import java.util.Formatter;

public class TFLiteImageClassification implements ClassifyBehavior {
    // Индекс тензора с параметрами входного изображения
    private final static int IMAGE_TENSOR_INDEX = 0;

    // Индексы параметров входного изображения
    private final static int MODEL_INPUT_WIDTH_INDEX = 1;
    private final static int MODEL_INPUT_HEIGHT_INDEX = 2;
    private final static int MODEL_INPUT_COLOR_DIM_INDEX = 3;

    // Индекс параметра выходного массива-результата
    private final static int MODEL_OUTPUT_LENGTH_INDEX = 1;

    private Interpreter mInterpreter;

    private int mImageHeight;

    private int mImageWidth;

    private int mImageColorLength;

    private int mOutputLength;

    public TFLiteImageClassification(Interpreter interpreter) {
        mInterpreter = interpreter;
        setImageParameters();
    }

    @Override
    public float[][] classify(float[] input) {
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
        float[][] outputArr = new float[1][mOutputLength];

        // Запускаем классификацию
        mInterpreter.run(formattedInput, outputArr);

        return outputArr;
    }

    private void setImageParameters() {
        // Получаем параметры входного изображения из модели
        int[] inputShape = mInterpreter.getInputTensor(IMAGE_TENSOR_INDEX).shape();
        mImageWidth = inputShape[MODEL_INPUT_WIDTH_INDEX];
        mImageHeight = inputShape[MODEL_INPUT_HEIGHT_INDEX];
        mImageColorLength = inputShape[MODEL_INPUT_COLOR_DIM_INDEX];

        // Получаем параметры выходного массива-результата
        int[] outputShape = mInterpreter.getOutputTensor(IMAGE_TENSOR_INDEX).shape();
        mOutputLength = outputShape[MODEL_OUTPUT_LENGTH_INDEX];
    }
}
