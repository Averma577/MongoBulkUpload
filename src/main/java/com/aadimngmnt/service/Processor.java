package com.aadimngmnt.service;

import com.aadimngmnt.Collection.Invoice;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
@Component
public class Processor implements ItemProcessor<Invoice,Invoice> {
    @Override
    public Invoice process(Invoice item) throws Exception {
        // inside u can manipulate your data
        item.setDate(LocalDate.now());
        return item;
    }
}
