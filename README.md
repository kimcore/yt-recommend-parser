# YouTube recommend parser
YouTube recommend parser is a ported version of [youtube-related](https://github.com/kijk2869/youtube-related) by [Mary](https://github.com/kijk2869).

YouTube recommend parser helps you fetch the YouTube recommmends for certain YouTube video.

## Usage
**NOTE: retrieveRecommends() should be used in suspend function.**
**Returns:** List<Recommendation>
```kotlin
val api = RecommendAPI(null)
api.retrieveRecommends("YOUTUBE_VIDEO_URL") List<Recommendation>
```

You can put assigned IPv6 block instead of putting null in order to prevent 429 responses while fetching YouTube.
