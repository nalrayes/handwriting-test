package com.mygdx.game;


//import android.content.res;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.content.Context;
import android.graphics.Color;
import android.app.Activity;
//import android.uirendering.cts.CanvasCompareActivityTest;
import android.graphics.drawable.shapes.RectShape;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.System.exit;


/**
 * Image comparison using Structural Similarity Index, developed by Wang, Bovik, Sheikh, and
 * Simoncelli. Details can be read in their paper :
 *
 * https://ece.uwaterloo.ca/~z70wang/publications/ssim.pdf
 */
public class AndroidSSIM implements SSIM {
    // These values were taken from the publication
    public static final double CONSTANT_L = 254;
    public static final double CONSTANT_K1 = 0.01;
    public static final double CONSTANT_K2 = 0.03;
    public static final double CONSTANT_C1 = Math.pow(CONSTANT_L * CONSTANT_K1, 2);
    public static final double CONSTANT_C2 = Math.pow(CONSTANT_L * CONSTANT_K2, 2);
    public static final float MIN_SSIM = 0.7f;
    public static final int WINDOW_SIZE = 10;
//    private int WINDOW_SIZE;

    private Resources res;

    public AndroidSSIM(Resources resources) {
        res = resources;
    }
    private float calculateSSIM(int[] ideal, int[] given, int offset, int stride, int width,
                              int height) {
        float SSIMTotal = 0;
        int interestingRegions = 0;
        for (int i = 0 ; i < height ; i += WINDOW_SIZE) {
            for (int j = 0 ; j < width ; j += WINDOW_SIZE) {
                int start = j + (i * stride) + offset;
//                if (inspectRegions(ideal, start, stride) ||
//                        inspectRegions(given, start, stride)) {
                    interestingRegions++;
                    float meanX = meanIntensityOfWindow(ideal, start, stride);
                    float meanY = meanIntensityOfWindow(given, start, stride);
                    float stdX = standardDeviationIntensityOfWindow(ideal, meanX, start, offset);
                    float stdY = standardDeviationIntensityOfWindow(given, meanY, start, offset);
                    float stdBoth = standardDeviationBothWindows(ideal, given, meanX, meanY, start,
                            stride);
//                    System.out.println("meanX: " + meanX + " meanY: " + meanY + " stdX: " + stdX + " stdY: " + stdY + " stdBoth: " + stdBoth);
                    if (Float.isNaN(stdBoth)) {
                        interestingRegions--;
                        continue;
                    }
                    SSIMTotal += SSIM(meanX, meanY, stdX, stdY, stdBoth);
//                }
            }
        }
        if (interestingRegions == 0) {
            return 1;
        }
        System.out.println(SSIMTotal + " INTERESTING: " + interestingRegions);
        SSIMTotal /= interestingRegions;
        return SSIMTotal;
    }
    /**
     * Checks to see if the entire region is white, and if so it returns true
     */
    private boolean inspectRegions(int[] colors, int x, int stride) {
        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                if (colors[x + j + (i * stride)] != Color.WHITE) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Finds the SSIM for the given parameters
     */
    private float SSIM(float muX, float muY, float sigX, float sigY, float sigXY) {
        float SSIM = (float) ((2 * muX * muY + CONSTANT_C1) * (2 * sigXY + CONSTANT_C2));
        SSIM /= (muX * muX + muY * muY + CONSTANT_C1);
        SSIM /= (sigX * sigX + sigY * sigY + CONSTANT_C2);
        return SSIM;
    }
    /**
     * Finds the standard deviation amongst the two windows
     */
    private float standardDeviationBothWindows(int[] pixel1, int[] pixel2, float mean1, float mean2,
                                               int start, int stride) {
        float val = 0;
        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                val += ((getIntensity(pixel1[index]) - mean1) * (getIntensity(pixel2[index]) - mean2));
//                if (Float.isNaN(getIntensity(pixel1[index])) || Float.isNaN(mean1)
//                        || Float.isNaN(getIntensity(pixel2[index])) || Float.isNaN(mean2)
//                        || Float.isNaN(val)) {
//                    System.out.println("NAN FOUND: ");
//                    System.out.println("Intensity1: " + getIntensity(pixel1[index]) + "mean1: " + mean1);
//                    System.out.println("Intensity2: " + getIntensity(pixel2[index]) + "mean2: " + mean2);
//                    System.out.println("val: " + val);
//                    exit(0);
//                }
            }
        }
        val /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        val = (float) Math.pow(val, .5);
//        if (Float.isNaN(val)) {
//            val = 0.0f;
//        }
        return val;
    }
    /**
     * Finds the standard deviation of the given window
     */
    private float standardDeviationIntensityOfWindow(int[] pixels, float meanIntensity, int start,
                                                     int stride) {
        float stdDev = 0f;
        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                stdDev += Math.pow(getIntensity(pixels[index]) - meanIntensity, 2);
            }
        }
        stdDev /= (WINDOW_SIZE * WINDOW_SIZE) - 1;
        stdDev = (float) Math.pow(stdDev, .5);
        return stdDev;
    }
    /**
     * Finds the mean of the given window
     */
    private float meanIntensityOfWindow(int[] pixels, int start, int stride) {
        float avgL = 0f;
        for (int i = 0 ; i < WINDOW_SIZE ; i++) {
            for (int j = 0 ; j < WINDOW_SIZE ; j++) {
                int index = start + (i * stride) + j;
                avgL += getIntensity(pixels[index]);
            }
        }
        return (avgL / (WINDOW_SIZE * WINDOW_SIZE));
    }
    /**
     * Gets the intensity of a given pixel in RGB using luminosity formula
     *
     * l = 0.21R + 0.72G + 0.07B
     */
    private float getIntensity(int pixel) {
        float l = 0;
        l += (0.21f * Color.red(pixel));
        l += (0.72f * Color.green(pixel));
        l += (0.07f * Color.blue(pixel));
        return l;
    }

    @Override
    public float getSSIM(String path1, String path2) {

//        File image1 = new File(path1);
//        File image2 = new File(path2);
        AssetManager manager = res.getAssets();
        InputStream open1 = null;
        InputStream open2 = null;

        try {
            open1 = manager.open(path1);
            open2 = manager.open(path2);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        if (!image1.canRead() || !image2.canRead()) {
//            System.out.print("NONONO\n");
//            if (!image1.exists()) {
//                System.out.println("CANNOT READ 1");
//            }
//            if (!image2.exists()) {
//                System.out.println("CANNOT READ 2");
//            }
//            return -3;
//        }

//        BitmapDrawable image1BMD = new BitmapDrawable(res, image1.getAbsolutePath());
//        BitmapDrawable image2BMD = new BitmapDrawable(res, image2.getAbsolutePath());

//        if (image1BMD == null || image2BMD == null) {
//            System.out.println("FUCKING NUL");
//            return -2;
//        }



//        String absPath1 = new File(path1).getAbsolutePath();
//        String absPath2 = new File(path2).getAbsolutePath();
//
//        Bitmap image1Bitmap = BitmapFactory.decodeFile(path1);
//        Bitmap image2Bitmap = BitmapFactory.decodeFile(path2);

        Bitmap image1Bitmap = BitmapFactory.decodeStream(open1);
        Bitmap image2Bitmap = BitmapFactory.decodeStream(open2);

        int width1 = image1Bitmap.getWidth();
        int width2 = image2Bitmap.getWidth();
        int height1 = image1Bitmap.getHeight();
        int height2 = image2Bitmap.getHeight();

        if (height1 != height2|| width1 != width2) {
            return -100;
        }
        int[] image1Arr = new int[width1 * height1];
        int[] image2Arr = new int[width2 * height2];

        image1Bitmap.getPixels(image1Arr, 0, width1, 0, 0, width1, height1);
        image2Bitmap.getPixels(image2Arr, 0, width2, 0, 0, width2, height2);

        float SSIM = calculateSSIM(image1Arr, image2Arr, 0, width1, width1, height1);
        return SSIM;
    }
}