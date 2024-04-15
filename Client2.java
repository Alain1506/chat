package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client2 {

    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress(){

        ConsoleHelper.writeMessage("Введите адрес сервера");
        String serverAddress = ConsoleHelper.readString();

        return serverAddress;
    }

    protected int getServerPort() {

        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();

        return port;
    }

    protected String getUserName() {

        ConsoleHelper.writeMessage("Введите имя пользователя");
        String userName = ConsoleHelper.readString();

        return userName;
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Произошло инсключение при отправке сообщения");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Во время ожидания возникло исключение");
                return;
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        String message = null;
        while (clientConnected) {
            message = ConsoleHelper.readString();

            if (message.equals("exit")) {break;}

            if (shouldSendTextFromConsole()) {
                sendTextMessage(message);
            }
        }




    }

    public static void main(String[] args) {
        Client2 client = new Client2();
        client.run();
    }

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + "покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client2.this.clientConnected = clientConnected;
            synchronized (Client2.this) {
                Client2.this.notify();
            }
        }

        protected void clientHandshake() throws IOException,ClassNotFoundException {

            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST) {
                    String name = getUserName();
                    Message newMessage = new Message(MessageType.USER_NAME, name);
                    connection.send(newMessage);
                } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    break;
                } else {
                    throw new IOException ("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {

            while (true) {
                Message messageFromServer = connection.receive();
                if (messageFromServer.getType() == MessageType.TEXT) {
                    processIncomingMessage(messageFromServer.getData());
                } else if (messageFromServer.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(messageFromServer.getData());
                } else if (messageFromServer.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(messageFromServer.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        public void run() {

            String serverAddress = getServerAddress();
            int port = getServerPort();

            try {
                Socket socket = new Socket(serverAddress, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }


    }

}
