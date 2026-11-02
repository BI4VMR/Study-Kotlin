package net.bi4vmr.tool.kotlin.gui.interaction.terminal

/**
 * 知名终端程序。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class Terminals : Terminal {

    /**
     * Windows 终端。
     */
    WINDOWS_TERMINAL {
        override val executableName: String = "wt"
    },

    /**
     * Linux Konsole 终端。
     */
    KONSOLE {
        override val executableName: String = "konsole"
    };
}
