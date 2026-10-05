package com.creativeblisslabs.peqlearn.app.data

/**
 * Normalizes file paths to prevent mismatching between "file://" URIs and raw paths.
 */
fun normalizePath(path: String?): String {
    if (path == null) return ""
    var p: String = path
    if (p.startsWith("file://")) {
        p = p.substring(7)
    } else if (p.startsWith("file:/")) {
        p = p.substring(5)
    } else if (p.startsWith("file:")) {
        p = p.substring(5)
    }
    // Remove duplicate slashes if any
    while (p.startsWith("//")) {
        p = p.substring(1)
    }
    return p
}
