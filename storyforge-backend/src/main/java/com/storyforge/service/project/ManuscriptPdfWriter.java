package com.storyforge.service.project;

import com.storyforge.service.project.ManuscriptBlocks.Block;
import com.storyforge.service.project.ManuscriptBlocks.Span;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 한글이 깨지지 않도록 나눔고딕(OFL, resources/fonts)을 PDF에 내장합니다.
 * 기울임 글꼴은 없어서 기울임은 일반체로 나옵니다.
 */
@Component
class ManuscriptPdfWriter {

    private final BaseFont regular = font("fonts/NanumGothic-Regular.ttf");
    private final BaseFont bold = font("fonts/NanumGothic-Bold.ttf");

    private static BaseFont font(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] write(List<Block> blocks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 64, 64, 72, 72);
        PdfWriter.getInstance(doc, out);
        doc.open();
        for (Block block : blocks) {
            if (block.kind() == ManuscriptBlocks.Kind.CHAPTER_HEADING) {
                doc.newPage();
            }
            doc.add(paragraph(block));
        }
        doc.close();
        return out.toByteArray();
    }

    private Paragraph paragraph(Block block) {
        Paragraph p = new Paragraph();
        float size = 11;
        boolean forceBold = false;
        p.setSpacingAfter(8);
        p.setLeading(0, 1.6f);
        switch (block.kind()) {
            case TITLE -> {
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingAfter(24);
                size = 24;
                forceBold = true;
            }
            case CHAPTER_HEADING -> {
                p.setSpacingAfter(16);
                size = 16;
                forceBold = true;
            }
            case HEADING -> {
                size = 13;
                forceBold = true;
            }
            case QUOTE -> p.setIndentationLeft(24);
            case BULLET -> {
                p.setIndentationLeft(18);
                p.add(new Chunk("• ", new Font(regular, size)));
            }
            case PARAGRAPH -> {
            }
        }
        for (Span span : block.spans()) {
            p.add(new Chunk(span.text(), new Font(forceBold || span.bold() ? bold : regular, size)));
        }
        return p;
    }
}
