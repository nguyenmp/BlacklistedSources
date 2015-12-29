package ninja.mpnguyen

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.json.JSONObject


fun main(args : Array<String>) {
    val client = OkHttpClient()
    val request = Request
            .Builder()
            .get()
            .url("http://reddit.com/.json")
            .build()
    val response = client.newCall(request).execute()
    val html = response.body().string()

    Thread.sleep(2000)

    val json = JSONObject(html)
    val array = json.getJSONObject("data").getJSONArray("children")
    for (index in 0..(array.length() - 1)) {
        val post : JSONObject = array.getJSONObject(index).getJSONObject("data")
        validatePost(post)
    }
}

fun getThread(post : JSONObject) {
    val threadID = post.getString("id")
    val client = OkHttpClient()
    val request = Request.Builder().get().url("http://redit.com/$threadID/.json").build()
    val jsonString = client.newCall(request).execute().body().string()
}

fun validatePost(post: JSONObject) {
    val author = post.getString("author")
    val client = OkHttpClient()
    val request = Request
            .Builder()
            .get()
            .url("http://reddit.com/u/$author/comments/.json")
            .build()
    val jsonString = client.newCall(request).execute().body().string()
    Thread.sleep(2000)
    val comments = JSONObject(jsonString).getJSONObject("data").getJSONArray("children")
    for (index in 0..(comments.length() - 1)) {
        val comment = comments.getJSONObject(index)
        val data = comment.getJSONObject("data").getString("body")
        println(data)
    }
}
