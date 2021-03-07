package com.example.gps_sportmap

class C {
    companion object {
        private const val PREFIX = "com.example.gps_sportmap."

        const val NOTIFICATION_CHANNEL = "default_channel"
        const val NOTIFICATION_ACTION_WP = PREFIX + "wp"
        const val NOTIFICATION_ACTION_CP = PREFIX + "cp"

        const val LOCATION_UPDATE_ACTION = PREFIX + "location_update"

        const val LOCATION_UPDATE_STOP = PREFIX + "location_stop"

        const val LOCATION_UPDATE_ACTION_LAT = PREFIX + "location_update.lat"
        const val LOCATION_UPDATE_ACTION_LON = PREFIX + "location_update.lon"

        const val LOCATION_UPDATE_ACTION_OVERALL_DIRECT = PREFIX + "location_update.overall_direct"
        const val LOCATION_UPDATE_ACTION_OVERALL_TOTAL = PREFIX + "location_update.overall_total"
        const val LOCATION_UPDATE_ACTION_OVERALL_TIME = PREFIX + "location_update.overall_time"

        const val LOCATION_UPDATE_ACTION_CP_DIRECT = PREFIX + "location_update.cp_direct"
        const val LOCATION_UPDATE_ACTION_CP_TOTAL = PREFIX + "location_update.cp_total"
        const val LOCATION_UPDATE_ACTION_CP_TIME = PREFIX + "location_update.cp_time"

        const val LOCATION_UPDATE_ACTION_WP_DIRECT = PREFIX + "location_update.wp_direct"
        const val LOCATION_UPDATE_ACTION_WP_TOTAL = PREFIX + "location_update.wp_total"
        const val LOCATION_UPDATE_ACTION_WP_TIME = PREFIX + "location_update.wp_time"

        const val NOTIFICATION_ID = 4321
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34

        const val REST_BASE_URL = "https://sportmap.akaver.com/api/v1.0/"

        var REST_EMAIL = "Email" //karmo@marvex.ee
        var REST_PASSWORD = "Password" //DucksG0Quack!
        var REST_TOKEN = "Token"
        var USER_ID: Int = 0
        var cpPoint = false
        var wpPoint = false
        var sessionName = ""
        var sessionDescription = ""


        const val REST_LOCATIONID_LOC = "00000000-0000-0000-0000-000000000001"
        const val REST_LOCATIONID_WP = "00000000-0000-0000-0000-000000000002"
        const val REST_LOCATIONID_CP = "00000000-0000-0000-0000-000000000003"


        const val IN_USE_COLOR = "#990000"
        const val NOT_IN_USE_COLOR = "#00FFE1"
        const val NOT_AVAILABLE_COLOR = "#A9A9A9"
    }

}