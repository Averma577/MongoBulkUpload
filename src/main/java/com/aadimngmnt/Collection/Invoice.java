package com.aadimngmnt.Collection;





import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "invoice")
public class Invoice {

    @Id
    private String id;
    private String uploadCode;
    private String entryCode;
    private String entryType;
    private String ledgerCode;
    private String clientCode;
    private String clientName;

    private LocalDate date; // Consider using LocalDate if it's a proper date
    private String securityCode;

    private Double amount;
    private Double cgst;
    private Double sgst;
    private Double igst;
    private Double total;

    private String ledgerName;
    private String dc; // “D/C” converted to “dc”
    private String narration;
    private String recordNumber;

}
