package com.lampa.emotionrecognition.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImagePreprocessor {
    public static int[] toGreyScale(Bitmap src) {
        // Константы для перевода пикселя в чёрно-белый полутоновый формат
        final double RED2GS = 0.299;
        final double GREEN2GS = 0.587;
        final double BLUE2GS = 0.114;

        // Информация о пикселе
        int pixel;
        int redPart;
        int greenPart;
        int bluePart;

        int width = src.getWidth();
        int height = src.getHeight();

        int[] pixels = new int[width * height];

        // Проходим по каждому пикселю
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = src.getPixel(x, y);

                // Получаем все цветовые компоненты конкретного пикселя
                redPart = Color.red(pixel);
                greenPart = Color.green(pixel);
                bluePart = Color.blue(pixel);

                // Переводим пиксель в чёрно-белый полутоновый формат
                pixels[y * width + x] =
                        (int) (redPart * RED2GS + greenPart * GREEN2GS + bluePart * BLUE2GS);
            }
        }

        return pixels;
    }
}
