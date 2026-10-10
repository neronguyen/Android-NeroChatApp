package io.github.neronguyen.chat.core.model

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    FAILED

    // TODO(future): Expand delivery lifecycle once backend supports receipt events:
    // - DELIVERED: Message reached recipient device (double grey checkmark).
    // - READ: Recipient opened and viewed the message (double blue checkmark / seen receipt)
}
