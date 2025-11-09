package fileserver.filesystem;

import fileserver.datastructures.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemManager {
  private RandomAccessFile disk;
  private FEntry[] inodeTable; // Array of inodes
  private boolean[] freeBlockList; // Bitmap for free blocks
  private int writeStartOffset;
  private int fileSize = 20;
  private static FileSystemManager instance;
  private final int maxfiles = 5;
  private final int maxblocks = 10;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final static int BLOCK_SIZE = 128;

  public FileSystemManager(String filename, int totalSize) {
    if (instance == null) {
      readWriteLock.writeLock().lock();
      try {
        if (instance == null) {
          this.inodeTable = new FEntry[maxfiles];
          this.freeBlockList = new boolean[maxblocks];
          this.writeStartOffset = maxfiles * fileSize + maxblocks;

          File file = new File(filename);
          if (!file.exists()) {
            System.out.println("Creating a new file system: " + filename);
            this.disk = new RandomAccessFile(file, "rw");
            this.disk.setLength(totalSize);
            Arrays.fill(this.freeBlockList, true);
          } else {
            System.out.println("Repurposing the existing file system: " + filename);
            Arrays.fill(this.freeBlockList, true);
            this.disk = new RandomAccessFile(file, "rw");
          }
        }
      } catch (Exception e) {
        System.out.println("Error loading the file system: " + e.getMessage());
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } else {
      throw new IllegalStateException("FileSystemManager is already initialized.");
    }
  }

  private long getBlockOffset(int blockIndex) { return writeStartOffset + (long)blockIndex * BLOCK_SIZE; }

  private int findFileInode(String fileName) {
    for (int i = 0; i < maxfiles; i++) {
      if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) return i;
    }
    return -1;
  }

  private int findFreeBlock() {
    for (int i = 0; i < maxblocks; i++) {
      if (freeBlockList[i]) return i;
    }
    return -1;
  }

  public void createFile(String fileName) throws Exception {
    readWriteLock.writeLock().lock();
    try {
      if (findFileInode(fileName) != -1) throw new Exception("File already exists: " + fileName);

      int inodeIndex;
      for (inodeIndex = 0; inodeIndex < maxfiles; inodeIndex++) {
        if (inodeTable[inodeIndex] == null) break;
      }
      if (inodeIndex == maxfiles) throw new Exception("No free inode slot available.");

      FEntry newFile = new FEntry(fileName, (short)0, (short)-1);
      inodeTable[inodeIndex] = newFile;

      System.out.println("Created file: " + fileName + " at inode index " + inodeIndex);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public void deleteFile(String fileName) throws Exception {
    readWriteLock.writeLock().lock();
    try {
      int inodeIndex = findFileInode(fileName);
      if (inodeIndex == -1) throw new Exception("File not found: " + fileName);

      FEntry fileEntry = inodeTable[inodeIndex];
      short currentBlock = fileEntry.getFirstBlock();

      if (currentBlock >= 0 && currentBlock < maxblocks) freeBlockList[currentBlock] = true;

      inodeTable[inodeIndex] = null;

      System.out.println("Deleted file: " + fileName);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public void writeFile(String fileName, byte[] contents) throws Exception {
    readWriteLock.writeLock().lock();
    try {
      if (contents.length > BLOCK_SIZE) throw new Exception("Content size exceeds single block limit(" + BLOCK_SIZE + " bytes).");

      int inodeIndex = findFileInode(fileName);
      if (inodeIndex == -1) throw new Exception("File not found: " + fileName);

      FEntry fileEntry = inodeTable[inodeIndex];
      short blockIndex = fileEntry.getFirstBlock();

      if (blockIndex == -1) {
        blockIndex = (short)findFreeBlock();
        if (blockIndex == -1) throw new Exception("No free block available.");
      }
      freeBlockList[blockIndex] = false;

      long offset = getBlockOffset(blockIndex);
      disk.seek(offset);
      disk.write(contents);

      fileEntry.setFirstBlock(blockIndex);
      fileEntry.setFilesize((short)contents.length);

      System.out.println("Wrote " + contents.length + " bytes to file: " + fileName + " at " + blockIndex);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public byte[] readFile(String fileName) throws Exception {
    readWriteLock.readLock().lock();
    try {
      int inodeIndex = findFileInode(fileName);
      if (inodeIndex == -1) throw new Exception("file not found: " + fileName);

      FEntry fileEntry = inodeTable[inodeIndex];
      short blockIndex = fileEntry.getFirstBlock();
      int fileSize = fileEntry.getFilesize();

      if (blockIndex == -1 || fileSize == 0) {
        System.out.println("ERROR: File at index " + blockIndex + " has size: " + fileSize);
        return new byte[0];
      }

      long offset = getBlockOffset(blockIndex);
      disk.seek(offset);

      byte[] content = new byte[fileSize];
      int bytesRead = disk.read(content);

      if (bytesRead != fileSize) throw new IOException("Corrupted file meta data.");

      System.out.println("Read " + bytesRead + " bytes from file: " + fileName);
      return content;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  public String[] listFiles() throws Exception {
    readWriteLock.readLock().lock();
    try {
      List<String> fileNames = new ArrayList<>();
      for (FEntry entry : inodeTable) {
        if (entry != null) fileNames.add(entry.getFilename());
      }

      System.out.println("Listed " + fileNames.size() + " files.");
      return fileNames.toArray(new String[0]);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }
}
