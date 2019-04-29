package br.com.berkleysocket.server;

import br.com.berkleysocket.view.ServerView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

// SERVER : Multi ServerView
// TIPE : Two-Way Communication (Client to ServerView, ServerView to Client)
// DESCRIPTION :
// A simple server that will accept multi client connection and display everything the client says on the screen.
// The ServerView can handle multiple clients simultaneously.
// The ServerView can sends all text received from any of the connected clients to all clients,
// this means the ServerView has to receive and send, and the client has to send as well as receive.
// If the client user types "exit", the client will quit.
public class Server implements Runnable {

    private int port = 7777;
    private ServerSocket serverSocket = null;
    private Thread thread = null;
    private ChatServerThread clients[] = new ChatServerThread[50];
    private int clientCount = 0;
    private ServerView view = new ServerView();

    public Server() {
        try {
            serverSocket = new ServerSocket(port);
            view.setVisible(true);
            view.addMessage("ServerView started on port " + serverSocket.getLocalPort() + "...");
            view.addMessage("Waiting for client...");
            thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            view.addMessage("Can not bind to port : " + e);
        }
    }

    @Override
    public void run() {
        while (thread != null) {
            try {
                // wait until client socket connecting, then add new thread
                addThreadClient(serverSocket.accept());
            } catch (IOException e) {
                view.addMessage("ServerView accept error : " + e);
                stop();
            }
        }
    }

    private void stop() {
        if (thread != null) {
            thread = null;
        }
    }

    private int findClient(SocketAddress ID) {
        for (int i = 0; i < clientCount; i++) {
            if (clients[i].getID() == ID) {
                return i;
            }
        }
        return -1;
    }

    synchronized void handle(SocketAddress ID, String input) {
        if (input.equals("exit")) {
            clients[findClient(ID)].send("exit");
            remove(ID);
        } else {
            for (int i = 0; i < clientCount; i++) {
                if(clients[i].getID() == ID){
                    // if this client ID is the sender, just skip it
                    continue;
                }
                clients[i].send("\n" + ID + " says : " + input);
            }
        }
    }

    synchronized void remove(SocketAddress ID) {
        int index = findClient(ID);
        if (index >= 0) {
            ChatServerThread threadToTerminate = clients[index];
            view.addMessage("Removing client thread " + ID + " at " + index);
            if (index < clientCount - 1) {
                for (int i = index + 1; i < clientCount; i++) {
                    clients[i - 1] = clients[i];
                }
            }
            clientCount--;
            try {
                threadToTerminate.close();
            } catch (IOException e) {
                view.addMessage("Error closing thread : " + e.getMessage());
            }
        }
    }

    private void addThreadClient(Socket socket) {
        if (clientCount < clients.length) {
            clients[clientCount] = new ChatServerThread(this, socket);
            clients[clientCount].start();
            clientCount++;
        } else {
            view.addMessage("Client refused : maximum " + clients.length + " reached.");
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
    }
}