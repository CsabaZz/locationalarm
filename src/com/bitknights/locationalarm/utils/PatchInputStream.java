package com.bitknights.locationalarm.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PatchInputStream extends FilterInputStream {

    public PatchInputStream(InputStream in) {
	super(in);
    }

    public long skip(long n) throws IOException {
	long totalBytesSkipped = 0L;
	while (totalBytesSkipped < n) {
	    long bytesSkipped = in.skip(n - totalBytesSkipped);

	    if (bytesSkipped == 0L) {
		int _byte = read();

		if (_byte < 0) {
		    break;
		} else {
		    bytesSkipped = _byte;
		}
	    }

	    totalBytesSkipped += bytesSkipped;
	}
	return totalBytesSkipped;
    }
}
