package com.fischerabruzese.graphvcli

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val storageDir: String = "/home/sky/dev/graphv/src/tempstroage/",
)
