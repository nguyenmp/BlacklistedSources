package ninja.mpnguyen

import org.junit.Test
import java.util.*
import java.util.logging.Logger

class MainKtTest {

    @Test
    fun testScript() {
        val TEST_DATA = TestData()
        val app_id = TEST_DATA.script_id
        val app_secret = TEST_DATA.script_secret
        val username = TEST_DATA.username
        val password = TEST_DATA.password

        val authenticator = ScriptAuthenticator(app_id, app_secret, username, password)
        val authentication = authenticator.call()
        Logger.getGlobal().info(authentication.toString())
    }

    @Test
    fun testWebAppPhase1() {
        val TEST_DATA = TestData()
        val client_id = TEST_DATA.webapp_id
        val callback = TEST_DATA.callback
        val duration = CodeGenerator.Duration.PERMANENT
        val scopes = arrayOf(CodeGenerator.Scope.READ)
        val state = UUID.randomUUID().toString()
        val url_generator = CodeGenerator(
                client_id,
                callback,
                duration,
                scopes,
                state
        )
        val url = url_generator.call()
        Logger.getGlobal().info(url)
    }

    @Test
    fun testWebAppPhase2() {
        val TEST_DATA = TestData()
        val code = "KXguQ4alrngpYt5Qdx4arq78e7E"
        val callback = TEST_DATA.callback
        val client_id = TEST_DATA.webapp_id
        val client_secret = TEST_DATA.webapp_secret
        val authenticator = WebAppAuthenticator(code, callback, client_id, client_secret)
        val authentication = authenticator.call()
        Logger.getGlobal().info(authentication.toString())
    }

    @Test
    fun testInstalledAppPhase1() {
        val TEST_DATA = TestData()
        val client_id = TEST_DATA.installed_id
        val callback = TEST_DATA.callback
        val duration = CodeGenerator.Duration.PERMANENT
        val scope = arrayOf(CodeGenerator.Scope.READ)
        val state = UUID.randomUUID().toString()
        val code_generator = CodeGenerator(client_id, callback, duration, scope, state)
        val code_url = code_generator.call()
        Logger.getGlobal().info(code_url)
    }

    @Test
    fun testInstalledAppPhase2() {
        val TEST_DATA = TestData()
        val client_id = TEST_DATA.installed_id
        val redirect_url = TEST_DATA.callback
        val code = "hx3RQLLafR67OgvvWAgjhtA--Ec"
        val authenticator = InstalledAppAuthenticator(code, redirect_url, client_id)
        val authentication = authenticator.call()
        Logger.getGlobal().info(authentication.toString())
    }

    @Test
    fun testRefreshToken() {
        val TEST_DATA = TestData()
        val client_id = TEST_DATA.installed_id
        val client_secret = ""
        val refresh_token = "49776830-XyTaKShF0ivg1ymoUL_8awBQSdA"
        val authenticator = RefreshAuthenticator(client_id, client_secret, refresh_token)
        val authentication = authenticator.call()
        Logger.getGlobal().info(authentication.toString())
    }

    @Test
    fun testAppOnly() {
        val TEST_DATA = TestData()
        val grant_type = AppOnlyOauth.Grant.INSTALLED
        val client_id = TEST_DATA.webapp_id
        val client_secret = TEST_DATA.webapp_secret
        val device_id = UUID.randomUUID().toString()
        val authenticator = AppOnlyOauth(client_id, client_secret, device_id, grant_type)
        val authentication = authenticator.call()
        Logger.getGlobal().info(authentication.toString())
        val listing = StoriesGet(authentication).call()
        Logger.getGlobal().info(listing.toString())
    }
}
