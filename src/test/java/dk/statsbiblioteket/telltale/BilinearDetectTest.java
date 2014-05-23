package dk.statsbiblioteket.telltale;

import junit.framework.Test;
import junit.framework.TestSuite;

import javax.imageio.ImageIO;
import java.awt.image.AffineTransformOp;
import java.io.File;
import java.io.IOException;

public class BilinearDetectTest extends TelltaleBaseCase {

    public BilinearDetectTest(String testName) {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(BilinearDetectTest.class);
    }

    @Override
    protected ImageAnalyzer getAnalyzer() {
        return new BilinearDetect();
    }

    public void testBilinearScaled() throws IOException {
        final File SCALED_BL = new File(System.getProperty("java.io.tmpdir"), TEXT.getName() + ".scaled_bilinear.png");
        scale(TEXT, SCALED_BL, 2.0, AffineTransformOp.TYPE_BILINEAR);

        System.out.println(getAnalyzer().analyze(SCALED_BL, ImageIO.read(SCALED_BL)));
    }

    public void testBilinearCar() throws IOException {
        write(CAR_RAW);
        write(CAR_NEAR);
        write(CAR_LINEAR);
        write(CAR_BICUBIC);
        write(CAR_LANCZOS);
        write(CAR_LOW_CONTRAST);
    }


}
