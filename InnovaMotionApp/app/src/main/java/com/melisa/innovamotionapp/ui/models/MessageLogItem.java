package com.melisa.innovamotionapp.ui.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Immutable UI model representing a single message in the log.
 * Used by MessageLogAdapter for RecyclerView display.
 */
public class MessageLogItem {
    
    private final long id;
    private final long timestamp;
    @NonNull
    private final String sensorId;
    @NonNull
    private final String displayName;
    @NonNull
    private final String hexCode;
    @DrawableRes
    private final int postureIconRes;
    private final boolean isFall;

    public MessageLogItem(
            long id,
            long timestamp,
            @NonNull String sensorId,
            @NonNull String displayName,
            @NonNull String hexCode,
            @DrawableRes int postureIconRes,
            boolean isFall
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.sensorId = sensorId;
        this.displayName = displayName;
        this.hexCode = hexCode;
        this.postureIconRes = postureIconRes;
        this.isFall = isFall;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getSensorId() {
        return sensorId;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getHexCode() {
        return hexCode;
    }

    @DrawableRes
    public int getPostureIconRes() {
        return postureIconRes;
    }

    public boolean isFall() {
        return isFall;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageLogItem that = (MessageLogItem) o;
        return id == that.id &&
                timestamp == that.timestamp &&
                postureIconRes == that.postureIconRes &&
                isFall == that.isFall &&
                sensorId.equals(that.sensorId) &&
                displayName.equals(that.displayName) &&
                hexCode.equals(that.hexCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, sensorId, displayName, hexCode, postureIconRes, isFall);
    }

    @NonNull
    @Override
    public String toString() {
        return "MessageLogItem{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", sensorId='" + sensorId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", hexCode='" + hexCode + '\'' +
                ", isFall=" + isFall +
                '}';
    }
}
