package com.lampa.emotionrecognition.classifiers;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Map;

public interface IClassifier {
    Map<String, Float> classify(float[] input);

    MappedByteBuffer loadModel(String labelsFileName) throws IOException;
}
