package com.lampa.emotionrecognition.classifiers.behaviors;

import java.util.Map;

// Интерфейс для некоторого классификатора
public interface ClassifyBehavior {
    // Метод классификации
    float[][] classify(float[] input);
}
