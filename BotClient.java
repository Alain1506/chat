package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BotClient extends Client {

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public class BotSocketThread extends SocketThread {

        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            BotClient.this.sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {

            ConsoleHelper.writeMessage(message);

            String[] split = message.split(": ");
            if (split.length != 2) return;
            String name = split[0];
            String text = split[1];

            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat simpleDateFormat = null;

            switch (text) {
                case "дата":
                    simpleDateFormat = new SimpleDateFormat("d.MM.YYYY");
                    break;

                case "день":
                    simpleDateFormat = new SimpleDateFormat("d");
                    break;

                case "месяц":
                    simpleDateFormat = new SimpleDateFormat("MMMM");
                    break;

                case "год":
                    simpleDateFormat = new SimpleDateFormat("YYYY");
                    break;

                case "время":
                    simpleDateFormat = new SimpleDateFormat("H:mm:ss");
                    break;

                case "час":
                    simpleDateFormat = new SimpleDateFormat("H");
                    break;

                case "минуты":
                    simpleDateFormat = new SimpleDateFormat("m");
                    break;

                case "секунды":
                    simpleDateFormat = new SimpleDateFormat("s");
                    break;

                default:
                    return;
            }

            BotClient.this.sendTextMessage(String.format("Информация для %s: %s", name, simpleDateFormat.format(date)));

        }
    }

}
