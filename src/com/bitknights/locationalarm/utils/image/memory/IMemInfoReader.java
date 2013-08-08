
package com.bitknights.locationalarm.utils.image.memory;

public interface IMemInfoReader {
    boolean matchText(byte[] buffer, int index, String text);

    long extractMemValue(byte[] buffer, int index);

    void readMemInfo();

    long getTotalSize();

    long getFreeSize();

    long getCachedSize();
}
