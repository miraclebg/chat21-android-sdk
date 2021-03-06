package org.chat21.android.ui.messages.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.vanniktech.emoji.EmojiTextView;

import org.chat21.android.R;
import org.chat21.android.core.ChatManager;
import org.chat21.android.core.messages.models.Message;
import org.chat21.android.ui.ChatUI;
import org.chat21.android.ui.messages.activities.ImageDetailsActivity;
import org.chat21.android.ui.messages.listeners.OnMessageClickListener;
import org.chat21.android.utils.StringUtils;
import org.chat21.android.utils.TimeUtils;
import org.chat21.android.utils.image.ImageUtils;
import org.chat21.android.utils.views.TextViewLinkHandler;

import java.util.Date;
import java.util.Map;

//import com.bumptech.glide.load.resource.drawable.GlideDrawable;
//import com.bumptech.glide.request.RequestListener;
//import com.bumptech.glide.request.target.Target;

/**
 * Created by stefano on 25/11/2016.
 */

class RecipientViewHolder extends RecyclerView.ViewHolder {

    private final EmojiTextView mMessage;
    private final TextView mDate;
    private final TextView mTimestamp;
    private final ConstraintLayout mBackgroundBubble;
    private final TextView mSenderDisplayName;
    private final ImageView mPreview; // Resolve Issue #32
    private final ProgressBar mProgressBar;   // Resolve Issue #52

    RecipientViewHolder(View itemView) {
        super(itemView);
        mMessage = itemView.findViewById(R.id.message);
        mDate = itemView.findViewById(R.id.date);
        mTimestamp = itemView.findViewById(R.id.timestamp);
        mBackgroundBubble = itemView.findViewById(R.id.message_group);
        mSenderDisplayName = itemView.findViewById(R.id.sender_display_name);
        mPreview = itemView.findViewById(R.id.preview); // Resolve Issue #32
        mProgressBar = itemView.findViewById(R.id.progress);  // Resolve Issue #52
    }

    void bind(final Message previousMessage, final Message message,
              int position, OnMessageClickListener onMessageClickListener) {

        Log.d("TAG", "RecipientViewHolder");

        switch (message.getType()) {
            case Message.TYPE_IMAGE:
                mMessage.setVisibility(View.GONE);
                mPreview.setVisibility(View.VISIBLE);
                setPreview(message);

                break;
            case Message.TYPE_FILE:
                mMessage.setVisibility(View.GONE);
                mPreview.setVisibility(View.VISIBLE);

                setFilePreview(message);

                break;
            case Message.TYPE_AUDIO:
                mMessage.setVisibility(View.GONE);
                mPreview.setVisibility(View.VISIBLE);

                setAudioPreview(message);

                break;
            case Message.TYPE_TEXT:
                mProgressBar.setVisibility(View.GONE);  // Resolve Issue #52

                mMessage.setVisibility(View.VISIBLE);
                mMessage.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/Montserrat-Regular.otf"));
                mPreview.setVisibility(View.GONE);
                setMessage(message);
                break;
        }

        setBubble();

        setDate(previousMessage, message, position);

        setTimestamp(message);

        setSenderDisplayName(message);

        // click on the item
        setOnMessageClickListener(onMessageClickListener);
    }

    private String getImageUrl(Message message) {
        String imgUrl = "";

        Map<String, Object> metadata = message.getMetadata();
        if (metadata != null) {
            imgUrl = (String) metadata.get("src");
        }

        return imgUrl;
    }

    // Resolve Issue #32
    private void setPreview(final Message message) {

        // Resolve Issue #52
        mProgressBar.setVisibility(View.VISIBLE);

        Glide.with(itemView.getContext()).load(message.getImageSrc()).addListener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                mProgressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

                //if you want to convert the drawable to ImageView
                Bitmap bitmapImage = ((BitmapDrawable) resource).getBitmap();

                mProgressBar.setVisibility(View.GONE);

                return false;
            }
        }).into(mPreview);

        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImagePreviewActivity(message);
            }
        });
    }

    private void setAudioPreview(final Message message) {

        mPreview.setImageResource(R.drawable.play_circle_outline_black);

        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = message.getAudioUri();

                if (uri != null) {
                    ChatManager.getInstance().playAudio(uri);
                }
            }
        });
    }

    private void setFilePreview(final Message message) {

        Glide.with(itemView.getContext())
                .load(message.getActualText())
                .placeholder(R.drawable.ic_placeholder_file_recipient_24dp)
                .into(mPreview);


        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: 06/09/17 aprire il file in base al mime
            }
        });
    }

    private void startImagePreviewActivity(Message message) {
        Intent intent = new Intent(itemView.getContext(), ImageDetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ChatUI.BUNDLE_MESSAGE, message);
        itemView.getContext().startActivity(intent);
    }

    private void setMessage(Message message) {
        // set message text
        mMessage.setText(Html.fromHtml(message.getActualText()));
        // clickable link support

        //        con LinkMovementMethod.getInstance() nn funziona
//        mMessage.setMovementMethod(LinkMovementMethod.getInstance()); // clickable link support
    }

    private void setTimestamp(Message message) {
        mTimestamp.setText(TimeUtils.formatTimestamp(message.getTimestamp(), "HH:mm"));
        mTimestamp.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/Montserrat-Regular.otf"));
    }

    private void setDate(Message previousMessage, Message message, int position) {
        Date previousMessageDate = null;
        if (previousMessage != null) {
            previousMessageDate = new Date(previousMessage.getTimestamp());
        }

        Date messageDate = new Date(message.getTimestamp());
        // it's today. show the label "today"
        if (TimeUtils.isDateToday(message.getTimestamp())) {
            mDate.setText(itemView.getContext().getString(R.string.today));
        } else {
            // it's not today. shows the week of day label
            mDate.setText(TimeUtils.getFormattedTimestamp(itemView.getContext(), message.getTimestamp()));
        }

        // hides or shows the date label
        if (previousMessageDate != null && position > 0) {
            if (TimeUtils.getDayOfWeek(messageDate)
                    .equals(TimeUtils.getDayOfWeek(previousMessageDate))) {
                mDate.setVisibility(View.GONE);
            } else {
                mDate.setVisibility(View.VISIBLE);
            }
        } else {
            mDate.setVisibility(View.VISIBLE);
        }

        mDate.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/Montserrat-Bold.otf"));
    }

    private void setBubble() {
        // set bubble color and background
        Drawable drawable = ImageUtils.changeDrawableColor(itemView.getContext(),
                R.color.background_bubble_recipient, R.drawable.balloon_recipient);
        mBackgroundBubble.setBackground(drawable);
    }

    private void setSenderDisplayName(Message message) {

        if (message.isGroupChannel()) {
            mSenderDisplayName.setVisibility(View.VISIBLE);

            String senderDisplayName = StringUtils.isValid(message.getSenderFullname()) ?
                    message.getSenderFullname() : message.getSender();
            mSenderDisplayName.setText(senderDisplayName);
        } else if (message.isDirectChannel()) {
            mSenderDisplayName.setVisibility(View.GONE);
        } else {
            // default case: consider it as direct message
            mSenderDisplayName.setVisibility(View.GONE);
        }
    }

    private void setOnMessageClickListener(final OnMessageClickListener callback) {

        mMessage.setMovementMethod(new TextViewLinkHandler() {
            @Override
            public void onLinkClick(ClickableSpan clickableSpan) {
                callback.onMessageLinkClick(mMessage, clickableSpan);
            }
        });
    }
}