package ninja.mpnguyen

import com.googlecode.lanterna.gui2.AbstractTextGUIThread
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.TextGUIThreadFactory
import java.io.EOFException
import java.io.IOException
import java.lang.Thread
import java.util.concurrent.CountDownLatch

/**
 * Default implementation of TextGUIThread
 * @author Martin
 */
public class HackedSeparateTextGUIThread(textGUI: TextGUI) : AbstractTextGUIThread(textGUI) {
    /**
     * Returns the current status of the GUI thread
     * @return Current status of the GUI thread
     */
    internal var status: Status? = null
        private set
    private var textGUIThread: Thread? = null
    private var waitLatch: CountDownLatch? = null

    init {
        this.status = Status.CREATED
        this.waitLatch = CountDownLatch(0)
        this.textGUIThread = null
    }

    /**
     * This will start the thread responsible for processing the input queue and update the screen.
     * @throws java.lang.IllegalStateException If the thread is already started
     */
    @Throws(IllegalStateException::class)
    fun start() {
        if (status == Status.STARTED) {
            throw IllegalStateException("TextGUIThread is already started")
        }

        textGUIThread = object : Thread("LanternaGUI") {
            override fun run() {
                mainGUILoop()
            }
        }
        textGUIThread!!.start()
        status = Status.STARTED
        this.waitLatch = CountDownLatch(1)
    }

    /**
     * Calling this will mark the GUI thread to be stopped after all pending events have been processed. It will exit
     * immediately however, call `waitForStop()` to block the current thread until the GUI thread has exited.
     */
    fun stop() {
        if (status != Status.STARTED) {
            return
        }

        status = Status.STOPPING
    }

    /**
     * Awaits the GUI thread to reach stopped state
     * @throws InterruptedException In case this thread was interrupted while waiting for the GUI thread to exit
     */
    @Throws(InterruptedException::class)
    fun waitForStop() {
        waitLatch!!.await()
    }

    override fun getThread(): Thread {
        return textGUIThread!!
    }

    @Throws(IllegalStateException::class)
    override fun invokeLater(runnable: Runnable) {
        if (status != Status.STARTED) {
            throw IllegalStateException("Cannot schedule $runnable for execution on the TextGUIThread because the thread is in $status state")
        }
        super.invokeLater(runnable)
    }

    private fun mainGUILoop() {
        try {
            //Draw initial screen, after this only draw when the GUI is marked as invalid
            try {
                textGUI.updateScreen()
            } catch (e: IOException) {
                exceptionHandler.onIOException(e)
            } catch (e: RuntimeException) {
                exceptionHandler.onRuntimeException(e)
            }

            while (status == Status.STARTED) {
                try {
                    if (!processEventsAndUpdate()) {
                        try {
                            Thread.sleep(1)
                        } catch (ignored: InterruptedException) {
                        }

                    }
                } catch (e: EOFException) {
                    stop()
                    break //Break out quickly from the main loop
                } catch (e: IOException) {
                    if (exceptionHandler.onIOException(e)) {
                        stop()
                        break
                    }
                } catch (e: RuntimeException) {
                    if (exceptionHandler.onRuntimeException(e)) {
                        stop()
                        break
                    }
                }

            }
        } finally {
            status = Status.STOPPED
            waitLatch!!.countDown()
        }
    }


    internal enum class Status {
        CREATED,
        STARTED,
        STOPPING,
        STOPPED
    }

    class Factory : TextGUIThreadFactory {
        override fun createTextGUIThread(textGUI: TextGUI): TextGUIThread {
            return HackedSeparateTextGUIThread(textGUI)
        }
    }
}
