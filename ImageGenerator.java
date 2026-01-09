import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageGenerator {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java ImageGenerator <outputPath> <width> <height>");
            return;
        }

        String outputPath = args[0];
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill with a dark grey color, similar to Minecraft's UI
        g2d.setColor(new Color(50, 50, 50, 255));
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs(); 
        ImageIO.write(image, "png", outputFile);

        System.out.println("Image created at: " + outputPath);
    }
}
