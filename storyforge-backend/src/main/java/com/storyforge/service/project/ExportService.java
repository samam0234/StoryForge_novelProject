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
 * 승인된 챕터를 원고 파일로 내보냅니다 (기획서 §6.4: Markdown, DOCX, PDF).
 * 검토 중인 초안은 포함하지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    public record ExportFile(String filename, String contentType, byte[] bytes) {
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
        String name = safeFilename(project.getTitle());
        return switch (format) {
            case "md" -> new ExportFile(name + ".md", "text/markdown; charset=UTF-8",
                    markdown(project, confirmed).getBytes(StandardCharsets.UTF_8));
            case "docx" -> new ExportFile(name + ".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    docxWriter.write(blocks(project, confirmed)));
            case "pdf" -> new ExportFile(name + ".pdf", "application/pdf", pdfWriter.write(blocks(project, confirmed)));
            default -> throw new InvalidStateException("지원하지 않는 형식입니다: " + format);
        };
    }

    private static String markdown(Project project, List<Chapter> confirmed) {
        StringBuilder out = new StringBuilder("# ").append(project.getTitle()).append("\n\n");
        for (Chapter c : confirmed) {
            out.append("## ").append(heading(c)).append("\n\n");
            out.append(c.getContent() == null ? "" : c.getContent().strip()).append("\n\n");
        }
        return out.toString();
    }

    private static List<ManuscriptBlocks.Block> blocks(Project project, List<Chapter> confirmed) {
        List<ManuscriptBlocks.Block> blocks = new ArrayList<>();
        blocks.add(ManuscriptBlocks.plain(ManuscriptBlocks.Kind.TITLE, project.getTitle()));
        for (Chapter c : confirmed) {
            blocks.add(ManuscriptBlocks.plain(ManuscriptBlocks.Kind.CHAPTER_HEADING, heading(c)));
            blocks.addAll(ManuscriptBlocks.parse(c.getContent() == null ? "" : c.getContent()));
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
