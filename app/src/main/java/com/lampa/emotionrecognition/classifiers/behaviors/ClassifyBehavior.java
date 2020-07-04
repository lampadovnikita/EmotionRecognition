package com.lampa.emotionrecognition.classifiers.behaviors;

public interface ClassifyBehavior {
    float[][] classify(float[] input);
}
