package imageSpectrogram;

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
    //
    final static double TWO_PI = Math.PI*2;

    public static void main(String[] args) throws InterruptedException, LineUnavailableException
    {
        //Position through the sine wave as a percentage (i.e. 0 to 1 is 0 to 2*PI)

        try {
            BufferedImage image = ImageIO.read(Main.class.getResource("takeshi2.png"));
            final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            final int srcW = image.getWidth();
            final int srcH = image.getHeight();
            final int pixelLength = 4;

            // Create a wav file with the name specified as the first argument
            WavFile wavFile = WavFile.newWavFile(new File(args[0]), 1,
                    (long)(srcW/0.2*SAMPLING_RATE), 16, SAMPLING_RATE);

            HashMap<Integer, Double> t = new HashMap<Integer, Double>();
            for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
                int r = (((int) pixels[pixel+1]) >>16) & 0xff; // blue
                int g = (((int) pixels[pixel+2]) >>8) & 0xff; // green
                int b = ((int) pixels[pixel+3]) & 0xff; // red
;
                if( r > 10 || g > 10 && b > 10 ) {

                    int k = (int)(22000 - (row + 1.0) / (srcH + 1) * 22000);
                    double c = 4.25 - 4.25 * (r + g + b) / (256 * 3);
                    t.put(k, c );
                }

                row++;
                if (row == srcH) {
                    row = 0;
                    add_sine(wavFile,0.2,(int)(0.2*SAMPLING_RATE*col),t);
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

    private static void add_sine(WavFile line, double length, int offset, HashMap<Integer, Double> freqs) throws InterruptedException, IOException, WavFileException {
        double max_no = Math.pow(2, 16) / 2;
        length *= SAMPLING_RATE;
        int[] buf = new int[(int)length];

        for (int pos=0; pos <length;pos++) {
            double val = 0;
            double time = 0;

            Iterator<Map.Entry<Integer, Double>> entries = freqs.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry<Integer, Double> freq = entries.next();
                time = (pos / (double)SAMPLING_RATE) * freq.getKey();
                val += Math.sin(TWO_PI* time)*10/(Math.pow(10, freq.getValue()));
            }
            val /= freqs.size()*10+1;
            buf[pos] = (int)(val*max_no);
        }
        line.writeFrames(buf,  (int)length);
    }
}
