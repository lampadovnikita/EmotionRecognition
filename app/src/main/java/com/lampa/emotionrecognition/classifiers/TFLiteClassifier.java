package com.lampa.emotionrecognition.classifiers;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class TFLiteClassifier implements IClassifier {
    protected AssetManager mAssetManager;

    protected Interpreter mTFLiteInterpreter;

    protected Interpreter.Options mTFLiteInterpreterOptions;

    protected List<String> mLabels;

    TFLiteClassifier(AssetManager assetManager, String modelFileName, String labelsFileName) {
        mAssetManager = assetManager;

        GpuDelegate delegate = new GpuDelegate();
        mTFLiteInterpreterOptions = new Interpreter.Options().addDelegate(delegate);

        try {
            mTFLiteInterpreter = new Interpreter(loadModel(modelFileName), mTFLiteInterpreterOptions);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mLabels = loadLabels(labelsFileName);
    }

    public MappedByteBuffer loadModel(String modelFileName) throws IOException {
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = mAssetManager.openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public ArrayList<String> loadLabels(String labelsFileName) {
        ArrayList<String> labels = new ArrayList<String>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(mAssetManager.open(labelsFileName), StandardCharsets.UTF_8));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                labels.add(mLine);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return labels;
    }

}
