package com.storyforge.service.project;

import com.storyforge.service.project.ManuscriptBlocks.Block;
import com.storyforge.service.project.ManuscriptBlocks.Kind;
import com.storyforge.service.project.ManuscriptBlocks.Span;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 외부 라이브러리 없이 만드는 형식: TXT, HTML, RTF, EPUB, ODT.
 * 본문은 모두 블록에서 만들고 글자를 이스케이프하므로, 원고에 적힌 HTML이 그대로 실행되지 않습니다.
 */
final class ManuscriptWriters {

    private ManuscriptWriters() {
    }

    private static String text(Block block) {
        return block.spans().stream().map(s -> s.text()).collect(Collectors.joining());
    }

    // ───────────── TXT ─────────────

    static byte[] txt(List<Block> blocks) {
        StringBuilder out = new StringBuilder();
        for (Block b : blocks) {
            String line = text(b);
            switch (b.kind()) {
                case TITLE -> out.append(line).append("\r\n\r\n");
                case CHAPTER_HEADING -> out.append("\r\n\r\n").append(line).append("\r\n\r\n");
                case BULLET -> out.append("- ").append(line).append("\r\n");
                case QUOTE -> out.append("  ").append(line).append("\r\n\r\n");
                case HEADING, PARAGRAPH -> out.append(line).append("\r\n\r\n");
            }
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ───────────── HTML ─────────────

    static byte[] html(String title, String author, List<Block> blocks) {
        StringBuilder out = new StringBuilder("""
                <!doctype html>
                <html lang="ko">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>""").append(xml(title)).append("""
                </title>
                <style>
                body { max-width: 42rem; margin: 0 auto; padding: 2rem 1rem; font-family: 'Noto Serif KR', serif; line-height: 1.85; }
                h1 { text-align: center; }
                .author { text-align: center; color: #666; }
                h2 { margin-top: 3rem; }
                blockquote { margin: 0 0 1em; padding-left: 1em; border-left: 3px solid #ccc; color: #555; }
                </style>
                </head>
                <body>
                """);
        boolean inList = false;
        for (Block b : blocks) {
            if (b.kind() != Kind.BULLET && inList) {
                out.append("</ul>\n");
                inList = false;
            }
            switch (b.kind()) {
                case TITLE -> {
                    out.append("<h1>").append(htmlSpans(b)).append("</h1>\n");
                    if (author != null) {
                        out.append("<p class=\"author\">").append(xml(author)).append("</p>\n");
                    }
                }
                case CHAPTER_HEADING -> out.append("<h2>").append(htmlSpans(b)).append("</h2>\n");
                case HEADING -> out.append("<h3>").append(htmlSpans(b)).append("</h3>\n");
                case QUOTE -> out.append("<blockquote>").append(htmlSpans(b)).append("</blockquote>\n");
                case BULLET -> {
                    if (!inList) {
                        out.append("<ul>\n");
                        inList = true;
                    }
                    out.append("<li>").append(htmlSpans(b)).append("</li>\n");
                }
                case PARAGRAPH -> out.append("<p>").append(htmlSpans(b)).append("</p>\n");
            }
        }
        if (inList) {
            out.append("</ul>\n");
        }
        return out.append("</body>\n</html>\n").toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String htmlSpans(Block b) {
        StringBuilder out = new StringBuilder();
        for (Span s : b.spans()) {
            String t = xml(s.text()).replace("\n", "<br/>");
            if (s.italic()) {
                t = "<em>" + t + "</em>";
            }
            if (s.bold()) {
                t = "<strong>" + t + "</strong>";
            }
            out.append(t);
        }
        return out.toString();
    }

    /** XML 1.0에 넣을 수 없는 제어 문자를 지우고 특수 문자를 이스케이프합니다. */
    static String xml(String value) {
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(c -> {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> {
                    boolean valid = c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF)
                            || (c >= 0xE000 && c <= 0xFFFD) || c >= 0x10000;
                    if (valid) {
                        out.appendCodePoint(c);
                    }
                }
            }
        });
        return out.toString();
    }

    // ───────────── RTF ─────────────

    static byte[] rtf(List<Block> blocks) {
        StringBuilder out = new StringBuilder("{\\rtf1\\ansi\\ansicpg949\\deff0\\uc1{\\fonttbl{\\f0\\fnil\\fcharset129 Malgun Gothic;}}\\f0\\fs22\n");
        for (Block b : blocks) {
            switch (b.kind()) {
                case TITLE -> out.append("\\pard\\qc\\b\\fs48 ").append(rtfSpans(b)).append("\\b0\\fs22\\par\\par\n");
                case CHAPTER_HEADING -> out.append("\\page\\pard\\b\\fs32 ").append(rtfSpans(b)).append("\\b0\\fs22\\par\\par\n");
                case HEADING -> out.append("\\pard\\b\\fs26 ").append(rtfSpans(b)).append("\\b0\\fs22\\par\n");
                case QUOTE -> out.append("\\pard\\li480 ").append(rtfSpans(b)).append("\\par\n");
                case BULLET -> out.append("\\pard\\li360 \\u8226? ").append(rtfSpans(b)).append("\\par\n");
                case PARAGRAPH -> out.append("\\pard\\sa160 ").append(rtfSpans(b)).append("\\par\n");
            }
        }
        // RTF는 7비트 문서라 ASCII로 인코딩합니다. 다른 글자는 모두 \\uN으로 나갑니다.
        return out.append("}").toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String rtfSpans(Block b) {
        StringBuilder out = new StringBuilder();
        for (Span s : b.spans()) {
            if (s.bold()) {
                out.append("\\b ");
            }
            if (s.italic()) {
                out.append("\\i ");
            }
            for (char c : s.text().toCharArray()) {
                if (c == '\\' || c == '{' || c == '}') {
                    out.append('\\').append(c);
                } else if (c == '\n') {
                    out.append("\\line ");
                } else if (c < 0x20) {
                    continue;
                } else if (c < 0x80) {
                    out.append(c);
                } else {
                    // RTF의 \\u는 부호 있는 16비트입니다. 이모지 같은 서로게이트는 두 단위로 나갑니다.
                    out.append("\\u").append((int) (short) c).append('?');
                }
            }
            if (s.italic()) {
                out.append("\\i0 ");
            }
            if (s.bold()) {
                out.append("\\b0 ");
            }
        }
        return out.toString();
    }

    // ───────────── EPUB 3 ─────────────

    private record EpubChapter(String heading, List<Block> blocks) {
    }

    static byte[] epub(String title, String author, List<Block> blocks) {
        List<EpubChapter> chapters = new ArrayList<>();
        for (Block b : blocks) {
            if (b.kind() == Kind.CHAPTER_HEADING) {
                chapters.add(new EpubChapter(text(b), new ArrayList<>()));
            } else if (b.kind() != Kind.TITLE && !chapters.isEmpty()) {
                chapters.get(chapters.size() - 1).blocks().add(b);
            }
        }
        String id = UUID.nameUUIDFromBytes((title + "|" + author).getBytes(StandardCharsets.UTF_8)).toString();
        String modified = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

        StringBuilder manifest = new StringBuilder("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
        StringBuilder spine = new StringBuilder();
        StringBuilder toc = new StringBuilder();
        Map<String, String> files = new LinkedHashMap<>();
        files.put("META-INF/container.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                </container>
                """);
        for (int i = 0; i < chapters.size(); i++) {
            EpubChapter c = chapters.get(i);
            String name = "ch" + (i + 1) + ".xhtml";
            manifest.append("<item id=\"ch").append(i + 1).append("\" href=\"").append(name).append("\" media-type=\"application/xhtml+xml\"/>\n");
            spine.append("<itemref idref=\"ch").append(i + 1).append("\"/>\n");
            toc.append("<li><a href=\"").append(name).append("\">").append(xml(c.heading())).append("</a></li>\n");
            files.put("OEBPS/" + name, xhtml(c.heading(), "<h2>" + xml(c.heading()) + "</h2>\n" + epubBody(c.blocks())));
        }
        files.put("OEBPS/nav.xhtml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="ko" lang="ko">
                <head><title>%s</title></head>
                <body><nav epub:type="toc"><h1>목차</h1><ol>
                %s</ol></nav></body>
                </html>
                """.formatted(xml(title), toc));
        files.put("OEBPS/content.opf", """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id" xml:lang="ko">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:identifier id="book-id">urn:uuid:%s</dc:identifier>
                    <dc:title>%s</dc:title>
                    <dc:language>ko</dc:language>
                    %s
                    <meta property="dcterms:modified">%s</meta>
                  </metadata>
                  <manifest>
                %s  </manifest>
                  <spine>
                %s  </spine>
                </package>
                """.formatted(id, xml(title), author == null ? "" : "<dc:creator>" + xml(author) + "</dc:creator>",
                modified, manifest, spine));
        return zip("application/epub+zip", files);
    }

    private static String xhtml(String title, String body) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ko" lang="ko">
                <head><title>%s</title></head>
                <body>
                %s
                </body>
                </html>
                """.formatted(xml(title), body);
    }

    private static String epubBody(List<Block> blocks) {
        StringBuilder out = new StringBuilder();
        boolean inList = false;
        for (Block b : blocks) {
            if (b.kind() != Kind.BULLET && inList) {
                out.append("</ul>\n");
                inList = false;
            }
            switch (b.kind()) {
                case HEADING -> out.append("<h3>").append(htmlSpans(b)).append("</h3>\n");
                case QUOTE -> out.append("<blockquote><p>").append(htmlSpans(b)).append("</p></blockquote>\n");
                case BULLET -> {
                    if (!inList) {
                        out.append("<ul>\n");
                        inList = true;
                    }
                    out.append("<li>").append(htmlSpans(b)).append("</li>\n");
                }
                default -> out.append("<p>").append(htmlSpans(b)).append("</p>\n");
            }
        }
        if (inList) {
            out.append("</ul>\n");
        }
        return out.toString();
    }

    // ───────────── ODT ─────────────

    static byte[] odt(List<Block> blocks) {
        StringBuilder body = new StringBuilder();
        for (Block b : blocks) {
            String style = switch (b.kind()) {
                case TITLE -> "SfTitle";
                case CHAPTER_HEADING -> "SfChapter";
                case HEADING -> "SfHeading";
                case QUOTE -> "SfQuote";
                case BULLET -> "SfBullet";
                case PARAGRAPH -> "SfBody";
            };
            String prefix = b.kind() == Kind.BULLET ? "• " : "";
            body.append("<text:p text:style-name=\"").append(style).append("\">").append(prefix)
                    .append(odtSpans(b)).append("</text:p>\n");
        }
        String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                  xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                  xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                  office:version="1.3">
                <office:automatic-styles>
                  <style:style style:name="SfTitle" style:family="paragraph"><style:paragraph-properties fo:text-align="center" fo:margin-bottom="1cm"/><style:text-properties fo:font-size="24pt" fo:font-weight="bold"/></style:style>
                  <style:style style:name="SfChapter" style:family="paragraph"><style:paragraph-properties fo:break-before="page" fo:margin-bottom="0.6cm"/><style:text-properties fo:font-size="16pt" fo:font-weight="bold"/></style:style>
                  <style:style style:name="SfHeading" style:family="paragraph"><style:paragraph-properties fo:margin-top="0.4cm" fo:margin-bottom="0.2cm"/><style:text-properties fo:font-size="13pt" fo:font-weight="bold"/></style:style>
                  <style:style style:name="SfBody" style:family="paragraph"><style:paragraph-properties fo:margin-bottom="0.3cm"/></style:style>
                  <style:style style:name="SfQuote" style:family="paragraph"><style:paragraph-properties fo:margin-left="0.8cm" fo:margin-bottom="0.3cm"/></style:style>
                  <style:style style:name="SfBullet" style:family="paragraph"><style:paragraph-properties fo:margin-left="0.6cm"/></style:style>
                  <style:style style:name="SfBold" style:family="text"><style:text-properties fo:font-weight="bold"/></style:style>
                  <style:style style:name="SfItalic" style:family="text"><style:text-properties fo:font-style="italic"/></style:style>
                  <style:style style:name="SfBoldItalic" style:family="text"><style:text-properties fo:font-weight="bold" fo:font-style="italic"/></style:style>
                </office:automatic-styles>
                <office:body><office:text>
                %s</office:text></office:body>
                </office:document-content>
                """.formatted(body);
        Map<String, String> files = new LinkedHashMap<>();
        files.put("content.xml", content);
        files.put("META-INF/manifest.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.3">
                  <manifest:file-entry manifest:full-path="/" manifest:version="1.3" manifest:media-type="application/vnd.oasis.opendocument.text"/>
                  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
                </manifest:manifest>
                """);
        return zip("application/vnd.oasis.opendocument.text", files);
    }

    private static String odtSpans(Block b) {
        StringBuilder out = new StringBuilder();
        for (Span s : b.spans()) {
            String t = xml(s.text()).replace("\n", "<text:line-break/>");
            String style = s.bold() && s.italic() ? "SfBoldItalic" : s.bold() ? "SfBold" : s.italic() ? "SfItalic" : null;
            out.append(style == null ? t : "<text:span text:style-name=\"" + style + "\">" + t + "</text:span>");
        }
        return out.toString();
    }

    // ───────────── ZIP ─────────────

    /** EPUB·ODT 공통: mimetype을 압축 없이 맨 앞에 둡니다 (두 형식의 규칙). */
    private static byte[] zip(String mimetype, Map<String, String> files) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            byte[] mime = mimetype.getBytes(StandardCharsets.US_ASCII);
            ZipEntry first = new ZipEntry("mimetype");
            first.setMethod(ZipEntry.STORED);
            first.setSize(mime.length);
            first.setCompressedSize(mime.length);
            CRC32 crc = new CRC32();
            crc.update(mime);
            first.setCrc(crc.getValue());
            zip.putNextEntry(first);
            zip.write(mime);
            zip.closeEntry();
            for (Map.Entry<String, String> f : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(f.getKey()));
                zip.write(f.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
