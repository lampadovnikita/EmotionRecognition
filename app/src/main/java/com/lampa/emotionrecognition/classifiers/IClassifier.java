package com.lampa.emotionrecognition.classifiers;


import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public interface IClassifier {
    Map<String, Float> classify(float[] input);

    ArrayList<String> loadLabels(String fileName);

    MappedByteBuffer loadModel(String fileName) throws IOException;
}
