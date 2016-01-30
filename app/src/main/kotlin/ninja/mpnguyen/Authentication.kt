package ninja.mpnguyen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.squareup.okhttp.Credentials
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import java.net.URLEncoder
import java.util.concurrent.Callable


data class RedditAuth(
        val access_token : String,
        val expires_in : Int,
        val scope : String,
        val token_type : String,

        /**
         * Refresh tokens are only available when you request permanent oauth
         * tokens.  If you requested temporary access, this value will be null.
         * Otherwise, this value will be the refresh token.
         */
        val refresh_token : String? = null
)

interface Authenticator : Callable<RedditAuth>

abstract class BaseOauthImpl(
        val client_id : String,
        val client_secret : String
) : Authenticator {
    abstract fun body() : Array<Pair<String, String>>
    abstract fun base() : String
    abstract fun endpoint() : String

    override fun call(): RedditAuth {
        val url = "${base()}/${endpoint()}"
        val body = body().fold(FormEncodingBuilder(), fun (result, current) : FormEncodingBuilder {
            return result.add(current.first, current.second)
        }).build()
        val credentials = Credentials.basic(client_id, client_secret)
        val client = OkHttpClient()
        val request = Request.Builder().url(url).post(body).header("Authorization", credentials).build()
        val response = client.newCall(request).execute()
        val json = response.body().string()
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val type = object : TypeReference<RedditAuth>() {}
        return mapper.readValue<RedditAuth>(json, type)
    }
}

class ScriptAuthenticator(
        client_id : String,
        client_secret : String,
        val username : String,
        val password : String
) : BaseOauthImpl(client_id, client_secret) {
    override fun body(): Array<Pair<String, String>> = arrayOf(
            Pair("grant_type", "password"),
            Pair("username", username),
            Pair("password", password)
    )

    override fun base(): String = "https://www.reddit.com"

    override fun endpoint(): String = "api/v1/access_token"
}

class CodeGenerator(
        val client_id : String,
        val redirect_url : String,
        val duration : Duration,
        val scope : Array<Scope>,
        val state : String? = null
) : Callable<String> {

    enum class Duration(val representation : String) {
        TEMPORARY("temporary"),
        PERMANENT("permanent"),
    }

    enum class Scope(val representation : String) {
        IDENTITY("identity"),
        EDIT("edit"),
        FLAIR("flair"),
        HISTORY("history"),
        MOD_CONFIG("modconfig"),
        MOD_FLAIR("modflair"),
        MOD_LOG("modlog"),
        MOD_POSTS("modposts"),
        MOD_WIKI("modwiki"),
        MY_SUBREDDITS("mysubreddits"),
        PRIVATE_MESSAGES("privatemessages"),
        READ("read"),
        REPORT("report"),
        SAVE("save"),
        SUBMIT("submit"),
        SUBSCRIBE("subscribe"),
        VOTE("vote"),
        WIKI_EDIT("wikiedit"),
        WIKI_READ("wikiread"),
    }

    override fun call() : String {
        val base = "https://www.reddit.com"
        val endpoint = "api/v1/authorize"
        val query = arrayOf(
                Pair("client_id", client_id),
                Pair("response_type", "code"),
                Pair("state", state),
                Pair("redirect_uri", redirect_url),
                Pair("duration", duration.representation),
                Pair("scope", scope.map{it.representation}.joinToString(","))
        )
        val query_string = query.map {
            pair ->
            val key = URLEncoder.encode(pair.first, "UTF-8")
            val value = URLEncoder.encode(pair.second, "UTF-8")
            "$key=$value"
        }.joinToString("&")
        val url = "$base/$endpoint?$query_string"
        return url
    }
}

class InstalledAppAuthenticator(
        val code : String,
        val redirect_url : String,
        client_id : String
) : BaseOauthImpl(client_id, "") {
    override fun body(): Array<Pair<String, String>> = arrayOf(
            Pair("grant_type", "authorization_code"),
            Pair("code", code),
            Pair("redirect_uri", redirect_url)
    )

    override fun base(): String = "https://www.reddit.com"

    override fun endpoint(): String = "api/v1/access_token"
}

class WebAppAuthenticator(
        val code : String,
        val redirect_url : String,
        client_id : String,
        client_secret : String
) : BaseOauthImpl(client_id, client_secret) {
    override fun body(): Array<Pair<String, String>> = arrayOf(
            Pair("grant_type", "authorization_code"),
            Pair("code", code),
            Pair("redirect_uri", redirect_url)
    )

    override fun base(): String = "https://www.reddit.com"

    override fun endpoint(): String = "api/v1/access_token"
}

class RefreshAuthenticator(
        client_id : String,
        client_secret : String,
        val refresh_token : String
) : BaseOauthImpl(client_id, client_secret) {
    override fun body(): Array<Pair<String, String>> = arrayOf(
            Pair("grant_type", "refresh_token"),
            Pair("refresh_token", refresh_token)
    )

    override fun base(): String = "https://www.reddit.com"

    override fun endpoint(): String = "api/v1/access_token"
}

class AppOnlyOauth(
        client_id : String,
        client_secret : String,
        val device_id : String,
        val grant_type : Grant
) : BaseOauthImpl(client_id, client_secret) {
    override fun body(): Array<Pair<String, String>> = arrayOf(
            Pair("grant_type", grant_type.representation),
            Pair("device_id", device_id)
    )

    override fun base(): String = "https://www.reddit.com"

    override fun endpoint(): String = "api/v1/access_token"

    enum class Grant(val representation: String) {
        INSTALLED("https://oauth.reddit.com/grants/installed_client"),
        CREDENTIALS("client_credentials")
    }
}