package com.fatihsenturk.try_layer;

import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by TOSHIBA on 16.2.2016. Şubat
 * Dont worry !
 */
public class MessageView {

    //The parent object (in this case, a LinearLayout object with a ScrollView parent)
    private LinearLayout myParent;

    //The sender and message views
    private TextView senderTV;
    private TextView messageTV;

    private ImageView statusImage;

    private LinearLayout messageDetails;

    public MessageView(LinearLayout parent, Message message) {

        myParent = parent;

        messageDetails = new LinearLayout(parent.getContext());
        messageDetails.setOrientation(LinearLayout.HORIZONTAL);
        myParent.addView(messageDetails);

        //Creates the sender text view, sets the text to be italic, and attaches it to the parent
        senderTV = new TextView(parent.getContext());
        senderTV.setTypeface(null, Typeface.ITALIC);
        messageDetails.addView(senderTV);

        //Creates the message text view and attaches it to the parent
        messageTV = new TextView(parent.getContext());
        myParent.addView(messageTV);

        //The status is displayed with an icon, depending on whether the message has been read,
        // delivered, or sent
        //statusImage = new ImageView(parent.getContext());
        statusImage = createStatusImage(message);//statusImage.setImageResource(R.drawable.sent);
        messageDetails.addView(statusImage);

        //Populates the text views
        UpdateMessage(message);

    }

    private ImageView createStatusImage(Message message) {
        ImageView status = new ImageView(myParent.getContext());

        switch (getMessageStatus(message)) {

            case SENT:
                status.setImageResource(R.drawable.sent);
                break;

            case DELIVERED:
                status.setImageResource(R.drawable.delivered);
                break;

            case READ:
                status.setImageResource(R.drawable.read);
                break;
        }

        //Have the icon fill the space vertically
        status.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams
                .WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

        return status;

    }

    private Message.RecipientStatus getMessageStatus(Message msg) {

        if (msg == null || msg.getSender() == null || msg.getSender().getUserId() == null)
            return Message.RecipientStatus.PENDING;

        if (!msg.getSender().getUserId().equalsIgnoreCase(MainActivity.getUserID()))
            return Message.RecipientStatus.READ;

        Message.RecipientStatus status = Message.RecipientStatus.SENT;

        for (int i = 0; i < MainActivity.getAllParticipants().size(); i++) {

            //Don't check the status of the current user
            String participant = MainActivity.getAllParticipants().get(i);
            if (participant.equalsIgnoreCase(MainActivity.getUserID()))
                continue;

            if (status == Message.RecipientStatus.SENT) {

                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.DELIVERED)
                    status = Message.RecipientStatus.DELIVERED;

                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.READ)
                    return Message.RecipientStatus.READ;

            } else if (status == Message.RecipientStatus.DELIVERED) {
                if (msg.getRecipientStatus(participant) == Message.RecipientStatus.READ)
                    return Message.RecipientStatus.READ;
            }
        }

        return status;
    }

    private void UpdateMessage(Message message) {
        String senderTxt = craftSenderText(message);
        String msgTxt = craftMsgText(message);

        senderTV.setText(senderTxt);
        messageTV.setText(msgTxt);
    }

    private String craftMsgText(Message msg) {
        //The message text
        String msgText = "";

        //Go through each part, and if it is text (which it should be by default), append it to the
        // message text
        List<MessagePart> parts = msg.getMessageParts();
        for (int i = 0; i < msg.getMessageParts().size(); i++) {

            //You can always set the mime type when creating a message part, by default the mime
            // type is initialized to plain text when the message part is created
            if (parts.get(i).getMimeType().equalsIgnoreCase("text/plain")) {
                try {
                    msgText += new String(parts.get(i).getData(), "UTF-8") + "\n";
                } catch (UnsupportedEncodingException e) {
                    // we catch the errroor
                }
            }
        }

        //Return the assembled text
        return msgText;

    }

    private String craftSenderText(Message msg) {
        if (msg == null)
            return "";

        //The User ID
        String senderTxt = "";
        if (msg.getSender() != null && msg.getSender().getUserId() != null)
            senderTxt = msg.getSender().getUserId();

        //Add the timestamp
        if (msg.getSentAt() != null) {
            senderTxt += " @ " + new SimpleDateFormat("HH:mm:ss").format(msg.getReceivedAt());
        }

        //Add some formatting before the status icon
        senderTxt += "   ";

        //Return the formatted text
        return senderTxt;
    }
}
