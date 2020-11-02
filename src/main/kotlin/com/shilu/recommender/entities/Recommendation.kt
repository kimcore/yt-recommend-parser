package com.shilu.recommender.entities

data class Recommendation(
    val title: String,
    val identifier: String,
    val length: String,
    val lengthMs: Long,
    val thumbnail: List<String>,
    val view: String,
    val published: String
)