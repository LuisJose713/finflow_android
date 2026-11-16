package com.softdevcode.finflow_android.di

/** Identity requests must not reuse or refresh a previously stored session. */
internal val publicAuthPaths = setOf(
    "/auth/login", "/auth/register", "/auth/refresh", "/auth/logout",
    "/auth/google/login", "/auth/google/register",
)
