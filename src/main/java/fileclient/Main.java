package fileclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
  private static void handleReadResponse(String response) {
    if (response.startsWith("READ: ")) {
      try {
        String encodedContent = response.substring("READ: ".length());
        byte[] content = Base64.getDecoder().decode(encodedContent);
        String dataString = new String(content, StandardCharsets.UTF_8);
        System.out.println(dataString);
      } catch (IllegalArgumentException e) {
        System.out.println("ERROR: Decoding file content.");
      }
    } else {
      System.out.println("Response from server: " + response);
    }
  }

  private static String createWriteRequest(String fileName, String content) {
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    String encodedContent = Base64.getEncoder().encodeToString(contentBytes);
    return "WRITE " + fileName + " " + encodedContent;
  }

  public static void main(String[] args) {
    //Socket CLient
    System.out.println("Hello and welcome!");
    Scanner scanner = new Scanner(System.in);

    try {
      Socket clientSocket = new Socket("localhost", 12345);
      System.out.println("Connected to the server at localhost:12345.");

      //read user input from console
      String userInput = scanner.nextLine();

      try (
          BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
      ) {
        while (userInput != null && !userInput.isEmpty() && !userInput.equalsIgnoreCase("exit") && !userInput.equalsIgnoreCase("quit")) {
          String userCommand = userInput.trim();

          if (userCommand.toUpperCase().startsWith("WRITE ")) {
            String[] parts = userCommand.split(" ", 3);
            if (parts.length == 3) {
              userCommand = createWriteRequest(parts[1], parts[2]);
            }
          }

          writer.println(userCommand);
          System.out.println("Message sent to the server: " + userCommand);
          //get response
          String response = reader.readLine();

          if (userCommand.toUpperCase().startsWith("READ ")) {
            handleReadResponse(response);
          } else {
            System.out.println("Response from server: " + response);
          }

          userInput = scanner.nextLine(); // Read next line
        }

        // Close the socket
        clientSocket.close();
        System.out.println("Connection closed.");
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        scanner.close();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
