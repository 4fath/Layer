package com.fatihsenturk.try_layer;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.listeners.LayerSyncListener;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.LayerObject;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessageOptions;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.messaging.Metadata;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Hashtable;
import java.util.Random;

/**
 * Created by TOSHIBA on 16.2.2016. Şubat
 * Dont worry !
 */
public class ConversationViewController implements View.OnClickListener, LayerChangeEventListener
        .MainThread, TextWatcher, LayerTypingIndicatorListener, LayerSyncListener {

    private static final String TAG = ConversationViewController.class.getSimpleName();

    private LayerClient layerClient;

    //GUI elements
    private Button sendButton;
    private LinearLayout topBar;
    private EditText userInput;
    private ScrollView conversationScroll;
    private LinearLayout conversationView;
    private TextView typingIndicator;

    //List of all users currently typing
    private ArrayList<String> typingUsers;

    //Current conversation
    private Conversation activeConversation;

    //All messages
    private Hashtable<String, MessageView> allMessages;


    public ConversationViewController(MainActivity ma, LayerClient client){

        layerClient = client;
        layerClient.registerEventListener(this);
        typingUsers = new ArrayList<>();
        ma.setContentView(R.layout.activity_main);


        //Cache off gui objects
        sendButton = (Button) ma.findViewById(R.id.send);
        topBar = (LinearLayout) ma.findViewById(R.id.topbar);
        userInput = (EditText) ma.findViewById(R.id.input);
        conversationScroll = (ScrollView) ma.findViewById(R.id.scrollView);
        conversationView = (LinearLayout) ma.findViewById(R.id.conversation);
        typingIndicator = (TextView) ma.findViewById(R.id.typingIndicator);

        sendButton.setOnClickListener(this);
        topBar.setOnClickListener(this);
        userInput.setText(getInitialMessage());
        userInput.addTextChangedListener(this);

        activeConversation = getConversation();

        drawConversation();

        if (activeConversation != null)
            getTopBarMetaData();

    }



    private void drawConversation() {

        if (activeConversation != null) {

            //Clear the GUI first and empty the list of stored messages
            conversationView.removeAllViews();
            allMessages = new Hashtable<String, MessageView>();

            //Grab all the messages from the conversation and add them to the GUI
            List<Message> allMsgs = layerClient.getMessages(activeConversation);
            for (int i = 0; i < allMsgs.size(); i++) {
                addMessageToView(allMsgs.get(i));
            }

            //After redrawing, force the scroll view to the bottom (most recent message)
            conversationScroll.post(new Runnable() {
                @Override
                public void run() {
                    conversationScroll.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

    }

    private void addMessageToView(Message message) {
        if (message == null || message.getSender() == null || message.getSender().getUserId() == null)
            return;

        if (!message.getSender().getUserId().equalsIgnoreCase(layerClient.getAuthenticatedUserId()))
            message.markAsRead();

        String msgId = message.getId().toString();

        if (!allMessages.contains(msgId)) {
            //Build the GUI element and save it
            MessageView msgView = new MessageView(conversationView, message);
            allMessages.put(msgId, msgView);
        }
    }

    private Conversation getConversation() {

        if (activeConversation == null) {

            Query query = Query.builder(Conversation.class)
                    .predicate(new Predicate(Conversation.Property.PARTICIPANTS, Predicate
                            .Operator.EQUAL_TO, MainActivity.getAllParticipants()))
                    .sortDescriptor(new SortDescriptor(Conversation.Property.CREATED_AT,
                            SortDescriptor.Order.DESCENDING)).build();

            List<Conversation> results = layerClient.executeQuery(query, Query.ResultType.OBJECTS);
            if (results != null && results.size() > 0) {
                return results.get(0);
            }
        }
        return activeConversation;
    }

    public static String getInitialMessage() {
        return "Hey, everyone! This is your friend, " + MainActivity.getUserID();
    }

    private void getTopBarMetaData() {
        if (activeConversation != null) {

            Metadata current = activeConversation.getMetadata();
            if (current.containsKey("backgroundColor")) {

                Metadata colors = (Metadata) current.get("backgroundColor");

                if (colors != null) {

                    float red = Float.parseFloat((String) colors.get("red"));
                    float green = Float.parseFloat((String) colors.get("green"));
                    float blue = Float.parseFloat((String) colors.get("blue"));

                    setTopBarColor(red, green, blue);
                }
            }
        }
    }

    private void setTopBarColor(float red, float green, float blue) {
        if (topBar != null) {
            topBar.setBackgroundColor(Color.argb(255, (int) (255.0f * red), (int) (255.0f *
                    green), (int) (255.0f * blue)));
        }
    }

    @Override
    public void onClick(View v) {
        //When the "send" button is clicked, grab the ongoing conversation (or create it) and
        // send the message
        if (v == sendButton) {
            sendButtonClicked();
        }

        //When the Layer logo bar is clicked, randomly change the color and store it in the
        // conversation's metadata
        if (v == topBar) {
            topBarClicked();
        }
    }

    private void topBarClicked() {

        Random r = new Random();
        float red = r.nextFloat();
        float green = r.nextFloat();
        float blue = r.nextFloat();

        setTopBarMetaData(red, green, blue);
        setTopBarColor(red, green, blue);

    }

    private void setTopBarMetaData(float red, float green, float blue) {
        if (activeConversation == null) {
            activeConversation = getConversation();

            //If there isn't, create a new conversation with those participants
            if (activeConversation == null) {
                activeConversation = layerClient.newConversation(MainActivity.getAllParticipants());
            }
        }

        sendMessage(userInput.getText().toString());

        //Clears the text input field
        userInput.setText("");
    }

    private void sendMessage(String text) {
        MessagePart messagePart = layerClient.newMessagePart(text);

        //Formats the push notification that the other participants will receive
        MessageOptions options = new MessageOptions();
        options.pushNotificationMessage(MainActivity.getUserID() + ": " + text);

        //Creates and returns a new message object with the given conversation and array of
        // message parts
        Message message = layerClient.newMessage(options, Arrays.asList(messagePart));

        //Sends the message
        if (activeConversation != null)
            activeConversation.send(message);
    }

    private void sendButtonClicked() {

        if (activeConversation == null) {
            activeConversation = getConversation();

            //If there isn't, create a new conversation with those participants
            if (activeConversation == null) {
                activeConversation = layerClient.newConversation(MainActivity.getAllParticipants());
            }
        }

        sendMessage(userInput.getText().toString());

        //Clears the text input field
        userInput.setText("");

    }


    @Override
    public void onBeforeSync(LayerClient layerClient) {
        Log.v(TAG, "Sync starting");
    }

    @Override
    public void onSyncProgress(LayerClient layerClient, int pctComplete) {
        Log.v(TAG, "Sync is " + pctComplete + "% Complete");
    }

    @Override
    public void onAfterSync(LayerClient layerClient) {
        Log.v(TAG, "Sync complete");
    }

    @Override
    public void onSyncError(LayerClient layerClient, List<LayerException> list) {
        for(LayerException e : list){
            Log.v(TAG, "onSyncError: " + e.toString());
        }
    }

    @Override
    public void onTypingIndicator(LayerClient layerClient, Conversation conversation, String userID, TypingIndicator indicator) {

        if (conversation != activeConversation)
            return;

        switch (indicator) {
            case STARTED:
                // This user started typing, so add them to the typing list if they are not
                // already on it.
                if (!typingUsers.contains(userID))
                    typingUsers.add(userID);
                break;

            case FINISHED:
                // This user isn't typing anymore, so remove them from the list.
                typingUsers.remove(userID);
                break;
        }

        if (typingUsers.size() == 0) {

            //No one is typing, so clear the text
            typingIndicator.setText("");

        } else if (typingUsers.size() == 1) {

            //Name the one user that is typing (and make sure the text is grammatically correct)
            typingIndicator.setText(typingUsers.get(0) + " is typing");

        } else if (typingUsers.size() > 1) {

            //Name all the users that are typing (and make sure the text is grammatically correct)
            String users = "";
            for (int i = 0; i < typingUsers.size(); i++) {
                users += typingUsers.get(i);
                if (i < typingUsers.size() - 1)
                    users += ", ";
            }

            typingIndicator.setText(users + " are typing");
        }


    }

    @Override
    public void onEventMainThread(LayerChangeEvent layerChangeEvent) {

        List<LayerChange> changes = layerChangeEvent.getChanges();
        for (int i = 0; i < changes.size(); i++) {
            LayerChange change = changes.get(i);
            if (change.getObjectType() == LayerObject.Type.CONVERSATION) {

                Conversation conversation = (Conversation) change.getObject();
                Log.v(TAG, "Conversation " + conversation.getId() + " attribute " +
                        change.getAttributeName() + " was changed from " + change.getOldValue() +
                        " to " + change.getNewValue());

                switch (change.getChangeType()) {
                    case INSERT:
                        break;

                    case UPDATE:
                        break;

                    case DELETE:
                        break;
                }

            } else if (change.getObjectType() == LayerObject.Type.MESSAGE) {

                Message message = (Message) change.getObject();
                Log.v(TAG, "Message " + message.getId() + " attribute " + change
                        .getAttributeName() + " was changed from " + change.getOldValue() + " to " +
                        "" + change.getNewValue());

                switch (change.getChangeType()) {
                    case INSERT:
                        break;

                    case UPDATE:
                        break;

                    case DELETE:
                        break;
                }
            }
        }

        //If we don't have an active conversation, grab the oldest one
        if (activeConversation == null)
            activeConversation = getConversation();

        //If anything in the conversation changes, re-draw it in the GUI
        drawConversation();

        //Check the meta-data for color changes
        getTopBarMetaData();

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (activeConversation != null)
            activeConversation.send(TypingIndicator.STARTED);
    }
}
