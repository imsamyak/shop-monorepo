package com.ecom.asset;

/**
 * An image that was stored.
 *
 * @param key         where it is kept, {@code images/<owner>/<random>.<extension>}
 * @param url         the address anybody can fetch it from
 * @param contentType the type found from the file itself
 * @param size        size in bytes
 */
public record StoredImage(String key, String url, String contentType, long size) {
}
