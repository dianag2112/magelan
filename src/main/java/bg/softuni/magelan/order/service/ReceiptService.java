package bg.softuni.magelan.order.service;

import bg.softuni.magelan.order.model.Order;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final DateTimeFormatter RECEIPT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateReceiptPdf(Order order) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD);
            Paragraph title = new Paragraph("Magelan Payment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String formattedCreatedOn = order.getCreatedOn().format(RECEIPT_TIME_FORMATTER);

            document.add(new Paragraph("\nOrder ID: " + order.getId()));
            document.add(new Paragraph("Customer: " + order.getCustomer().getUsername()));
            document.add(new Paragraph("Status: " + order.getOrderStatus()));
            document.add(new Paragraph("Created: " + formattedCreatedOn));
            document.add(new Paragraph("\nItems:"));
            document.add(new Paragraph("----------------------------------"));

            order.getItems().forEach(item -> {
                String line = "- " + item.getProduct().getName()
                        + " x " + item.getQuantity()
                        + " = " + item.getUnitPrice().multiply(
                        java.math.BigDecimal.valueOf(item.getQuantity())
                );
                document.add(new Paragraph(line));
            });

            document.add(new Paragraph("----------------------------------"));
            document.add(new Paragraph("Total amount: " + order.getAmount()));
            document.add(new Paragraph("\nThank you for your order, Captain! ☠️"));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Could not generate PDF", e);
        }
    }
}
