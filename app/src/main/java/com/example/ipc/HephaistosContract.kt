package com.example.ipc

object HephaistosContract {
    const val PROTOCOL_VERSION = "1.0"
    const val PACKAGE_PREFIX = "com.hephaistos."
    const val LAUNCHER_PACKAGE = "com.hephaistos.launcher"

    // Discovery & Meta-Data
    const val ACTION_MODULE = "com.hephaistos.action.MODULE"
    const val META_MANIFEST = "com.hephaistos.module.manifest"

    // Commands Launcher/Module -> Target Module
    const val ACTION_EXECUTE = "com.hephaistos.action.EXECUTE"
    const val ACTION_CANCEL_TASK = "com.hephaistos.action.CANCEL_TASK"
    const val ACTION_GET_MANUAL = "com.hephaistos.action.GET_MANUAL"

    // Events Module -> HEPHAISTOS
    const val ACTION_LOG = "com.hephaistos.action.LOG"
    const val ACTION_PROGRESS = "com.hephaistos.action.PROGRESS"
    const val ACTION_SHOW_POPUP = "com.hephaistos.action.SHOW_POPUP"
    const val ACTION_COMMAND_RESULT = "com.hephaistos.action.COMMAND_RESULT"

    // Extras
    const val EXTRA_COMMAND_ID = "hephaistos.extra.COMMAND_ID"
    const val EXTRA_PARAMS_JSON = "hephaistos.extra.PARAMS_JSON"
    const val EXTRA_SOURCE_PACKAGE = "hephaistos.extra.SOURCE_PACKAGE"
    const val EXTRA_TASK_ID = "hephaistos.extra.TASK_ID"
    const val EXTRA_TITLE = "hephaistos.extra.TITLE"
    const val EXTRA_MESSAGE = "hephaistos.extra.MESSAGE"
    const val EXTRA_LEVEL = "hephaistos.extra.LEVEL"
    const val EXTRA_PROGRESS = "hephaistos.extra.PROGRESS"
    const val EXTRA_COMPLETED = "hephaistos.extra.COMPLETED"
    const val EXTRA_ERROR = "hephaistos.extra.ERROR"
    const val EXTRA_CAN_CANCEL = "hephaistos.extra.CAN_CANCEL"
    const val EXTRA_CONFIRM_LABEL = "hephaistos.extra.CONFIRM_LABEL"
    const val EXTRA_CANCEL_LABEL = "hephaistos.extra.CANCEL_LABEL"
    const val EXTRA_RESULT_JSON = "hephaistos.extra.RESULT_JSON"
    const val EXTRA_OPEN_MODULE_PACKAGE = "hephaistos.extra.OPEN_MODULE_PACKAGE"

    const val GITHUB_DOCS_URL = "https://rakotonirinatdaniel.github.io/hephaistos/"
    const val GITHUB_REPO_URL = "https://github.com/RakotonirinaTDaniel/hephaistos"
}
