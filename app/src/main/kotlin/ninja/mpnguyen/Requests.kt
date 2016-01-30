package ninja.mpnguyen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.squareup.okhttp.*
import java.util.concurrent.Callable

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
        val requestBuilder = Request.Builder().url(url)
        val body = body()
        if (body != null) requestBuilder.post(body)
        val request = requestBuilder.build()
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

class ThreadGet(val story_id : String, val comment_id : String = "", val auth : RedditAuth? = null) : RedditRequest<ListingWrapper<Story>>() {
    override fun endpoint(): String = "comments/$story_id/_/$comment_id.json?limit=3"

    override fun parse(response: Response): ListingWrapper<Story> {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val json = response.body().string()
        val json_node = mapper.readTree(json)
        val story_type = object : TypeReference<ListingWrapper<Story>>() {}
        val story = mapper.convertValue<ListingWrapper<Story>>(json_node.get(0), story_type)
        return TODO()
    }

    override fun auth() = auth
}

