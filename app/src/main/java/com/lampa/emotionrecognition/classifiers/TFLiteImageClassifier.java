package com.lampa.emotionrecognition.classifiers;

import com.lampa.emotionrecognition.utils.ImageUtils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

public class TFLiteImageClassifier extends TFLiteClassifier {
    private final static int IMAGE_TENSOR_INDEX = 0;

    private final static int MODEL_INPUT_WIDTH_INDEX = 1;
    private final static int MODEL_INPUT_HEIGHT_INDEX = 2;
    private final static int MODEL_INPUT_COLOR_DIM_INDEX = 3;

    private final static int MODEL_OUTPUT_LENGTH_INDEX = 1;

    private int mImageColorLength;

    private int mImageWidth;
    private int mImageHeight;

    public TFLiteImageClassifier(AssetManager assetManager, String modelFileName, String[] labels) {

        super(assetManager, modelFileName, labels);

        int[] inputShape = mTFLiteInterpreter.getInputTensor(IMAGE_TENSOR_INDEX).shape();
        mImageWidth = inputShape[MODEL_INPUT_WIDTH_INDEX];
        mImageHeight = inputShape[MODEL_INPUT_HEIGHT_INDEX];
        mImageColorLength = inputShape[MODEL_INPUT_COLOR_DIM_INDEX];

        int[] outputShape = mTFLiteInterpreter.getOutputTensor(IMAGE_TENSOR_INDEX).shape();
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

        float[][][][] formattedInput = new float[1][mImageHeight][mImageWidth][mImageColorLength];

        for (int y = 0; y < mImageHeight; y++) {
            for (int x = 0; x < mImageWidth; x++) {
                for (int c = 0; c < mImageColorLength; c++) {
                    formattedInput[0][y][x][c] = input[y * mImageHeight + x * mImageColorLength + c];
                }
            }
        }
        float[][] outputArr = new float[1][mLabels.size()];

        mTFLiteInterpreter.run(formattedInput, outputArr);

        Map<String, Float> outputMap = new HashMap<>();

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
