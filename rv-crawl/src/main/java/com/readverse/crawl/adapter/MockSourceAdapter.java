package com.readverse.crawl.adapter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in legal demo source. It does not fetch any real website.
 */
@Component
public class MockSourceAdapter {

    public record MockBook(String externalId, String title, String author, String categoryCode,
                           String intro, List<MockChapter> chapters) {
    }

    public record MockChapter(int chapterNo, String title, String content) {
    }

    private static final Object[][] BOOKS = {
            {"m-001", "星海拾遗", "林观澜", "scifi", "在星海边缘，一枚遗落的信标唤醒了沉睡百年的航线。"},
            {"m-002", "长安夜行录", "沈照", "history", "灯火长安，夜行人不问归途，只问真相。"},
            {"m-003", "雾港来信", "苏晚", "mystery", "一封没有署名的信，把雾港的旧案重新翻开。"},
            {"m-004", "山海小馆", "陆青禾", "literature", "山脚下的小馆，收留每一个赶路的人。"},
            {"m-005", "第七次日落", "顾行舟", "scifi", "太阳第七次落下时，城市开始学会记忆。"},
            {"m-006", "风起南城", "程野", "urban", "南城的风穿过旧巷，也穿过一群年轻人的理想。"},
            {"m-007", "纸上江湖", "白砚", "literature", "一册残卷，半生江湖，写字的人也在被文字改变。"},
            {"m-008", "深空邮差", "许星野", "scifi", "在深空站之间送信，是孤独者最后的浪漫。"},
            {"m-009", "旧城手记", "周迟", "urban", "旧城改造前的最后一个月，他决定记录每一扇窗。"},
            {"m-010", "听雪集", "温言", "literature", "雪落无声，故事有声。"}
    };

    public List<MockBook> fetchBooks() {
        List<MockBook> books = new ArrayList<>();
        for (Object[] row : BOOKS) {
            String externalId = (String) row[0];
            books.add(new MockBook(externalId, (String) row[1], (String) row[2], (String) row[3],
                    (String) row[4], chapters(externalId, (String) row[1])));
        }
        return books;
    }

    private List<MockChapter> chapters(String externalId, String title) {
        List<MockChapter> chapters = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String content = """
                    %s 第%d章。

                    这是%s的示例章节内容，仅用于演示阅读、缓存、排行榜与阅读进度功能。
                    夜色从窗沿慢慢落下来，风把远处的灯影吹得很轻。故事里的人停下脚步，
                    听见城市在低声呼吸，也听见自己心里那一点不肯熄灭的光。

                    他想起很多年前的一个下午，阳光落在书页上，像一条安静的金色河流。
                    那时他还不知道，所有远行都会有回声，所有告别都会在某个章节里重逢。

                    本作品为 ReadVerse 内置示例文本，不代表真实出版物或外部站点内容。
                    """.formatted(title, i, title);
            chapters.add(new MockChapter(i, "第" + i + "章 " + chapterTitle(i), content));
        }
        return chapters;
    }

    private String chapterTitle(int i) {
        return switch (i) {
            case 1 -> "初见";
            case 2 -> "回声";
            case 3 -> "远行";
            case 4 -> "灯影";
            default -> "归途";
        };
    }
}
