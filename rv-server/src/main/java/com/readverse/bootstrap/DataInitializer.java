package com.readverse.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.content.entity.BookComment;
import com.readverse.content.entity.BookRating;
import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.entity.PointsGoods;
import com.readverse.content.entity.ReaderBookshelf;
import com.readverse.content.entity.ReaderPointsAccount;
import com.readverse.content.entity.ReaderPointsLog;
import com.readverse.content.entity.ReaderReadRecord;
import com.readverse.content.mapper.BookCommentMapper;
import com.readverse.content.mapper.BookRatingMapper;
import com.readverse.content.mapper.ContentAuthorMapper;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentCategoryMapper;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.mapper.PointsGoodsMapper;
import com.readverse.content.mapper.ReaderBookshelfMapper;
import com.readverse.content.mapper.ReaderPointsAccountMapper;
import com.readverse.content.mapper.ReaderPointsLogMapper;
import com.readverse.content.mapper.ReaderReadRecordMapper;
import com.readverse.content.service.AuthorService;
import com.readverse.content.service.BookService;
import com.readverse.crawl.entity.CrawlSource;
import com.readverse.crawl.mapper.CrawlSourceMapper;
import com.readverse.system.entity.SysPermission;
import com.readverse.system.entity.SysRole;
import com.readverse.system.entity.SysRolePermission;
import com.readverse.system.entity.SysUser;
import com.readverse.system.entity.SysUserRole;
import com.readverse.system.mapper.SysPermissionMapper;
import com.readverse.system.mapper.SysRoleMapper;
import com.readverse.system.mapper.SysRolePermissionMapper;
import com.readverse.system.mapper.SysUserMapper;
import com.readverse.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ContentAuthorMapper authorMapper;
    private final ContentCategoryMapper categoryMapper;
    private final ContentBookMapper bookMapper;
    private final ContentChapterMapper chapterMapper;
    private final BookRatingMapper ratingMapper;
    private final BookCommentMapper commentMapper;
    private final ReaderReadRecordMapper readRecordMapper;
    private final ReaderBookshelfMapper bookshelfMapper;
    private final PointsGoodsMapper goodsMapper;
    private final ReaderPointsAccountMapper accountMapper;
    private final ReaderPointsLogMapper pointsLogMapper;
    private final CrawlSourceMapper crawlSourceMapper;
    private final AuthorService authorService;
    private final BookService bookService;
    private final PasswordEncoder passwordEncoder;

    private static final String[] AUTHORS = {"林观澜", "沈照", "苏晚", "陆青禾", "顾行舟", "程野", "白砚", "许星野", "周迟", "温言"};
    private static final String[] BOOK_TITLES = {
            "星海拾遗", "长安夜行录", "雾港来信", "山海小馆", "第七次日落", "风起南城",
            "纸上江湖", "深空邮差", "旧城手记", "听雪集", "赤潮纪元", "北境灯塔",
            "无声证词", "春日列车", "山河故人", "数据迷宫", "夜航西飞", "青瓦巷",
            "远山有灯", "逆光而行", "时间旅馆", "荒原信使", "月落长街", "人间处方",
            "云端旧事", "琥珀时钟", "微光之城", "渡口无风", "星尘备忘录", "城南旧梦"
    };
    private static final String[][] CATEGORIES = {
            {"fantasy", "玄幻"}, {"urban", "都市"}, {"history", "历史"},
            {"scifi", "科幻"}, {"mystery", "悬疑"}, {"literature", "文学"}
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleMapper.selectCount(null) > 0) {
            log.info("Demo data already present, skip seeding");
            return;
        }
        log.info("Seeding ReadVerse demo data...");
        Map<String, Long> roles = seedRoles();
        Map<String, Long> permissions = seedPermissions();
        assignPermissions(roles, permissions);
        Map<String, Long> users = seedUsers(roles);
        Map<String, Long> categories = seedCategories();
        seedAuthorsAndBooks(users, categories);
        seedPointsAndCrawl(users);
        log.info("Demo data seeded: roles={}, permissions={}, users={}, books={}",
                roles.size(), permissions.size(), users.size(), bookMapper.selectCount(null));
    }

    private Map<String, Long> seedRoles() {
        Map<String, Long> roles = new LinkedHashMap<>();
        roles.put("ADMIN", createRole("ADMIN", "管理员", "系统最高权限", 1));
        roles.put("OPERATOR", createRole("OPERATOR", "运营", "内容、采集与积分运营", 2));
        roles.put("READER", createRole("READER", "读者", "门户阅读用户", 3));
        return roles;
    }

    private Long createRole(String code, String name, String description, int sort) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setStatus(1);
        role.setSort(sort);
        roleMapper.insert(role);
        return role.getId();
    }

    private Map<String, Long> seedPermissions() {
        Map<String, Long> ids = new HashMap<>();
        Long dashboard = perm(0L, "menu:dashboard", "数据看板", "MENU", "/dashboard", "dashboard/index", "DataLine", 1, true);
        Long content = perm(0L, "menu:content", "内容管理", "MENU", "/content", "Layout", "Reading", 2, true);
        Long books = perm(content, "menu:content:books", "书籍管理", "MENU", "/content/books", "content/books/index", "Notebook", 1, true);
        Long chapters = perm(content, "menu:content:chapters", "章节管理", "MENU", "/content/chapters", "content/chapters/index", "Document", 2, true);
        Long authors = perm(content, "menu:content:authors", "作者管理", "MENU", "/content/authors", "content/authors/index", "User", 3, true);
        Long categories = perm(content, "menu:content:categories", "分类管理", "MENU", "/content/categories", "content/categories/index", "Collection", 4, true);
        Long operation = perm(0L, "menu:operation", "采集运营", "MENU", "/operation", "Layout", "Connection", 3, true);
        Long sources = perm(operation, "menu:operation:sources", "书源管理", "MENU", "/operation/sources", "operation/sources/index", "Link", 1, true);
        Long tasks = perm(operation, "menu:operation:tasks", "采集任务", "MENU", "/operation/tasks", "operation/tasks/index", "List", 2, true);
        Long points = perm(0L, "menu:points", "积分运营", "MENU", "/points", "Layout", "Coin", 4, true);
        Long goods = perm(points, "menu:points:goods", "积分商品", "MENU", "/points/goods", "points/goods/index", "Goods", 1, true);
        Long orders = perm(points, "menu:points:orders", "兑换订单", "MENU", "/points/orders", "points/orders/index", "Tickets", 2, true);
        Long system = perm(0L, "menu:system", "系统管理", "MENU", "/system", "Layout", "Setting", 5, true);
        Long sysUsers = perm(system, "menu:system:users", "用户管理", "MENU", "/system/users", "system/users/index", "UserFilled", 1, true);
        Long sysRoles = perm(system, "menu:system:roles", "角色权限", "MENU", "/system/roles", "system/roles/index", "Key", 2, true);

        ids.put("menu:dashboard", dashboard);
        ids.put("menu:content", content);
        ids.put("menu:content:books", books);
        ids.put("menu:content:chapters", chapters);
        ids.put("menu:content:authors", authors);
        ids.put("menu:content:categories", categories);
        ids.put("menu:operation", operation);
        ids.put("menu:operation:sources", sources);
        ids.put("menu:operation:tasks", tasks);
        ids.put("menu:points", points);
        ids.put("menu:points:goods", goods);
        ids.put("menu:points:orders", orders);
        ids.put("menu:system", system);
        ids.put("menu:system:users", sysUsers);
        ids.put("menu:system:roles", sysRoles);

        button(ids, books, "content:book:list", "书籍查询", 1);
        button(ids, books, "content:book:add", "新增书籍", 2);
        button(ids, books, "content:book:edit", "编辑书籍", 3);
        button(ids, books, "content:book:delete", "删除书籍", 4);
        button(ids, books, "content:book:publish", "上下架", 5);
        button(ids, books, "content:book:audit", "审核书籍", 6);
        button(ids, chapters, "content:chapter:list", "章节查询", 1);
        button(ids, chapters, "content:chapter:add", "新增章节", 2);
        button(ids, chapters, "content:chapter:edit", "编辑章节", 3);
        button(ids, chapters, "content:chapter:delete", "删除章节", 4);
        button(ids, chapters, "content:chapter:publish", "发布章节", 5);
        button(ids, authors, "content:author:list", "作者查询", 1);
        button(ids, authors, "content:author:add", "新增作者", 2);
        button(ids, authors, "content:author:edit", "编辑作者", 3);
        button(ids, authors, "content:author:delete", "删除作者", 4);
        button(ids, authors, "content:author:audit", "审核作者", 5);
        button(ids, categories, "content:category:list", "分类查询", 1);
        button(ids, categories, "content:category:add", "新增分类", 2);
        button(ids, categories, "content:category:edit", "编辑分类", 3);
        button(ids, categories, "content:category:delete", "删除分类", 4);
        button(ids, sources, "crawl:source:list", "书源查询", 1);
        button(ids, sources, "crawl:source:add", "新增书源", 2);
        button(ids, sources, "crawl:source:edit", "编辑书源", 3);
        button(ids, sources, "crawl:source:delete", "删除书源", 4);
        button(ids, sources, "crawl:source:test", "测试书源", 5);
        button(ids, tasks, "crawl:task:list", "任务查询", 1);
        button(ids, tasks, "crawl:task:retry", "重试任务", 2);
        button(ids, goods, "points:goods:list", "商品查询", 1);
        button(ids, goods, "points:goods:add", "新增商品", 2);
        button(ids, goods, "points:goods:edit", "编辑商品", 3);
        button(ids, goods, "points:goods:delete", "删除商品", 4);
        button(ids, orders, "points:order:list", "订单查询", 1);
        button(ids, sysUsers, "system:user:list", "用户查询", 1);
        button(ids, sysUsers, "system:user:edit", "用户编辑", 2);
        button(ids, sysRoles, "system:role:list", "角色查询", 1);
        button(ids, sysRoles, "system:role:add", "新增角色", 2);
        button(ids, sysRoles, "system:role:edit", "编辑角色", 3);
        button(ids, sysRoles, "system:role:delete", "删除角色", 4);
        button(ids, sysRoles, "system:role:assign", "分配权限", 5);
        button(ids, system, "system:permission:list", "权限查询", 1);
        button(ids, system, "system:log:list", "操作日志", 2);
        button(ids, dashboard, "stats:view", "看板查看", 1);
        return ids;
    }

    private Long perm(Long parentId, String code, String name, String type, String path,
                      String component, String icon, int sort, boolean visible) {
        SysPermission permission = new SysPermission();
        permission.setParentId(parentId);
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setPath(path);
        permission.setComponent(component);
        permission.setIcon(icon);
        permission.setSort(sort);
        permission.setVisible(visible);
        permission.setStatus(1);
        permissionMapper.insert(permission);
        return permission.getId();
    }

    private void button(Map<String, Long> ids, Long parentId, String code, String name, int sort) {
        ids.put(code, perm(parentId, code, name, "BUTTON", null, null, null, sort, false));
    }

    private void assignPermissions(Map<String, Long> roles, Map<String, Long> permissions) {
        List<Long> all = new ArrayList<>(permissions.values());
        for (Long permissionId : all) {
            SysRolePermission admin = new SysRolePermission();
            admin.setRoleId(roles.get("ADMIN"));
            admin.setPermissionId(permissionId);
            rolePermissionMapper.insert(admin);
        }
        for (Map.Entry<String, Long> entry : permissions.entrySet()) {
            if (entry.getKey().startsWith("system:") || entry.getKey().startsWith("menu:system")) {
                continue;
            }
            SysRolePermission operator = new SysRolePermission();
            operator.setRoleId(roles.get("OPERATOR"));
            operator.setPermissionId(entry.getValue());
            rolePermissionMapper.insert(operator);
        }
    }

    private Map<String, Long> seedUsers(Map<String, Long> roles) {
        Map<String, Long> users = new HashMap<>();
        users.put("admin", createUser("admin", "Admin@123", "阅界管理员", roles.get("ADMIN")));
        users.put("operator", createUser("operator", "Operator@123", "阅界运营", roles.get("OPERATOR")));
        users.put("reader", createUser("reader", "Reader@123", "书友小阅", roles.get("READER")));
        return users;
    }

    private Long createUser(String username, String rawPassword, String nickname, Long roleId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
        ReaderPointsAccount account = new ReaderPointsAccount();
        account.setUserId(user.getId());
        account.setBalance(120);
        account.setTotalEarned(120);
        account.setTotalSpent(0);
        account.setVersion(0);
        accountMapper.insert(account);
        ReaderPointsLog initLog = new ReaderPointsLog();
        initLog.setUserId(user.getId());
        initLog.setChangeAmount(120);
        initLog.setBalanceAfter(120);
        initLog.setBizType("INIT");
        initLog.setBizId(user.getId());
        initLog.setRemark("初始演示积分");
        pointsLogMapper.insert(initLog);
        return user.getId();
    }

    private Map<String, Long> seedCategories() {
        Map<String, Long> ids = new LinkedHashMap<>();
        int sort = 1;
        for (String[] row : CATEGORIES) {
            ContentCategory category = new ContentCategory();
            category.setParentId(0L);
            category.setCode(row[0]);
            category.setName(row[1]);
            category.setSort(sort++);
            category.setStatus(1);
            categoryMapper.insert(category);
            ids.put(row[0], category.getId());
        }
        return ids;
    }

    private void seedAuthorsAndBooks(Map<String, Long> users, Map<String, Long> categories) {
        List<Long> authorIds = new ArrayList<>();
        for (String name : AUTHORS) {
            ContentAuthor author = new ContentAuthor();
            author.setName(name);
            author.setIntro("ReadVerse 签约示例作者，" + name + " 的作品以细腻的叙事见长。");
            author.setStatus("APPROVED");
            author.setBookCount(0);
            author.setDeleted(0);
            authorMapper.insert(author);
            authorIds.add(author.getId());
        }
        List<String> categoryCodes = new ArrayList<>(categories.keySet());
        List<Long> bookIds = new ArrayList<>();
        List<Long> firstChapterIds = new ArrayList<>();
        for (int i = 0; i < BOOK_TITLES.length; i++) {
            ContentBook book = new ContentBook();
            book.setTitle(BOOK_TITLES[i]);
            book.setSubtitle("ReadVerse 示例书库 · 第 " + (i + 1) + " 本");
            book.setAuthorId(authorIds.get(i % authorIds.size()));
            book.setCategoryId(categories.get(categoryCodes.get(i % categoryCodes.size())));
            book.setCoverUrl("/api/portal/books/0/cover");
            book.setIntro("《" + BOOK_TITLES[i] + "》是 ReadVerse 内置示例书籍，用于展示首页、搜索、阅读器、书架、评分与排行榜。");
            book.setStatus("ON_SHELF");
            book.setScore(BigDecimal.valueOf(4.0 + (i % 10) / 10.0));
            book.setRatingCount(8 + i * 2);
            book.setVisitCount((long) (1200 - i * 23));
            book.setWordCount(0);
            book.setChapterCount(0);
            book.setIsVip(false);
            book.setPublishTime(LocalDateTime.now().minusDays(i % 7));
            book.setCreateTime(LocalDateTime.now().minusDays(i % 7));
            book.setUpdateTime(LocalDateTime.now().minusDays(i % 7));
            book.setDeleted(0);
            bookMapper.insert(book);
            book.setCoverUrl("/api/portal/books/" + book.getId() + "/cover");
            bookMapper.updateById(book);
            bookIds.add(book.getId());

            for (int c = 1; c <= 5; c++) {
                ContentChapter chapter = new ContentChapter();
                chapter.setBookId(book.getId());
                chapter.setChapterNo(c);
                chapter.setTitle("第" + c + "章 " + chapterTitle(c));
                chapter.setContent(chapterContent(BOOK_TITLES[i], c));
                chapter.setWordCount(chapter.getContent().replaceAll("\\s", "").length());
                chapter.setIsVip(false);
                chapter.setStatus("PUBLISHED");
                chapter.setPublishTime(LocalDateTime.now().minusDays(i % 7).plusMinutes(c));
                chapter.setDeleted(0);
                chapterMapper.insert(chapter);
                if (c == 1) {
                    firstChapterIds.add(chapter.getId());
                }
            }
            bookService.refreshMeta(book.getId());
            authorService.refreshBookCount(book.getAuthorId());

            if (i < 10) {
                BookRating rating = new BookRating();
                rating.setUserId(users.get("reader"));
                rating.setBookId(book.getId());
                rating.setScore(5);
                ratingMapper.insert(rating);
                for (int c = 1; c <= 2; c++) {
                    BookComment comment = new BookComment();
                    comment.setUserId(users.get("reader"));
                    comment.setBookId(book.getId());
                    comment.setParentId(0L);
                    comment.setContent(c == 1 ? "开篇很有画面感，已加入书架。" : "读到第三章，节奏越来越稳。");
                    comment.setLikeCount(c * 3);
                    comment.setStatus(1);
                    comment.setDeleted(0);
                    commentMapper.insert(comment);
                }
            }
        }
        seedReaderActivity(users.get("reader"), bookIds, firstChapterIds);
    }

    private void seedReaderActivity(Long readerId, List<Long> bookIds, List<Long> firstChapterIds) {
        for (int i = 0; i < Math.min(6, bookIds.size()); i++) {
            ReaderReadRecord record = new ReaderReadRecord();
            record.setUserId(readerId);
            record.setBookId(bookIds.get(i));
            record.setChapterId(firstChapterIds.get(i));
            record.setChapterNo(1);
            record.setProgress(BigDecimal.valueOf(0.18));
            record.setReadSeconds(120 + i * 30);
            record.setUpdateTime(LocalDateTime.now().minusHours(i));
            readRecordMapper.insert(record);

            ReaderBookshelf shelf = new ReaderBookshelf();
            shelf.setUserId(readerId);
            shelf.setBookId(bookIds.get(i));
            shelf.setLastChapterId(firstChapterIds.get(i));
            shelf.setLastChapterNo(1);
            bookshelfMapper.insert(shelf);
        }
    }

    private String chapterTitle(int no) {
        return switch (no) {
            case 1 -> "初见";
            case 2 -> "回声";
            case 3 -> "远行";
            case 4 -> "灯影";
            default -> "归途";
        };
    }

    private String chapterContent(String title, int no) {
        return """
                %s 第%d章。

                夜色从窗沿慢慢落下来，风把远处的灯影吹得很轻。故事里的人停下脚步，
                听见城市在低声呼吸，也听见自己心里那一点不肯熄灭的光。

                他想起很多年前的一个下午，阳光落在书页上，像一条安静的金色河流。
                那时他还不知道，所有远行都会有回声，所有告别都会在某个章节里重逢。

                本作品为 ReadVerse 内置示例文本，不代表真实出版物或外部站点内容。
                """.formatted(title, no);
    }

    private void seedPointsAndCrawl(Map<String, Long> users) {
        createGoods("章节解锁券", "CHAPTER_UNLOCK", 30, 999, "解锁指定章节 7 天");
        createGoods("7 天畅读券", "BOOK_VIP", 100, 200, "指定书籍 7 天畅读");
        createGoods("限定书签", "COUPON", 10, 500, "兑换后获得一枚示例书签码");

        CrawlSource source = new CrawlSource();
        source.setName("内置示例书源");
        source.setSourceType("MOCK");
        source.setBaseUrl("classpath:mock/books.json");
        source.setConfigJson("{\"note\":\"仅用于演示的本地示例源，不抓取真实站点\"}");
        source.setStatus(1);
        crawlSourceMapper.insert(source);
    }

    private void createGoods(String name, String type, int price, int stock, String config) {
        PointsGoods goods = new PointsGoods();
        goods.setName(name);
        goods.setType(type);
        goods.setPointsPrice(price);
        goods.setStock(stock);
        goods.setStatus(1);
        goods.setConfigJson(config);
        goodsMapper.insert(goods);
    }
}
