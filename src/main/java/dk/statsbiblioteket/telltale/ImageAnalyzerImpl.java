/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.telltale;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public abstract class ImageAnalyzerImpl implements ImageAnalyzer {

    protected double averageContrast(BufferedImage image, int rectSide) {
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

    protected void extractChannelValues(BufferedImage image, int y, int x, int width, int height, long[][] rectRGB) {
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

    protected double getMaxDeviation(long[][] rectRGB, int... entries) {
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

    // Create all permutations of elements in the list
    protected <T> List<List<T>> permute(List<T> candidates) {
        List<List<T>> permutations = new ArrayList<>();
        if (candidates.size() == 1) {
            permutations.add(new ArrayList<T>(candidates));
        }
        for (T candidate: candidates) {
            List<T> sub = new ArrayList<>(candidates);
            sub.remove(candidate);
            for (List<T> subPermutation: permute(sub)) {
                subPermutation.add(0, candidate);
                permutations.add(subPermutation);
            }
        }
        return permutations;
    }

}
