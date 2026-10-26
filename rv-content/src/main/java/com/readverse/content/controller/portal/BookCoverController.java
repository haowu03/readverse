package com.readverse.content.controller.portal;

import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.mapper.ContentAuthorMapper;
import com.readverse.content.mapper.ContentBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Generates deterministic SVG book covers, so the demo has no external image dependency.
 */
@RestController
@RequestMapping("/api/portal/books")
@RequiredArgsConstructor
public class BookCoverController {

    private final ContentBookMapper bookMapper;
    private final ContentAuthorMapper authorMapper;

    private static final String[][] PALETTES = {
            {"#223047", "#5b728a", "#f1dcc6"},
            {"#3f2b2b", "#8d5b4c", "#f4e2d2"},
            {"#233b34", "#587c6b", "#efe7d2"},
            {"#3a2e4a", "#74608d", "#f1e4d8"},
            {"#4b3425", "#9a6b4a", "#f8ead9"},
            {"#2c3540", "#65798c", "#e8ddcf"}
    };

    @GetMapping(value = "/{id}/cover", produces = "image/svg+xml")
    public ResponseEntity<String> cover(@PathVariable Long id) {
        ContentBook book = bookMapper.selectById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        ContentAuthor author = authorMapper.selectById(book.getAuthorId());
        String svg = svg(book, author == null ? "ReadVerse" : author.getName());
        return ResponseEntity.ok()
                .contentType(new MediaType("image", "svg+xml", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(svg);
    }

    private String svg(ContentBook book, String author) {
        String[] palette = PALETTES[(int) Math.floorMod(book.getId(), PALETTES.length)];
        String title = escape(book.getTitle());
        String authorText = escape(author);
        String[] lines = splitTitle(title);
        String line1 = lines[0];
        String line2 = lines.length > 1 ? lines[1] : "";
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="300" height="420" viewBox="0 0 300 420">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="%s"/>
                      <stop offset="1" stop-color="%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="300" height="420" fill="url(#g)"/>
                  <circle cx="252" cy="58" r="86" fill="#ffffff" opacity="0.07"/>
                  <circle cx="34" cy="382" r="120" fill="#000000" opacity="0.12"/>
                  <rect x="20" y="20" width="260" height="380" fill="none" stroke="#ffffff" stroke-opacity="0.26"/>
                  <text x="30" y="62" fill="%s" opacity="0.72" font-size="11" letter-spacing="4" font-family="Inter, sans-serif">READVERSE</text>
                  <line x1="30" y1="78" x2="88" y2="78" stroke="%s" stroke-opacity="0.7" stroke-width="2"/>
                  <text x="30" y="150" fill="%s" font-size="29" font-family="Songti SC, SimSun, serif">%s</text>
                  <text x="30" y="190" fill="%s" font-size="29" font-family="Songti SC, SimSun, serif">%s</text>
                  <text x="30" y="300" fill="%s" opacity="0.82" font-size="13" font-family="Songti SC, SimSun, serif">%s 著</text>
                  <text x="30" y="360" fill="%s" opacity="0.6" font-size="10" letter-spacing="2" font-family="Inter, sans-serif">READVERSE · 阅界</text>
                </svg>
                """.formatted(palette[0], palette[1], palette[2], palette[2], palette[2], line1,
                palette[2], line2, palette[2], authorText, palette[2]);
    }

    private String[] splitTitle(String title) {
        if (title.length() <= 7) {
            return new String[]{title, ""};
        }
        return new String[]{title.substring(0, 7), title.substring(7, Math.min(14, title.length()))};
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
