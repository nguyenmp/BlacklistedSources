package ninja.mpnguyen

import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.collections.emptySet
import kotlin.collections.joinToString
import kotlin.collections.plus
import kotlin.text.removePrefix

fun main(args: Array<String>) {
    searchForMissingComments(args)
}

private fun searchForMissingComments(subreddits : Array<String>) {
    val submissions : JSONArray = getSubmissions(subreddits)

    val num_processes = Runtime.getRuntime().availableProcessors()
    val exec_service = Executors.newFixedThreadPool(num_processes)
    for (i in 0 .. (submissions.length() - 1)) {
        val submission = submissions.getJSONObject(i)
        exec_service.submit(SubmissionVerifier(submission))
    }
    exec_service.awaitTermination(30L, TimeUnit.MINUTES)
}

private fun getSubmissions(subreddits : Array<String>) : JSONArray {
    val url = "https://www.reddit.com/r/${subreddits.joinToString("+")}/new/.json?limit=100"
    val request = Request.Builder().url(url).build()
    val response = RequestManager.handleRequest(request)
    val response_string = response.body().string()
    return JSONObject(response_string)
            .getJSONObject("data")
            .getJSONArray("children")
}

class SubmissionVerifier(val submission : JSONObject) : Runnable {
    override fun run() {
        try {
            checkSubmission(submission)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkSubmission(submission : JSONObject) {
        val submission_data = submission.getJSONObject("data")
        val username = submission_data.getString("author")
        val user_comments = getUserComments(username)
        val submission_comments = getSubmissionComments(submission)
        crossReferenceComments(submission, user_comments, submission_comments)
    }

    private fun crossReferenceComments(
            submission: JSONObject,
            user_comments: JSONArray,
            submission_comments: Set<String>
    ) {
        val submission_name = submission.getJSONObject("data").getString("name")
        Logger.getGlobal().info("Cross referencing comments for $submission_name")

        for (i in 0 .. (user_comments.length() - 1)) {
            val user_comment = user_comments.getJSONObject(i)
            val user_comment_data = user_comment.getJSONObject("data")
            val link_id = user_comment_data.getString("link_id")

            val comment_id = user_comment_data.getString("id")
            if (submission_name != link_id) {
                // Do nothing
            } else if (!submission_comments.contains(comment_id)) {
                Logger.getGlobal().severe("Failed to find $comment_id for $link_id")
                Logger.getGlobal().info("https://reddit.com/comments/${link_id.removePrefix("t3_")}/_/$comment_id/")
            } else {
                Logger.getGlobal().warning("Found $comment_id in $link_id")
            }

        }
    }

    private fun getUserComments(username : String) : JSONArray {
        return UserCommentCache.get(username)
    }

    private fun getSubmissionComments(submission : JSONObject) : Set<String> {
        val submission_id = submission.getJSONObject("data").getString("id")
        val url = "https://www.reddit.com/comments/$submission_id/_/.json"
        val request = Request.Builder().url(url).build()
        val response = RequestManager.handleRequest(request)
        val response_string = response.body().string()
        val submission_comments = JSONArray(response_string).getJSONObject(1)
        return aggregateSubmissionComments(submission_comments)
    }

    private fun aggregateSubmissionComments(listing : JSONObject) : Set<String> {
        var comment_ids = emptySet<String>()

        val comments = listing.getJSONObject("data")
                .getJSONArray("children")

        for (i in 0 .. (comments.length() - 1)) {
            val comment = comments.getJSONObject(i)
            val comment_data = comment.getJSONObject("data")
            comment_ids += comment_data.getString("id")

            val replies : JSONObject? = comment_data.optJSONObject("replies")
            if (replies != null) {
                comment_ids += aggregateSubmissionComments(replies)
            }
        }

        return comment_ids
    }
}

object UserCommentCache {
    private val cache : MutableMap<String, SoftReference<JSONArray>> = HashMap()

    @Synchronized
    fun get(username : String) : JSONArray {
        // Check cache first
        val cached_comments : JSONArray? = cache[username]?.get()
        if (cached_comments != null) {
            Logger.getGlobal().info("Cache hit for $username")
            return cached_comments
        }

        val fetched_comments = fetch(username)
        cache.put(username, SoftReference(fetched_comments))
        return fetched_comments
    }

    private fun fetch(username : String) : JSONArray {
        val url = "https://www.reddit.com/user/$username/comments/.json?sort=new&limit=100"
        val request = Request.Builder().url(url).build()
        val response = RequestManager.handleRequest(request)
        val response_string = response.body().string()
        return JSONObject(response_string)
                .getJSONObject("data")
                .getJSONArray("children")
    }
}

object RequestManager {
    private var lastRunTime: Long = 0L
    private val client : OkHttpClient = OkHttpClient()

    init {
        client.interceptors().add(UserAgentInterceptor())
        client.interceptors().add(LogInterceptor())
    }

    @Synchronized
    fun handleRequest(request : Request) : Response {
        // Wait for 2 seconds since last request
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRunTime
        val timeout = 2000
        val time_to_wait = timeout - elapsedTime
        if (time_to_wait > 0) {
            Logger.getGlobal()
                    .info("Waiting for $time_to_wait milliseconds")
            Thread.sleep(time_to_wait)
        }

        // Now do the thing
        val response =  client.newCall(request).execute()
        lastRunTime = System.currentTimeMillis()
        return response
    }

    private class UserAgentInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val new_request = request
                    .newBuilder()
                    .header("User-Agent", "source finder by /u/markerz")
                    .build()
            return chain.proceed(new_request)
        }
    }

    private class LogInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().urlString()
            Logger.getGlobal().info("Fetching data from $url")
            return chain.proceed(chain.request())
        }
    }
}