package ninja.mpnguyen

import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.util.*

fun main(args: Array<String>) {
    val oauth_url = get_oauth_url()
    val gui = init_lanterna()
    login(gui)
}

fun login(gui: MultiWindowTextGUI) {
    val window = BasicWindow("Foo")
    window.setHints(setOf(Window.Hint.CENTERED))
    val panel = Panel()
    panel.addComponent(TextBox("Foooo").withBorder(Borders.singleLine("Bar")))
    window.component = panel
    gui.addWindow(window)
    gui.updateScreen()
}

fun get_oauth_url() : String {
    val TEST_DATA = TestData()
    val client_id = TEST_DATA.installed_id
    val redirect_url = TEST_DATA.callback
    val duration = CodeGenerator.Duration.PERMANENT
    val scope = CodeGenerator.Scope.values()
    val state = UUID.randomUUID().toString()
    val code_url_generator = CodeGenerator(
            client_id,
            redirect_url,
            duration,
            scope,
            state
    )
    return code_url_generator.call()
}

fun init_lanterna() : MultiWindowTextGUI {
    val terminal = DefaultTerminalFactory().createTerminal()
    val screen = TerminalScreen(terminal)
    screen.startScreen()
    val thread_factory = SeparateTextGUIThread.Factory()
    val gui = MultiWindowTextGUI(thread_factory, screen)
    (gui.guiThread as SeparateTextGUIThread).start()
    return gui
}