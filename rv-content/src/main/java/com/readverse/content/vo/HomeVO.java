package com.readverse.content.vo;

import java.util.List;

public record HomeVO(
        List<CategoryVO> categories,
        List<BookCardVO> newBooks,
        List<BookCardVO> hotBooks,
        List<RankItemVO> rankings) {
}
