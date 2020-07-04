package com.lampa.emotionrecognition.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
        // Get the angle of rotation of an image in memory
    public static int getOrientationAngle(ContentResolver contentResolver, Uri uri) {
        int degree = 0;
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            ExifInterface exifInterface = new ExifInterface(inputStream);

            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap rotateToNormalOrientation(ContentResolver contentResolver,
                                                   Bitmap imageBitmap, Uri imageUri) {

        int orientationAngle = ImageUtils.getOrientationAngle(contentResolver, imageUri);
        if (orientationAngle != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientationAngle);
            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    imageBitmap,
                    0,
                    0,
                    imageBitmap.getWidth(),
                    imageBitmap.getHeight(),
                    matrix,
                    true);

            return rotatedBitmap;
        }

        return imageBitmap;
    }

    public static int[] toGreyScale(Bitmap src) {
        // Constants for converting a pixel to black and white grayscale
        // Used in python PIL library
        final double RED2GS = 0.299;
        final double GREEN2GS = 0.587;
        final double BLUE2GS = 0.114;

        int pixel;
        int redPart;
        int greenPart;
        int bluePart;

        int width = src.getWidth();
        int height = src.getHeight();

        int[] pixels = new int[width * height];

        // For each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = src.getPixel(x, y);

                // Get all the color components of a current pixel.
                redPart = Color.red(pixel);
                greenPart = Color.green(pixel);
                bluePart = Color.blue(pixel);

                // Translate a pixel to grayscale format
                pixels[y * width + x] =
                        (int) (redPart * RED2GS + greenPart * GREEN2GS + bluePart * BLUE2GS);
            }
        }

        return pixels;
    }
}
