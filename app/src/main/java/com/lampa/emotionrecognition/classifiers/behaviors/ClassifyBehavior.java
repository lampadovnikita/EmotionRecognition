package com.lampa.emotionrecognition.classifiers.behaviors;

// Интерфейс для некоторого классификатора
public interface ClassifyBehavior {
    // Метод классификации
    float[][] classify(float[] input);
}
