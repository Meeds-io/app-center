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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageTypeUtilsTest {

  @Test
  void detectImageMimeTypeReadsTheSignatureOfEachAcceptedType() {
    assertEquals(ImageTypeUtils.PNG_MIME_TYPE,
                 ImageTypeUtils.detectImageMimeType(bytes(0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13)));
    assertEquals(ImageTypeUtils.JPEG_MIME_TYPE, ImageTypeUtils.detectImageMimeType(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 16)));
    assertEquals(ImageTypeUtils.GIF_MIME_TYPE, ImageTypeUtils.detectImageMimeType(ascii("GIF87a\u0001\u0000")));
    assertEquals(ImageTypeUtils.GIF_MIME_TYPE, ImageTypeUtils.detectImageMimeType(ascii("GIF89a\u0001\u0000")));
    assertEquals(ImageTypeUtils.WEBP_MIME_TYPE, ImageTypeUtils.detectImageMimeType(ascii("RIFF$\u0000\u0000\u0000WEBPVP8 ")));
  }

  @Test
  void detectImageMimeTypeRefusesAnyOtherContent() {
    assertNull(ImageTypeUtils.detectImageMimeType(null));
    assertNull(ImageTypeUtils.detectImageMimeType(new byte[0]));
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")));
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("<?xml version=\"1.0\"?><svg/>")));
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("<html><script>alert(1)</script></html>")));
    // A RIFF container of another form type, such as a WAVE sound
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("RIFF$\u0000\u0000\u0000WAVEfmt ")));
    // Truncated signatures
    assertNull(ImageTypeUtils.detectImageMimeType(bytes(0x89, 'P', 'N', 'G')));
    assertNull(ImageTypeUtils.detectImageMimeType(bytes(0xFF, 0xD8)));
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("GIF8")));
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("RIFF$\u0000\u0000\u0000WEB")));
    // A signature elsewhere than at the start
    assertNull(ImageTypeUtils.detectImageMimeType(ascii("xGIF89a")));
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.ISO_8859_1);
  }

}
