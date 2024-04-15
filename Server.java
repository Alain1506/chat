package com.javarush.task.task30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static void main(String[] args) {

        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Cервер запущен.");
            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске или работе сервера:" + e.getMessage());
        }

    }

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        for (String name : connectionMap.keySet()) {
            try {
                connectionMap.get(name).send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Сообщение не было отправлено");
            }
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом: " + socket.getRemoteSocketAddress());

            String userName = null;

            try (Connection connection = new Connection(socket)) {

                userName = serverHandshake(connection);

                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName)); //оповещает юзеров о добавлении пользователя

                notifyUsers(connection,userName); //оповещает добавившегося о юзерах в чате

                serverMainLoop(connection, userName);

            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом: " + e.getMessage());
            }


            if (userName != null) {
                connectionMap.remove(userName);
            }

            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));

            ConsoleHelper.writeMessage("Cоединение с удаленным адресом закрыто.");

        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            while (true) {
                Message nameRequest = new Message(MessageType.NAME_REQUEST);
                connection.send(nameRequest);
                Message response = connection.receive();

                String userName = response.getData();

                if (response.getType() != MessageType.USER_NAME || userName.isEmpty() || connectionMap.containsKey(userName)) {
                    continue;
                }

                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));

                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {

            for (String name : connectionMap.keySet()) {
                if (name.equals(userName)) {
                    continue;
                }
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
//                else if (message.getType() == MessageType.BYEBYE) {
//                    break;
//                }
                else {
                    ConsoleHelper.writeMessage("Введенное сообщение не корректно");
                }
            }
        }


    }

}
