package ninja.mpnguyen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.squareup.okhttp.*
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.Callable
import java.util.logging.Logger

fun main(args: Array<String>) {
    val app_id = System.getenv("app_id")
    val app_secret = System.getenv("app_secret")
    val username = System.getenv("username")
    val password = System.getenv("password")
    val auth = Authenticate(app_id, app_secret, username, password)
    val reddit_auth = auth.call()
    Logger.getGlobal().info(reddit_auth.toString())
    StoriesGet(reddit_auth).call().data.children.forEach {
        if (it.data.saved) println(it.data.title)
    }
    OtherAuthenticate(
            "UexYNFgc2lbvxA",
            "http://scooterlabs.com/echo",
            OtherAuthenticate.Duration.TEMPORARY,
            arrayOf(OtherAuthenticate.Scope.READ),
            UUID.randomUUID().toString()
    ).call()
}

abstract class RedditRequest<T> : Callable<T> {
    private val auth_root = "https://oauth.reddit.com"
    private val non_auth_root = "https://www.reddit.com"

    override fun call() : T {
        val client = OkHttpClient()
        val interceptors = client.interceptors()
        interceptors.add(user_agent_interceptor)
        interceptors.add(oauth_interceptor)

        val auth = auth()
        val root = if (auth == null) non_auth_root else auth_root

        val url = "$root/${endpoint()}.json?${query()}"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return parse(response)
    }

    abstract fun endpoint() : String
    open fun query() : Array<Pair<String, String>>? = null
    open fun body() : RequestBody? = null
    open fun auth() : RedditAuth? = null
    abstract fun parse(response : Response) : T

    private val user_agent_interceptor = Interceptor {
        val original_request = it.request()
        val new_request = original_request
                .newBuilder()
                .header("User-Agent", "Jaws")
                .build()
        it.proceed(new_request)
    }

    private val oauth_interceptor = Interceptor {
        val auth = auth()
        if (auth == null) {
            it.proceed(it.request())
        } else {
            val original_request = it.request()
            val new_request = original_request
                    .newBuilder()
                    .header("Authorization", "${auth.token_type} ${auth.access_token}")
                    .build()
            it.proceed(new_request)
        }
    }
}

class StoriesGet(val auth : RedditAuth? = null) : RedditRequest<ListingWrapper<Story>>() {
    override fun endpoint(): String = "new"

    override fun parse(response: Response): ListingWrapper<Story >{
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val type = object : TypeReference<ListingWrapper<Story>>() {}
        val json = response.body().string()
        return mapper.readValue(json, type)
    }

    override fun auth() = auth
}

data class RedditAuth(
        val access_token : String,
        val expires_in : Int,
        val scope : String,
        val token_type : String
)

class Authenticate(
        val client_id : String,
        val client_secret : String,
        val username : String,
        val password : String
) : Callable<RedditAuth> {
    override fun call() : RedditAuth {
        val url = "https://www.reddit.com/api/v1/access_token"
        val body = FormEncodingBuilder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .build()
        val client = OkHttpClient()
        val credentials = Credentials.basic(client_id, client_secret)
        val request = Request
                .Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", credentials)
                .build()
        val response = client.newCall(request).execute()
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val type = object : TypeReference<RedditAuth>() {}
        val json = response.body().string()
        return mapper.readValue<RedditAuth>(json, type)
    }
}

class OtherAuthenticate(
        val client_id : String,
        val redirect_url : String,
        val duration : Duration,
        val scope : Array<Scope>,
        val state : String? = null
) : Callable<RedditAuth> {
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

    override fun call(): RedditAuth {
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
        Logger.getGlobal().info(url)
        return TODO()
    }
}