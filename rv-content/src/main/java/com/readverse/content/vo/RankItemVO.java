package com.readverse.content.vo;

public record RankItemVO(
        int rank,
        BookCardVO book,
        double score) {
}
