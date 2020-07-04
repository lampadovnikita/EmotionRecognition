package com.lampa.emotionrecognition.classifiers;

import org.tensorflow.lite.Interpreter;

public class InterpreterImageParams {
    // Tensor indices of an image parameters
    private final static int IMAGE_INPUT_TENSOR_INDEX = 0;
    private final static int IMAGE_OUTPUT_TENSOR_INDEX = 0;

    // Indices of an input image parameters
    private final static int MODEL_INPUT_WIDTH_INDEX = 1;
    private final static int MODEL_INPUT_HEIGHT_INDEX = 2;
    private final static int MODEL_INPUT_COLOR_DIM_INDEX = 3;

    // Index of an output result array
    private final static int MODEL_OUTPUT_LENGTH_INDEX = 1;

    public static int getInputImageWidth(Interpreter interpreter) {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_WIDTH_INDEX];
    }

    public static int getInputImageHeight(Interpreter interpreter) {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_HEIGHT_INDEX];
    }

    public static int getInputColorDimLength(Interpreter interpreter) {
        return interpreter.getInputTensor(IMAGE_INPUT_TENSOR_INDEX).shape()[MODEL_INPUT_COLOR_DIM_INDEX];
    }

    public static int getOutputLength(Interpreter interpreter) {
        return interpreter.getOutputTensor(IMAGE_OUTPUT_TENSOR_INDEX).shape()[MODEL_OUTPUT_LENGTH_INDEX];
    }
}
