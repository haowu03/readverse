package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.content.dto.CategorySaveRequest;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentCategoryMapper;
import com.readverse.content.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ContentCategoryMapper categoryMapper;
    private final ContentBookMapper bookMapper;

    public List<ContentCategory> listAll() {
        return categoryMapper.selectList(Wrappers.<ContentCategory>lambdaQuery()
                .orderByAsc(ContentCategory::getSort).orderByAsc(ContentCategory::getId));
    }

    public List<CategoryVO> tree() {
        List<ContentCategory> categories = listAll().stream()
                .filter(c -> c.getStatus() == null || c.getStatus() == 1)
                .toList();
        return buildTree(categories);
    }

    public List<CategoryVO> buildTree(List<ContentCategory> categories) {
        Map<Long, CategoryVO> nodes = new HashMap<>();
        List<CategoryVO> roots = new ArrayList<>();
        for (ContentCategory category : categories) {
            nodes.put(category.getId(), new CategoryVO(category.getId(), category.getParentId(),
                    category.getName(), category.getCode(), category.getSort(), category.getIcon(), new ArrayList<>()));
        }
        for (ContentCategory category : categories) {
            CategoryVO node = nodes.get(category.getId());
            if (category.getParentId() == null || category.getParentId() == 0 || !nodes.containsKey(category.getParentId())) {
                roots.add(node);
            } else {
                nodes.get(category.getParentId()).children().add(node);
            }
        }
        roots.sort(Comparator.comparing(CategoryVO::sort, Comparator.nullsLast(Integer::compareTo)));
        return roots;
    }

    @Transactional
    public ContentCategory create(CategorySaveRequest request) {
        Long exists = categoryMapper.selectCount(Wrappers.<ContentCategory>lambdaQuery()
                .eq(ContentCategory::getCode, request.code()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "分类编码已存在");
        }
        ContentCategory category = new ContentCategory();
        category.setParentId(request.parentId() == null ? 0L : request.parentId());
        category.setName(request.name());
        category.setCode(request.code());
        category.setSort(request.sort() == null ? 0 : request.sort());
        category.setIcon(request.icon());
        category.setStatus(request.status() == null ? 1 : request.status());
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public ContentCategory update(Long id, CategorySaveRequest request) {
        ContentCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        if (id.equals(request.parentId())) {
            throw new BizException("分类不能以自己为父分类");
        }
        category.setParentId(request.parentId() == null ? 0L : request.parentId());
        category.setName(request.name());
        category.setCode(request.code());
        if (request.sort() != null) {
            category.setSort(request.sort());
        }
        category.setIcon(request.icon());
        if (request.status() != null) {
            category.setStatus(request.status());
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Long children = categoryMapper.selectCount(Wrappers.<ContentCategory>lambdaQuery()
                .eq(ContentCategory::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ErrorCode.CONFLICT, "请先删除子分类");
        }
        Long books = bookMapper.selectCount(Wrappers.<ContentBook>lambdaQuery()
                .eq(ContentBook::getCategoryId, id));
        if (books != null && books > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该分类下仍有书籍，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    public String nameOf(Long id) {
        if (id == null) {
            return "未分类";
        }
        ContentCategory category = categoryMapper.selectById(id);
        return category == null ? "未分类" : category.getName();
    }
}
