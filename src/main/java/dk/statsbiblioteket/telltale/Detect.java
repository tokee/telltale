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


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for running detection.
 */
public class Detect {
    private final List<ImageAnalyzer> analyzers = new ArrayList<>();

    public static final String USAGE =
            "Attempts to determine if an image has been upscaled.\n"
            + "\n"
            + "Usage:\n"
            + "Detect image*\n"
            + "\n"
            +"Sample\n"
            + "Detect myimage.jpg myotherimage.tif";


    public Detect() {
        analyzers.add(new NearestNeighbourDetect());
        analyzers.add(new BilinearDetect());
    }

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

        new Detect().analyze(images);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void analyze(List<File> images) {
        for (File image: images) {
            try  {
                BufferedImage bImage = ImageIO.read(image);
                for (ImageAnalyzer analyzer: analyzers) {
                    try {
                        System.out.println(analyzer.analyze(image, bImage));
                    } catch (Exception e) {
                        System.err.println("Exception analyzing '" + image + "' with " + analyzer.getName());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Unable to open '" + image + "' as an image");
                e.printStackTrace();
            }
        }
    }
}
