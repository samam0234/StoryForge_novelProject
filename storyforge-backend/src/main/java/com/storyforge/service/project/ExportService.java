package com.storyforge.service.project;

import com.storyforge.domain.chapter.Chapter;
import com.storyforge.domain.chapter.ChapterRepository;
import com.storyforge.domain.chapter.ChapterStatus;
import com.storyforge.domain.project.Project;
import com.storyforge.exception.InvalidStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 승인된 챕터를 원고 파일로 내보냅니다 (기획서 §6.4: Markdown, DOCX, PDF + 추가 형식 TXT, HTML, EPUB, ODT, RTF).
 * 검토 중인 초안은 포함하지 않습니다. 게시 작품과 패키지는 render를 함께 씁니다.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    public record ExportFile(String filename, String contentType, byte[] bytes) {
    }

    public record ManuscriptChapter(String heading, String markdown) {
    }

    public record Manuscript(String title, String author, List<ManuscriptChapter> chapters) {
    }

    private final ProjectService projectService;
    private final ChapterRepository chapters;
    private final ManuscriptDocxWriter docxWriter;
    private final ManuscriptPdfWriter pdfWriter;

    public ExportService(ProjectService projectService, ChapterRepository chapters,
                         ManuscriptDocxWriter docxWriter, ManuscriptPdfWriter pdfWriter) {
        this.projectService = projectService;
        this.chapters = chapters;
        this.docxWriter = docxWriter;
        this.pdfWriter = pdfWriter;
    }

    public ExportFile export(Long projectId, String requestedFormat) {
        Project project = projectService.load(projectId);
        List<Chapter> confirmed = chapters.findByProjectIdOrderByNumber(projectId).stream()
                .filter(c -> c.getStatus() == ChapterStatus.CONFIRMED)
                .toList();
        if (confirmed.isEmpty()) {
            throw new InvalidStateException("승인된 챕터가 있어야 내보낼 수 있습니다.");
        }
        String format = requestedFormat == null || requestedFormat.isBlank() ? project.getExportFormat() : requestedFormat;
        List<ManuscriptChapter> sections = confirmed.stream()
                .map(c -> new ManuscriptChapter(heading(c), c.getContent()))
                .toList();
        return render(new Manuscript(project.getTitle(), null, sections), format);
    }

    public ExportFile render(Manuscript m, String format) {
        String name = safeFilename(m.title());
        return switch (format) {
            case "md" -> new ExportFile(name + ".md", "text/markdown; charset=UTF-8",
                    markdown(m).getBytes(StandardCharsets.UTF_8));
            case "txt" -> new ExportFile(name + ".txt", "text/plain; charset=UTF-8", ManuscriptWriters.txt(blocks(m)));
            case "docx" -> new ExportFile(name + ".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    docxWriter.write(blocks(m)));
            case "pdf" -> new ExportFile(name + ".pdf", "application/pdf", pdfWriter.write(blocks(m)));
            case "html" -> new ExportFile(name + ".html", "text/html; charset=UTF-8",
                    ManuscriptWriters.html(m.title(), m.author(), blocks(m)));
            case "epub" -> new ExportFile(name + ".epub", "application/epub+zip",
                    ManuscriptWriters.epub(m.title(), m.author(), blocks(m)));
            case "odt" -> new ExportFile(name + ".odt", "application/vnd.oasis.opendocument.text", ManuscriptWriters.odt(blocks(m)));
            case "rtf" -> new ExportFile(name + ".rtf", "application/rtf", ManuscriptWriters.rtf(blocks(m)));
            default -> throw new InvalidStateException("지원하지 않는 형식입니다: " + format);
        };
    }

    private static String markdown(Manuscript m) {
        StringBuilder out = new StringBuilder("# ").append(m.title()).append("\n\n");
        for (ManuscriptChapter c : m.chapters()) {
            out.append("## ").append(c.heading()).append("\n\n");
            out.append(c.markdown() == null ? "" : c.markdown().strip()).append("\n\n");
        }
        return out.toString();
    }

    private static List<ManuscriptBlocks.Block> blocks(Manuscript m) {
        List<ManuscriptBlocks.Block> blocks = new ArrayList<>();
        blocks.add(ManuscriptBlocks.plain(ManuscriptBlocks.Kind.TITLE, m.title()));
        for (ManuscriptChapter c : m.chapters()) {
            blocks.add(ManuscriptBlocks.plain(ManuscriptBlocks.Kind.CHAPTER_HEADING, c.heading()));
            blocks.addAll(ManuscriptBlocks.parse(c.markdown() == null ? "" : c.markdown()));
        }
        return blocks;
    }

    private static String heading(Chapter c) {
        return c.getNumber() + "장. " + (c.getTitle() == null ? "" : c.getTitle());
    }

    private static String safeFilename(String title) {
        String cleaned = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").strip();
        return cleaned.isEmpty() ? "storyforge" : cleaned;
    }
}
