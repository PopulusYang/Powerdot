import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;

public class SimplePdfExporter {

    public static void export(Slide slide, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            writePdf(slide, fos);
        }
    }

    private static void writePdf(Slide slide, FileOutputStream os) throws IOException {
        List<Long> xrefs = new ArrayList<>();
        long byteCount = 0;

        // Header
        String header = "%PDF-1.4\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
        os.write(headerBytes);
        byteCount += headerBytes.length;

        int totalPages = slide.getTotalPages();
        int nextObjId = 3;

        List<Integer> pageObjIds = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageObjIds.add(nextObjId);
            nextObjId += 3; // Page, Content, Image
        }

        // Obj 1: Catalog
        xrefs.add(byteCount);
        String catalog = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        byte[] catalogBytes = catalog.getBytes(StandardCharsets.US_ASCII);
        os.write(catalogBytes);
        byteCount += catalogBytes.length;

        // Obj 2: Pages
        xrefs.add(byteCount);
        StringBuilder kids = new StringBuilder("[");
        for (Integer id : pageObjIds) {
            kids.append(id).append(" 0 R ");
        }
        kids.append("]");

        int width = slide.getWidth();
        int height = slide.getHeight();

        String pages = "2 0 obj\n<< /Type /Pages /Kids " + kids.toString() + " /Count " + totalPages
                + " /MediaBox [0 0 " + width + " " + height + "] >>\nendobj\n";
        byte[] pagesBytes = pages.getBytes(StandardCharsets.US_ASCII);
        os.write(pagesBytes);
        byteCount += pagesBytes.length;

        // Write Pages
        for (int i = 0; i < totalPages; i++) {
            SlidePage page = slide.getAllPages().get(i);
            int pageObjId = pageObjIds.get(i);
            int contentObjId = pageObjId + 1;
            int imageObjId = pageObjId + 2;

            // Render page to image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // Anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw background
            page.renderBackground(g2d, width, height);
            // Draw elements
            for (SlideElement element : page.getElements()) {
                element.draw(g2d);
            }
            g2d.dispose();

            // Convert to JPEG
            ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", imgBaos);
            byte[] imgData = imgBaos.toByteArray();

            // Obj 3+3i: Page
            xrefs.add(byteCount);
            String pageStr = pageObjId + " 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /XObject << /Img" + i + " "
                    + imageObjId + " 0 R >> >> /Contents " + contentObjId + " 0 R >>\nendobj\n";
            byte[] pageBytes = pageStr.getBytes(StandardCharsets.US_ASCII);
            os.write(pageBytes);
            byteCount += pageBytes.length;

            // Obj 3+3i+1: Content
            String streamContent = "q " + width + " 0 0 " + height + " 0 0 cm /Img" + i + " Do Q";
            byte[] streamBytes = streamContent.getBytes(StandardCharsets.US_ASCII);

            xrefs.add(byteCount);
            String contentObjStr = contentObjId + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
            byte[] contentHeaderBytes = contentObjStr.getBytes(StandardCharsets.US_ASCII);
            os.write(contentHeaderBytes);
            os.write(streamBytes);
            byte[] contentFooterBytes = "\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII);
            os.write(contentFooterBytes);
            byteCount += contentHeaderBytes.length + streamBytes.length + contentFooterBytes.length;

            // Obj 3+3i+2: Image XObject
            xrefs.add(byteCount);
            String imgObjHeader = imageObjId + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + width + " /Height "
                    + height + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                    + imgData.length + " >>\nstream\n";
            byte[] imgHeaderBytes = imgObjHeader.getBytes(StandardCharsets.US_ASCII);
            os.write(imgHeaderBytes);
            os.write(imgData);
            byte[] imgFooterBytes = "\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII);
            os.write(imgFooterBytes);
            byteCount += imgHeaderBytes.length + imgData.length + imgFooterBytes.length;
        }

        // Xref
        long xrefOffset = byteCount;
        os.write("xref\n".getBytes(StandardCharsets.US_ASCII));
        os.write(("0 " + nextObjId + "\n").getBytes(StandardCharsets.US_ASCII));
        os.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (Long offset : xrefs) {
            String offsetStr = String.format("%010d", offset);
            os.write((offsetStr + " 00000 n \n").getBytes(StandardCharsets.US_ASCII));
        }

        // Trailer
        String trailer = "trailer\n<< /Size " + nextObjId + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n";
        os.write(trailer.getBytes(StandardCharsets.US_ASCII));
    }
}
