package com.mygdx.game;

/**
 * Created by nayef on 7/16/16.
 */
public interface SSIM {

    /**
     * Compares the two photos and returns an index that represents
     * how similar the two photos are to each other.
     * @param path1 path to the first photo
     * @param path2 path to the second photo
     * @return the SSIM index of the two photos
     */
    float getSSIM(String path1, String path2);
}
