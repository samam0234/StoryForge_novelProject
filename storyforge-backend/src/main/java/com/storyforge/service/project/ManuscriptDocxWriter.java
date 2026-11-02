package com.storyforge.service.project;

import com.storyforge.service.project.ManuscriptBlocks.Block;
import com.storyforge.service.project.ManuscriptBlocks.Span;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
class ManuscriptDocxWriter {

    private static final String FONT = "Malgun Gothic";

    byte[] write(List<Block> blocks) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Block block : blocks) {
                paragraph(doc, block);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void paragraph(XWPFDocument doc, Block block) {
        XWPFParagraph p = doc.createParagraph();
        int size = 11;
        boolean forceBold = false;
        p.setSpacingAfter(160);
        switch (block.kind()) {
            case TITLE -> {
                p.setAlignment(ParagraphAlignment.CENTER);
                p.setSpacingAfter(480);
                size = 24;
                forceBold = true;
            }
            case CHAPTER_HEADING -> {
                p.setPageBreak(true);
                p.setSpacingAfter(320);
                size = 16;
                forceBold = true;
            }
            case HEADING -> {
                size = 13;
                forceBold = true;
            }
            case QUOTE -> p.setIndentationLeft(480);
            case BULLET -> {
                p.setIndentationLeft(360);
                run(p, "• ", size, false, false);
            }
            case PARAGRAPH -> {
            }
        }
        for (Span span : block.spans()) {
            run(p, span.text(), size, forceBold || span.bold(), span.italic());
        }
    }

    private static void run(XWPFParagraph p, String text, int size, boolean bold, boolean italic) {
        XWPFRun run = p.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setItalic(italic);
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i], i);
        }
    }
}
