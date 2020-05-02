package com.lampa.emotionrecognition.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImagePreprocessor {
    public static int[] toGreyScale(Bitmap src) {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;

        // pixel information
        int R, G, B;
        int pixel;

        // get image size
        int width = src.getWidth();
        int height = src.getHeight();

        int[] pixels = new int[width * height];

        // scan through every single pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // get one pixel color
                pixel = src.getPixel(x, y);
                // retrieve color of all channels
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                pixels[y * width + x] = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
            }
        }

        return pixels;
    }
}
