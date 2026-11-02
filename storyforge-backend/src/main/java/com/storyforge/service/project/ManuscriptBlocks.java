package com.storyforge.service.project;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 챕터 본문(Markdown)을 DOCX·PDF가 함께 쓰는 단순한 블록 구조로 바꿉니다.
 */
final class ManuscriptBlocks {

    enum Kind { TITLE, CHAPTER_HEADING, HEADING, PARAGRAPH, QUOTE, BULLET }

    record Span(String text, boolean bold, boolean italic) {
    }

    record Block(Kind kind, List<Span> spans) {
    }

    private static final Parser PARSER = Parser.builder().build();

    private ManuscriptBlocks() {
    }

    static Block plain(Kind kind, String text) {
        return new Block(kind, List.of(new Span(text, false, false)));
    }

    static List<Block> parse(String markdown) {
        List<Block> out = new ArrayList<>();
        children(PARSER.parse(markdown), Kind.PARAGRAPH, out);
        return out;
    }

    private static void children(Node parent, Kind kind, List<Block> out) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            block(n, kind, out);
        }
    }

    private static void block(Node node, Kind kind, List<Block> out) {
        if (node instanceof Heading) {
            out.add(new Block(Kind.HEADING, spans(node)));
        } else if (node instanceof Paragraph) {
            out.add(new Block(kind, spans(node)));
        } else if (node instanceof BlockQuote) {
            children(node, Kind.QUOTE, out);
        } else if (node instanceof ListBlock || node instanceof ListItem) {
            children(node, Kind.BULLET, out);
        } else if (node instanceof FencedCodeBlock code) {
            out.add(plain(kind, code.getLiteral().strip()));
        } else if (node instanceof IndentedCodeBlock code) {
            out.add(plain(kind, code.getLiteral().strip()));
        }
    }

    private static List<Span> spans(Node block) {
        List<Span> out = new ArrayList<>();
        inline(block, false, false, out);
        return out;
    }

    private static void inline(Node parent, boolean bold, boolean italic, List<Span> out) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Text text) {
                out.add(new Span(text.getLiteral(), bold, italic));
            } else if (n instanceof Code code) {
                out.add(new Span(code.getLiteral(), bold, italic));
            } else if (n instanceof SoftLineBreak) {
                out.add(new Span(" ", bold, italic));
            } else if (n instanceof HardLineBreak) {
                out.add(new Span("\n", bold, italic));
            } else if (n instanceof StrongEmphasis) {
                inline(n, true, italic, out);
            } else if (n instanceof Emphasis) {
                inline(n, bold, true, out);
            } else {
                inline(n, bold, italic, out);
            }
        }
    }
}
