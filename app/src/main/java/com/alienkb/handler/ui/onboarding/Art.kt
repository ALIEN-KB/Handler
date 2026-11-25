package com.alienkb.handler.ui.onboarding

/** ASCII art for the intro slides. Keep every line narrow so it fits a phone. */
object Art {

    val LOGO = """
 _  _              _ _
| || |__ _ _ _  __| | |___ _ _
| __ / _` | ' \/ _` | / -_) '_|
|_||_\__,_|_||_\__,_|_\___|_|

   .------------------------.
   |  accessibility : [ON]  |
   |  bank apps     : [OK]  |
   |  autopilot     : [ON]  |
   '------------------------'
""".trimIndent()

    val PROBLEM = """
 .--------------------.
 |  [#]   MY  BANK    |
 |--------------------|
 |                    |
 |        /!\         |
 |   ACCESS  DENIED   |
 |                    |
 |  An accessibility  |
 |  service is ON.    |
 |  Turn it off to    |
 |  continue.         |
 |                    |
 |   [  SETTINGS  ]   |
 '--------------------'
     you:  ( -_- )
""".trimIndent()

    val FLOW = """
   [ tap  (lock) Bank ]
            |
            v
   +------------------+
   |  a11y  ->  OFF   |
   +------------------+
            |
            v
   [  bank opens: OK  ]
            |
   leave  /  lock phone
            |
            v
   +------------------+
   |  a11y  ->  ON    |
   +------------------+
""".trimIndent()

    val WATCHDOG = """
        __
   (___()'`;    WOOF!
   /,    /`     a11y was OFF
   \\"--\\      -> back ON

 [x] instantly on change
 [x] on boot          ok
 [x] on unlock        ok
 [x] every 15 min     ok
 [x] stuck service    kick
""".trimIndent()

    val SETUP = """
 +------------------------------+
 | ~ > adb shell pm grant \     |
 |     com.alienkb.handler \    |
 |     android.permission.\     |
 |     WRITE_SECURE_SETTINGS    |
 |                              |
 | > permission granted         |
 | > one time. survives reboot. |
 | > no internet. no tracking._ |
 +------------------------------+
""".trimIndent()
}
