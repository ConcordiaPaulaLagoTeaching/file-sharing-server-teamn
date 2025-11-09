package fileserver.clienthandler;

import fileserver.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private final FileSystemManager fsManager;

  public ClientHandler(Socket socket, FileSystemManager manager) {
    this.clientSocket = socket;
    this.fsManager = manager;
  }

  @Override
  public void run() {
    System.out.println("Handling client: " + clientSocket);
    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
    ) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("Received from client: " + clientSocket.getInetAddress() + ": " + line);
        String response = handleRequest(line);
        writer.println(response);
      }
    } catch (IOException e) {
      System.out.println("ERROR: " + e.getMessage());
      System.out.println("Error stacktrace: ");
      e.printStackTrace();
    } finally {
      try {
        clientSocket.close();
        System.out.println("Closed client socket: " + clientSocket);
      } catch (IOException e) {}
    }
  }
 
  private String handleRequest(String line) {
    String[] parts = line.split(" ", 3);
    String command = parts[0].toUpperCase();
    String fileName;
    try {
      switch (command) {
        case "CREATE":
          if (parts.length < 2) return "ERROR: CREATE requires a file name.";
          fileName = parts[1];
          fsManager.createFile(fileName);
          return "SUCCESS: File '" + fileName + "' created.";
        case "DELETE":
          if (parts.length < 2) return "ERROR: DELETE requires a file name.";
          fileName = parts[1];
          fsManager.deleteFile(fileName);
          return "SUCCESS: File '" + fileName + "' deleted.";
        case "WRITE":
          if (parts.length < 3) return "ERROR: WRITE requires a file name and content.";
          fileName = parts [1];
          String base64Content = parts[2];
          byte[] contents = Base64.getDecoder().decode(base64Content);
          fsManager.writeFile(fileName, contents);
          return "SUCCSS: Wrote " + contents.length + " bytes to file '" + fileName + "'.";
        case "READ":
          if (parts.length < 2) return "ERROR: READ requires a file name.";
          fileName = parts[1];
          byte[] content = fsManager.readFile(fileName);
          String encodedContent = Base64.getEncoder().encodeToString(content);
          return "READ: " + encodedContent;
        case "LIST":
          String[] files = fsManager.listFiles();
          String list = String.join(", ", files);
          return "SUCCESS: " + list;
        case "EXIT":
        case "QUIT":
          return "SUCCESS: Disconnecting";
        default:
          return "ERROR: Unknown command";
      }
    } catch (Exception e) {
      return "ERROR: " + e.getMessage();
    }
  }
}
