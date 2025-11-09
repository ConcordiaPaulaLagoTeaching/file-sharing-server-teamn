package fileserver.server;

import fileserver.clienthandler.ClientHandler;
import fileserver.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class FileServer {
  private FileSystemManager fsManager;
  private int port;

  public FileServer(int port, String fileSystemName, int totalSize) {
    try {
      this.fsManager = new FileSystemManager(fileSystemName, totalSize);
      this.port = port;
    } catch (Exception e) {
      System.err.println("File system error: " + e.getMessage());
    }
  }

  public void start(){
    try (ServerSocket serverSocket = new ServerSocket(this.port)) {
      System.out.println("Server started. Listening on port " + this.port + "...");

      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(new ClientHandler(clientSocket, fsManager)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Could not start server on port " + port);
    }
  }
}
