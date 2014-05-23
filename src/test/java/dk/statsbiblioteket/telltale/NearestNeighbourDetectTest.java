package dk.statsbiblioteket.telltale;

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

public class NearestNeighbourDetectTest extends TelltaleBaseCase {

    public NearestNeighbourDetectTest(String testName) {
        super( testName );
    }

    public static Test suite() {
        return new TestSuite(NearestNeighbourDetectTest.class);
    }

    @Override
    protected ImageAnalyzer getAnalyzer() {
        return new NearestNeighbourDetect();
    }
}
