package com.ariel.balloonspy.utility;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AndroidBmpUtil {

    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 3;

    private byte[] Color_palette = new byte[1024]; //a palette containing 256 colors
    private byte[] BMP_File_Header = new byte[14];
    private byte[] DIB_header = new byte[40];
    private byte[] Bitmap_Data = null;

    /**
     * Android Bitmap Object to Window's v3 24bit Bmp Format File
     *
     * @param orgBitmap
     * @param filePath
     * @return file saved result
     */
    public static boolean save(Bitmap orgBitmap, String filePath) throws IOException {
        long start = System.currentTimeMillis();
        if (orgBitmap == null) {
            return false;
        }

        if (filePath == null) {
            return false;
        }

        boolean isSaveSuccess = true;

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
        if (rowWidthInBytes % BMP_WIDTH_OF_TIMES > 0) {
            hasDummy = true;
            //the number of dummy bytes we need to add on each row
            dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES - (rowWidthInBytes % BMP_WIDTH_OF_TIMES))];
            //just fill an array with the dummy bytes we need to append at the end of each row
            for (int i = 0; i < dummyBytesPerRow.length; i++) {
                dummyBytesPerRow[i] = (byte) 0xFF;
            }
        }

        //an array to receive the pixels from the source image
        int[] pixels = new int[width * height];

        //the number of bytes used in the file to store raw image data (excluding file headers)
        int imageSize = (rowWidthInBytes + (hasDummy ? dummyBytesPerRow.length : 0)) * height;
        //file headers size
        int imageDataOffset = 0x36;

        //final size of the file
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        /**
         * BITMAP FILE HEADER Write Start
         **/
        buffer.put((byte) 0x42);
        buffer.put((byte) 0x4D);

        //size
        buffer.put(writeInt(fileSize));

        //reserved
        buffer.put(writeShort((short) 0));
        buffer.put(writeShort((short) 0));

        //image data start offset
        buffer.put(writeInt(imageDataOffset));

        /** BITMAP FILE HEADER Write End */

        //*******************************************

        /** BITMAP INFO HEADER Write Start */
        //size
        buffer.put(writeInt(0x28));

        //width, height
        //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
        buffer.put(writeInt(width + (hasDummy ? (dummyBytesPerRow.length == 3 ? 1 : 0) : 0)));
        buffer.put(writeInt(height));

        //planes
        buffer.put(writeShort((short) 1));

        //bit count
        buffer.put(writeShort((short) 24));

        //bit compression
        buffer.put(writeInt(0));

        //image data size
        buffer.put(writeInt(imageSize));

        //horizontal resolution in pixels per meter
        buffer.put(writeInt(0));

        //vertical resolution in pixels per meter (unreliable)
        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        buffer.put(writeInt(0));

        /** BITMAP INFO HEADER Write End */

        int row = height;
        int col = width;
        int startPosition = (row - 1) * col;
        int endPosition = row * col;
        while (row > 0) {
            for (int i = startPosition; i < endPosition; i++) {
                buffer.put((byte) (pixels[i] & 0x000000FF));
                buffer.put((byte) ((pixels[i] & 0x0000FF00) >> 8));
                buffer.put((byte) ((pixels[i] & 0x00FF0000) >> 16));
            }
            if (hasDummy) {
                buffer.put(dummyBytesPerRow);
            }
            row--;
            endPosition = startPosition;
            startPosition = startPosition - col;
        }

        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(buffer.array());
        fos.close();
        Log.v("AndroidBmpUtil", System.currentTimeMillis() - start + " ms");

        return isSaveSuccess;
    }

    //returns a byte array of a grey scale bitmap image
    public byte[] CreateGrayBitmapArray(Bitmap Image) {
        try {
            create_parts(Image);
            //Create the array
            byte[] bitmap_array = new byte[BMP_File_Header.length + DIB_header.length
                    + Color_palette.length + Bitmap_Data.length];
            Copy_to_Index(bitmap_array, BMP_File_Header, 0);
            Copy_to_Index(bitmap_array, DIB_header, BMP_File_Header.length);
            Copy_to_Index(bitmap_array, Color_palette, BMP_File_Header.length + DIB_header.length);
            Copy_to_Index(bitmap_array, Bitmap_Data, BMP_File_Header.length + DIB_header.length + Color_palette.length);

            return bitmap_array;
        } catch (Exception e) {
            return null; //return a null single byte array if fails
        }
    }


    //creates byte array of 256 color grayscale palette
    private byte[] create_palette() {
        byte[] color_palette = new byte[1024];
        for (int i = 0; i < 256; i++) {
            color_palette[i * 4 + 0] = (byte) (i); //bule
            color_palette[i * 4 + 1] = (byte) (i); //green
            color_palette[i * 4 + 2] = (byte) (i); //red
            color_palette[i * 4 + 3] = (byte) 0; //padding
        }
        return color_palette;
    }


    //adds dtata of Source array to Destinition array at the Index
    private boolean Copy_to_Index(byte[] destination, byte[] source, int index) {
        try {
            for (int i = 0; i < source.length; i++) {
                destination[i + index] = source[i];
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    //create different part of a bitmap file
    private void create_parts(Bitmap img) throws IOException{
        //Create Bitmap Data
        Bitmap_Data = ConvertToGrayscale(img);
        //Create Bitmap File Header (populate BMP_File_Header array)
        Copy_to_Index(BMP_File_Header, new byte[]{(byte) 'B', (byte) 'M'}, 0); //magic number
        Copy_to_Index(BMP_File_Header, writeInt(BMP_File_Header.length
                + DIB_header.length + Color_palette.length + Bitmap_Data.length), 2); //file size
        Copy_to_Index(BMP_File_Header, new byte[]{(byte) 'M', (byte) 'C', (byte) 'A', (byte) 'T'}, 6); //reserved for application generating the bitmap file (not imprtant)
        Copy_to_Index(BMP_File_Header, writeInt(BMP_File_Header.length
                + DIB_header.length + Color_palette.length), 10); //bitmap raw data offset
        //Create DIB Header (populate DIB_header array)
        Copy_to_Index(DIB_header, writeInt(DIB_header.length), 0); //DIB header length
        Copy_to_Index(DIB_header, writeInt(((Bitmap) img).getWidth()), 4); //image width
        Copy_to_Index(DIB_header, writeInt(((Bitmap) img).getHeight()), 8); //image height
        Copy_to_Index(DIB_header, new byte[]{(byte) 1, (byte) 0}, 12); //color planes. N.B. Must be set to 1
        Copy_to_Index(DIB_header, new byte[]{(byte) 8, (byte) 0}, 14); //bits per pixel
        Copy_to_Index(DIB_header, writeInt(0), 16); //compression method N.B. BI_RGB = 0
        Copy_to_Index(DIB_header, writeInt(Bitmap_Data.length), 20); //lenght of raw bitmap data
        Copy_to_Index(DIB_header, writeInt(1000), 24); //horizontal reselution N.B. not important
        Copy_to_Index(DIB_header, writeInt(1000), 28); //vertical reselution N.B. not important
        Copy_to_Index(DIB_header, writeInt(256), 32); //number of colors in the palette
        Copy_to_Index(DIB_header, writeInt(0), 36); //number of important colors used N.B. 0 = all colors are imprtant
        //Create Color palett
        Color_palette = create_palette();
    }

    //convert the color pixels of Source image into a grayscale bitmap (raw data)
    private byte[] ConvertToGrayscale(Bitmap Source) {
        Bitmap source = (Bitmap) Source;
        int padding = (source.getWidth() % 4) != 0 ? 4 - (source.getWidth() % 4) : 0; //determine padding needed for bitmap file
        byte[] bytes = new byte[source.getWidth() * source.getHeight() + padding * source.getHeight()]; //create array to contain bitmap data with paddin
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getPixel(x, y);
                int g = (int) (0.3 * Color.red(pixel) + 0.59 * Color.green(pixel) + 0.11 * Color.blue(pixel)); //grayscale shade corresponding to rgb
                bytes[(source.getHeight() - 1 - y) * source.getWidth() + (source.getHeight() - 1 - y) * padding + x] = (byte) g;
            }
            //add the padding
            for (int i = 0; i < padding; i++) {
                bytes[(source.getHeight() - y) * source.getWidth() + (source.getHeight() - 1 - y) * padding + i] = (byte) 0;
            }
        }
        return bytes;
    }

    /**
     * Write integer to little-endian
     *
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte) (value & 0x000000FF);
        b[1] = (byte) ((value & 0x0000FF00) >> 8);
        b[2] = (byte) ((value & 0x00FF0000) >> 16);
        b[3] = (byte) ((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write short to little-endian byte array
     *
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte) (value & 0x00FF);
        b[1] = (byte) ((value & 0xFF00) >> 8);

        return b;
    }
}