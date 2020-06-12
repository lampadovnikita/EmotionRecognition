package com.lampa.emotionrecognition.classifiers;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Абстрактыный классификатор, использующий формат tflite
public abstract class TFLiteClassifier implements IClassifier {
    protected AssetManager mAssetManager;

    protected Interpreter mTFLiteInterpreter;

    protected Interpreter.Options mTFLiteInterpreterOptions;

    protected List<String> mLabels;

    TFLiteClassifier(AssetManager assetManager, String modelFileName, String[] labels) {
        mAssetManager = assetManager;

        // Выносим вычисление на GPU
        GpuDelegate delegate = new GpuDelegate();
        mTFLiteInterpreterOptions = new Interpreter.Options().addDelegate(delegate);

        try {
            // Создаём интерпретатор загруженной модели
            mTFLiteInterpreter = new Interpreter(
                    loadModel(modelFileName),
                    mTFLiteInterpreterOptions);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mLabels = new ArrayList<>(Arrays.asList(labels));
    }
    // Загружаем модель в буффер из файла
    public MappedByteBuffer loadModel(String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = mAssetManager.openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Закрваем интерпретатор, чтобы избежать утечек памяти
    public void close() {
        mTFLiteInterpreter.close();
    }
}
