package com.aadimngmnt.controller;
import com.aadimngmnt.Collection.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class InvoiceCon {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceCon.class);

    @Value("classpath:invoice_data.csv")
    private Resource csvFile;

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/mongo")
    @Transactional
    public String direct() {
        try {
            int processedCount = commitBulkUpdate();
            if (processedCount > 0) {
                return String.format("Successfully processed %d records into MongoDB", processedCount);
            }
            return "No data was processed (file might be empty or invalid)";
        } catch (Exception e) {
            logger.error("Error processing data", e);
            return "Error processing data: " + e.getMessage();
        }
    }

    private int commitBulkUpdate() throws IOException, CsvException {
        List<Invoice> invoices = new ArrayList<>();

        // 1. Read CSV file
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(csvFile.getInputStream()))) {
            csvReader.readNext(); // Skip header row

            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                try {
                    if (nextLine.length >= 17) {
                        Invoice invoice = mapToInvoice(nextLine);
                        invoices.add(invoice);
                    }
                } catch (Exception e) {
                    logger.warn("Skipping invalid row: " + String.join(",", nextLine), e);
                }
            }
        }

        // 2. Insert into MongoDB if we have data
        if (!invoices.isEmpty()) {
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Invoice.class);

            invoices.forEach(invoice -> {
                // Use clientCode as unique identifier if id is not set
                String identifier = invoice.getId() != null ? invoice.getId() : invoice.getClientCode();
                Query query = new Query(Criteria.where("clientCode").is(identifier));

                Update update = new Update()
                        .set("uploadCode", invoice.getUploadCode())
                        .set("entryCode", invoice.getEntryCode())
                        .set("entryType", invoice.getEntryType())
                        .set("ledgerCode", invoice.getLedgerCode())
                        .set("clientName", invoice.getClientName())
                        .set("date", invoice.getDate())
                        .set("securityCode", invoice.getSecurityCode())
                        .set("amount", invoice.getAmount())
                        .set("cgst", invoice.getCgst())
                        .set("sgst", invoice.getSgst())
                        .set("igst", invoice.getIgst())
                        .set("total", invoice.getTotal())
                        .set("ledgerName", invoice.getLedgerName())
                        .set("dc", invoice.getDc())
                        .set("narration", invoice.getNarration())
                        .set("recordNumber", invoice.getRecordNumber());

                bulkOps.upsert(query, update);
            });

            bulkOps.execute();
            return invoices.size();
        }

        return 0;
    }

    private Invoice mapToInvoice(String[] data) {
        Invoice invoice = new Invoice();
        invoice.setUploadCode(data[0]);
        invoice.setEntryCode(data[1]);
        invoice.setEntryType(data[2]);
        invoice.setLedgerCode(data[3]);
        invoice.setClientCode(data[4]);
        invoice.setClientName(data[5]);
        invoice.setDate(LocalDate.parse(data[6])); // Ensure date format matches
        invoice.setSecurityCode(data[7]);
        invoice.setAmount(Double.parseDouble(data[8]));
        invoice.setCgst(Double.parseDouble(data[9]));
        invoice.setSgst(Double.parseDouble(data[10]));
        invoice.setIgst(Double.parseDouble(data[11]));
        invoice.setTotal(Double.parseDouble(data[12]));
        invoice.setLedgerName(data[13]);
        invoice.setDc(data[14]);
        invoice.setNarration(data[15]);
        invoice.setRecordNumber(data[16]);
        return invoice;
    }
}
