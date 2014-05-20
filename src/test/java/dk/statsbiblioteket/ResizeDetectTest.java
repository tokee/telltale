package dk.statsbiblioteket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ResizeDetectTest extends TestCase {

    final File TEXT = getFile("raw/text_300dpi.png");
    final File RASTER = getFile("raw/raster_300dpi.png");
    final File HVIDE1 = getFile("raw/hvide_19340402_5.png");
    final File HVIDE2 = getFile("raw/hvide_19681224_1.png");
    final File DEFF = getFile("raw/deff_8772880279_184.png");
    final File[] ALL = new File[]{TEXT, RASTER, HVIDE1, HVIDE2, DEFF};

    public ResizeDetectTest(String testName ) {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(ResizeDetectTest.class);
    }

    // No upscaling
    public void testReference() throws IOException {
        assertTrue("The file '" + TEXT + "' should be readable", TEXT.canRead());

        System.out.println(new ResizeDetect().analyze(TEXT));
    }

    public void testAll() throws IOException {
        for (File image: ALL) {
            System.out.println(new ResizeDetect().analyze(image));
        }
    }

    public void testScaled2() throws IOException {
        final File SCALED = new File(System.getProperty("java.io.tmpdir"), TEXT.getName() + ".scaled.png");
        scale(TEXT, SCALED, 2.0);

        System.out.println(new ResizeDetect().analyze(SCALED));
    }

    // Bi-linear scale
    private void scale(File in, File out, double factor) throws IOException {
        BufferedImage before = ImageIO.read(in);
        // http://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
        int w = before.getWidth();
        int h = before.getHeight();
        BufferedImage after = new BufferedImage((int) (w*factor), (int) (h*factor), BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(factor, factor);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        after = scaleOp.filter(before, after);
        ImageIO.write(after, "png", out);
        System.out.println("Bi-linear scaled " + in + " x " + factor + " to " + out);
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
