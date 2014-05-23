package dk.statsbiblioteket.telltale;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Open an image and attempts to detect whether is has been resized (or more specifically upscaled)
 * using Nearest-Neighbour (simple duplication of pixels). The detection looks for x2 upscaling and
 * is resilient against noise from sharpening, JPEG compression etc.
 * </p><p>
 * The detection is extremely reliable if no post-processing has been applied (the trivial case) and
 * quite reliable even with substantial sub-processing.
 * </p><p>
 * For x2 detection, the spread of the values of 2x2-blocks of pixels are collected and the spread of
 * the collected values are calculated. This is done for all offsets: (0, 0), (0, 1), (1, 0) and (1, 1).
 * If one of the spreads are
 */
// TODO: x1½
public class NearestNeighbourDetect extends ImageAnalyzerImpl {

    @Override
    public AnalysisResult analyze(File imageFile, BufferedImage image) {
        final int CONTRAST_SIDE = 3; // Something small that is not likely to match a scale factor
        final int RECT_WIDTH = 2;
        final int RECT_HEIGHT = 2;
        final long[][] rectRGB = new long[3][RECT_WIDTH*RECT_HEIGHT];
        final int a = 0;
        final int b = 1;
        final int c = 2;
        final int d = 3;

        StringBuilder sb = new StringBuilder();

        double averageContrast = averageContrast(image, CONTRAST_SIDE);
        sb.append(String.format("Overall image contrast (average %dx%d block deviation): %.2f\n",
                                CONTRAST_SIDE, CONTRAST_SIDE, averageContrast));
        sb.append("2x2 blocks\n");
        sb.append("Offset   ABCD  AB_CD  AC_BD\n");
        List<Candidate> candidates = new ArrayList<>(RECT_WIDTH*RECT_HEIGHT);
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
                candidates.add(new Candidate(
                        offsetX, offsetY, sumMaxDeviationABCD / blockCount, sumMaxDeviationAB_CD / blockCount,
                        sumMaxDeviationAC_BD / blockCount));


                /*sb.append(String.format("(%d, %d)  %5.2f  %5.2f  %5.2f\n", offsetX, offsetY,
                                        sumMaxDeviationABCD / blockCount, sumMaxDeviationAB_CD / blockCount,
                                        sumMaxDeviationAC_BD / blockCount));*/
/*                deviations[deviationIndex] =   sumMaxDeviationABCD / blockCount;
                deviations[deviationIndex+1] = sumMaxDeviationAB_CD / blockCount;
                deviations[deviationIndex+2] = sumMaxDeviationAC_BD / blockCount;
                deviationIndex += 3;*/
            }
        }
        List<Candidate> sortedCandidates = getBestPermutation(candidates, averageContrast, sb);
        for (Candidate candidate: sortedCandidates) {
            sb.append(candidate);
        }
        sb.append(String.format("Upscale model conformance score (lower is better): %.2f\n",
                                score(sortedCandidates, averageContrast)));
        return new AnalysisResult(imageFile, getName(), sb.toString());
    }

    // Attempts to classify candidates according to the overall hypothesis of 2x2 up-scaled super pixels
    private List<Candidate> getBestPermutation(List<Candidate> candidates, double averageContrast, StringBuilder sb) {
        List<Candidate> best = null;
        double bestScore = Double.MAX_VALUE;
        for (List<Candidate> permutation : permute(candidates)) {
            double modelScore = score(permutation, averageContrast);
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
    // | Hypothesis | ABCD        | AB_CD | AC_BD |
    // |------------|-------------|-------|-------|
    // |          1 | Low         | Low   | Low   |
    // |          2 | Medium/high | Low   | High  |
    // |          3 | Medium/high | High  | Low   |
    // |          4 | High        | High  | High  |
    private double score(List<Candidate> candidates, double averageContrast) {
        double score = 0;

        // The averaged grouped deviations should be about the same
        score += Stats.standardDeviation(new double[]{
                candidates.get(0).getAB_CD(), candidates.get(0).getAC_BD(),
                candidates.get(1).getAB_CD(),
                candidates.get(2).getAC_BD()
        });

        // The full deviation for the grouped super-pixels should be about the same
        score += Stats.standardDeviation(new double[]{
                candidates.get(1).getABCD(),
                candidates.get(2).getABCD()
        });

        // The order of ABCD-numbers is significant
        score *= candidates.get(0).getABCD() > candidates.get(1).getABCD() ||
                 candidates.get(0).getABCD() > candidates.get(2).getABCD() ?
                10 : 1;
        score *= candidates.get(1).getABCD() > candidates.get(3).getABCD() ||
                 candidates.get(2).getABCD() > candidates.get(3).getABCD() ?
                10 : 1;
        score *= candidates.get(0).getABCD() > candidates.get(3).getABCD() ?
                100 : 1;

        // TODO: Use distance between ABCD-numbers to influence score instead of binary checks

        return score;
    }

    private static class Candidate {
        private final int offsetX;
        private final int offsetY;
        private final double abcd;
        private final double ab_cd;
        private final double ac_bd;

        private Candidate(int offsetX, int offsetY, double abcd, double ab_cd, double ac_bd) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.abcd = abcd;
            this.ab_cd = ab_cd;
            this.ac_bd = ac_bd;
        }

        public double getABCD() {
            return abcd;
        }

        public double getAB_CD() {
            return ab_cd;
        }

        public double getAC_BD() {
            return ac_bd;
        }

        public String getOffsetHuman() {
            return "(" + offsetX + ", " + offsetY + ")";
        }

        public String toString() {
            return String.format("(%d, %d)  %5.2f  %5.2f  %5.2f\n",
                                 offsetX, offsetY, abcd, ab_cd, ac_bd);
        }
    }

    @Override
    public String getName() {
        return "Nearest-Neighbour upscale detector";
    }
}
