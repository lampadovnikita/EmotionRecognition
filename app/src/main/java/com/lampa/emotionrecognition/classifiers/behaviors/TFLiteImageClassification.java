package com.lampa.emotionrecognition.classifiers.behaviors;

import com.lampa.emotionrecognition.classifiers.InterpreterImageParams;

import org.tensorflow.lite.Interpreter;

import java.util.Formatter;

public class TFLiteImageClassification implements ClassifyBehavior {

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
        // Check the size of the input array for compliance
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

        // Translate the array into the matrix
        for (int y = 0; y < mImageHeight; y++) {
            for (int x = 0; x < mImageWidth; x++) {
                for (int c = 0; c < mImageColorLength; c++) {
                    formattedInput[0][y][x][c] = input[y * mImageHeight + x * mImageColorLength + c];
                }
            }
        }

        float[][] outputArr = new float[1][mOutputLength];

        mInterpreter.run(formattedInput, outputArr);

        return outputArr;
    }

    private void setImageParameters() {
        // Get an input image params from the interpreter
        mImageWidth = InterpreterImageParams.getInputImageWidth(mInterpreter);
        mImageHeight = InterpreterImageParams.getInputImageHeight(mInterpreter);
        mImageColorLength = InterpreterImageParams.getInputColorDimLength(mInterpreter);

        // Get an output length from the interpreter
        mOutputLength = InterpreterImageParams.getOutputLength(mInterpreter);
    }
}
