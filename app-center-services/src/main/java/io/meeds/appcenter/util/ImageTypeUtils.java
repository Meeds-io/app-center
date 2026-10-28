/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Decides the type of an image from its first bytes, never from a name or a
 * label a client sent.
 */
public final class ImageTypeUtils {

  /** Type of a PNG image. */
  public static final String  PNG_MIME_TYPE   = "image/png";

  /** Type of a JPEG image. */
  public static final String  JPEG_MIME_TYPE  = "image/jpeg";

  /** Type of a GIF image. */
  public static final String  GIF_MIME_TYPE   = "image/gif";

  /** Type of a WebP image. */
  public static final String  WEBP_MIME_TYPE  = "image/webp";

  private static final byte[] PNG_SIGNATURE   = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

  private static final byte[] JPEG_SIGNATURE  = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  private static final byte[] GIF87_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] GIF89_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] RIFF_SIGNATURE  = "RIFF".getBytes(StandardCharsets.US_ASCII);

  private static final byte[] WEBP_SIGNATURE  = "WEBP".getBytes(StandardCharsets.US_ASCII);

  /** Offset of the WEBP form type in a RIFF container, after its size field. */
  private static final int    WEBP_OFFSET     = 8;

  private ImageTypeUtils() {
    // Utility class
  }

  /**
   * Decides the type of an image from its content.
   *
   * @param content the content of a file
   * @return the MIME type of a PNG, JPEG, GIF or WebP image, null for any other
   *         content, SVG included
   */
  public static String detectImageMimeType(byte[] content) {
    if (content == null) {
      return null;
    } else if (startsWith(content, 0, PNG_SIGNATURE)) {
      return PNG_MIME_TYPE;
    } else if (startsWith(content, 0, JPEG_SIGNATURE)) {
      return JPEG_MIME_TYPE;
    } else if (startsWith(content, 0, GIF87_SIGNATURE) || startsWith(content, 0, GIF89_SIGNATURE)) {
      return GIF_MIME_TYPE;
    } else if (startsWith(content, 0, RIFF_SIGNATURE) && startsWith(content, WEBP_OFFSET, WEBP_SIGNATURE)) {
      return WEBP_MIME_TYPE;
    } else {
      return null;
    }
  }

  private static boolean startsWith(byte[] content, int offset, byte[] signature) {
    return content.length >= offset + signature.length
           && Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
  }

}
