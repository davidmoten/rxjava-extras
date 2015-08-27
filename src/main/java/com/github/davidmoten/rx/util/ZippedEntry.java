package com.github.davidmoten.rx.util;

import java.io.InputStream;
import java.util.zip.ZipEntry;

public final class ZippedEntry {

    final String name; // entry name
    final long time; // last modification time
    // final FileTime mtime; // last modification time, from extra field data
    // final FileTime atime; // last access time, from extra field data
    // final FileTime ctime; // creation time, from extra field data
    final long crc; // crc-32 of entry data
    final long size; // uncompressed size of entry data
    final long csize; // compressed size of entry data
    final int method; // compression method
    final byte[] extra; // optional extra field data for entry
    final String comment; // optional comment string for entry
    private final InputStream is;

    public ZippedEntry(ZipEntry e, InputStream is) {
        this.name = e.getName();
        this.time = e.getTime();
        // this.mtime = e.getLastModifiedTime();
        // this.atime = e.getLastAccessTime();
        // this.ctime = e.getCreationTime();
        this.crc = e.getCrc();
        this.size = e.getSize();
        this.csize = e.getCompressedSize();
        this.method = e.getMethod();
        this.extra = e.getExtra();
        this.comment = e.getComment();
        this.is = is;
    }

    public InputStream getInputStream() {
        return is;
    }

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
    }

    // public FileTime getLastModifiedTime() {
    // return mtime;
    // }

    // public FileTime getLastAccessTime() {
    // return atime;
    // }

    // public FileTime getCreatedtime() {
    // return ctime;
    // }

    public long getCrc() {
        return crc;
    }

    public long getSize() {
        return size;
    }

    public long getCompressedSize() {
        return csize;
    }

    public int getMethod() {
        return method;
    }

    public byte[] getExtra() {
        return extra;
    }

    public String getComment() {
        return comment;
    }

}
