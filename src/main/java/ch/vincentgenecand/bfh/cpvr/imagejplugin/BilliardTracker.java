package ch.vincentgenecand.bfh.cpvr.imagejplugin;

import ch.vincentgenecand.bfh.cpvr.imagejplugin.util.DeBayerConverter;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PNG_Writer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class BilliardTracker implements PlugInFilter {

    private final static String IMG_LOCATION = "images/billiard/";

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip1) {
        int width = ip1.getWidth();
        int height = ip1.getHeight();
        byte[] pixRaw = (byte[]) ip1.getPixels();
        DeBayerConverter deBayerConverter = new DeBayerConverter(pixRaw, width, height);

        ImagePlus imgGray = NewImage.createByteImage("GrayDeBayered", width, height, 1, NewImage.FILL_BLACK);
        ImageProcessor ipGray = imgGray.getProcessor();

        ImagePlus imgRGB = NewImage.createRGBImage("RGBDeBayered", width, height, 1, NewImage.FILL_BLACK);
        ImageProcessor ipRGB = imgRGB.getProcessor();

        ImagePlus imgHue = NewImage.createByteImage("Hue", width, height, 1, NewImage.FILL_BLACK);
        ImageProcessor ipHue = imgHue.getProcessor();


        long msStart = System.currentTimeMillis();

        deBayerConverter.convert();

        long ms = System.currentTimeMillis() - msStart;
        System.out.println(ms);
        ImageStatistics stats = ipGray.getStatistics();
        System.out.println("Mean:" + stats.mean);

        ipGray.setPixels(deBayerConverter.brightness());
        ipRGB.setPixels(deBayerConverter.deBayer());
        ipHue.setPixels(deBayerConverter.hue());

        PNG_Writer png = new PNG_Writer();
        try {
            png.writeImage(imgRGB, IMG_LOCATION + "Billard1024x544x3.png", 0);
            png.writeImage(imgHue, IMG_LOCATION + "Billard1024x544x1H.png", 0);
            png.writeImage(imgGray, IMG_LOCATION + "Billard1024x544x1B.png", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        imgGray.show();
        imgGray.updateAndDraw();
        imgRGB.show();
        imgRGB.updateAndDraw();
        imgHue.show();
        imgHue.updateAndDraw();
    }

    public static void main(String[] args) {
        ImageJ ij = new ImageJ(ImageJ.NO_SHOW);
        BilliardTracker plugin = new BilliardTracker();

        ImagePlus im = new ImagePlus(IMG_LOCATION + "Billard2048x1088x1.png");
        im.show();
        plugin.setup("", im);
        plugin.run(im.getProcessor());
    }
}
