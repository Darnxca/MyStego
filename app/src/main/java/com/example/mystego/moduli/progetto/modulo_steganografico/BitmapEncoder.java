package com.example.mystego.moduli.progetto.modulo_steganografico;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mat on 16/11/15 .
 */
public class BitmapEncoder {

  public static final int HEADER_SIZE = Long.SIZE / Byte.SIZE + 4;

  // header è un parametro che si basa sulla lunghezza in byte del messaggio
  // e viene usato per prendere i bit meno significativi dell'immagine (82)
  public static byte[] createHeader(long size) {
    byte[] header = new byte[HEADER_SIZE];
    int i = 0;
    header[i++] = (byte) 0x5B;
    header[i++] = (byte) 0x5B;
    for (byte b : longToBytes(size)) {
      header[i++] = b;
    }
    return header;
  }

  private static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
    buffer.putLong(x);
    return buffer.array();
  }

  public static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
    buffer.put(bytes);
    buffer.flip();//need flip
    return buffer.getLong();
  }

  public static Bitmap encode(Bitmap inBitmap, byte[] bytes) {

    byte[] header = createHeader(bytes.length);

    // Riempie l'array di byte in modo che sia divisibile per 24 (8 bit per byte * i 3 canali di colore)
    // questo spreca una quantità insignificante di spazio ma rende il codice più ordinato.
    if (bytes.length % 24 != 0) {
      bytes = Arrays.copyOf(bytes, bytes.length + (24 - bytes.length % 24));
    }

    return encodeByteArrayIntoBitmap(inBitmap, header, bytes);
  }

  public static byte[] decode(Bitmap inBitmap) {

    byte[] header = decodeBitmapToByteArray(inBitmap, 0, HEADER_SIZE);
    int len = (int) bytesToLong(Arrays.copyOfRange(header, 2, HEADER_SIZE - 2));

    return decodeBitmapToByteArray(inBitmap, HEADER_SIZE, len);
  }

  /**
   * @param inBitmap bitmap in cui inseriremo il messaggio
   * @param bytes messaggio in byte da nascondere
   * @return copia della bitmpa iniziale con il mesaggio nascosto al suo interno
   */
  private static Bitmap encodeByteArrayIntoBitmap(Bitmap inBitmap, byte[] header, byte[] bytes) {

    // creo una bitmap copia della bitmap originale
    Bitmap outBitmap = inBitmap.copy(Bitmap.Config.ARGB_8888, true);

    int x = 0;
    int y = 0;
    int width = inBitmap.getWidth();

    int r, g, b;
    int color;

    int bufferPos = 0;
    int[] buffer = { 0, 0, 0 };

    for (int i = 0; i < header.length + bytes.length; i++) {
      // Eseguiamo un ciclo su ogni byte e quindi lavoriamo con ciascuno degli 8 bit

      for (int j = 0; j < 8; j++) {

        // Prendi il bit in posizione i e salvalo nel buffer
        if (i < header.length) {
          buffer[bufferPos] = (header[i] >> j) & 1;
        } else {
          buffer[bufferPos] = (bytes[i - header.length] >> j) & 1;
        }

        // Svuota il buffer (disegna) in un pixel
        if (bufferPos == 2) {
          // Ottieni il colore  originale del pixel
          color = inBitmap.getPixel(x, y);

          // Divido in canali
          r = Color.red(color);
          g = Color.green(color);
          b = Color.blue(color);

          // Modify the least significant bit (if needed)
          r = (r % 2 == (1 - buffer[0])) ? r + 1 : r;
          g = (g % 2 == (1 - buffer[1])) ? g + 1 : g;
          b = (b % 2 == (1 - buffer[2])) ? b + 1 : b;

          // Elimino eventuale  overflow
          if (r == 256) r = 254;
          if (g == 256) g = 254;
          if (b == 256) b = 254;

          // Disegno il pixel
          outBitmap.setPixel(x, y, Color.argb(255, r, g, b));

          // Passo al pixel successivo, spostando la riga se necessario
          x = x + 1;
          if (x == width) {
            x = 0;
            y++;
          }
          bufferPos = 0;
        } else {
          bufferPos++;
        }
      }
    }

    return outBitmap;
  }

  /**
   * @param inBitmap bitmap con il messaggio da estrarre
   * @param offset offset da cui iniziare a leggere
   * @param length lunghezza dell'array di byte che ci aspettiamo di leggere
   * @return il messaggio nascosto nell'immagine
   */
  private static byte[] decodeBitmapToByteArray(Bitmap inBitmap, int offset, int length) {

    // variabile per il risultato
    byte[] bytes = new byte[length];

    int width = inBitmap.getWidth();
    int height = inBitmap.getHeight();

    int color;

    int bitNo = 0;
    int byteNo = 0;

    int[] bitBuffer = new int[3];

    // Ciclo su tutti i pixel dell'immagine
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        color = inBitmap.getPixel(x, y);

        bitBuffer[0] = Color.red(color) % 2;
        bitBuffer[1] = Color.green(color) % 2;
        bitBuffer[2] = Color.blue(color) % 2;

        for (int i = 0; i < 3; i++) {

          // Ignora i bit prima dell'offset
          if (byteNo >= offset) {
            // Setta ogni bit dal buffer nel corrispondente bit
            bytes[byteNo - offset] =
                bitBuffer[i] == 1 ? (byte) (bytes[byteNo - offset] | (1 << bitNo))
                    : (byte) (bytes[byteNo - offset] & ~(1 << bitNo));
          }

          bitNo++;
          if (bitNo == 8) {
            bitNo = 0;
            byteNo++;
          }
          if (byteNo - offset >= bytes.length) break;
        }

        // Si ferma quando tutti i byte sono stati letti
        if (byteNo - offset >= bytes.length) break;
      }
      if (byteNo - offset >= bytes.length) break;
    }

    return bytes;
  }
}
