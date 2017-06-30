/*
 * 	Copyright (c) 2017. Toshi Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.toshi.view.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.toshi.R;
import com.toshi.model.local.Recipient;
import com.toshi.model.local.User;
import com.toshi.model.sofa.Message;
import com.toshi.model.sofa.Payment;
import com.toshi.model.sofa.PaymentRequest;
import com.toshi.model.sofa.SofaAdapters;
import com.toshi.model.sofa.SofaMessage;
import com.toshi.model.sofa.SofaType;
import com.toshi.util.LogUtil;
import com.toshi.view.BaseApplication;
import com.toshi.view.adapter.listeners.OnItemClickListener;
import com.toshi.view.adapter.viewholder.FileViewHolder;
import com.toshi.view.adapter.viewholder.ImageViewHolder;
import com.toshi.view.adapter.viewholder.PaymentRequestViewHolder;
import com.toshi.view.adapter.viewholder.PaymentViewHolder;
import com.toshi.view.adapter.viewholder.TextViewHolder;
import com.toshi.view.adapter.viewholder.TimestampMessageViewHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public final class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static int SENDER_MASK = 0x1000;

    private final List<SofaMessage> sofaMessages;
    private OnItemClickListener<SofaMessage> onPaymentRequestApproveListener;
    private OnItemClickListener<SofaMessage> onPaymentRequestRejectListener;
    private OnItemClickListener<String> onUsernameClickListener;
    private OnItemClickListener<String> onImageClickListener;
    private OnItemClickListener<String> onFileClickListener;
    private Recipient recipient;

    public MessageAdapter() {
        this.sofaMessages = new ArrayList<>();
    }

    public final MessageAdapter addOnPaymentRequestApproveListener(final OnItemClickListener<SofaMessage> listener) {
        this.onPaymentRequestApproveListener = listener;
        return this;
    }

    public final MessageAdapter addOnPaymentRequestRejectListener(final OnItemClickListener<SofaMessage> listener) {
        this.onPaymentRequestRejectListener = listener;
        return this;
    }

    public final MessageAdapter addOnUsernameClickListener(final OnItemClickListener<String> listener) {
        this.onUsernameClickListener = listener;
        return this;
    }

    public final MessageAdapter addOnImageClickListener(final OnItemClickListener<String> listener) {
        this.onImageClickListener = listener;
        return this;
    }

    public final MessageAdapter addOnFileClickListener(final OnItemClickListener<String> listener) {
        this.onFileClickListener = listener;
        return this;
    }

    private boolean shouldShowChatMessage(final SofaMessage sofaMessage) {
        return sofaMessage.getType() != SofaType.UNKNOWN
                && sofaMessage.getType() != SofaType.INIT
                && sofaMessage.getType() != SofaType.INIT_REQUEST;
    }

    public MessageAdapter setMessages(final List<SofaMessage> messages) {
        final List<SofaMessage> messagesToAdd = messages == null
                ? new ArrayList<>(0)
                : messages;
        addMessages(messagesToAdd);
        notifyItemInserted(this.sofaMessages.size() - 1);
        return this;
    }

    public MessageAdapter setRecipient(final Recipient recipient) {
        this.recipient = recipient;
        return this;
    }

    private void addMessages(final Collection<SofaMessage> sofaMessages) {
        this.sofaMessages.clear();
        notifyDataSetChanged();

        for (SofaMessage sofaMessage : sofaMessages) {
            if (shouldShowChatMessage(sofaMessage)) {
                this.sofaMessages.add(sofaMessage);
                notifyItemInserted(this.sofaMessages.size() - 1);
            }
        }
    }

    private void addMessage(final SofaMessage sofaMessage) {
        if (!shouldShowChatMessage(sofaMessage)) return;
        this.sofaMessages.add(sofaMessage);
        notifyItemInserted(this.sofaMessages.size() - 1);
    }

    public final void updateMessage(final SofaMessage sofaMessage) {
        if (!shouldShowChatMessage(sofaMessage)) return;
        final int position = this.sofaMessages.indexOf(sofaMessage);
        if (position == -1) {
            addMessage(sofaMessage);
            return;
        }

        this.sofaMessages.set(position, sofaMessage);
        notifyItemChanged(position);
    }

    @Override
    public int getItemViewType(final int position) {
        final SofaMessage sofaMessage = this.sofaMessages.get(position);
        final @SofaType.Type int sofaType = sofaMessage.hasAttachment() ? sofaMessage.getAttachmentType() : sofaMessage.getType();
        return sofaMessage.isSentBy(getCurrentLocalUser()) ? sofaType : sofaType | SENDER_MASK;
    }

    @Override
    public final RecyclerView.ViewHolder onCreateViewHolder(
            final ViewGroup parent,
            final int viewType) {

        final boolean isRemote = viewType >= SENDER_MASK;
        final int messageType = isRemote ? viewType ^ SENDER_MASK : viewType;

        switch (messageType) {

            case SofaType.PAYMENT_REQUEST: {
                final View v = isRemote
                        ? LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__request_remote, parent, false)
                        : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__request_local, parent, false);
                return new PaymentRequestViewHolder(v);
            }

            case SofaType.PAYMENT: {
                final View v = isRemote
                        ? LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__payment_remote, parent, false)
                        : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__payment_local, parent, false);
                return new PaymentViewHolder(v);
            }

            case SofaType.IMAGE: {
                final View v = isRemote
                        ? LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__image_message_remote, parent, false)
                        : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__image_message_local, parent, false);
                return new ImageViewHolder(v);
            }

            case SofaType.TIMESTAMP: {
                final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__timestamp, parent, false);
                return new TimestampMessageViewHolder(v);
            }

            case SofaType.FILE: {
                final View v = isRemote
                        ? LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__file_message_remote, parent, false)
                        : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__file_message_local, parent, false);
                return new FileViewHolder(v);
            }

            case SofaType.UNKNOWN:
            case SofaType.PLAIN_TEXT:
            default: {
                final View v = isRemote
                    ? LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__text_message_remote, parent, false)
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item__text_message_local, parent, false);
                return new TextViewHolder(v);
            }
        }
    }

    @Override
    public final void onBindViewHolder(
            final RecyclerView.ViewHolder holder,
            final int position) {

        final SofaMessage sofaMessage = this.sofaMessages.get(position);
        final String payload = sofaMessage.getPayload();
        if (payload == null) return;

        try {
            renderChatMessageIntoViewHolder(holder, sofaMessage, payload);
        } catch (final IOException ex) {
            LogUtil.error(getClass(), "Unable to render view holder: " + ex);
        }
    }

    private void renderChatMessageIntoViewHolder(
            final RecyclerView.ViewHolder holder,
            final SofaMessage sofaMessage,
            final String payload) throws IOException {

        final boolean isRemote = holder.getItemViewType() >= SENDER_MASK;
        final int messageType = isRemote ? holder.getItemViewType() ^ SENDER_MASK : holder.getItemViewType();

        switch (messageType) {
            case SofaType.COMMAND_REQUEST:
            case SofaType.PLAIN_TEXT: {
                final TextViewHolder vh = (TextViewHolder) holder;
                final Message message = SofaAdapters.get().messageFrom(payload);
                vh
                        .setText(message.getBody())
                        .setAvatarUri(sofaMessage.getSender() != null ? sofaMessage.getSender().getAvatar() : null)
                        .setSendState(sofaMessage.getSendState())
                        .draw()
                        .setClickableUsernames(this.onUsernameClickListener);
                break;
            }

            case SofaType.IMAGE: {
                final ImageViewHolder vh = (ImageViewHolder) holder;
                vh
                        .setAvatarUri(sofaMessage.getSender() != null ? sofaMessage.getSender().getAvatar() : null)
                        .setSendState(sofaMessage.getSendState())
                        .setAttachmentFilePath(sofaMessage.getAttachmentFilePath())
                        .setClickableImage(this.onImageClickListener, sofaMessage.getAttachmentFilePath())
                        .draw();
                break;
            }

            case SofaType.FILE: {
                final FileViewHolder vh = (FileViewHolder) holder;
                vh
                        .setAttachmentPath(sofaMessage.getAttachmentFilePath())
                        .setAvatarUri(sofaMessage.getSender() != null ? sofaMessage.getSender().getAvatar() : null)
                        .setOnClickListener(this.onFileClickListener, sofaMessage.getAttachmentFilePath())
                        .draw();
                break;
            }

            case SofaType.PAYMENT: {
                final PaymentViewHolder vh = (PaymentViewHolder) holder;
                final Payment payment = SofaAdapters.get().paymentFrom(payload);
                vh
                        .setPayment(payment)
                        .setAvatarUri(sofaMessage.getSender() != null ? sofaMessage.getSender().getAvatar() : null)
                        .setSendState(sofaMessage.getSendState())
                        .draw();
                break;
            }

            case SofaType.PAYMENT_REQUEST: {
                final PaymentRequestViewHolder vh = (PaymentRequestViewHolder) holder;
                final PaymentRequest request = SofaAdapters.get().txRequestFrom(payload);
                if (this.recipient != null && this.recipient.isGroup()) {
                    // Todo - support group payment requests
                    LogUtil.i(getClass(), "Payment requests to groups currently not supported.");
                    return;
                }

                vh.setPaymentRequest(request)
                  .setAvatarUri(sofaMessage.getSender() != null ? sofaMessage.getSender().getAvatar() : null)
                  .setRemoteUser(this.recipient.getUser())
                  .setSendState(sofaMessage.getSendState())
                  .__setIsFromRemote(isRemote)
                  .setOnApproveListener(this.handleOnPaymentRequestApproved)
                  .setOnRejectListener(this.handleOnPaymentRequestRejected)
                  .draw();
                break;
            }

            case SofaType.TIMESTAMP: {
                final TimestampMessageViewHolder vh = (TimestampMessageViewHolder) holder;
                vh.setTime(sofaMessage.getCreationTime());
            }
        }
    }

    @Override
    public final int getItemCount() {
        return this.sofaMessages.size();
    }

    private final OnItemClickListener<Integer> handleOnPaymentRequestApproved = new OnItemClickListener<Integer>() {
        @Override
        public void onItemClick(final Integer position) {
            if (onPaymentRequestApproveListener == null) return;

            final SofaMessage sofaMessage = sofaMessages.get(position);
            onPaymentRequestApproveListener.onItemClick(sofaMessage);
        }
    };

    private final OnItemClickListener<Integer> handleOnPaymentRequestRejected = new OnItemClickListener<Integer>() {
        @Override
        public void onItemClick(final Integer position) {
            if (onPaymentRequestRejectListener == null) return;

            final SofaMessage sofaMessage = sofaMessages.get(position);
            onPaymentRequestRejectListener.onItemClick(sofaMessage);
        }
    };

    public void clear() {
        this.sofaMessages.clear();
        notifyDataSetChanged();
    }

    private User getCurrentLocalUser() {
        // Yes, this blocks. But realistically, a value should be always ready for returning.
        return BaseApplication
                .get()
                .getUserManager()
                .getCurrentUser()
                .toBlocking()
                .value();
    }
}