package net.morphedit.partyrtp.common.util;

public final class Constants {
    private Constants() {}

    // Default party settings
    public static final int DEFAULT_MAX_SIZE = 6;
    public static final boolean DEFAULT_REQUIRE_NEAR = true;
    public static final int DEFAULT_NEAR_RADIUS = 8;
    public static final boolean DEFAULT_MEMBERS_MUST_BE_ONLINE = true;

    // Default go settings
    public static final int DEFAULT_COOLDOWN_SECONDS = 300;
    public static final int DEFAULT_TIMEOUT_SECONDS = 15;
    public static final int DEFAULT_PULL_DELAY_TICKS = 2;
    public static final double DEFAULT_MIN_DISTANCE = 64.0;

    // Default world settings
    public static final boolean DEFAULT_WORLDS_ENABLED = false;
    public static final String DEFAULT_WORLD_MODE = "BLACKLIST";
    public static final boolean DEFAULT_REQUIRE_SAME_WORLD = true;

    // Default limits settings
    public static final boolean DEFAULT_LIMITS_ENABLED = false;

    // Plugin Messaging
    public static final String CHANNEL_MAIN = "partyrtp:main";

    // Message Types
    public static final String MSG_RTP_REQUEST = "RTP_REQUEST";
    public static final String MSG_RTP_SUCCESS = "RTP_SUCCESS";
    public static final String MSG_RTP_FAILED = "RTP_FAILED";
    public static final String MSG_PULL_MEMBERS = "PULL_MEMBERS";

    // Safety thresholds
    public static final double LEADER_MOVEMENT_THRESHOLD = 100.0;
    public static final long AUTOSAVE_INTERVAL_TICKS = 20L * 120;
}