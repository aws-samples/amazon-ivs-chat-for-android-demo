package com.amazon.ivs.chatdemo.common

import com.amazon.ivs.chatdemo.repository.models.Avatar
import com.amazon.ivs.chatdemo.repository.models.Sticker

const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
const val REGION_URL = "us-west-2"

val AVATARS get() = listOf(
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/bear.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/bird.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/bird2.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/giraffe.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/hedgehog.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/hippo.png"
).mapIndexed { index, url -> Avatar(index, url) }

val STICKERS get() = listOf(
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-1.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-2.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-3.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-4.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-5.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-6.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-7.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-8.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-9.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-10.png",
    "https://d39ii5l128t5ul.cloudfront.net/assets/chat/v1/sticker-11.png"
).mapIndexed { index, resource -> Sticker(index, resource)  }

val MEASURE_REPEAT_COUNT = (0..3).count()

const val IVS_PLAYBACK_URL_BASE = "live-video.net"
const val PREFERENCES_NAME = "app_preferences"
const val MAX_QUALITY = "1080p"
const val MEASURE_REPEAT_DELAY = 200L
const val SCROLL_DELAY = 600L
const val KEYBOARD_DELAY = 300L
const val MESSAGE_HISTORY = 20
const val MESSAGE_TIMEOUT = 20 * 1000L
const val MESSAGE_EXPIRATION_RETRY_TIME = 500L
const val ALPHA_VISIBLE = 1f
const val ALPHA_GONE = 0f
const val ITEM_SCALE_SMALL = 0.5f
const val ITEM_SCALE_BIG = 1.3f
const val ANIMATION_DURATION_NORMAL = 250L
const val ANIMATION_DURATION_LONG = 500L
const val ANIMATION_START_OFFSET = 200L
const val TOKEN_REFRESH_DELAY = 1000 * 60 * 55L
