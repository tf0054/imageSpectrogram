package imageSpectrogram;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Main {
    //This is just an example - you would want to handle LineUnavailable properly...
    final static int SAMPLING_RATE = 44100;
    final static int BIT_SAMPLES = 16;

    final static double SIN_LENGTH_SEC = 0.2;
    //
    final static double TWO_PI = Math.PI*2;

    public static void main(String[] args) throws InterruptedException, LineUnavailableException {
        try {
            BufferedImage image = rotate(ImageIO.read(Main.class.getResource("takeshi2.png")));
            final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            final int srcW = image.getWidth();
            final int srcH = image.getHeight();
            final int pixelLength = 4;

            // Create a wav file with the name specified as the first argument
            WavFile wavFile = WavFile.newWavFile(new File(args[0]), 1,
                    (long)(srcW/SIN_LENGTH_SEC*SAMPLING_RATE), BIT_SAMPLES, SAMPLING_RATE);

            HashMap<Integer, Double> t = new HashMap<Integer, Double>();
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int r = (((int) pixels[pixel + 1]) >> 16) & 0xff;
                int g = (((int) pixels[pixel + 2]) >> 8) & 0xff;
                int b = ((int) pixels[pixel + 3]) & 0xff;
;
                if( r > 10 || g > 10 && b > 10 ) {

                    int k = (int)(22000 - (row + 1.0) / (srcH + 1) * 22000);
                    double c = 4.25 - 4.25 * (r + g + b) / (256 * 3);
                    t.put(k, c);
                }

                row++;
                if (row == srcH) {
                    row = 0;
                    add_sine(wavFile, SIN_LENGTH_SEC, t);
                    t.clear();
                    col++;
                }
            }
            wavFile.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // needed because ImageIO provides seq access for pixcels with x axsis
    private static BufferedImage rotate(BufferedImage buffer) {
        AffineTransform at = AffineTransform.getScaleInstance(1, -1);

        at.translate(0, -buffer.getHeight(null));
        at.rotate(Math.toRadians(-90),buffer.getWidth() / 2, buffer.getHeight() / 2);

        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(buffer, null);
    }

    private static void add_sine(WavFile waveFile, double length, HashMap<Integer, Double> freqs) throws InterruptedException, IOException, WavFileException {
        double max_no = Math.pow(2, BIT_SAMPLES) / 2;
        length *= SAMPLING_RATE;
        int[] buf = new int[(int)length];

        for (int pos=0; pos <length;pos++) {
            double val = 0;
            double time = 0;

            Map.Entry<Integer, Double> freq = null;
            Iterator<Map.Entry<Integer, Double>> entries = freqs.entrySet().iterator();
            while(entries.hasNext()) {
                freq = entries.next();
                time = (pos / (double)SAMPLING_RATE) * freq.getKey();
                val += Math.sin(TWO_PI * time) * 10 / (Math.pow(10, freq.getValue()));
            }
            val /= freqs.size() * 10 + 1;
            buf[pos] = (int)(val * max_no);
        }
        waveFile.writeFrames(buf, (int)length);
    }
}