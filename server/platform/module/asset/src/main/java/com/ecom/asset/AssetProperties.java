package com.ecom.asset;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limits for uploads, under {@code ecom.asset}.
 *
 * @param maxImageBytes the largest single image, 5 MB when not set
 */
@ConfigurationProperties(prefix = "ecom.asset")
public record AssetProperties(@DefaultValue("5242880") long maxImageBytes) {
}
