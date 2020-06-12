package com.lampa.emotionrecognition.classifiers;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Map;

// Интерфейс для некоторого классификатора
public interface IClassifier {
    // Метод классификации
    Map<String, Float> classify(float[] input);

    // Метод загрузки модели из файла
    MappedByteBuffer loadModel(String labelsFileName) throws IOException;
}
