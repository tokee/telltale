package dk.statsbiblioteket.telltale;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Attempts to detect whether an image has been resized (or more specifically upscaled) using Bilinear scaling.
 * The detection looks for x2 upscaling.
 * </p><p>
 * Given the 5 pixel image {@code 1391}, it is assumed that horizontal upscale will result in {@code 12369511},
 * where 2 is taken from (1+3/2), 6 is taken from (3+9)/2 and 5 is from (9+1)/2.
 */
public class BilinearDetect extends ImageAnalyzerImpl {
    @Override
    public AnalysisResult analyze(File imageFile, BufferedImage image) {
        final int SCALE_X = 2;
        final int SCALE_Y = 2;
        final long[][] rectRGB = new long[3][(SCALE_X+1)*(SCALE_Y+1)];
        int c0_0 = 0;
        int c0_1 = 1;
        int c0_2 = 2;
        int c1_0 = 3;
        int c1_1 = 4;
        int c1_2 = 5;
        int c2_0 = 6;
        int c2_1 = 7;
        //int c2_2 = 8;

        StringBuilder sb = new StringBuilder();

        sb.append("Offset  Horizontal   Vertical     Center  avgCenter  fullImage\n");
        List<Candidate> candidates = new ArrayList<>(SCALE_X*SCALE_Y);
        double[] deviationsH = new double[3];
        double[] deviationsV = new double[3];
        double[] deviationsC = new double[3];
        for (int offsetX = 0 ; offsetX < SCALE_X ; offsetX++) {
            for (int offsetY = 0 ; offsetY < SCALE_Y ; offsetY++) {
                double sumMaxDeviationH = 0.0;
                double sumMaxDeviationV = 0.0;
                double sumMaxDeviationC = 0.0;

                // Iterate over all the rectangles in the image
                // We ignore the last row and column it they do not match the rectangle size
                int blockCount = 0;
                for (int y = offsetY ; y < image.getHeight()-SCALE_Y-1 ; y+=SCALE_Y) {
                    for (int x = offsetX ; x < image.getWidth()-SCALE_X-1 ; x+=SCALE_X) {
                        blockCount++;
                        // Calculate max standard deviation for all color channels within the pixels in the rectangle
                        extractChannelValues(image, y, x, (SCALE_X + 1), (SCALE_Y + 1), rectRGB);
                        for (int channel = 0 ; channel < 3 ; channel++) {
                            long[] rgb = rectRGB[channel];
                            deviationsH[channel] = Stats.standardDeviation(new double[]{
                                    (double) rgb[c1_0],
                                    1.0 * (rgb[c0_0] + rgb[c2_0]) / 2});
                            deviationsV[channel] = Stats.standardDeviation(new double[]{
                                    (double) rgb[c0_1],
                                    1.0 * (rgb[c0_0] + rgb[c0_2]) / 2});
                            deviationsC[channel] = Stats.standardDeviation(new double[]{
                                    (double) rgb[c1_1],
                                    1.0 * (rgb[c0_1] + rgb[c2_1] + rgb[c1_0] + rgb[c1_2]) / 4});
/*                            if (blockCount == 1 && channel == 0) {
                                System.out.println(String.format(
                                        "%d %d %d hd:%.2f vd:%.2f cd:%.2f\n%d %d %d\n%d %d %d\n",
                                        rgb[c0_0], rgb[c1_0], rgb[c2_0],
                                        deviationsH[channel], deviationsV[channel], deviationsC[channel],
                                        rgb[c0_1], rgb[c1_1], rgb[c2_1],
                                        rgb[c0_2], rgb[c1_2], rgb[c2_2]));
                            }*/
                        }

                        sumMaxDeviationH += Stats.max(deviationsH);
                        sumMaxDeviationV += Stats.max(deviationsV);
                        sumMaxDeviationC += Stats.max(deviationsC);
                    }
                }
                candidates.add(new Candidate(offsetX, offsetY,
                                             sumMaxDeviationH / blockCount, sumMaxDeviationV / blockCount,
                                             sumMaxDeviationC / blockCount,
                                             maxCenterFullDeviation(offsetX, offsetY, SCALE_X, SCALE_Y, image),
                                             avgCenterLocalDeviation(offsetX, offsetY, SCALE_X, SCALE_Y, image)));
            }
        }
        List<Candidate> sortedCandidates = getBestPermutation(candidates);
        for (Candidate candidate: sortedCandidates) {
            sb.append(candidate);
        }
        sb.append(String.format("Upscale model conformance score (lower is better): %.4f\n",
                                score(sortedCandidates)));
        return new AnalysisResult(imageFile, getName(), sb.toString());
    }

    private double maxCenterFullDeviation(int offsetX, int offsetY, int scaleX, int scaleY, BufferedImage image) {
        long[][] imgRGB = new long[3][(image.getWidth()/scaleX)*(image.getHeight()/scaleY)];
        int index = 0;
        for (int y = offsetY+1 ; y < image.getHeight()-scaleY-1 ; y+=scaleY) {
            for (int x = offsetX+1 ; x < image.getWidth()-scaleX-1 ; x+=scaleX) {
                fillValues(imgRGB, index++, image.getRGB(x, y));
            }
        }
        return Stats.max(new double[]{Stats.standardDeviation(imgRGB[0]), Stats.standardDeviation(imgRGB[1]),
                Stats.standardDeviation(imgRGB[2])});
    }

