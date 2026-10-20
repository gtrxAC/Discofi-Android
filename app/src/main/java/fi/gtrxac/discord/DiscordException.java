package fi.gtrxac.discord;

import android.content.Context;
import android.util.Log;
import android.app.AlertDialog;
import java.util.*;

public class DiscordException extends Exception {
    public String friendlyMessage;
    public String message;
    public Vector tips;

    public DiscordException(Exception exception) {
        this(exception.toString());
    }

    public DiscordException(String message) {
        this.message = message;
        this.tips = new Vector();

        if (message.equals("401: Unauthorized")) {
            this.friendlyMessage = "Invalid session";
            this.tips.addElement("log in again");
            this.tips.addElement("if you're using token login, make sure the token is written correctly");
            this.tips.addElement("use a different connection method");
            this.tips.addElement("use a different account");
        }
        else if (message.startsWith("java.io.IOException") || message.indexOf("UnknownHost") != -1 || message.indexOf("SocketException") != -1) {
            this.friendlyMessage = "Failed to load data";
            this.tips.addElement("try again");
            this.tips.addElement("check that your internet connection is working");
            this.tips.addElement("check that other online apps can connect");
            this.tips.addElement("if you're using mobile data, check that your APN settings are correct");
        }
        else {
            this.friendlyMessage = "Error";
        }
    }

    public DiscordException(String friendlyMessage, String message, Vector tips) {
        this.friendlyMessage = friendlyMessage;
        this.message = message;
        this.tips = tips;
    }

    public String getTitle() {
        return (friendlyMessage != null) ? friendlyMessage : message;
    }

    public String getDisplayMessage() {
        StringBuilder msg = new StringBuilder();
        if (tips != null && !tips.isEmpty()) {
            msg.append("Please try the following:\n");
            for (int i = 0; i < tips.size(); i++) {
                String tip = (String) tips.elementAt(i);
                msg.append("- ").append(tip).append("\n");
            }
        }
        if (friendlyMessage != null) msg.append("\nDetails:\n").append(message);
        return msg.toString();
    }

    public void show(Context context) {
        Log.e("DiscordException", message, this);
        new AlertDialog.Builder(context)
            .setTitle(getTitle())
            .setMessage(getDisplayMessage())
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    public String toString() {
        return friendlyMessage + " (" + message + ")";
    }
}
