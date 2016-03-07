package ch.vincentgenecand.bfh.cpvr.imagejplugin.util;

import java.awt.*;
import java.util.Arrays;

/**
 * Created by Vincent Genecand on 06.03.2016.
 */
public class DeBayerConverter {

    private final byte[] pixels;
    private final int width;
    private final int height;

    private final int[] pixelsRGB;
    private final byte[] pixelsHue;
    private final byte[] pixelsBrightness;

    public DeBayerConverter(byte[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;

        this.pixelsRGB = new int[width * height];
        this.pixelsHue = new byte[width * height];
        this.pixelsBrightness = new byte[width * height];
    }

    public int[] deBayer() {
        return pixelsRGB;
    }

    public byte[] brightness() {
        return pixelsBrightness;
    }

    public byte[] hue() {
        return pixelsHue;
    }

    public void convert() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int rgb[] = this.getRGBValueForPixel(x, y);
                float[] hsb = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);

                pixelsRGB[index] = ((rgb[0] & 0xff) << 16) + ((rgb[1] & 0xff) << 8) + (rgb[2] & 0xff);
                pixelsHue[index] = (byte) (hsb[0] * 255f);
                pixelsBrightness[index] = (byte) (hsb[2] * 255f);
            }
        }
    }

    private int[] getRGBValueForPixel(int x, int y) {
        int rgb[] = new int[3];

        rgb[0] = this.getColorValueFromSurrounding(BayerColor.RED, x, y);
        rgb[1] = this.getColorValueFromSurrounding(BayerColor.GREEN, x, y);
        rgb[2] = this.getColorValueFromSurrounding(BayerColor.BLUE, x, y);

        return rgb;
    }

    private BayerColor getPixelColor(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return BayerColor.NONE;
        }

        return BayerColor.getById((y % 2 - x % 2));
    }

    private int getPixelValue(int x, int y) {
        return this.pixels[y * this.width + x] & 0xff;
    }

    private int getColorValueFromSurrounding(BayerColor bayerColor, int x, int y) {
        int value = 0;
        int count = 0;

        if (this.getPixelColor(x, y).equals(bayerColor)) {
            value = this.getPixelValue(x, y);
            count++;
        } else {
            for (int i = x - 1; i < x + 2; i++) {
                for (int j = y - 1; j < y + 2; j++) {
                    if ((i != x || j != y) && this.getPixelColor(i, j).equals(bayerColor)) {
                        value += this.getPixelValue(i, j);
                        count++;
                    }
                }
            }
        }

        return value / count;
    }

    private enum BayerColor {
        RED(1),
        BLUE(-1),
        GREEN(0),
        NONE(Integer.MAX_VALUE);

        private final int id;

        BayerColor(int id) {
            this.id = id;
        }

        public static BayerColor getById(int id) {
            return Arrays.asList(BayerColor.values()).stream().filter(c -> c.id == id).findFirst().orElse(NONE);
        }

        @Override
        public String toString() {
            return this.name();
        }
    }
}