    private void fillValues(long[][] imgRGB, int index, int rgb) {
        imgRGB[0][index] = (rgb & 0x00ff0000) >> 16; // red
        imgRGB[1][index] = (rgb & 0x0000ff00) >> 8;  // green
        imgRGB[2][index] = rgb & 0x000000ff;         // blue
    }

    private double avgCenterLocalDeviation(int offsetX, int offsetY, int scaleX, int scaleY, BufferedImage image) {
        long[][] valRGB = new long[3][4];
        int samples = 0;
        double sum = 0.0;

        for (int y = offsetY+scaleY+1 ; y < image.getHeight()-scaleY*2-1 ; y+=scaleY) {
            for (int x = offsetX+scaleX+1 ; x < image.getWidth()-scaleX*2-1 ; x+=scaleX) {
                fillValues(valRGB, 0, image.getRGB(x-scaleX, y-scaleY));
                fillValues(valRGB, 1, image.getRGB(x-scaleX, y+scaleY));
                fillValues(valRGB, 2, image.getRGB(x+scaleX, y-scaleY));
                fillValues(valRGB, 3, image.getRGB(x+scaleX, y+scaleY));
                sum += Stats.max(new double[]{
                        Stats.standardDeviation(valRGB[0]),
                        Stats.standardDeviation(valRGB[1]),
                        Stats.standardDeviation(valRGB[2])});
                samples++;
            }
        }
        return sum / samples;
    }

    // Attempts to classify candidates according to the overall hypothesis of 2x2 up-scaled super pixels
    private List<Candidate> getBestPermutation(List<Candidate> candidates) {
        List<Candidate> best = null;
        double bestScore = Double.MAX_VALUE;
        for (List<Candidate> permutation : permute(candidates)) {
            double modelScore = score(permutation);
            if (modelScore < bestScore) {
                bestScore = modelScore;
                best = permutation;
            }
        }
        return best;
    }

    // Check for conformance to the model, where the first candidate is assumed to be hypothesis 1 etc.
    // Lower scores are better
    //
    // | Hypothesis | Horizontal  | Vertical |
    // |------------|-------------|-------|
    // |          1 | Low         | Low   |
    // |          2 | High        | Low   |
    // |          3 | Low         | High  |
    // |          4 | High        | High  |
    private double score(List<Candidate> candidates) {
        double score = 0;

        // TOO simple as low contrast images also triggers this.
        // Can we compare with overall contrast for the image?
        score += Stats.standardDeviation(new double[]{
                candidates.get(0).getCenter(), candidates.get(1).getCenter(),
                candidates.get(2).getCenter(), candidates.get(3).getCenter()
        });

        /*
        // The averaged grouped deviations should be about the same
        score += Stats.standardDeviation(new double[]{
                candidates.get(0).getHorizontal(), candidates.get(0).getVertical(),
                candidates.get(1).getVertical(),
                candidates.get(2).getHorizontal()
        });

        // The order of ABCD-numbers is significant
        score *= candidates.get(0).getHorizontal() > candidates.get(1).getHorizontal() ||
                 candidates.get(0).getHorizontal() > candidates.get(2).getVertical() ?
                10 : 1;
        score *= candidates.get(1).getVertical() > candidates.get(3).getHorizontal() ||
                 candidates.get(1).getVertical() > candidates.get(3).getVertical() ?
                10 : 1;
        score *= candidates.get(2).getHorizontal() > candidates.get(3).getHorizontal() ||
                 candidates.get(2).getHorizontal() > candidates.get(3).getVertical() ?
                10 : 1;
        score *= candidates.get(0).getHorizontal() > candidates.get(3).getHorizontal() ||
                 candidates.get(0).getHorizontal() > candidates.get(3).getVertical() ?
                10 : 1;

        // TODO: Use distance to influence score instead of binary checks
          */
        return score;
    }


    private static class Candidate {
        private final int offsetX;
        private final int offsetY;
        private final double horizontal;
        private final double vertical;
        private final double center;
        private final double overallCenterDeviation;
        private final double avgCenterLocalDeviation;

        private Candidate(int offsetX, int offsetY, double horizontal, double vertical, double center,
                          double overallCenterDeviation, double avgCenterLocalDeviation) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.center = center;
            this.overallCenterDeviation = overallCenterDeviation;
            this.avgCenterLocalDeviation = avgCenterLocalDeviation;
        }

        public double getHorizontal() {
            return horizontal;
        }

        public double getVertical() {
            return vertical;
        }

        public double getCenter() {
            return vertical;
        }

        public String toString() {
            return String.format("(%d, %d)   %9.2f  %9.2f  %9.2f  %9.2f  %9.2f\n",
                                 offsetX, offsetY, horizontal, vertical, center,
                                 avgCenterLocalDeviation, overallCenterDeviation);
        }
    }

    @Override
    public String getName() {
        return "Bilinear upscale detector";
    }
}
