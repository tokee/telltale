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
package dk.statsbiblioteket;

/**
 * Simple stats with mean, variance & deviation.
 */
@SuppressWarnings("ForLoopReplaceableByForEach") // No iterator objects, please. We would like to keep this lightweight.
public class Stats {

    /**
     * @param vals the values to process.
     * @return the standard deviation of the values.
     */
    public static double standardDeviation(long[] vals) {
        return Math.sqrt(variance(vals, 0, vals.length));
    }
    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the standard deviation of the values.
     */
    public static double standardDeviation(long[] vals, int start, int end) {
        return Math.sqrt(variance(vals, start, end));
    }

    /**
     * @param variance a previously calculated variance.
     *                 Use {@link #standardDeviation(long[], int, int)} if this is unknown.
     * @return the standard deviation {@code sqrt(variance)}.
     */
    public static double standardDeviation(double variance) {
        return Math.sqrt(variance);
    }

    /**
     * @param vals the values to process.
     * @return the variance of the values.
     */
    public static double variance(long[] vals) {
        return variance(vals, 0, vals.length, mean(vals, 0, vals.length));
    }
    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the variance of the values.
     */
    public static double variance(long[] vals, int start, int end) {
        return variance(vals, start, end, mean(vals, start, end));
    }
    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @param mean  the mean of the values. Use {@link #variance(long[], int, int)} is this is not known.
     * @return the variance of the values.
     */
    public static double variance(long[] vals, int start, int end, double mean) {
        double squareDiffSum = 0.0;
        for (int i = start ; i < end ; i++) {
            final double diff = mean - vals[i];
            squareDiffSum += diff*diff;
        }
        return squareDiffSum / (end-start-1); // TODO: Consider if n-1 (end-start-1) is the right solution
    }

    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the mean of the values.
     */
    public static double mean(long[] vals, int start, int end) {
        return 1.0 * sum(vals, start, end) / (end-start);
    }
    /**
     * @param vals the values to process.
     * @return the mean of the values.
     */
    public static double mean(long[] vals) {
        return 1.0 * sum(vals, 0, vals.length) / vals.length;
    }

    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the sum of the values.
     */
    public static long sum(long[] vals, int start, int end) {
        long sum = 0;
        for (int i = start ; i < end ; i++) {
            sum += vals[i];
        }
        return sum;
    }
    /**
     * @param vals the values to process.
     * @return the sum of the values.
     */
    public static long sum(final long[] vals) {
        long sum = 0;
        for (int i = 0 ; i < vals.length ; i++) {
            sum += vals[i];
        }
        return sum;
    }

    /**
     * @param vals  the values to process.
     * @return the maximum of the values.
     */
    public static long max(long... vals) {
        return max(vals, 0, vals.length);
    }
    /**
     * @param vals the values to process.
     * @return the maximum of the values.
     */
    public static double max(double[] vals) {
        return max(vals, 0, vals.length);
    }
    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the maximum of the values.
     */
    public static long max(long[] vals, int start, int end) {
        long max = vals[start];
        for (int i = start+1 ; i < end ; i++) {
            max = vals[i] > max ? vals[i] : max;
        }
        return max;
    }
    /**
     * @param vals  the values to process.
     * @param start process values from this point (inclusive).
     * @param end   process values up to this point (exclusive).
     * @return the maximum of the values.
     */
    public static double max(double[] vals, int start, int end) {
        double max = vals[start];
        for (int i = start+1 ; i < end ; i++) {
            max = vals[i] > max ? vals[i] : max;
        }
        return max;
    }

}
