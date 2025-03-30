package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "-"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "-"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "-"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo she;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);

            showMainMenu("Start", "/start",
                    "Profile", "/profile",
                    "Opener", "/opener",
                    "Message", "/message",
                    "Date", "/date",
                    "ChatGPT", "/gpt");
            return;
        }

        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");

            Message msg = sendTextMessage("Подаждите chatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        if (message.equals("/data")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендая", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);

                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Подаждите девушка набирает текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            String text = loadMessage("message");
            sendTextButtonsMessage(text,
                    "Следующие сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage("Подаждите chatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
                return;
            }

            list.add(message);
            return;
        }

        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("У вас есть хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам НЕ нравится в людях?");
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакоства?");
                case 5:
                    me.goals = message;
                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage("Подаждите chatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Имя девушки?");
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount){
                case 1:
                    she.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = message;
                    questionCount = 3;
                    sendTextMessage("Есть лт у неё хобби, какие?");
                    return;
                case 3:
                    she.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;

                    String aboutFriend = message;
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage("Подаждите chatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        sendTextMessage("*Hello!*");
        sendTextMessage("_Hello!_");

        sendTextMessage(message);

        sendTextButtonsMessage("Choice: ",
                "Start", "start",
                "Stop", "stop");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
