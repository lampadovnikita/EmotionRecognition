package com.lampa.emotionrecognition.classifiers;

import com.lampa.emotionrecognition.utils.ImagePreprocessor;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;


public class TFLiteImageClassifier extends TFLiteClassifier {

    public enum ImageColorMode {
        GREYSCALE,
        RGB
    }

    private ImageColorMode mImageColorMode;

    private int mImageWidth;
    private int mImageHeight;

    public TFLiteImageClassifier(AssetManager assetManager, String modelFileName, String labelsFileName,
                                 int imageWidth, int imageHeight, ImageColorMode imageColorMode) {

        super(assetManager, modelFileName, labelsFileName);

        mImageWidth = imageWidth;
        mImageHeight = imageHeight;

        mImageColorMode = imageColorMode;
    }

    @Override
    public Map<String, Float> classify(float[] input) {
        int colorDimSize;
        switch (mImageColorMode) {
            case RGB:
                colorDimSize = 3;
                break;
            default:
                colorDimSize = 1;
        }

        float[][][][] formattedInput = new float[1][mImageHeight][mImageWidth][colorDimSize];

        for (int y = 0; y < mImageHeight; y++) {
            for (int x = 0; x < mImageWidth; x++) {
                for (int c = 0; c < colorDimSize; c++) {
                    formattedInput[0][y][x][c] = input[y * mImageHeight + x * colorDimSize + c];
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
        Bitmap scaledImage = Bitmap.createScaledBitmap(imageBitmap, mImageWidth, mImageHeight, useFilter);

        int[] greyScaleImage = ImagePreprocessor.toGreyScale(scaledImage);

        float[] preprocessedImage = new float[greyScaleImage.length];
        for (int i = 0; i < preprocessedImage.length; i++) {
            preprocessedImage[i] = greyScaleImage[i] / 255.0f;
        }

        return preprocessedImage;
    }
}
