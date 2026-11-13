package com.ragdocqa.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {
    
    public List<String> extractPages(Path pdfPath) throws IOException {  

        try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())){  
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = pdf.getNumberOfPages();
            List<String> pages = new ArrayList<>(pageCount);

            for(int page = 1; page <= pageCount; page++){  
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(pdf).strip());
            }
            return pages;
        }
        

    }

}
