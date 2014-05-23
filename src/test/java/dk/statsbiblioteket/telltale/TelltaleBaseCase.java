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

import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public abstract class TelltaleBaseCase extends TestCase {

    public final File TEXT = getFile("raw/text_300dpi.png");
    public final File RASTER = getFile("raw/raster_300dpi.png");
    public final File HVIDE1 = getFile("raw/hvide_19340402_5.png");
    public final File HVIDE2 = getFile("raw/hvide_19681224_1.png");
    public final File DEFF = getFile("raw/deff_8772880279_184.png");

    public final File CAR_RAW = getFile("gimp/car_raw.png");
    public final File CAR_NEAR = getFile("gimp/car_near.png");
    public final File CAR_LINEAR = getFile("gimp/car_linear_gimp.png");
    public final File CAR_BICUBIC = getFile("gimp/car_bicubic_gimp.png");
    public final File CAR_LANCZOS = getFile("gimp/car_lanczos3_gimp.png");
    public final File CAR_LOW_CONTRAST = getFile("gimp/car_lowcontrast.png");

    public final File[] ALL = new File[]{TEXT, RASTER, HVIDE1, HVIDE2, DEFF,
            CAR_RAW, CAR_NEAR, CAR_LINEAR, CAR_BICUBIC, CAR_LANCZOS, CAR_LOW_CONTRAST};

    public TelltaleBaseCase(String testName) {
        super(testName);
    }

    protected abstract ImageAnalyzer getAnalyzer();

    // *********************************************************************************************************

    // No upscaling
    public void testReference() throws IOException {
        assertTrue("The file '" + TEXT + "' should be readable", TEXT.canRead());
        write(TEXT);
    }

    public void testAll() throws IOException {
        for (File image: ALL) {
            write(image);
        }
    }

    public void testScaled() throws IOException {
        testScales(TEXT, getAnalyzer());
    }

    // *********************************************************************************************************

    protected void write(File image) throws IOException {
        System.out.println(getAnalyzer().analyze(image, ImageIO.read(image)));
    }

    protected void scale(File in, File out, double factor, int transformOp) throws IOException {
        BufferedImage before = ImageIO.read(in);
        // http://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
        int w = before.getWidth();
        int h = before.getHeight();
        BufferedImage after = new BufferedImage((int) (w*factor), (int) (h*factor), BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(factor, factor);
        AffineTransformOp scaleOp = new AffineTransformOp(at, transformOp);
        after = scaleOp.filter(before, after);
        ImageIO.write(after, "png", out);
        System.out.println("Bi-linear scaled " + in + " x " + factor + " to " + out);
    }

    protected void testScales(File image, ImageAnalyzer analyzer) throws IOException {
        final File SCALED_NO = new File(System.getProperty("java.io.tmpdir"), image.getName() + ".scaled_not.png");
        final File SCALED_NE = new File(System.getProperty("java.io.tmpdir"), image.getName() + ".scaled_near.png");
        final File SCALED_BL = new File(System.getProperty("java.io.tmpdir"), image.getName() + ".scaled_bilinear.png");
        final File SCALED_BC = new File(System.getProperty("java.io.tmpdir"), image.getName() + ".scaled_bicubic.png");

        scale(TEXT, SCALED_NO, 1.0, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scale(TEXT, SCALED_NE, 2.0, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scale(TEXT, SCALED_BL, 2.0, AffineTransformOp.TYPE_BILINEAR);
        scale(TEXT, SCALED_BC, 2.0, AffineTransformOp.TYPE_BICUBIC);

        System.out.println(analyzer.analyze(SCALED_NO, ImageIO.read(SCALED_NO)));
        System.out.println(analyzer.analyze(SCALED_NE, ImageIO.read(SCALED_NE)));
        System.out.println(analyzer.analyze(SCALED_BL, ImageIO.read(SCALED_BL)));
        System.out.println(analyzer.analyze(SCALED_BC, ImageIO.read(SCALED_BC)));
    }

    @SuppressWarnings("ConstantConditions")
    public static File getFile(String path) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            throw new RuntimeException("Unable to locate resource '" + path + "'");
        }
        return new File(url.getFile());
    }
    
}
