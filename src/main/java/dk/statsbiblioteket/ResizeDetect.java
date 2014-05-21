package dk.statsbiblioteket;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Open an image and attempts to detect whether is has been resized (or more specifically upscaled).
 * The detection looks for x1½ upscaling and x2 upscaling. These two factors has been seen in the
 * wild as 200DPI -> 300DPI and 200DPI -> 400DPI upscales.
 * </p><p>
 * The detection is extremely reliable if no interpolation has been used (the trivial case) and
 * fairly reliable when linear interpolation has been used. More advances interpolation, notably
 * Bi-cubic/Lanczos leaves fainter signs and detections works poorly for those.
 * </p><p>
 * For x2 detection, the spread of the values of 2x2-blocks of pixels are collected and the spread of
 * the collected values are calculated. This is done for all offsets: (0, 0), (0, 1), (1, 0) and (1, 1).
 * If one of the spreads are
 */
// TODO: x1½
public class ResizeDetect {
    public static final String USAGE =
            "Attempts to determine if an image has been upscaled 1½ or 2 times.\n"
            + "\n"
            + "Usage:\n"
            + "ResizeDetect image*\n"
            + "\n"
            +"Sample\n"
            + "ResizeDetect myimage.jpg myotherimage.tif";

    /**
     * Analyze the given images and attempt to guess if they have been upscaled.
     * @param args image*
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(2);
        }
        List<File> images = new ArrayList<>(args.length);
        for (String arg: args) {
            images.add(new File(arg));
            if (!images.get(images.size()).exists()) {
                throw new FileNotFoundException(
                        "The image '" + images.get(images.size()) + "' could not be located\n" + USAGE);
            }
        }


        ResizeDetect resizeDetector = new ResizeDetect();
        List<AnalysisResult> results = new ArrayList<>(images.size());
        for (File image: images) {
            results.add(resizeDetector.analyze(image));
        }
        for (AnalysisResult result: results) {
            System.out.println(result);
        }
    }

    public AnalysisResult analyze(File image) throws IOException {
        return analyze2x2(image, ImageIO.read(image));
    }

    private AnalysisResult analyze2x2(File imageFile, BufferedImage image) {
        final int CONTRAST_SIDE = 7; // Something small that is not likely to match a scale factor
        final int RECT_WIDTH = 2;
        final int RECT_HEIGHT = 2;
        final long[][] rectRGB = new long[3][RECT_WIDTH*RECT_HEIGHT];
        final int a = 0;
        final int b = 1;
        final int c = 2;
        final int d = 3;
        final double[] deviations = new double[3*RECT_WIDTH*RECT_HEIGHT]; // stats for stats

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Overall image contrast (average %dx%d block deviation): %.2f\n",
                                CONTRAST_SIDE, CONTRAST_SIDE, averageContrast(image, CONTRAST_SIDE)));
        sb.append("2x2 blocks\n");
        sb.append("Offset   ABCD  AB_CD  AC_BD\n");
        int deviationIndex = 0;
        for (int offsetX = 0 ; offsetX < RECT_WIDTH ; offsetX++) {
            for (int offsetY = 0 ; offsetY < RECT_HEIGHT ; offsetY++) {
                // AB
                // CD
                double sumMaxDeviationABCD = 0.0;
                double sumMaxDeviationAB_CD = 0.0;
                double sumMaxDeviationAC_BD = 0.0;

                // Iterate over all the rectangles in the image
                // We ignore the last row and column if they are 1 pixel wide
                int blockCount = 0;
                for (int y = offsetY ; y < image.getHeight()-1 ; y+=RECT_HEIGHT) {
                    for (int x = offsetX ; x < image.getWidth()-1 ; x+=RECT_WIDTH) {
                        blockCount++;
                        // Calculate max standard deviation for all color channels within the pixels in the rectangle
                        extractChannelValues(image, y, x, RECT_WIDTH, RECT_HEIGHT, rectRGB);
                        sumMaxDeviationABCD += getMaxDeviation(rectRGB, a, b, c, d);
                        sumMaxDeviationAB_CD += (getMaxDeviation(rectRGB, a, b) + getMaxDeviation(rectRGB, c, d)) / 2;
                        sumMaxDeviationAC_BD += (getMaxDeviation(rectRGB, a, c) + getMaxDeviation(rectRGB, b, d)) / 2;
                    }
                }
                sb.append(String.format("(%d, %d)  %5.2f  %5.2f  %5.2f\n", offsetX, offsetY,
                                        sumMaxDeviationABCD / blockCount, sumMaxDeviationAB_CD / blockCount,
                                        sumMaxDeviationAC_BD / blockCount));
                deviations[deviationIndex] =   sumMaxDeviationABCD / blockCount;
                deviations[deviationIndex+1] = sumMaxDeviationAB_CD / blockCount;
                deviations[deviationIndex+2] = sumMaxDeviationAC_BD / blockCount;
                deviationIndex += 3;
            }
        }
        sb.append(String.format("Standard deviation of mean standard deviations (all the numbers in the table): %.2f\n",
                                Stats.standardDeviation(deviations)));
        return new AnalysisResult(imageFile, getName(), sb.toString());
    }

    private void extractChannelValues(BufferedImage image, int y, int x, int width, int height, long[][] rectRGB) {
        int index = 0;
        for (int rectY = y ; rectY < y+ height; rectY++) {
            for (int rectX = x ; rectX < x+ width; rectX++) {
                final int clr = image.getRGB(rectX, rectY);
                rectRGB[0][index] = (clr & 0x00ff0000) >> 16; // red
                rectRGB[1][index] = (clr & 0x0000ff00) >> 8;  // green
                rectRGB[2][index++] = clr & 0x000000ff;       // blue
            }
        }
    }

    private double averageContrast(BufferedImage image, int rectSide) {
        final long[][] rectRGB = new long[3][rectSide*rectSide];
        final double[] maxCache = new double[3];
        double sumMaxDeviationABCD = 0.0;
        int rectCount = 0;

        for (int y = 0 ; y < image.getHeight()-rectSide ; y+=rectSide) {
            for (int x = 0 ; x < image.getWidth()-rectSide ; x+=rectSide) {
                rectCount++;
                extractChannelValues(image, y, x, rectSide, rectSide, rectRGB);
                for (int channel = 0 ; channel < 3 ; channel++) {
                    maxCache[channel] = Stats.standardDeviation(rectRGB[channel]);
                }
                sumMaxDeviationABCD += Stats.max(maxCache);
            }
        }
        return 1.0 * sumMaxDeviationABCD / rectCount;
    }

    private double getMaxDeviation(long[][] rectRGB, int... entries) {
        final double[] maxCache = new double[3];
        final long[] channelValues = new long[entries.length];

        for (int channel = 0 ; channel < 3 ; channel++) {
            for (int i = 0 ; i < entries.length ; i++) {
                channelValues[i] = rectRGB[channel][entries[i]];
            }
            maxCache[channel] = Stats.standardDeviation(channelValues);
        }
        return Stats.max(maxCache);
    }

    public String getName() {
        return "Upscale detector";
    }
    public String usage() {
        return USAGE;
    }
}
