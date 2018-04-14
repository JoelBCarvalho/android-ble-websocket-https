package com.github.nkzawa.socketio.androidchat;

/**
 * Created by x240 on 01-03-2018.
 */

public enum GattServices {
    BATTERY_SERVICE(    "0000180f-0000-1000-8000-00805f9b34fb"),
    USER_DATA(          "0000181c-0000-1000-8000-00805f9b34fb"),
    USER_INDEX(         "00002a9a-0000-1000-8000-00805f9b34fb"),
    LANGUAGE(           "00002aa2-0000-1000-8000-00805f9b34fb"),
    FIRST_NAME(         "00002a8a-0000-1000-8000-00805f9b34fb"),
    LAST_NAME(          "00002a90-0000-1000-8000-00805f9b34fb"),
    DATE_TIME(          "00002a08-0000-1000-8000-00805f9b34fb"),
    CURRENT_TIME(       "00001805-0000-1000-8000-00805f9b34fb"),
    ALERT_NOTIFICATION( "00001811-0000-1000-8000-00805f9b34fb"),
    GENERIC_ATTRIBUTE(  "00001801-0000-1000-8000-00805f9b34fb"),
LOCATION_AND_NAVIGATION("00001819-0000-1000-8000-00805f9b34fb"),
    LOCATION_NAME(      "00002ab5-0000-1000-8000-00805f9b34fb"),
    NAVIGATION(         "00002a68-0000-1000-8000-00805f9b34fb");

    private final String service;

    /**
     * @param service
     */
    GattServices(final String service) {
        this.service = service;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return service;
    }

    public static GattServices byValue(String val){
        for(GattServices en : values()){
            if(en.service.equalsIgnoreCase(val)){
                return en;
            }
        }
        return null;
    }

}
