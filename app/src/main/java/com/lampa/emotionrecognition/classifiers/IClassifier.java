package com.lampa.emotionrecognition.classifiers;

import android.util.ArrayMap;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;

public interface IClassifier {
    ArrayMap<String, Float> classify(float[] input);

    ArrayList<String> loadLabels(String fileName);

    MappedByteBuffer loadModel(String fileName) throws IOException;
}
