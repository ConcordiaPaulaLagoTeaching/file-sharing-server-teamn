package fileserver;

import fileserver.server.FileServer;

public class Main {
  public static void main(String[] args) {
    System.out.println("Hello and welcome!");

    FileServer server = new FileServer(12345, "fileserver/prog_assign.fs", 10 * 128);
    server.start();
  }
}
