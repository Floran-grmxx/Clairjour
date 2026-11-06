package com.clairjour.app.shield

/** Whether the "keep the shield on" settings are all done, so their card can fold away. */
object ShieldKeepAlive {

    /**
     * Battery must be unrestricted. Autostart only blocks when the phone has such a setting
     * and we could read it as refused: an unknown value (brands we cannot query) does not block.
     */
    fun isDone(batteryUnrestricted: Boolean, autostartAllowed: Boolean?, hasAutostartSettings: Boolean): Boolean =
        batteryUnrestricted && !(hasAutostartSettings && autostartAllowed == false)
}
