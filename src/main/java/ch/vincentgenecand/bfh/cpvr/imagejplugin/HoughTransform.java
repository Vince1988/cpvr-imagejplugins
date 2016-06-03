package ch.vincentgenecand.bfh.cpvr.imagejplugin;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class HoughTransform implements PlugInFilter {

    public static void main(String[] args) {
        HoughTransform plugin = new HoughTransform();
        ImagePlus im = new ImagePlus("images/Polygon2.png");
        im.show();
        plugin.setup("", im);
        plugin.run(im.getProcessor());
    }

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
        long msStart = System.currentTimeMillis();

        // Set up Hough space
        final int nAng = 256;                   // number of angels
        final int nRad = 256;                   // number of radii
        final int nLines = 4;                   // number of lines to search
        final int nonMaxR = 1;                  // Radius for non max. suppression

        int xC = ip.getWidth() / 2;               // x-coordinate of image center
        int yC = ip.getHeight() / 2;              // y-coordinate of image center
        double dAng = (Math.PI / nAng);           // step size of angle
        double rMax = Math.sqrt(xC * xC + yC * yC); // max. radius (center to corner)
        double dRad = (2 * rMax) / nRad;            // step size radius
        int[][] hough1 = new int[nAng][nRad];   // Hough accumulator space
        int[][] hough2 = new int[nAng][nRad];   // Hough accumulator space w. non max. supression

        int maxAccum = 0;                       // max. value in Hough space
        double[] cos = new double[nAng];        // array for precalculated cos values
        double[] sin = new double[nAng];        // array for precalculated sin values
        int[] lineRad = new int[nLines];        // array for radius of strongest lines
        int[] lineAng = new int[nLines];        // array for angle of strongest lines

        // Precalculate cos & sin values
        for (int t = 0; t < nAng; t++) {
            double theta = dAng * t;
            cos[t] = Math.cos(theta);
            sin[t] = Math.sin(theta);
        }

        // Fill Hough array & keep maximum
        for (int y = 0; y < ip.getHeight(); y++) {
            for (int x = 0; x < ip.getWidth(); x++) {
                int pixel = ip.getPixel(x, y);
                if (pixel > 0) {
                    int cx = x - xC, cy = y - yC;

                    // Calculate for all theta angles the radius r & increment the hough array
                    for (int t = 0; t < nAng; t++) {
                        int r = (int) Math.round((cx * cos[t] + cy * sin[t]) / dRad) + nRad / 2;
                        if (r >= 0 && r < nRad) {
                            hough1[t][r]++;
                            if (hough1[t][r] > maxAccum) maxAccum++;
                        }
                    }
                }
            }
        }

        long msSpace = System.currentTimeMillis();

        // Build non maximum supression with radius nonMaxR into hough2
        for (int x = 0; x < hough1.length; x++) {
            for (int y = 0; y < hough1[x].length; y++) {
                hough2[x][y] = valueFor(x, y, hough1, nonMaxR);
            }
        }

        // Build histogram of the array hough2
        int[] histogram = new int[maxAccum + 1];
        for (int[] hough2Lines : hough2) {
            for (int hough2Cell : hough2Lines) {
                histogram[hough2Cell]++;
            }
        }

        // Get n strongest lines into arrays lineAng & lineRad
        int linesFound = 0;
        for (int i = maxAccum; linesFound < nLines && i >= 0; i--) {
            if (histogram[i] > 0) {
                for (int x = 0; x < hough2.length; x++) {
                    for (int y = 0; y < hough2[x].length; y++) {
                        if (hough2[x][y] == i) {
                            lineAng[linesFound] = (int) (x * dAng);
                            lineRad[linesFound] = (int) (y * dRad);
                            linesFound++;
                        }
                    }
                }
            }
        }

        long msLines = System.currentTimeMillis();

        // Create RGB image and fill in Hough space as gray scale image
        ImagePlus imgAccum = NewImage.createRGBImage("imgAccum", nAng, nRad, 1, NewImage.FILL_BLACK);
        ImageProcessor ipAccum = imgAccum.getProcessor();
        int[] rgb = new int[3];
        for (int y = 0; y < ipAccum.getHeight(); y++) {
            for (int x = 0; x < ipAccum.getWidth(); x++) {   // Scale Hough space so that
                rgb[0] = rgb[1] = rgb[2] = (int) (((float) hough1[x][y] / (float) maxAccum) * 255.0f);
                ipAccum.putPixel(x, y, rgb);
            }
        }

        // Show and save Hough space image
        imgAccum.show();
        imgAccum.updateAndDraw();
        PNG_Writer png = new PNG_Writer();
        try {
            png.writeImage(imgAccum, "images/PolygonAccum.png", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("maxAccum: " + maxAccum);
        System.out.println("Time for Hough space: " + (msSpace - msStart) + " ms");
        System.out.println("Time for Hough lines: " + (msLines - msSpace) + " ms");
    }

    private int valueFor(int x, int y, int[][] hough, int gridRadius) {
        int max = hough[x][y];
        for (int dX = minValue(x, gridRadius); dX <= maxValue(x, hough.length, gridRadius); dX++) {
            for (int dY = minValue(y, gridRadius); dY <= maxValue(x, hough[x].length, gridRadius); dY++) {
                int val = hough[dX][dY];
                if (val > max) {
                    return 0;
                }
            }
        }
        return max;
    }


    private int maxValue(int value, int length, int gridRadius) {
        return value + gridRadius < length ? value + gridRadius : length - 1;
    }

    private int minValue(int value, int gridRadius) {
        return value - gridRadius >= 0 ? value - gridRadius : 0;
    }
}
