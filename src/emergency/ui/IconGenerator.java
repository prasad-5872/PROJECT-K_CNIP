package emergency.ui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates and loads high-resolution tactical emergency icons in PNG and Windows ICO formats.
 */
public final class IconGenerator {
    private IconGenerator() {}

    /**
     * Renders a scalable, tactical Emergency Network icon at the given size.
     */
    public static BufferedImage createIconImage(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Theme.applyAntiAliasing(g2);

        double s = size / 256.0;

        // 1. Dark Navy Shield / Disc Background
        RoundRectangle2D.Double bg = new RoundRectangle2D.Double(12 * s, 12 * s, 232 * s, 232 * s, 48 * s, 48 * s);
        Paint grad = new GradientPaint(
                0, 0, new Color(13, 20, 36),
                size, size, new Color(6, 11, 20)
        );
        g2.setPaint(grad);
        g2.fill(bg);

        // Subtle inner glow border
        g2.setColor(Theme.BLUE_ACCENT);
        g2.setStroke(new BasicStroke((float) (4.0 * s)));
        g2.draw(bg);

        // 2. Concentric Radar Rings
        g2.setStroke(new BasicStroke((float) (2.5 * s)));
        g2.setColor(new Color(6, 182, 212, 120));
        g2.drawOval((int) (40 * s), (int) (40 * s), (int) (176 * s), (int) (176 * s));

        g2.setColor(new Color(16, 185, 129, 140));
        g2.drawOval((int) (68 * s), (int) (68 * s), (int) (120 * s), (int) (120 * s));

        // 3. Radar Beam / Pulse Line
        g2.setColor(new Color(56, 189, 248, 160));
        g2.setStroke(new BasicStroke((float) (2.0 * s)));
        g2.drawLine((int) (128 * s), (int) (128 * s), (int) (204 * s), (int) (60 * s));

        // 4. Central Emergency Signal Cross
        int cx = (int) (128 * s);
        int cy = (int) (128 * s);
        int crossW = (int) (14 * s);
        int crossL = (int) (38 * s);

        g2.setColor(Theme.GREEN_OPERATIONAL);
        // Vertical arm
        g2.fillRoundRect(cx - crossW / 2, cy - crossL / 2, crossW, crossL, (int) (4 * s), (int) (4 * s));
        // Horizontal arm
        g2.fillRoundRect(cx - crossL / 2, cy - crossW / 2, crossL, crossW, (int) (4 * s), (int) (4 * s));

        // Cross border glow
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke((float) (1.2 * s)));
        g2.drawRoundRect(cx - crossW / 2, cy - crossL / 2, crossW, crossL, (int) (4 * s), (int) (4 * s));
        g2.drawRoundRect(cx - crossL / 2, cy - crossW / 2, crossL, crossW, (int) (4 * s), (int) (4 * s));

        // 5. Tactical Center Beacon Dot
        g2.setColor(Theme.CYAN_ACCENT);
        int beaconR = Math.max(3, (int) (6 * s));
        g2.fillOval(cx - beaconR / 2, cy - beaconR / 2, beaconR, beaconR);

        g2.dispose();
        return img;
    }

    /**
     * Writes multi-resolution Windows ICO file containing PNG streams (Vista+ standard).
     */
    public static void writeIcoFile(List<BufferedImage> images, File outputFile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // ICO Header
        dos.writeShort(Short.reverseBytes((short) 0)); // Reserved
        dos.writeShort(Short.reverseBytes((short) 1)); // Type 1 = ICO
        dos.writeShort(Short.reverseBytes((short) images.size())); // Count

        List<byte[]> pngBytesList = new ArrayList<>();
        for (BufferedImage img : images) {
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", pngStream);
            pngBytesList.add(pngStream.toByteArray());
        }

        int offset = 6 + (16 * images.size());

        for (int i = 0; i < images.size(); i++) {
            BufferedImage img = images.get(i);
            byte[] pngData = pngBytesList.get(i);

            int w = img.getWidth() >= 256 ? 0 : img.getWidth();
            int h = img.getHeight() >= 256 ? 0 : img.getHeight();

            dos.writeByte(w);
            dos.writeByte(h);
            dos.writeByte(0); // Colors
            dos.writeByte(0); // Reserved
            dos.writeShort(Short.reverseBytes((short) 1)); // Color planes
            dos.writeShort(Short.reverseBytes((short) 32)); // Bits per pixel
            dos.writeInt(Integer.reverseBytes(pngData.length)); // Size of image data
            dos.writeInt(Integer.reverseBytes(offset)); // Offset

            offset += pngData.length;
        }

        for (byte[] pngData : pngBytesList) {
            dos.write(pngData);
        }

        dos.flush();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(baos.toByteArray());
        }
    }

    /**
     * Generates resources/icon.png and resources/icon.ico.
     */
    public static void generateResources(File resourcesDir) {
        if (!resourcesDir.exists()) resourcesDir.mkdirs();
        try {
            BufferedImage icon256 = createIconImage(256);
            ImageIO.write(icon256, "png", new File(resourcesDir, "icon.png"));

            List<BufferedImage> icoSizes = List.of(
                    createIconImage(16),
                    createIconImage(32),
                    createIconImage(48),
                    createIconImage(64),
                    createIconImage(128),
                    icon256
            );
            writeIcoFile(icoSizes, new File(resourcesDir, "icon.ico"));
            System.out.println("Generated icon.png and icon.ico in: " + resourcesDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to generate icons: " + e.getMessage());
        }
    }

    public static Image getAppIcon() {
        File f = new File("resources/icon.png");
        if (f.exists()) {
            try {
                return ImageIO.read(f);
            } catch (Exception ignored) {}
        }
        return createIconImage(128);
    }

    public static void main(String[] args) {
        File dir = new File(args.length > 0 ? args[0] : "resources");
        generateResources(dir);
    }
}
