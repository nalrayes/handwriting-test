package com.mygdx.game.desktop;

import com.mygdx.game.SSIM;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.applet.*;
import java.awt.geom.*;
import java.awt.font.*;

/**
 * Created by nayef on 7/17/16.
 */
public class DesktopSSIM implements SSIM{
    protected ImagePlus image_1_imp, image_2_imp;  //THIS PLUGIN WORKS WITH TWO (AND ONLY TWO) IMAGES OPEN IN IMAGEJ
    protected ImageProcessor image_1_p, image_2_p;

    @Override
    public float getSSIM(String path1, String path2) {

        String title_1, title_2;
        int  pointer, filter_length, image_height, image_width, image_dimension, bits_per_pixel_1, bits_per_pixel_2, a, b, c;
        float filter_weights [];
        double [] ssim_map;
        double ssim_index;
//
// ERROR CONTROLS. TWO IMAGES SHOULD BE OPENED AND BOTH WITH THE SAME DIMENSIONS
//

        image_1_imp = new ImagePlus(path1);
        image_2_imp = new ImagePlus(path2);
        image_height = image_1_imp.getHeight();
        a= image_2_imp.getHeight();
        if (a!=image_height) {
//            IJ.error("Both images must have the same height");
            return -1;
        }
        image_width = image_1_imp.getWidth();
        a= image_2_imp.getWidth();
        if (a!=image_width) {
//            IJ.error("Both images must have the same width");
            return -1;
        }
        bits_per_pixel_1=image_1_imp.getBitDepth();
        bits_per_pixel_2=image_2_imp.getBitDepth();
        if (bits_per_pixel_1 != bits_per_pixel_2){
//            IJ.error("Both images must have the same number of bits per pixel");
            return -1;
        }
        if (bits_per_pixel_1 == 24){
//            IJ.error("RGB images are not supportedl");
            return -1;
        }
//
// END OF CONTROL ERRORS
//
//
// THIS DIALOG BOX SHOWS DIFFERENT OPTIONS TO CREATE THE WINDOW WE ARE GOING TO USE TO EVALUATE SSIM INDEX OVER THE ENTIRE IMAGES
//
        double sigma_gauss = 1.5;
        int filter_width = 11;
        int filter_scale = 20;
        double K1 = 0.01;
        double K2 = 0.03;
        double downsampled = (int) image_height / 256;
        double downsampled_backup = downsampled;
        boolean gaussian_window = true;
        String[] window_type = {"Gaussian","Same weight"};  // WE CAN WEIGHTS THE WINDOW WITH A GAUSSIAN WEIGHTING FUNCTION OR GIVING THE SAME WEIGHT TO ALL THE PIXELS IN THE WINDOW
        String window_selection = window_type[0];
        boolean out=false;
        boolean show_downsampled_images= false;
        boolean show_gaussian_filter= false;
        boolean show_ssim_map= false;

        double C1 = (Math.pow(2, bits_per_pixel_1) - 1)*K1;
        C1= C1*C1;
        double C2 = (Math.pow(2, bits_per_pixel_1) - 1)*K2;
        C2=C2*C2;
//
// NOW, WE CREATE THE FILTER, GAUSSIAN OR MEDIA FILTER, ACCORDING TO THE VALUE OF boolean "gaussian_window"
//
        filter_length = filter_width*filter_width;
        float window_weights [] = new float [filter_length];
        double [] array_gauss_window = new double [filter_length];

        if (gaussian_window) {

            double value, distance = 0;
            int center = (filter_width/2);
            double total = 0;
            double sigma_sq=sigma_gauss*sigma_gauss;

            for (int y = 0; y < filter_width; y++){
                for (int x = 0; x < filter_width; x++){
                    distance = Math.abs(x-center)*Math.abs(x-center)+Math.abs(y-center)*Math.abs(y-center);
                    pointer = y*filter_width + x;
                    array_gauss_window[pointer] = Math.exp(-0.5*distance/sigma_sq);
                    total = total + array_gauss_window[pointer];
                }
            }
            for (pointer=0; pointer < filter_length; pointer++) {
                array_gauss_window[pointer] = array_gauss_window[pointer] / total;
                window_weights [pointer] = (float) array_gauss_window[pointer];
            }
        }
        else { 								// NO WEIGHTS. ALL THE PIXELS IN THE EVALUATION WINDOW HAVE THE SAME WEIGHT
            for (pointer=0; pointer < filter_length; pointer++) {
                array_gauss_window[pointer]= (double) 1.0/ filter_length;
                window_weights [pointer] = (float) array_gauss_window[pointer];
            }
        }
        if (show_gaussian_filter) {					// IN CASE OF A GAUSSIAN FILTER, YOU CAN SHOW IT IF YOU WANT
            ColorModel cm=null;
            ImageProcessor gauss_window_ip = new FloatProcessor (filter_width, filter_width, window_weights, cm);
            gauss_window_ip = gauss_window_ip.resize (filter_width*filter_scale);
            String title_filtro_1 = "Sigma: " + sigma_gauss + " Width: "+ filter_width + " p�xeles";
            ImagePlus gauss_window_imp = new ImagePlus (title_filtro_1, gauss_window_ip);
            gauss_window_imp.show();
            gauss_window_imp.updateAndDraw();
        }
//
// END OF FILTER SELECTION
//
//
// MAIN ALGORITHM
//
        ImageProcessor image_1_original_p = image_1_imp.getProcessor();
        ImageProcessor image_2_original_p = image_2_imp.getProcessor();

        image_width = image_1_original_p.getWidth();
        image_width = (int) (image_width/downsampled);
        image_1_original_p.setInterpolate(true);
        image_2_original_p.setInterpolate(true);
        image_1_p= image_1_original_p.resize (image_width);
        image_2_p= image_2_original_p.resize (image_width);

        image_height = image_1_p.getHeight();
        image_width = image_1_p.getWidth();
        image_dimension = image_width*image_height;

        ImageProcessor mu1_ip = new FloatProcessor (image_width, image_height);
        ImageProcessor mu2_ip = new FloatProcessor (image_width, image_height);
        float [] array_mu1_ip = (float []) mu1_ip.getPixels();
        float [] array_mu2_ip = (float []) mu2_ip.getPixels();

        float [] array_mu1_ip_copy = new float [image_dimension];
        float [] array_mu2_ip_copy = new float [image_dimension];

        a=b=0;
        for (pointer =0; pointer<image_dimension; pointer++) {

            if (bits_per_pixel_1 == 8) {
                a = (0xff & image_1_p.get (pointer));
                b = (0xff & image_2_p.get(pointer));
            }
            if (bits_per_pixel_1 == 16) {
                a = (0xffff & image_1_p.get(pointer));
                b = (0xffff & image_2_p.get(pointer));
            }
            if (bits_per_pixel_1 == 32) {
                a = (image_1_p.get(pointer));
                b = (image_2_p.get(pointer));
            }
            array_mu1_ip [pointer] = array_mu1_ip_copy [pointer] = a; // Float.intBitsToFloat(a);
            array_mu2_ip [pointer] = array_mu2_ip_copy [pointer] = b; //Float.intBitsToFloat(b);
        }
        mu1_ip.convolve (window_weights, filter_width, filter_width);
        mu2_ip.convolve (window_weights, filter_width, filter_width);

        double [] mu1_sq = new double [image_dimension];
        double [] mu2_sq = new double [image_dimension];
        double [] mu1_mu2 = new double [image_dimension];

        for (pointer =0; pointer<image_dimension; pointer++) {
            mu1_sq[pointer] = (double) (array_mu1_ip [pointer]*array_mu1_ip [pointer]);
            mu2_sq[pointer] = (double) (array_mu2_ip[pointer]*array_mu2_ip[pointer]);
            mu1_mu2 [pointer]= (double) (array_mu1_ip [pointer]*array_mu2_ip[pointer]);
        }

        double [] sigma1_sq = new double [image_dimension];
        double [] sigma2_sq = new double [image_dimension];
        double [] sigma12 = new double [image_dimension];

        for (pointer =0; pointer<image_dimension; pointer++) {

            sigma1_sq[pointer] =(double) (array_mu1_ip_copy [pointer]*array_mu1_ip_copy [pointer]);
            sigma2_sq[pointer] =(double) (array_mu2_ip_copy [pointer]*array_mu2_ip_copy [pointer]);
            sigma12 [pointer] =(double) (array_mu1_ip_copy [pointer]*array_mu2_ip_copy [pointer]);
        }
//
//THERE IS A METHOD IN IMAGEJ THAT CONVOLVES ANY ARRAY, BUT IT ONLY WORKS WITH IMAGE PROCESSORS. THIS IS THE REASON BECAUSE I CREATE THE FOLLOWING PROCESSORS
//
        ImageProcessor soporte_1_ip = new FloatProcessor (image_width, image_height);
        ImageProcessor soporte_2_ip = new FloatProcessor (image_width, image_height);
        ImageProcessor soporte_3_ip = new FloatProcessor (image_width, image_height);
        float [] array_soporte_1 =  (float []) soporte_1_ip.getPixels();
        float [] array_soporte_2 =  (float []) soporte_2_ip.getPixels();
        float [] array_soporte_3 =  (float []) soporte_3_ip.getPixels();

        for (pointer =0; pointer<image_dimension; pointer++) {
            array_soporte_1[pointer] = (float) sigma1_sq[pointer];
            array_soporte_2[pointer] = (float) sigma2_sq[pointer];
            array_soporte_3[pointer] = (float) sigma12[pointer];
        }
        soporte_1_ip.convolve (window_weights, filter_width,  filter_width);
        soporte_2_ip.convolve (window_weights, filter_width,  filter_width);
        soporte_3_ip.convolve (window_weights, filter_width,  filter_width);

        for (pointer =0; pointer<image_dimension; pointer++) {
            sigma1_sq[pointer] =  array_soporte_1[pointer] - mu1_sq[pointer];
            sigma2_sq[pointer] =  array_soporte_2[pointer ]- mu2_sq[pointer];
            sigma12[pointer] =  array_soporte_3[pointer] - mu1_mu2[pointer];
        }
        ssim_map = new double [image_dimension];
        double suma=0;
        for (pointer =0; pointer<image_dimension; pointer++) {
            ssim_map[pointer] = (double) (( 2*mu1_mu2[pointer] + C1)* (2*sigma12[pointer] + C2)) / ((mu1_sq[pointer]+mu2_sq[pointer] + C1) * (sigma1_sq[pointer] + sigma2_sq[pointer] + C2));
            suma = suma + ssim_map[pointer];
        }
        ssim_index = (double) suma / image_dimension;
        String message_1= " ";
        if (show_ssim_map) {
            ImageProcessor ssim_map_ip = new FloatProcessor (image_width, image_height, ssim_map);
            message_1= "SSIM Index:   " + ssim_index;
            ImagePlus ssim_map_imp = new ImagePlus (message_1, ssim_map_ip);
            ssim_map_imp.show();
            ssim_map_imp.updateAndDraw();
        }
        if (show_downsampled_images) {
            title_1 = image_1_imp.getTitle();
            title_2 = image_2_imp.getTitle();
            title_1 = title_1 + " down scaled " + downsampled + " times";
            title_2 = title_2 + " down scaled " + downsampled + " times";
            ImagePlus image_1_final_imp = new ImagePlus (title_1, image_1_p);
            image_1_final_imp.show();
            image_1_final_imp.updateAndDraw();
            ImagePlus image_2_final_imp = new ImagePlus (title_2, image_2_p);
            image_2_final_imp.show();
            image_2_final_imp.updateAndDraw();
        }
        message_1= " ";
        return (float) ssim_index;
    }
}
