package ninja.mpnguyen

data class ListingWrapper<Thing>(
        val kind : String,
        val data : ListingData<Thing>
)

data class ListingData<Thing>(
        val modhash : String?,
        val children : Array<ThingWrapper<Thing>>,
        val after : String?,
        val before : String?
)

data class ThingWrapper<Thing>(
        val kind : String,
        val data : Thing
)

//@JsonIgnoreProperties(ignoreUnknown = true)
data class Story(
        val domain : String,
        val banned_by : Any?,
        val media_embed : Any?,
        val subreddit : String,
        val selftext_html : String?,
        val selftext : String,
        val likes : Boolean?,
        val suggested_sort : Any?,
        val user_reports : Array<Any>,
        val secure_media : Any?,
        val link_flair_text : Any?,
        val id : String,
        val from_kind : Any?,
        val gilded : Int, // What the fuck?
        val archived : Boolean,
        val clicked : Boolean,
        val report_reasons : Any?,
        val author : String,
        val media : Any?,
        val approved_by : Any?,
        val over_18 : Boolean,
        val hidden : Boolean,
        val preview : Any?,
        val num_comments : Int,
        val thumbnail : String,
        val subreddit_id : String,
        val hide_score : Boolean,
        val score : Int,
        val edited : Any, // This is any because it can be either a Long or a Boolean
        val author_flair_text : String?,
        val link_flair_css_class : String?,
        val author_flair_css_class : String?,
        val downs : Int,
        val secure_media_embed : Any,
        val saved : Boolean,
        val removal_reason : Any?,
        val post_hint : String?,
        val stickied : Boolean,
        val from : Any?,
        val is_self : Boolean,
        val from_id : Any?,
        val permalink : String,
        val locked : Boolean,
        val name : String,
        val created : Long,
        val created_utc : Long,
        val distinguished : Any?,
        val num_reports : Int,
        val mod_reports : Array<Any>,
        val visited : Boolean,
        val ups : Int,
        val title : String,
        val url : String,
        val quarantine : Boolean
)
